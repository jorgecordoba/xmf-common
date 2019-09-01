package com.devfields.xmf.common.messaging

import com.devfields.xmf.common.configuration.configuration.ConfigurationStore

interface MessagingFactory {
    fun createConnection(configuration : ConfigurationStore) : MessagingConnection
}