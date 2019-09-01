package com.devfields.xmf.common.serialization

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule

/**
 * Created by jco27 on 19/02/2017.
 */
object JacksonHelper {

    @JvmStatic fun createJacksonMapper(): ObjectMapper {
        val mapper = jacksonObjectMapper()
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        mapper.registerModule(JodaModule())
        mapper.registerModule(ParameterNamesModule(JsonCreator.Mode.DEFAULT))
        // make private fields of Person visible to Jackson
        // mapper.setVisibility(FIELD, ANY)
        mapper.configure(SerializationFeature.
                WRITE_DATES_AS_TIMESTAMPS, false)
        return mapper
    }

    @JvmStatic fun serialize(obj: Any) : String{
        try {
            return createJacksonMapper().writer().withDefaultPrettyPrinter().writeValueAsString(obj)
        }
        catch (ex: JsonProcessingException) {
            throw XmfSerializationException(ex)
        }
    }

    @JvmStatic fun <T> deserialize(json: String, clazz: Class<T>) : T {
        try {
            return createJacksonMapper().readValue(json, clazz)
        }
        catch(ex: JsonProcessingException){
            throw XmfSerializationException(ex)
        }
    }

}
