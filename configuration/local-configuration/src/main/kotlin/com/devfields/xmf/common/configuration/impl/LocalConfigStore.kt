package com.devfields.xmf.common.configuration.impl

import com.devfields.xmf.common.configuration.configuration.ConfigurationEntry
import com.devfields.xmf.common.configuration.configuration.ConfigurationStore
import com.devfields.xmf.common.types.Version
import org.apache.commons.io.FilenameUtils
import org.joda.time.DateTime
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

import java.io.IOException
import java.util.*
import java.util.regex.Pattern

/**
 * Created by jco27 on 29/11/16.
 * Defines a configuration stores that loads up all .properties files
 * on a given configuration folder and provides access to them.
 * Each file will add a first level qualifier to the property so
 * a file called myfile.properties with properties in the form:
 * serviceA.endpoint = "xxx" will provide a property key called:
 * myfile.serviceA.endpoint
 */
class LocalConfigStore(private val folder: String) : ConfigurationStore {
    private val propertiesFiles = HashMap<String, PropertiesWrapper>()

    override val local: ConfigurationStore
        get() = this

    private inner class PropertiesWrapper(var lastModifiedDate: DateTime, var fileName: String, var properties: Properties)

    private fun refreshProperties() {
        val resourcePatternResolver = PathMatchingResourcePatternResolver(this.javaClass.classLoader)
        try {
            val path = "classpath*:$folder/*.properties"
            val resources = resourcePatternResolver.getResources(path)
            for (resource in resources) {
                var lastModified: DateTime
                try {
                    val file = resource.file
                    lastModified = DateTime(file.lastModified())
                } catch (ioException: IOException) {
                    lastModified = DateTime(0)
                }

                val fileName = FilenameUtils.removeExtension(resource.filename)
                if (propertiesFiles.containsKey(fileName) && propertiesFiles[fileName]!!.lastModifiedDate.isAfter(lastModified)) {
                    // If we already have the file and the file hasn't been modified then skip the load
                    continue
                }
                // Reload the property
                val prop = Properties()
                prop.load(resource.inputStream)
                propertiesFiles[fileName] = PropertiesWrapper(lastModified, fileName, prop)
            }
        } catch (ioException: IOException) {
            // Log and continue, no properties would be available if we can't access the file

        }

    }

    private fun getPropertyName(key: String): String? {
        val index = key.indexOf('.')
        return if (-1 == index) null else key.substring(index + 1, key.length)

    }

    private fun getPropertyGroupName(key: String): String? {
        val index = key.indexOf('.')
        return if (-1 == index) null else key.substring(0, index)

    }

    private fun getWrapper(key: String): PropertiesWrapper? {
        refreshProperties()

        return propertiesFiles[getPropertyGroupName(key)]
    }

    override fun readValue(key: String): String? {
        val wrapper = getWrapper(key) ?: return null

        return wrapper.properties.getProperty(getPropertyName(key))
    }

    override fun readValue(key: String, defaultValue: String): String {
        return readValue(key) ?: return defaultValue
    }

    private fun constructPattern(keyPattern: String): Pattern {
        val tokens = keyPattern.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val bld = StringBuilder()
        for (i in 1 until tokens.size) {
            if (tokens[i] == "*")
                bld.append("(.+)")
            else
                bld.append(tokens[i])

            if (i < tokens.size - 1) {
                bld.append("\\.")
            }
        }
        return Pattern.compile(bld.toString())
    }

    override fun readValues(keyPattern: String): List<ConfigurationEntry> {
        val result = ArrayList<ConfigurationEntry>()
        val wrapper = getWrapper(keyPattern)
        if (wrapper != null) {

            val ptn = constructPattern(keyPattern)
            for (key in wrapper.properties.stringPropertyNames()) {
                if (ptn.matcher(key).find()) {
                    result.add(ConfigurationEntry(wrapper.fileName + "." + key, wrapper.properties.getProperty(key), wrapper.lastModifiedDate, Version(1, 0)))
                }
            }
        }

        return result
    }

    override fun read(key: String): ConfigurationEntry? {
        val wrapper = getWrapper(key) ?: return null

        val value = readValue(key) ?: return null

        return ConfigurationEntry(key, value, wrapper.lastModifiedDate, Version(1, 0, 0, 0))
    }
}
