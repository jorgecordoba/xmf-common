package com.devfields.xmf.common.messaging

interface MessagingConnection {
    fun createSession() : MessagingSession
    fun start()
    fun stop()
}
