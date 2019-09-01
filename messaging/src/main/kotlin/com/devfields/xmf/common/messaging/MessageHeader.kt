package com.devfields.xmf.common.messaging

import com.devfields.xmf.common.types.Version
import org.joda.time.DateTime

/**
 * Created by jco27 on 21/12/2016.
 */
data class MessageHeader(val type: String,
                         val version : Version,
                         val context: MessageContext,
                         var source : Destination,
                         var destination : Destination) {

    var messageDate : DateTime = DateTime.now()

}