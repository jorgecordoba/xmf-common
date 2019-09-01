package com.devfields.xmf.common.types

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Created by jco27 on 29/11/16.
 */
class Version {

    /**
     * Gets the major value of a version
     * @return The version major
     */
    val major: Int
    /**
     * Gets the minor value of a version
     * @return The version minor
     */
    val minor: Int
    /**
     * Gets the build value of a version
     * @return The version build
     */
    val build: Int
    /**
     * Gets the revision
     * @return
     */
    val revision: Int

    private var stringRepresentation: String

    private fun getStringRepresentation() : String {
        return major.toString() + "." + minor.toString() + "." + revision.toString() + "." + build.toString()
    }

    constructor(major: Int, minor: Int) : this(major,minor, 0, 0)

    constructor(major: Int, minor: Int, build : Int) : this(major, minor, build, 0)

    constructor(major: Int, minor: Int, revision: Int, build: Int) {
        this.major = major
        this.minor = minor
        this.build = build
        this.revision = revision
        this.stringRepresentation = getStringRepresentation()
    }

    @JsonCreator()
    constructor(version: String) {
        val v = TryParse(version) ?: throw IllegalArgumentException("The supplied version string is in the wrong format")

        this.major = v.major
        this.minor = v.minor
        this.revision = v.revision
        this.build = v.build
        this.stringRepresentation = getStringRepresentation()
    }

    /**
     * Returns the string representation of a Version in the format Major.Minor.Revision.Build, for example: 2.3.1.0
     * @return The string representation of a Version
     */
    @JsonValue
    override fun toString(): String {
        return stringRepresentation
    }

    companion object {

        private fun tryParse(text: String): Int? {
            try {
                return Integer.parseInt(text)
            } catch (e: NumberFormatException) {
                return null
            }

        }

        /**
         * Returns a new instance of a version from a string representation in the format: Major.Minor.Revision.Build
         * @param version The version string
         * *
         * @return The parsed Version object or null if the string is in the wrong format
         */
        fun TryParse(version: String?): Version? {
            val major: Int?
            val minor: Int?
            val build: Int?
            val revision: Int?
            var componentsToParse: Int

            if (null == version)
                return null

            val parts = version.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            componentsToParse = parts.size
            if (componentsToParse < 2 || componentsToParse > 4)
                return null

            major = tryParse(parts[0])
            minor = tryParse(parts[1])

            if (null == major || null == minor)
                return null

            componentsToParse -= 2

            if (componentsToParse > 0) {
                revision = tryParse(parts[2])
                if (null == revision)
                    return null

                componentsToParse--
                if (componentsToParse > 0) {
                    build = tryParse(parts[3])
                    if (null == build) {
                        return null
                    } else {
                        return Version(major, minor, revision, build)
                    }
                } else {
                    return Version(major, minor, revision)
                }

            } else {
                return Version(major, minor)
            }
        }
    }
}
