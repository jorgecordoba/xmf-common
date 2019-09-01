package com.devfields.xmf.common.logging

import org.slf4j.LoggerFactory

/**
 * Created by jco27 on 20/01/2017.
 */
object XmfLoggerFactory {

    val loggerpPrefix = "xmf"

    fun getLogger(name : String) : XmfLogger {
        return XmfLogger(LoggerFactory.getLogger("$loggerpPrefix.$name"))
    }

    fun getLogger(clazz: Class<out Any>) : XmfLogger {
        return getLogger(clazz.canonicalName)
    }
}