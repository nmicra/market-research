package com.github.nmicra.marketresearch.analysis

import kotlinx.serialization.Serializable

@Serializable
sealed class Indicator(val name : String = "") {
    object MIN : Indicator("MIN")
    object MAX : Indicator("MAX")
    object KNEE_JERK_LOW : Indicator("KNEE_JERK_LOW")
    object KNEE_JERK_HIGH : Indicator("KNEE_JERK_HIGH")
    object ClosedAboveOpen : Indicator("ClosedAboveOpen") // basic green candle
    object ClosedBelowOpen : Indicator("ClosedBelowOpen")// basic red candle
    object ClosedHigh : Indicator("ClosedHigh")
    object ClosedLow : Indicator("ClosedLow")
    object PenetratedPreviousHigh : Indicator("PenetratedPreviousHigh")
    object PenetratedPreviousLow : Indicator("PenetratedPreviousLow")
    object HigherHigh : Indicator("HigherHigh")
    object LowerLow : Indicator("LowerLow")
    object BullishOutsideReversal : Indicator("BullishOutsideReversal") // [x].open < [x-1].close && [x].close > [x-1].close
    object BearishOutsideReversal : Indicator("BearishOutsideReversal") // [x].open > [x-1].close && [x].close < [x-1].close

            // ---------------- Evening Star ------------------
    // https://www.quora.com/What-is-a-bearish-reversal
            // [x-2].open < [x-2].close && [x-1].open > [x-1].close && [x-1].close > [x-2].close && [x].close < [x].open && [x].open < [x-1].close
    object EveningStarBullishReversal : Indicator("EveningStarBullishReversal")
            // [x-2].open > [x-2].close && [x-1].open < [x-1].close && [x-1].close < [x-2].close && [x].close > [x].open && [x].open > [x-1].close
    object EveningStarBearishReversal : Indicator("EveningStarBearishReversal")
    // ---------------- Evening Star ------------------

    object BullishCandle : Indicator("BullishCandle")
    object BearishCandle : Indicator("BearishCandle")

    fun isBearishIndicator() : Boolean = bearishIndicators().contains(this)
    fun isBullishIndicator() : Boolean = bullishIndicators().contains(this)
    fun isNeutralIndicator() : Boolean = neutralIndicators().contains(this)

    companion object{

        fun neutralIndicators() : List<Indicator> = listOf<Indicator>(MIN, MAX, KNEE_JERK_LOW, KNEE_JERK_HIGH)
        fun bearishIndicators() : List<Indicator> = listOf<Indicator>(
            ClosedBelowOpen, ClosedLow, PenetratedPreviousLow, LowerLow, BullishOutsideReversal, BearishCandle,
            EveningStarBullishReversal
        )
        fun bullishIndicators() : List<Indicator> = listOf<Indicator>(ClosedAboveOpen, ClosedHigh, PenetratedPreviousHigh, HigherHigh, BearishOutsideReversal, BullishCandle, EveningStarBearishReversal)

        val allIndicators by lazy { Indicator::class.sealedSubclasses
            .map { it.objectInstance as Indicator } }

        fun fromString(indicator : String) : Indicator = allIndicators.first { it.toString() == indicator }
    }


    fun rawName() : String {
        return this.javaClass.simpleName
    }


    override fun toString(): String {
        return rawName()
    }

}

/*
fun main() {
    println("aaa")
    println(Indicator.bullishIndicators.contains(Indicator.BullishOutsideReversal))
    println(Indicator.BullishOutsideReversal.isBullishIndicator())
    val mlst : MutableList<@Serializable(with = IndicatorsSerializer::class) Indicator> = mutableListOf() //mutableListOf(Indicator.BullishOutsideReversal)
    mlst.add(Indicator.BullishOutsideReversal)
    mlst.filter { it.isBullishIndicator() }.forEach { println(">> $it") }
}*/
