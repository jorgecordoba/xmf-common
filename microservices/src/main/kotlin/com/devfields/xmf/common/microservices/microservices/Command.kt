package com.devfields.xmf.common.microservices.microservices

import com.devfields.xmf.common.messaging.MessageHeader
import java.util.*

/**
 * Created by jco27 on 28/12/2016.
 */
abstract class Command() {
    var header : MessageHeader? = null
    protected var uniqueIx : String = UUID.randomUUID().toString()

    constructor(msgHeader : MessageHeader) : this(){
        header = msgHeader
    }

    abstract fun validationMessages() : List<String>

    open fun getUniqueId() : String {
        return uniqueIx
    }

    fun isValid() : Boolean {
        return validationMessages().size == 0
    }
}