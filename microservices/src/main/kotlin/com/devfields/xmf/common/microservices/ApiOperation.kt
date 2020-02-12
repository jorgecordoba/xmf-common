package com.devfields.xmf.common.microservices

import com.devfields.xmf.common.configuration.configuration.ConfigurationStore
import com.devfields.xmf.common.configuration.configuration.ObservableConfigurationStore
import com.devfields.xmf.common.logging.XmfLoggerFactory
import com.devfields.xmf.common.serialization.JacksonHelper
import com.devfields.xmf.common.types.Version
import com.codahale.metrics.Counter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import org.apache.commons.lang.exception.ExceptionUtils
import javax.inject.Inject
import javax.jms.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * Created by jco27 on 27/12/2016.
 */
internal class ApiOperation<T : Command> @Inject internal constructor(configuration: ConfigurationStore,
                                                                      serviceName : String,
                                                                      val serviceVersion : Version,
                                                                      instanceName : String,
                                                                      listenerInstance: Int,
                                                                      val commandName : String,
                                                                      val classReference: KClass<T>,
                                                                      val handler : CommandHandler<T>?,
                                                                      val transactionalHandler : TransactionalHandler<T>?,
                                                                      metrics : MetricRegistry) : CommsBase(configuration, serviceName, instanceName, "$serviceName.$instanceName.${classReference.simpleName}.${listenerInstance}"), MessageListener {

    private val TEST_SUFFIX_DEFAULT : String = "_test"
    private val configChangeLock : Any = Any()

    private val failedRequests : Counter = metrics.counter("${commandName}.failures")
    private val commitedRequests : Counter = metrics.counter("${commandName}.committed")
    private val rolledbackRequests : Counter = metrics.counter("${commandName}.rolled-back")
    private val discardedRequests : Counter = metrics.counter("${commandName}.discarded")
    private val rejectedRequests : Counter = metrics.counter("${commandName}.rejected")
    private val timing : Timer = metrics.timer("${commandName}.timing")

    private val logger = XmfLoggerFactory.getLogger(this::class.java)

    private val serviceDestinationTemplateKey : String get() { return "services.${serviceName}.location" }

    private val serviceTestDestinationTemplateKey : String get() { return "services.${serviceName}.test"}

    private val serviceBadMsgTemplateKey : String get() { return "services.${serviceName}.dlq" }

    private val serviceModeKey : String get() { return "services.${serviceName}.mode" }

    private val instanceModeKey : String get() { return "services.${serviceName}.${instanceName}.mode"}

    private fun serviceBadMsgDefault(serviceName: String) : String {
        return "${serviceName}.dlq"
    }

    //private var inputDestination : Destination? = null
    private var consumer : MessageConsumer? = null
    private var operationType : OperationType


    private fun getQueueSelector() : String{
        return "XMF_EventId = '$commandName' and XMF_Version_Major = '${serviceVersion.major}'"
    }


    private fun getInputDestinationName() : String {
        return configuration.readValue(serviceDestinationTemplateKey, serviceName)
    }

    private fun getTestInputDestinationName() : String {
        return configuration.readValue(serviceTestDestinationTemplateKey, getInputDestinationName() + TEST_SUFFIX_DEFAULT)
    }

    private fun getBadMsgDestinationName() : String {
        return configuration.readValue(serviceBadMsgTemplateKey, serviceBadMsgDefault(serviceName))
    }

    private fun onConfigChange(key : String, oldValue : String?, newValue : String?) {
        // Note: This function can be invoked concurrently by several changing properties
        // TODO: Reinitialize only when a sensible key has changed
        synchronized(configChangeLock){
            initializeMessaging(configuration)
        }
    }

    init {
        if (classReference.isSubclassOf(Response::class))
            operationType = OperationType.Reply
        else
            operationType = OperationType.Request
    }

    private fun getServiceMode(key : String) : ServiceMode {
        val string = configuration.readValue(key, "Normal")
        try {
            return ServiceMode.valueOf(string)
        }
        catch (ex : Exception){
            return ServiceMode.Normal
        }
    }

    private fun initializeMessaging(configuration: ConfigurationStore) {

        val serviceMode = getServiceMode(serviceModeKey)
        val instanceMode = getServiceMode(instanceModeKey)
        val finalMode = serviceMode.Merge(instanceMode)
        logger.xmf_debug_log(serviceName, instanceName, "Starting up messaging in $finalMode mode. Service mode is: $serviceMode, Instance mode is $instanceMode")
        when (finalMode) {
            ServiceMode.Test -> startUpTestMessaging()
            ServiceMode.Normal -> startUpNormalMessaging()
        }
    }

    private fun startUpMessaging(destinationName : String) {
        shutDownMessaging()
        logger.xmf_debug_log(serviceName, instanceName, "Starting up messaging subsystem in $destinationName")
        val inputDestination = session.createQueue(destinationName)
        consumer = session.createConsumer(inputDestination, getQueueSelector())
        consumer?.messageListener = this
        logger.xmf_debug_log(serviceName, instanceName, "Messaging has been started")
    }

    private fun startUpNormalMessaging() {
        startUpMessaging(getInputDestinationName())
    }

    private fun startUpTestMessaging() {
        startUpMessaging(getTestInputDestinationName())
    }

    private fun shutDownMessaging() {
        logger.xmf_debug_log(serviceName, instanceName, "Shutting down messaging subsystem")
        consumer?.close()
    }

    @Inject constructor(configuration: ConfigurationStore,
                        serviceName: String,
                        serviceVersion: Version,
                        instanceName: String,
                        listenerInstance: Int,
                        classReference: KClass<T>,
                        handler: CommandHandler<T>) : this(configuration, serviceName, serviceVersion, instanceName, listenerInstance, classReference, handler, MetricRegistry())

    @Inject constructor(configuration: ConfigurationStore,
                        serviceName: String,
                        serviceVersion: Version,
                        instanceName: String,
                        listenerInstance: Int,
                        commandName: String,
                        classReference: KClass<T>,
                        handler: CommandHandler<T>) : this(configuration, serviceName, serviceVersion, instanceName, listenerInstance, commandName, classReference, handler, null, MetricRegistry())

    @Inject constructor(configuration: ConfigurationStore,
                        serviceName: String,
                        serviceVersion: Version,
                        instanceName: String,
                        listenerInstance: Int,
                        commandName: String,
                        classReference: KClass<T>,
                        handler: CommandHandler<T>,
                        metrics: MetricRegistry) : this(configuration, serviceName, serviceVersion, instanceName, listenerInstance, commandName, classReference, handler, null, metrics)

    @Inject constructor(configuration: ConfigurationStore,
                        serviceName: String,
                        serviceVersion: Version,
                        instanceName: String,
                        listenerInstance: Int,
                        classReference: KClass<T>,
                        handler: CommandHandler<T>,
                        registry: MetricRegistry) : this(configuration, serviceName, serviceVersion, instanceName, listenerInstance, "$serviceName.${classReference.simpleName}", classReference, handler, null, registry){
    }

    @Inject constructor(configuration: ConfigurationStore,
                        serviceName: String,
                        serviceVersion: Version,
                        instanceName: String,
                        listenerInstance: Int,
                        classReference: KClass<T>,
                        handler: TransactionalHandler<T>) : this(configuration, serviceName, serviceVersion, instanceName, listenerInstance, classReference, handler, MetricRegistry())

    @Inject constructor(configuration: ConfigurationStore,
                        serviceName: String,
                        serviceVersion: Version,
                        instanceName: String,
                        listenerInstance: Int,
                        commandName: String,
                        classReference: KClass<T>,
                        handler: TransactionalHandler<T>) : this(configuration, serviceName, serviceVersion, instanceName, listenerInstance, commandName, classReference, null, handler, MetricRegistry())

    @Inject constructor(configuration: ConfigurationStore,
                        serviceName: String,
                        serviceVersion: Version,
                        instanceName: String,
                        listenerInstance: Int,
                        commandName: String,
                        classReference: KClass<T>,
                        handler: TransactionalHandler<T>,
                        metrics: MetricRegistry) : this(configuration, serviceName, serviceVersion, instanceName, listenerInstance, commandName, classReference, null, handler, metrics)

    @Inject constructor(configuration: ConfigurationStore,
                        serviceName: String,
                        serviceVersion: Version,
                        instanceName: String,
                        listenerInstance: Int,
                        classReference: KClass<T>,
                        handler: TransactionalHandler<T>,
                        registry: MetricRegistry) : this(configuration, serviceName, serviceVersion, instanceName, listenerInstance,"$serviceName.${classReference.simpleName}", classReference, null, handler, registry)

    override fun start(){
        logger.xmf_debug_log(serviceName, instanceName, "Starting API ${operationType} operation: $serviceName - $commandName")
        initializeMessaging(configuration)
        if (configuration is ObservableConfigurationStore) {
            configuration.watch(serviceModeKey, this::onConfigChange)
            configuration.watch(instanceModeKey, this::onConfigChange)
            configuration.watch(serviceTestDestinationTemplateKey, this::onConfigChange)
            configuration.watch(serviceDestinationTemplateKey, this::onConfigChange)
        }
        super.start()
    }

    override fun stop(){
        logger.xmf_debug_log(serviceName, instanceName, "Stopping API ${operationType} operation: $serviceName - $commandName")
        if (configuration is ObservableConfigurationStore) {
            configuration.unwatch(serviceModeKey, this::onConfigChange)
            configuration.unwatch(instanceModeKey, this::onConfigChange)
            configuration.unwatch(serviceTestDestinationTemplateKey, this::onConfigChange)
            configuration.unwatch(serviceDestinationTemplateKey, this::onConfigChange)
        }
        shutDownMessaging()
        super.stop()
    }

    private fun copyMessage(message: TextMessage) : TextMessage {
        val result = session.createTextMessage(message.text)
        for (name in message.propertyNames){
            result.setObjectProperty(name as String, message.getObjectProperty(name))
        }
        return result
    }

    internal fun processJsonMsg(jsonStr : String) : HandleAction {

        logger.xmf_trace_log(serviceName, instanceName, "Processing json string ${jsonStr}")

        val obj = JacksonHelper.deserialize(jsonStr, classReference.java)

        val context = timing.time()
        if (obj.isValid()) {
            var res : HandleAction?
            res = handler?.invoke(obj)
            if (null == res)
                res = transactionalHandler?.invoke(obj, Transaction(this.session))

            if (null == res) {
                logger.xmf_error_log(serviceName, instanceName, "Unexpected error processing message. Both normal and transactional handlers are null: ${jsonStr}")
                return HandleAction.BadMsg
            }

            context.stop()

            when (res) {
                HandleAction.Commit -> commitedRequests.inc()
                HandleAction.Rollback -> rolledbackRequests.inc()
                HandleAction.BadMsg -> rejectedRequests.inc()
                HandleAction.Discard -> discardedRequests.inc()
            }

            return res
        }
        else {
            val errors = obj.validationMessages()
            if (obj.header != null){
                logger.error(obj.header!!.context, serviceName, instanceName, "Unable to process request. Validation failed")
                errors.forEach { logger.error(obj.header!!.context, instanceName, "Validation error msg: $it") }
            }
            else {
                logger.xmf_error_log(serviceName, instanceName, "Received object has no header or header is invalid")
                errors.forEach { logger.xmf_error_log(serviceName, instanceName, "Validation error msg: $it") }
            }
            return HandleAction.BadMsg
        }
    }

    override fun onMessage(message: Message?) {

        try {

            val txt = (message as TextMessage).text

            val badMsgDestination = session.createQueue(getBadMsgDestinationName())

            try {

                logger.xmf_trace_log(serviceName, instanceName, "Processing message as part of the session ${session.hashCode()}")

                val res = processJsonMsg(txt)

                when (res) {
                    HandleAction.Commit -> {
                        session.commit()
                    }
                    HandleAction.Rollback -> {
                        logger.xmf_trace_log(serviceName, instanceName, "Received rollback for msg: $txt")
                        session.rollback()
                    }
                    HandleAction.BadMsg -> {
                        val producer = session.createProducer(badMsgDestination)
                        val resend = copyMessage(message)
                        resend.setStringProperty("XMF Failure Type", "Handle")
                        producer.send(resend)
                        session.commit()

                    }
                    HandleAction.Discard -> {
                    }
                }
            }
            catch (e: Exception) {
                logger.xmf_error_log(serviceName, instanceName, "Failure processing message: ${message.text}", e)
                val resend = copyMessage(message)
                resend.setStringProperty("XMF_Failure_Type", "Exception")
                resend.setStringProperty("XMF_Failure_Message", e.message)
                resend.setStringProperty("XMF_Failure_Root_Cause", ExceptionUtils.getRootCauseMessage(e))
                resend.setStringProperty("XMF_Failure_Stacktrace", ExceptionUtils.getStackTrace(e))
                // TODO: Something failed, we need to take the message and put it into the bad message queue
                val producer = session.createProducer(badMsgDestination)
                producer.send(resend)
                session.commit()
                failedRequests.inc()
            }
        }
        catch (e : Exception) {
            logger.xmf_error_log(serviceName, instanceName, "Failure unmarshalling message ${e.message}", e)
        }
    }

    protected fun finalize() {
        if (configuration is ObservableConfigurationStore) {
            configuration.watch(serviceModeKey, this::onConfigChange)
            configuration.watch(instanceModeKey, this::onConfigChange)
            configuration.watch(serviceTestDestinationTemplateKey, this::onConfigChange)
            configuration.watch(serviceDestinationTemplateKey, this::onConfigChange)
        }
        this.stop()
    }
}