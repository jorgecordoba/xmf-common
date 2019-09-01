package com.devfields.xmf.common.microservices.microservices

typealias CommandHandler<T> = (command: T) -> HandleAction
typealias TransactionalHandler<T> = (command: T, transaction: Transaction) -> HandleAction