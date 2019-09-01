package com.devfields.xmf.common.configuration.configuration

/**
 * Created by jco27 on 29/11/16.
 * Defines an interface for accessing configuration from a MIP service or application
 */
interface ConfigurationStore {

    /**
     * Reads a configuration store that is guaranteed to be local to the caller.
     * @return A local configuration store guranteed to be local to the caller
     */
    val local: ConfigurationStore

    /**
     * Reads a configuration value for the specified key.
     * @param key The key to the configuration value to retrieve
     * @return The configuration value if the key is present, null otherwise
     */
    fun readValue(key: String): String?

    /**
     * Reads a configuration value for the specified and returns the defaultValue if not found
     * @param key The key to the configuration value to retreive
     * @param defaultValue The configuration value if the key is present, null otherwise
     * @return
     */
    fun readValue(key: String, defaultValue: String): String

    fun readAsInteger(key: String, defaultValue: Int?): Int? {
        val `val` = readValue(key) ?: return defaultValue

        try {
            return Integer.valueOf(`val`)
        } catch (ex: NumberFormatException) {
            return defaultValue
        }

    }

    fun readAsDouble(key: String, defaultValue: Double?): Double? {
        val `val` = readValue(key) ?: return defaultValue

        try {
            return java.lang.Double.valueOf(`val`)
        } catch (ex: NumberFormatException) {
            return defaultValue
        }

    }

    fun readAsBool(key: String, defaultValue: Boolean?): Boolean? {
        val `val` = readValue(key) ?: return defaultValue

        try {
            return java.lang.Boolean.valueOf(`val`)
        } catch (ex: NumberFormatException) {
            return defaultValue
        }

    }

    fun <T : Enum<T>> readAsEnum(key: String, defaultValue: T, type: Class<T>): T {
        val value = readValue(key) ?: return defaultValue

        try {
            return java.lang.Enum.valueOf(type, value)
        } catch (ex: IllegalArgumentException) {
            return defaultValue
        }

    }

    /**
     * Reads all configuration values that match the supplied regex
     * @param keyPattern The regular expresion to use to look for properties
     * @return
     */
    fun readValues(keyPattern: String): List<ConfigurationEntry>

    /**
     * Reads a configuration entry for the specified key.
     * @param key The key to the configuration value to retrieve
     * @return The configuration entry
     */
    fun read(key: String): ConfigurationEntry?

}
