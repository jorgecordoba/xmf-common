package com.devfields.xmf.common.configuration.impl.tests

import com.devfields.xmf.common.configuration.configuration.ConfigurationEntry
import com.devfields.xmf.common.configuration.configuration.ConfigurationStore
import com.devfields.xmf.common.configuration.impl.LocalConfigStore
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.AnnotationSpec

/**
 * Created by jco27 on 30/01/2017.
 */
class LocalConfigStoreTest : AnnotationSpec() {
    @Test
    fun readValue() {
        val store = LocalConfigStore("conf")
        store.readValue("file1.example.value.for.test") shouldBe "value"
        store.readValue("file1.example.intVal.for.test") shouldBe "34"
        store.readValue("file1.another.value") shouldBe "anotherValue"
        store.readValue("file2.example.value.for.test") shouldBe "valueInFile2"
    }

    @Test
    fun readValueWithDefaultButValueExists() {
        val store = LocalConfigStore("conf")
        store.readValue("file1.example.value.for.test", "default") shouldBe "value"
        store.readValue("file1.example.intVal.for.test", "default")shouldBe "34"
        store.readValue("file1.another.value", "default") shouldBe "anotherValue"
        store.readValue("file2.example.value.for.test", "default") shouldBe "valueInFile2"
    }

    @Test
    fun readValueWithDefault() {
        val store = LocalConfigStore("conf")
        store.readValue("file1.unexisting.property", "default") shouldBe "default"
        store.readValue("file2.unexisting.property", "default") shouldBe "default"
        store.readValue("file3.unexisting.property", "default") shouldBe "default"
    }

    @Test
    fun readAllValuesWithPattern() {
        val store = LocalConfigStore("conf")

        var entries = store.readValues("file1.example.*.test")
        entries.size shouldBe 2
        entries.stream().anyMatch { p -> p.key == "file1.example.value.for.test" }.shouldBeTrue()
        entries.stream().anyMatch { p -> p.key == "file1.example.intVal.for.test" }.shouldBeTrue()

        entries = store.readValues("file1.example.*.val")
        entries.size.shouldBe(1)
        entries[0].key.shouldBe("file1.example.third.val")
        entries[0].value.shouldBe("99")
    }

    @Test
    fun read() {
        val store = LocalConfigStore("conf")
        val entry = store.read("file1.example.value.for.test")

        entry?.key shouldBe "file1.example.value.for.test"
        entry?.value shouldBe "value"
        entry?.version.toString() shouldBe  "1.0.0.0"
    }

}