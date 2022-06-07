package com.github.nmicra.marketresearch.entity

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.github.nmicra.marketresearch.analysis.TradingDay
import com.github.nmicra.marketresearch.utils.BIGDECIMAL_ROUND
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate


@Table("market_raw")
data class MarketRaw (
    @Id
    var id: Long? = null,
    var label: String = "",

    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonDeserialize(using = LocalDateDeserializer::class)
    val date: LocalDate,
    val close: Double,
    val open: Double,
    val low: Double,
    val high: Double,
    val volume: Long
    )

fun MarketRaw.toTradingDay() : TradingDay =
    TradingDay(kotlinx.datetime.LocalDate(this.date.year, this.date.month.value, this.date.dayOfMonth),
        open = BigDecimal(this.open).round(BIGDECIMAL_ROUND), close =  BigDecimal(this.close).round(BIGDECIMAL_ROUND),
        high = BigDecimal(this.high).round(BIGDECIMAL_ROUND), low = BigDecimal(this.low).round(BIGDECIMAL_ROUND),
        volume = volume)


data class ReversalReport(
    val date : LocalDate,
    val open : BigDecimal,
    val high : BigDecimal,
    val low : BigDecimal,
    val close : BigDecimal,
    val volume : Long,
    val delta: BigDecimal,
    val intraVolatility : BigDecimal,
    val scaledVolume : BigDecimal,
    val scaledTrend : BigDecimal,
    val trend : BigDecimal,
    val momentum : BigDecimal,
    val stochastic : BigDecimal,
    val bullishIndicators : String,
    val bearishIndicators : String,
    val neutralIndicators : String,
    val additionalData : String)