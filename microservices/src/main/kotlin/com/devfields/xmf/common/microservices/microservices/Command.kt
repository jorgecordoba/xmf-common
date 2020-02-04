package com.devfields.xmf.common.microservices.microservices

import com.devfields.xmf.common.messaging.MessageContext
import com.devfields.xmf.common.messaging.MessageHeader
import com.devfields.xmf.common.types.Version
import java.util.*

/**
 * Created by jco27 on 28/12/2016.
 */
abstract class Command(serviceName: String,
                       version : Version,
                       context: MessageContext) {
    protected var uniqueIx: String = UUID.randomUUID().toString()
    var header : MessageHeader

    init {
        header = MessageHeader(makeStandardHeaderTypeName(serviceName),version, context)
    }

    abstract fun validationMessages(): List<String>

    open fun getUniqueId(): String {
        return uniqueIx
    }

    fun isValid(): Boolean {
        return validationMessages().isEmpty()
    }

    companion object {

        fun makeStandardHeaderTypeName(serviceName: String): String {
            return "$serviceName.${this::class.simpleName}"
        }
    }
}