package com.devfields.xmf.common.configuration.configuration

/**
 * Created by jco27 on 26/02/2017.
 */
interface ObservableConfigurationStore : ConfigurationStore {

    fun watch(key: String, handler: ConfigChangeHandler)
    fun unwatch(key: String, handler: ConfigChangeHandler)
}
