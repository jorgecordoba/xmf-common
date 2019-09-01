package com.devfields.xmf.common.messaging

typealias MessageHandler = (message: String) -> Unit

interface MessagingSession {
    fun listen(handler: MessageHandler)
    fun sendMessage(queueName: String, message: String)
    fun sendNotification(topicName: String)
    fun commit()
    fun rollback()
}
