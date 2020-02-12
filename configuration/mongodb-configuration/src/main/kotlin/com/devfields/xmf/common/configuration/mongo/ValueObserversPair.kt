package com.devfields.xmf.common.configuration.mongo

import com.devfields.xmf.common.configuration.configuration.ConfigChangeHandler
import java.util.ArrayList

/**
 * Created by jco27 on 26/02/2017.
 */
class ValueObserversPair(@get:Synchronized @set:Synchronized var value: String?) {
    val handlers: MutableList<ConfigChangeHandler> = ArrayList()
}
