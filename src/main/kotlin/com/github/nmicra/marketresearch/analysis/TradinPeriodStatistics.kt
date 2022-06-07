package com.github.nmicra.marketresearch.analysis

import com.github.nmicra.marketresearch.serialize.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class TradingPeriodStatistics(
    @Serializable(with = BigDecimalSerializer::class) val mean : BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val standardDeviation : BigDecimal)