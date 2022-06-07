package com.github.nmicra.marketresearch.analysis

import com.github.nmicra.marketresearch.general.BIGDECIMAL_SCALE
import com.github.nmicra.marketresearch.serialize.BigDecimalSerializer
import com.github.nmicra.marketresearch.serialize.IndicatorsSerializer
import com.github.nmicra.marketresearch.serialize.MovingAvgMapSerializer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode

@Serializable
sealed class TradingPeriod : java.io.Serializable {
    abstract val startingDate: LocalDate
    abstract val open: BigDecimal
    abstract val high: BigDecimal
    abstract val low: BigDecimal
    abstract val close: BigDecimal
    abstract val volume: Long

    @Serializable(with = BigDecimalSerializer::class)
    lateinit var delta: BigDecimal

    @Serializable(with = BigDecimalSerializer::class)
    lateinit var scaledVolume : BigDecimal

    @Serializable(with = BigDecimalSerializer::class)
    lateinit var scaledTrend : BigDecimal

    /**
     * trend is calculated with VPT formula
     * VPT = Previous VPT + Volume x (Today’s Closing Price – Previous Closing Price) / Previous Closing Price
     */
    @Serializable(with = BigDecimalSerializer::class)
    lateinit var trend : BigDecimal
    /**
     * momentum is calculated with RSI formula
     * formula described here. https://www.macroption.com/rsi-calculation/
     */
    @Serializable(with = BigDecimalSerializer::class)
    lateinit var momentum : BigDecimal

    /**
     * Stochastic Oscillator Indicator described in the next link
     * https://www.investopedia.com/terms/s/stochasticoscillator.asp
     */
    @Serializable(with = BigDecimalSerializer::class)
    lateinit var stochastic : BigDecimal

    val tradingLabelsList : MutableSet<@Serializable(with = IndicatorsSerializer::class) Indicator> = mutableSetOf()
    val intraVolatility by lazy { high - low }
    val isHighClose by lazy { close > (high - low).multiply(BigDecimal(0.75)) + low }
    val isLowClose by lazy { close < (high - low).multiply(BigDecimal(0.25)) + low }


    @Serializable(with = MovingAvgMapSerializer::class)
    val movingAvg = mutableMapOf<Int, @Serializable(with = BigDecimalSerializer::class) BigDecimal>()

    fun hasDelta(): Boolean = this::delta.isInitialized
    fun hasScaledVolume(): Boolean = this::scaledVolume.isInitialized
    fun hasTrend(): Boolean = this::trend.isInitialized
    fun hasMomentum(): Boolean = this::momentum.isInitialized
    fun hasStochastic(): Boolean = this::stochastic.isInitialized

    /**
     * returns percentage delta, for previous trading period.
     */
    fun deltaPercentage(prevTrade: TradingPeriod): BigDecimal =
        (close - prevTrade.close).multiply(BigDecimal(100))
            .divide(prevTrade.close, BIGDECIMAL_SCALE, RoundingMode.HALF_UP)

}
