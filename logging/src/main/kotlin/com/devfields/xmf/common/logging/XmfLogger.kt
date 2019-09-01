package com.devfields.xmf.common.logging

import com.devfields.xmf.common.messaging.MessageContext
import org.slf4j.Logger
import org.slf4j.Marker

/**
 * Created by jco27 on 19/01/2017.
 */
class XmfLogger(internal var baseLogger: Logger) {

    fun formatMsg(context: MessageContext, instanceId: String, msg: String) : String{
        return "[${context.auditTrailId}] - [$instanceId] - [${context.domain}] - [${context.tenant}] - $msg"
    }

    fun formatMsg(instanceId: String, msg: String) : String {
        return "[XMF Diagnostics] - [$instanceId] - [Diagnostics] - [NA] - $msg"
    }

    fun formatMsg(context: MessageContext, service: String, instanceId: String, msg: String) : String{
        return "[${context.auditTrailId}] - [$service.$instanceId] - [${context.domain}] - [${context.tenant}] - $msg"
    }

    fun formatMsg(service: String, instanceId: String, msg: String) : String {
        return "[XMF Diagnostics] - [$service.$instanceId] - [Diagnostics] - [NA] - $msg"
    }

    fun debug(context : MessageContext, instanceId: String, msg: String) {
        baseLogger.debug(formatMsg(context, instanceId, msg))
    }

    fun debug(context: MessageContext, instanceId: String, marker: Marker, msg: String) {
        baseLogger.debug(marker, formatMsg(context, instanceId, msg))
    }

    fun debug(context: MessageContext, instanceId: String, msg: String, vararg args: Any) {
        this.baseLogger.debug(formatMsg(context, instanceId, msg), *args)
    }

    fun debug(context : MessageContext, service: String, instanceId: String, msg: String) {
        baseLogger.debug(formatMsg(context, service, instanceId, msg))
    }

    fun debug(context: MessageContext, service: String, instanceId: String, marker: Marker, msg: String) {
        baseLogger.debug(marker, formatMsg(context, service, instanceId, msg))
    }

    fun debug(context: MessageContext, service: String, instanceId: String, msg: String, vararg args: Any) {
        this.baseLogger.debug(formatMsg(context, service, instanceId, msg), *args)
    }

    fun error(context: MessageContext, instanceId: String, msg: String) {
        this.baseLogger.error(formatMsg(context, instanceId, msg))
    }

    fun error(context: MessageContext, instanceId: String, msg: String, vararg args: Any) {
        this.baseLogger.debug(formatMsg(context, instanceId, msg), *args)
    }

    fun error(context: MessageContext, instanceId: String, msg: String, exception: Throwable) {
        this.baseLogger.error(formatMsg(context, instanceId, msg), exception)
    }

    fun error(context: MessageContext, service: String, instanceId: String, msg: String) {
        this.baseLogger.error(formatMsg(context, service, instanceId, msg))
    }

    fun error(context: MessageContext, service: String, instanceId: String, msg: String, vararg args: Any) {
        this.baseLogger.debug(formatMsg(context, service, instanceId, msg), *args)
    }

    fun error(context: MessageContext, service: String, instanceId: String, msg: String, exception: Throwable) {
        this.baseLogger.error(formatMsg(context, service, instanceId, msg), exception)
    }

    fun info(context: MessageContext, instanceId: String, msg: String) {
        this.baseLogger.info(formatMsg(context, instanceId, msg))
    }

    fun info(context: MessageContext, instanceId: String, msg: String, vararg args: Any) {
        this.baseLogger.info(formatMsg(context, instanceId, msg), *args)
    }

    fun info(context: MessageContext, service: String, instanceId: String, msg: String) {
        this.baseLogger.info(formatMsg(context, service, instanceId, msg))
    }

    fun info(context: MessageContext, service: String, instanceId: String, msg: String, vararg args: Any) {
        this.baseLogger.info(formatMsg(context, service, instanceId, msg), *args)
    }

