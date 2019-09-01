package com.devfields.xmf.common.configuration.configuration

import org.joda.time.DateTime
import com.devfields.xmf.common.types.Version

/**
 * Created by jco27 on 29/11/16.
 */
class ConfigurationEntry(
        /**
         * Returns the key associated with the configuration entry
         * @return The key
         */
        val key: String,
        /**
         * Returns the value associated with the configuration entry
         * @return The value
         */
        val value: String,
        /**
         * Returns the last time the value was modified
         * @return The last modified date and time
         */
        val lastModified: DateTime,
        /**
         * Returns the version of the value
         * @return The version of the value
         */
        val version: Version)
