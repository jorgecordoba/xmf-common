package com.devfields.xmf.common.configuration.configuration

class KeyNotFoundException : ConfigurationException {

    val key : String

    constructor(key: String) : super() {
        this.key = key;
    }

    constructor(key: String, message: String) : super(message){
        this.key = key;
    }

    constructor(key: String, message: String, throwable: Throwable) : super(message, throwable){
        this.key = key;
    }

    constructor(key: String, throwable: Throwable) : super(throwable) {
        this.key = key;
    }
}