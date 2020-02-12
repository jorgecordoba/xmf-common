package com.devfields.xmf.common.microservices

/**
 * Created by jco27 on 26/02/2017.
 */
enum class ServiceMode {
    Normal, Test, Disabled;

    fun Merge(other : ServiceMode) : ServiceMode {
        return if (other == Disabled || this == Disabled) Disabled
               else if (other == Test || this == Test) Test
               else Normal

    }
}