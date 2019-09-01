package com.devfields.xmf.common.types.tests.tests

import com.devfields.xmf.common.types.Version
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.AnnotationSpec
import org.junit.Test


/**
 * Created by jco27 on 29/11/16.
 */
class VersionTests : AnnotationSpec() {

    @Test
    fun createVersionWithMajorMinor() {
        val v = Version(3, 4)

        v.major shouldBe  3
        v.minor shouldBe  4
        v.revision shouldBe  0
        v.build shouldBe  0
    }

    @Test
    fun createVersionWithMajorMinorRevision() {
        val v = Version(3, 4, 1)

        v.major shouldBe  3
        v.minor shouldBe  4
        v.revision shouldBe  1
        v.build shouldBe  0
    }

    @Test
    fun createVersionWithMajorMinorRevisionBuild() {
        val v = Version(3, 4, 1, 5)

        v.major shouldBe  3
        v.minor shouldBe  4
        v.revision shouldBe  1
        v.build shouldBe  5
    }

    @Test
    fun createVersionStringMajorMinor() {
        val v = Version.TryParse("3.4")

        v.shouldNotBeNull()
        v.major shouldBe  3
        v.minor shouldBe 4
        v.revision shouldBe  0
        v.build shouldBe  0
    }

    @Test
    fun createVersionStringMajorMinorRevision() {
        val v = Version.TryParse("3.4.1")

        v.shouldNotBeNull()
        v.major shouldBe 3
        v.minor shouldBe 4
        v.revision shouldBe  1
        v.build shouldBe  0
    }

    @Test
    fun createVersionStringMajorMinorRevisionBuild() {
        val v = Version.TryParse("3.4.1.5")

        v.shouldNotBeNull()
        v.major shouldBe  3
        v.minor shouldBe  4
        v.revision shouldBe  1
        v.build shouldBe  5
    }

    @Test
    fun createVersionInvalidString1() {
        val v = Version.TryParse("3.4,3")

        v.shouldBeNull()
    }

    @Test
    fun createVersionInvalidString2() {
        val v = Version.TryParse("3.4m")

        v.shouldBeNull()
    }

    @Test
    fun createVersionInvalidString3() {
        val v = Version.TryParse("3")

        v.shouldBeNull()
    }

    @Test
    fun createVersionInvalidString4() {
        val v = Version.TryParse("3.4.3.1.2")

        v.shouldBeNull()
    }
}
