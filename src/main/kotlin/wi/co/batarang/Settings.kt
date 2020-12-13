package wi.co.batarang

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import com.fasterxml.jackson.module.kotlin.KotlinModule

data class SettingKey(
    val description: String,
    val key: String
)

data class Setting(
    val key: SettingKey,
    val value: String
)

val mapper: ObjectMapper = ObjectMapper()
    .registerModule(KotlinModule())
    .disable(FAIL_ON_UNKNOWN_PROPERTIES)
    // .disable(FAIL_ON_EMPTY_BEANS)
    // .disable(FAIL_ON_MISSING_CREATOR_PROPERTIES)
    // .disable(FAIL_ON_INVALID_SUBTYPE)
    .enable(INDENT_OUTPUT)
// .enable(WRITE_BIGDECIMAL_AS_PLAIN)
// .enable(USE_BIG_DECIMAL_FOR_FLOATS)
