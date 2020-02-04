package com.devfields.xmf.common.microservices.microservices

import com.devfields.xmf.common.messaging.MessageContext
import com.devfields.xmf.common.types.Version

/**
 * Created by jco27 on 02/01/2017.
 */
abstract class Notification(serviceName: String,
                            version : Version,
                            context: MessageContext) : Command(serviceName, version, context) {
}