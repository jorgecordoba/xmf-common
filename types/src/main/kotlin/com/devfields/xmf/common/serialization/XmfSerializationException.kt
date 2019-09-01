package com.devfields.xmf.common.serialization

/**
 * Created by jco27 on 19/02/2017.
 */
class XmfSerializationException : RuntimeException {
   constructor() : super()
   constructor(message: String?) : super(message)
   constructor(message: String?, throwable: Throwable) : super(message, throwable)
   constructor(throwable: Throwable) : super(throwable)
}