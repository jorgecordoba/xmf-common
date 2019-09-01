package com.devfields.xmf.common.microservices.microservices

import com.devfields.xmf.common.configuration.configuration.ConfigurationStore
import com.devfields.xmf.common.configuration.configuration.ConfigurationException
import com.devfields.xmf.common.logging.XmfLoggerFactory
import com.devfields.xmf.common.messaging.MessageHeader
import com.devfields.xmf.common.serialization.JacksonHelper
import java.util.*
import javax.jms.*
import javax.naming.CommunicationException
import javax.naming.Context
import javax.naming.InitialContext


/**
 * Created by jco27 on 02/01/2017.
 */
abstract class CommsBase(val configuration : ConfigurationStore, val serviceName: String, val instanceName : String){

    protected val confFactoryKey = "broker.connectionFactory.name"
    protected val confFactoryDefault = "ConnectionFactory"
    protected val confInitialContextFactoryKey = "broker.InitialContextFactory"
    protected val confInitialContextFactoryDefault = "org.apache.activemq.jndi.ActiveMQInitialContextFactory"
    protected val confProviderUrlKey = "broker.provider.url"
    protected val confBrokerUserKey = "broker.user"
    protected val confBrokerPasswordKey = "broker.password"

    protected var connection : Connection
    protected var session : Session
    protected val ctx : InitialContext

    private val logger = XmfLoggerFactory.getLogger(this::class.java)

    init {
        val env = Hashtable<Any, String>()

        val providerUrl = configuration.readValue(confProviderUrlKey)
        val user = configuration.readValue(confBrokerUserKey)
        val password = configuration.readValue(confBrokerPasswordKey)
        val initialContextFactory = configuration.readValue(confInitialContextFactoryKey, confInitialContextFactoryDefault)
        val connectionFactoryName = configuration.readValue(confFactoryKey, confFactoryDefault)

        logger.xmf_debug_log(serviceName, instanceName, "Starting connection with broker")
        logger.xmf_debug_log(serviceName, instanceName, "Initial Context Factory: $initialContextFactory")
        logger.xmf_debug_log(serviceName, instanceName, "ProviderUrl: $providerUrl")
        logger.xmf_debug_log(serviceName, instanceName, "User: $user")

        if (providerUrl.isNullOrBlank())
            throw ConfigurationException("Unable to obtain value for providerUrl on $confProviderUrlKey")

        if (user.isNullOrBlank())
            throw ConfigurationException("Unable to obtain value for user name on $confBrokerUserKey")

        if (password.isNullOrBlank())
            throw ConfigurationException("Unable to obtain value for password on $confBrokerPasswordKey")

        env[Context.INITIAL_CONTEXT_FACTORY] = initialContextFactory
        env[Context.PROVIDER_URL] = providerUrl
        env[Context.SECURITY_PRINCIPAL] = user
        env[Context.SECURITY_CREDENTIALS] = password
        env["connectionFactoryNames"] = connectionFactoryName

        ctx = InitialContext(env)
        val factory = ctx.lookup(connectionFactoryName) as ConnectionFactory
        connection = factory.createConnection()
        connection.clientID = "$serviceName.$instanceName"
        session = connection.createSession(true, Session.SESSION_TRANSACTED)
    }

    open fun start() {
        connection.start()
    }

    open fun stop() {
        connection.close()
    }

    fun validateHeader(header : MessageHeader?) : Boolean {
        return header != null && header.type != null && header.version != null && header.destination != null
    }

    fun createMessage(command : Command) : TextMessage{
        if (!validateHeader(command.header))
            throw IllegalArgumentException("Invalid message header for message")

        var serialized = JacksonHelper.serialize(command)

        var msg = session.createTextMessage(serialized)
        msg.setStringProperty("XMF_EventId", command.header?.type)
        msg.setStringProperty("XMF_Version_Major", command.header?.version?.major.toString())
        msg.setStringProperty("XMF_Version_Minor", command.header?.version?.minor.toString())
        msg.setStringProperty("XMF_Destination", command.header?.destination?.name)
        msg.setStringProperty("XMF_Source", command.header?.source?.name)
        msg.setStringProperty("XMF_AuditTrailId", command.header?.context?.auditTrailId)
        msg.setStringProperty("XMF_Domain", command.header?.context?.domain)
        msg.setStringProperty("XMF_Tenant", command.header?.context?.tenant)
        msg.setStringProperty("XMF_Priority", command.header?.context?.priority.toString())
        try {
            val replyTo = ctx.lookup("services.${command.header?.source?.name}.location") as Destination
            msg.jmsReplyTo = replyTo
        }
        catch (any : Exception){
            // Don't handle the exception, just don't set the reply to header
            logger.trace(command.header!!.context, instanceName, "Unable to obtain reply to destination. Msg: $serialized")
        }
        return msg
    }

    private fun send(cmd : Command, toTopic : Boolean) {
        synchronized(session) {
            if (cmd.header?.destination?.name != null) {
                val location = configuration.readValue("services.${cmd.header?.destination?.name}.location") ?:
                        throw CommunicationException("Unable to find the address for the specified destination ${cmd.header?.destination?.name}")

                var dest = if (toTopic) session.createTopic(location) else session.createQueue(location)

                val producer = session.createProducer(dest)
                var msg = createMessage(cmd)
                producer.send(msg)
                session.commit()
                producer.close()
            } else {
                throw IllegalArgumentException("Destination can't be null")
            }
        }
    }

    fun sendMsg(cmd : Command){
        send(cmd, false)
    }

    fun sendNotification(notification: Notification) {
        send(notification, true)
    }

    fun makeStandardHeaderTypeName(serviceName : String, operation : Class<out Command>) : String {
        return "$serviceName.${operation.simpleName}"
    }

}