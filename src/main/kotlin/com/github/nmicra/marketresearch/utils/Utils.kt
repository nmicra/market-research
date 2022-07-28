package com.github.nmicra.marketresearch.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.nmicra.marketresearch.analysis.Indicator
import com.github.nmicra.marketresearch.analysis.TradingPeriod
import com.github.nmicra.marketresearch.general.BIGDECIMAL_SCALE
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode


val objectMapper by lazy {
    ObjectMapper().also {
        it.registerKotlinModule()
        it.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        it.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
}

val BIGDECIMAL_ROUND = MathContext(4)
typealias JavaLocalDate = java.time.LocalDate
inline fun <reified T> jacksonTypeRef(): TypeReference<T> = object : TypeReference<T>() {}
inline fun <reified T> ObjectMapper.readValue(jp: JsonParser): T = readValue(jp, jacksonTypeRef<T>())
inline fun <reified T> ObjectMapper.readValue(jp: String): T = readValue(jp, jacksonTypeRef<T>())
inline fun ObjectMapper.transformToJsonNode(str: String): JsonNode = objectMapper.readTree(objectMapper.factory.createParser(str))

val listOfBullishReversals = listOf(Indicator.BullishOutsideReversal, Indicator.BearishCandle, Indicator.EveningStarBullishReversal)
val listOfBearishReversals = listOf(Indicator.BearishOutsideReversal, Indicator.BullishCandle, Indicator.EveningStarBearishReversal)

fun BigDecimal.plusOnePercent() : BigDecimal = this.divide(BigDecimal(100), BIGDECIMAL_SCALE, RoundingMode.HALF_UP).plus(this)
fun BigDecimal.minusOnePercent() : BigDecimal = this.minus(this.divide(BigDecimal(100), BIGDECIMAL_SCALE, RoundingMode.HALF_UP))