package com.github.nmicra.marketresearch.analysis

import com.github.nmicra.marketresearch.serialize.BigDecimalSerializer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.serializers.LocalDateComponentSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class TradingDay(
    @Serializable(with = LocalDateComponentSerializer::class) override val startingDate: LocalDate,
    @Serializable(with = BigDecimalSerializer::class) override val open: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) override val high: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) override val low: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) override val close: BigDecimal,
    override val volume: Long = 0
) : TradingPeriod() {
    val weekNr by lazy { weekNumForDate(startingDate) }
}