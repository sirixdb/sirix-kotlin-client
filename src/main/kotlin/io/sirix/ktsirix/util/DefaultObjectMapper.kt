package io.sirix.ktsirix.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object DefaultObjectMapper : ObjectMapper() {
    init {
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        registerModule(JodaModule())
        registerKotlinModule()
    }
}
