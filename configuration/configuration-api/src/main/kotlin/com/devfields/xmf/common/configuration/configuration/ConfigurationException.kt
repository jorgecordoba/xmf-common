package com.devfields.xmf.common.configuration.configuration

/**
 * Created by jco27 on 19/02/2017.
 */
open class ConfigurationException : RuntimeException {
    constructor() : super() {}

    constructor(message: String) : super(message) {}

    constructor(message: String, throwable: Throwable) : super(message, throwable) {}

    constructor(throwable: Throwable) : super(throwable) {}
}
