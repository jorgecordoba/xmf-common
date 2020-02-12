package com.devfields.xmf.common.microservices

import com.devfields.xmf.common.configuration.configuration.ConfigurationStore
import com.devfields.xmf.common.logging.XmfLoggerFactory
import com.devfields.xmf.common.messaging.Destination
import com.devfields.xmf.common.messaging.MessageContext
import com.devfields.xmf.common.messaging.MessageHeader
import com.devfields.xmf.common.types.Version
import javax.inject.Inject

/**
 * Created by jco27 on 02/02/2017.
 */
class Messenger @Inject constructor(config : ConfigurationStore,
                                    serviceName : String,
                                    val serviceVersion : Version,
                                    instanceName : String) : CommsBase(config, serviceName, instanceName, "$serviceName.$instanceName.client") {

    private val logger = XmfLoggerFactory.getLogger(this::class.java)

    private val notificationsName : String = "notifications"

    fun sendMsg(cmd : Command, source: Destination, destination: Destination, context: MessageContext) {
        cmd.header = MessageHeader(makeStandardHeaderTypeName(serviceName, cmd::class.java),
                serviceVersion, context, source, destination)

        sendMsg(cmd)
    }

    fun sendResponse(response : Response, source: Destination, destination : Destination, context: MessageContext) {
        response.header = MessageHeader(makeStandardHeaderTypeName(serviceName, response::class.java),
                serviceVersion, context, source, destination)
        sendMsg(response)
    }

    fun sendNotification(notification : Notification, source: Destination, context: MessageContext) {
        notification.header = MessageHeader(makeStandardHeaderTypeName(serviceName, notification::class.java),
                serviceVersion, context, source, Destination(notificationsName))
        sendNotification(notification)
    }

    protected fun finalize() {
        this.stop()
    }
}