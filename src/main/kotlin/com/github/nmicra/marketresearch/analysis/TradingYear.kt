package com.github.nmicra.marketresearch.analysis

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class TradingYear(val tradingDayList: List<TradingDay>) : TradingPeriod() {
    init {
        tradingDayList.sortedBy { it.startingDate }
    }

    override val startingDate: LocalDate
        get() = tradingDayList.first().startingDate
    override val open: BigDecimal
        get() = tradingDayList.first().open
    override val high: BigDecimal
        get() = tradingDayList.map { it.high }.reduce { a, b -> maxOf(a, b) }
    override val low: BigDecimal
        get() = tradingDayList.map { it.low }.reduce { a, b -> minOf(a, b) }
    override val close: BigDecimal
        get() = tradingDayList.last().close
    override val volume: Long
        get() = tradingDayList.sumOf { it.volume }
    val year by lazy { startingDate.year }
}