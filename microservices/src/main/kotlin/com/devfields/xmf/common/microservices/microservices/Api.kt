package com.devfields.xmf.common.microservices.microservices

import com.devfields.xmf.common.configuration.configuration.ConfigurationStore
import com.devfields.xmf.common.logging.XmfLoggerFactory
import com.devfields.xmf.common.types.Version
import com.codahale.metrics.MetricRegistry
import spark.Request
import spark.Response
import spark.Spark
import javax.inject.Inject
import com.codahale.metrics.json.MetricsModule
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.elasticsearch.metrics.ElasticsearchReporter
import java.util.concurrent.TimeUnit

/**
 * Created by jco27 on 01/01/2017.
 */
class Api @Inject constructor(configurationStore: ConfigurationStore,
                              serviceName : String,
                              val serviceVersion : Version,
                              instanceName: String,
                              metricsReg: MetricRegistry) : CommsBase(configurationStore, serviceName, instanceName){

    private val operations : MutableList<ApiOperation<out Command>> = mutableListOf()
    private val metrics : MetricRegistry = metricsReg
    private var started : Boolean = false
    private val REST_ENABLE_KEY : String = "local.microservice.rest.enabled"
    private val REST_ENABLE_DEFAULT : String = "true"
    private val REST_PORT_KEY : String = "local.microservice.rest.port"
    private val REST_PORT_DEFAULT : String = "0"
    private val isRestEnabled : Boolean

    private val logger = XmfLoggerFactory.getLogger(this.javaClass)

    constructor(configurationStore: ConfigurationStore,
                serviceName: String,
                serviceVersion: Version,
                instanceName: String) : this(configurationStore, serviceName, serviceVersion, instanceName, MetricRegistry())

    private fun handleGet(rq : Request, rs : Response) : Any {
        val objectMapper = ObjectMapper()
        objectMapper.registerModule(MetricsModule(TimeUnit.HOURS, TimeUnit.MILLISECONDS, true))
        //objectMapper.registerModule(HealthCheckModule())
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT)
        rs.status(200)
        rs.type("application/json")
        return objectMapper.writeValueAsString(metrics)
    }

    private fun handlePost(rq: Request, rs : Response, operation : ApiOperation<out Command>) : Any {

        try {
            logger.xmf_trace_log(serviceName, instanceName, "Processing request: ${rq.body()}")
            var result = operation.processJsonMsg(rq.body())
            when (result){
                HandleAction.Commit -> rs.status(200)
                HandleAction.Discard -> rs.status(403)
                HandleAction.BadMsg -> rs.status(406)
                HandleAction.Rollback -> rs.status(503)
            }
            rs.body("{ \"result\": \"${result}\" }")
            rs.type("application/json")
            logger.xmf_trace_log(serviceName, instanceName, "Request processed: $result")
            return rs.body()
        }
        catch (e : Exception){
            logger.xmf_error_log(instanceName, "Error processing json request ${serviceName} - ${operation.commandName}", e)
            rs.body("{ \"msg\" : \"Unable to process request. Check logs for more information.\" }")
            rs.status(500)
            return rs.body()
        }
    }

    init{
        isRestEnabled = configurationStore.readValue(REST_ENABLE_KEY, REST_ENABLE_DEFAULT).toBoolean()
        var reporter = ElasticsearchReporter.forRegistry(metrics)
                                            .hosts("localhost:9200")
                                            .build()
        reporter.start(60, TimeUnit.SECONDS)

        var port : Int
        try {
            port = configurationStore.readValue(REST_PORT_KEY, REST_PORT_DEFAULT).toInt()
        }
        catch (ex : NumberFormatException) {
            port = 0
        }
        if (isRestEnabled) {
            Spark.port(port)
            Spark.init()
            logger.xmf_trace_log(instanceName, "Started administration rest interface on port ${Spark.port()}. /{serviceName}")
            println("Started administration rest interface on port ${Spark.port()}")
            Spark.get("/$serviceName/info", fun(rq: Request, rs: Response): Any = handleGet(rq, rs))
        }
    }

    private fun setUpRestInterface(operation : ApiOperation<out Command>){
        if (isRestEnabled)
            Spark.post("/${serviceName}/${operation.commandName}", fun (rq : Request, rs : Response) : Any  = handlePost(rq, rs, operation))
    }

    @JvmOverloads fun <T : Command>registerOperation(classReference: Class<T>, handler: CommandHandler<T>, listeners : Int = 1){
        for (i in 1..listeners) {
            var instance = ApiOperation(configuration, serviceName, serviceVersion, instanceName, classReference, handler, metrics)
            setUpRestInterface(instance)
            operations.add(instance)
        }
    }

    @JvmOverloads fun <T : Command>registerOperation(commandName : String, classReference: Class<T>, handler: CommandHandler<T>, listeners : Int = 1){
        for (i in 1..listeners) {
            var instance = ApiOperation(configuration, serviceName, serviceVersion, instanceName, commandName, classReference, handler, metrics)
            setUpRestInterface(instance)
            operations.add(instance)
        }
    }

    @JvmOverloads fun <T : Command>registerOperation(classReference: Class<T>, handler: TransactionalHandler<T>, listeners: Int = 1) {
        for (i in 1..listeners){
            var instance = ApiOperation(configuration, serviceName, serviceVersion, instanceName, classReference, handler, metrics)
            setUpRestInterface(instance)
            operations.add(instance)
        }
    }

    @JvmOverloads fun <T : Command>registerOperation(commandName: String, classReference: Class<T>, handler: TransactionalHandler<T>, listeners: Int = 1){
        for (i in 1..listeners){
            var instance = ApiOperation(configuration, serviceName, serviceVersion, instanceName, commandName, classReference, handler, metrics)
            setUpRestInterface(instance)
            operations.add(instance)
        }
    }

    override fun start(){
        super.start()
        for (x in operations){
            x.start()
        }
        started = true
    }

    override fun stop() {
        super.stop()
        for (x in operations){
            x.stop()
        }
        spark.Spark.stop()
        started = false
    }

    protected fun finalize() {
        this.stop()
    }}