package com.devfields.xmf.common.microservices

import com.devfields.xmf.common.messaging.MessageContext
import com.devfields.xmf.common.types.Version

/**
 * Created by jco27 on 02/01/2017.
 */
abstract class Response(val requestId: String) : Command(){
}