    fun trace(context: MessageContext, instanceId: String, msg: String) {
        this.baseLogger.trace(formatMsg(context, instanceId, msg))
    }

    fun trace(context: MessageContext, instanceId: String, msg: String, vararg args: Any) {
        this.baseLogger.trace(formatMsg(context, instanceId, msg), *args)
    }

    fun trace(context: MessageContext, service: String, instanceId: String, msg: String) {
        this.baseLogger.trace(formatMsg(context, service, instanceId, msg))
    }

    fun trace(context: MessageContext, service: String, instanceId: String, msg: String, vararg args: Any) {
        this.baseLogger.trace(formatMsg(context, service, instanceId, msg), *args)
    }

    fun warn(context: MessageContext, instanceId: String, msg: String) {
        this.baseLogger.warn(formatMsg(context, instanceId, msg))
    }

    fun warn(context: MessageContext, instanceId: String, msg: String, vararg args: Any) {
        this.baseLogger.warn(formatMsg(context, instanceId, msg), *args)
    }

    fun warn(context: MessageContext, service: String, instanceId: String, msg: String) {
        this.baseLogger.warn(formatMsg(context, service, instanceId, msg))
    }

    fun warn(context: MessageContext, service: String, instanceId: String, msg: String, vararg args: Any) {
        this.baseLogger.warn(formatMsg(context, service, instanceId, msg), *args)
    }

    fun xmf_trace_log(instanceId: String, msg: String) {
        this.baseLogger.trace(formatMsg(instanceId, msg))
    }

    fun xmf_trace_log(service: String, instanceId: String, msg: String) {
        this.baseLogger.trace(formatMsg(service, instanceId, msg))
    }

    fun xmf_debug_log(instanceId: String, msg: String) {
        this.baseLogger.debug(formatMsg(instanceId, msg))
    }

    fun xmf_debug_log(service: String, instanceId: String, msg: String) {
        this.baseLogger.debug(formatMsg(service, instanceId, msg))
    }

    fun xmf_error_log(instanceId: String, msg: String) {
        this.baseLogger.error(formatMsg(instanceId, msg))
    }

    fun xmf_error_log(instanceId: String, msg: String, exception: Throwable) {
        this.baseLogger.error(formatMsg(instanceId, msg), exception)
    }

    fun xmf_error_log(service : String, instanceId: String, msg: String) {
        this.baseLogger.error(formatMsg(service, instanceId, msg))
    }

    fun xmf_error_log(service: String, instanceId: String, msg: String, exception: Throwable) {
        this.baseLogger.error(formatMsg(service, instanceId, msg), exception)
    }

    fun xmf_warn_log(instanceId: String, msg: String) {
        this.baseLogger.warn(formatMsg(instanceId, msg))
    }

    fun xmf_warn_log(service: String, instanceId: String, msg: String) {
        this.baseLogger.warn(formatMsg(service, instanceId, msg))
    }

    val isDebugEnabled: Boolean?
        get() = baseLogger.isDebugEnabled

    fun isDebugEnabled(marker: Marker): Boolean? {
        return baseLogger.isDebugEnabled(marker)
    }

    val isErrorEnabled: Boolean?
        get() = baseLogger.isErrorEnabled

    fun isErrorEnabled(marker: Marker): Boolean? {
        return baseLogger.isErrorEnabled(marker)
    }

    val isInfoEnabled: Boolean?
        get() = baseLogger.isInfoEnabled

    fun isInfoEnabled(marker: Marker): Boolean? {
        return baseLogger.isInfoEnabled(marker)
    }

    val isTraceEnabled: Boolean?
        get() = baseLogger.isTraceEnabled

    fun isTraceEnabled(marker: Marker): Boolean? {
        return baseLogger.isTraceEnabled(marker)
    }

    val isWarnEnabled: Boolean?
        get() = baseLogger.isWarnEnabled

    fun isWarnEnabled(marker: Marker): Boolean? {
        return baseLogger.isWarnEnabled(marker)
    }
}
