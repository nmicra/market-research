package com.github.nmicra.marketresearch.analysis

import com.github.nmicra.marketresearch.general.BIGDECIMAL_SCALE
import com.github.nmicra.marketresearch.utils.listOfBearishReversals
import com.github.nmicra.marketresearch.utils.listOfBullishReversals
import com.github.nmicra.marketresearch.utils.minusOnePercent
import com.github.nmicra.marketresearch.utils.plusOnePercent
import kotlinx.datetime.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.exp
import kotlin.math.pow

fun List<TradingDay>.toTradingWeeks(): List<TradingWeek> {
    val groupedByWeeks =
        this.groupBy { it.startingDate.year }
            .mapValues { it.value.groupBy { tradingDay -> tradingDay.weekNr }.values.map { week -> TradingWeek(week) } }
    return groupedByWeeks.values.flatMap { it.toList() }
}

fun List<TradingDay>.toTradingMonth(): List<TradingMonth> {
    val groupedByMonth =
        this.groupBy { it.startingDate.year }.mapValues {
            it.value.groupBy { tradingDay -> tradingDay.startingDate.monthNumber }.values.map { week ->
                TradingMonth(week)
            }
        }
    return groupedByMonth.values.flatMap { it.toList() }
}

fun List<TradingDay>.toTradingYear(): List<TradingYear> {
    val groupedByYear =
        this.groupBy { it.startingDate.year }.mapValues {
            it.value.groupBy { tradingDay -> tradingDay.startingDate.year }.values.map { week ->
                TradingYear(week)
            }
        }
    return groupedByYear.values.flatMap { it.toList() }
}

fun List<TradingPeriod>.calculateMA(days: Int) {
    for (i in days..this.lastIndex) {
        val avg = this.subList(i - days, i).sumOf { it.close }.divide(BigDecimal(days), BIGDECIMAL_SCALE, RoundingMode.HALF_UP)
        this[i].movingAvg[days] = avg
    }
}

fun List<TradingPeriod>.calculateDelta() {
    for (i in 1..this.lastIndex) {
        this[i].delta = this[i].deltaPercentage(this[i - 1])
    }
}

/**
 * Before you use this method, please RUN:
 * 1. ==>> scaleVolume
 * 2. ==>> calculateDelta
 */
fun List<TradingPeriod>.calculateTrend() {
    //VPT = Previous VPT + Volume x DeltaPrice / Previous Closing Price
    this[0].trend = BigDecimal.ZERO
    for (i in 1..this.lastIndex) {
        this[i].trend = this[i - 1].trend + BigDecimal(this[i].volume).multiply(this[i].delta).divide(this[i - 1].close,BIGDECIMAL_SCALE, RoundingMode.HALF_UP)
    }
}

/**
 * Before you use this method, please RUN:
 * 2. ==>> calculateDelta
 */
fun List<TradingPeriod>.calculateMomentum(numTradingDaysForCalc : Int = 10) {
    fun positiveNumOrZero(n : BigDecimal) : BigDecimal = if (n > BigDecimal.ZERO) n else BigDecimal.ZERO
    fun negativeNumOrZero(n : BigDecimal) : BigDecimal = if (n <= BigDecimal.ZERO) n.abs() else BigDecimal.ZERO
    for (i in numTradingDaysForCalc+1..this.lastIndex) {

        val calculationPeriod = this.subList(i-numTradingDaysForCalc,i)
        val positiveDays = calculationPeriod.filter { it.delta > BigDecimal.ZERO }
        val avgPositiveDays = when{
            positiveDays.isEmpty() -> BigDecimal.ZERO
            else -> BigDecimal(positiveDays.map { it.delta.toDouble() }.average())
        }

        val negativeDays = calculationPeriod.filter { it.delta <= BigDecimal.ZERO }
        val avgNegativeDays = when {
            negativeDays.isEmpty() -> BigDecimal.ZERO
            else -> BigDecimal(negativeDays.map { it.delta.toDouble().absoluteValue }.average())
        }


        val avgGain = BigDecimal(numTradingDaysForCalc-1).multiply(avgPositiveDays).plus(positiveNumOrZero(this[i].delta)).divide(
            BigDecimal(numTradingDaysForCalc)
        )
        val avgLoss = BigDecimal(numTradingDaysForCalc-1).multiply(avgNegativeDays).plus(negativeNumOrZero(this[i].delta)).divide(
            BigDecimal(numTradingDaysForCalc)
        )

        if (avgLoss > BigDecimal.ZERO){
            val RS = avgGain.divide(avgLoss,BIGDECIMAL_SCALE, RoundingMode.HALF_UP)
            val RSI = BigDecimal(100).minus(BigDecimal(100).divide(BigDecimal.ONE.plus(RS),BIGDECIMAL_SCALE, RoundingMode.HALF_UP))

            this[i].momentum = RSI

        } else {
            this[i].momentum = BigDecimal(100)
        }
    }
}

fun List<TradingPeriod>.calculateStochastic(numTradingDaysForCalc : Int = 14) {

    for (i in numTradingDaysForCalc+1..this.lastIndex) {

        val calculationPeriod = this.subList(i-(numTradingDaysForCalc-1),i+1)
        val C = this[i].close
        val H14 = calculationPeriod.maxOf { it.close }
        val L14 = calculationPeriod.minOf { it.close }

        if (L14 == H14){
            this[i].stochastic = BigDecimal(50)
        } else {
//            this[i].stochastic = (C - L14).divide(H14 - L14,BIGDECIMAL_SCALE, RoundingMode.HALF_UP).multiply(BigDecimal(100))
            this[i].stochastic = ((C - L14).multiply(BigDecimal(100)).divide(H14 - L14,BIGDECIMAL_SCALE, RoundingMode.HALF_UP))
        }

    }
}

/**
 * will scale volume in the range of 0..1
 */
fun List<TradingPeriod>.scaleVolume() {
    val Xmin = 0
    val Xmax = BigDecimal(this.maxOf { it.volume })
    for (i in 1..this.lastIndex) {
        this[i].scaledVolume = BigDecimal(this[i].volume - Xmin).divide(BigDecimal(this[i].volume).plus(Xmax),BIGDECIMAL_SCALE, RoundingMode.HALF_UP)
    }
}

/**
 * will scale trend in the range of 0..1
 */
fun List<TradingPeriod>.scaleTrend() {
    val Xmin = BigDecimal.ZERO
    val Xmax = BigDecimal(this.maxOf { it.trend.toInt() })
    for (i in 1..this.lastIndex) {
        this[i].scaledTrend = (this[i].trend - Xmin).divide(this[i].trend.plus(Xmax),BIGDECIMAL_SCALE, RoundingMode.HALF_UP)
    }
}

/**
 * returns mean & standard deviation
 */
fun sd(data: List<BigDecimal>): TradingPeriodStatistics {
    val mean = data.map { it.toDouble() }.average()
    val sd = data.map { it.toDouble() }
        .reduce { acc, it -> acc + (mean - it).pow(2) }
        .div(data.size)
        .pow(0.5)
    return TradingPeriodStatistics(BigDecimal(mean).setScale(BIGDECIMAL_SCALE, RoundingMode.HALF_UP),
                                        BigDecimal(sd).setScale(BIGDECIMAL_SCALE, RoundingMode.HALF_UP))
}

/**
 * Determines which week num should be used for the given LocalDate
 */
fun weekNumForDate(dt: LocalDate): Int {
    return (dt.dayOfYear - firstMondayOfTheYear(dt)) / 7 + 1
}

fun firstMondayOfTheYear(dt: LocalDate): Int {
    for (i in 1..7) { // check first 7 days, which is monday.
        if (LocalDate(dt.year, Month.JANUARY,i).dayOfWeek == DayOfWeek.MONDAY) return i
    }
    error("shouldn't get yo this pont")
}


fun getCriticalPoints(list: List<TradingDay>, minimalWaveDelta: BigDecimal) : List<TradingDay> {
    val workingList = list.toList().sortedBy { it.startingDate }

    fun determineFirstElementMinMax() : Indicator {
        for (i in 1..workingList.lastIndex){
            val firstDelta = workingList[i].close.minus(workingList[0].close)

            if (firstDelta > BigDecimal.ZERO && firstDelta >= minimalWaveDelta) return Indicator.MIN
            if (firstDelta < BigDecimal.ZERO && firstDelta.abs() >= minimalWaveDelta) return Indicator.MAX
        }
        error("waveDeltaRange [$minimalWaveDelta] is too big. can't determine MIN/MAX")
    }
    val stack = Stack<TradingDay>()

    workingList[0].also { it.tradingLabelsList.add(determineFirstElementMinMax()) }
    stack.push(workingList[0])
    workingList.drop(1).forEach { it ->
        when  {
            stack.peek().tradingLabelsList.contains(Indicator.MIN) -> {
                when {
                    stack.peek().close > it.close -> {
                        stack.pop()
                        it.tradingLabelsList.add(Indicator.MIN)
                        stack.push(it)
                    }

                    stack.peek().close + minimalWaveDelta <= it.close -> {
                        it.tradingLabelsList.add(Indicator.MAX)
                        stack.push(it)
                    }
                }
            }
            stack.peek().tradingLabelsList.contains(Indicator.MAX) -> {
                when {
                    stack.peek().close < it.close -> {
                        stack.pop()
                        it.tradingLabelsList.add(Indicator.MAX)
                        stack.push(it)
                    }
                    stack.peek().close - minimalWaveDelta >= it.close -> {
                        it.tradingLabelsList.add(Indicator.MIN)
                        stack.push(it)
                    }
                }
            }
            else -> error("NOT SUPPORTED")
        }
    }
    return stack.toList()
}


fun List<TradingPeriod>.identifyCriticalPoints(minimalWaveDelta: BigDecimal){
    fun determineFirstElementMinMax() : Indicator {
        for (i in 1..this.lastIndex){
            val firstDelta = this[i].close.minus(this[0].close)

            if (firstDelta > BigDecimal.ZERO && firstDelta >= minimalWaveDelta) return Indicator.MIN
            if (firstDelta < BigDecimal.ZERO && firstDelta.abs() >= minimalWaveDelta) return Indicator.MAX
        }
        error("waveDeltaRange [$minimalWaveDelta] is too big. can't determine MIN/MAX")
    }
    val stack = Stack<TradingPeriod>()

    this[0].also { it.tradingLabelsList.add(determineFirstElementMinMax()) }
    stack.push(this[0])
    this.drop(1).forEach { it ->
        when  {
            stack.peek().tradingLabelsList.contains(Indicator.MIN) -> {
                when {
                    stack.peek().close > it.close -> {
                        stack.pop()
                        it.tradingLabelsList.add(Indicator.MIN)
                        stack.push(it)
                    }

                    stack.peek().close + minimalWaveDelta <= it.close -> {
                        it.tradingLabelsList.add(Indicator.MAX)
                        stack.push(it)
                    }
                }
            }
            stack.peek().tradingLabelsList.contains(Indicator.MAX) -> {
                when {
                    stack.peek().close < it.close -> {
                        stack.pop()
                        it.tradingLabelsList.add(Indicator.MAX)
                        stack.push(it)
                    }
                    stack.peek().close - minimalWaveDelta >= it.close -> {
                        it.tradingLabelsList.add(Indicator.MIN)
                        stack.push(it)
                    }
                }
            }
            else -> error("NOT SUPPORTED")
        }
    }

    stack.forEach { minMax -> this.first { it.startingDate == minMax.startingDate }
        .tradingLabelsList.add(minMax.tradingLabelsList.first { it == Indicator.MAX || it == Indicator.MIN }) }
}




fun List<TradingPeriod>.identifyEveningStarReversals(){
    for (i in 2..this.lastIndex){
        if (this[i-2].open < this[i-2].close && this[i-1].open > this[i-1].close && this[i-1].close > this[i-2].close && this[i].close < this[i].open && this[i].open < this[i-1].close){
            this[i].tradingLabelsList.add(Indicator.EveningStarBullishReversal)
        }
        if (this[i-2].open > this[i-2].close && this[i-1].open < this[i-1].close && this[i-1].close < this[i-2].close && this[i].close > this[i].open && this[i].open > this[i-1].close){
            this[i].tradingLabelsList.add(Indicator.EveningStarBearishReversal)
        }
    }
}

fun List<TradingPeriod>.identifyOutsideReversals(){
    for (i in 1..this.lastIndex){
        if (this[i].open < this[i-1].open && this[i].close > this[i-1].close){
            // Turning from bearish to bullish
            this[i].tradingLabelsList.add(Indicator.BullishOutsideReversal)
        }
        if (this[i].open > this[i-1].open && this[i].close < this[i-1].close){
            // Turning from bullish to bearish
            this[i].tradingLabelsList.add(Indicator.BearishOutsideReversal)
        }
    }
}

fun List<TradingPeriod>.identifyCandlePattern(){
    for (i in 1..this.lastIndex){
        if (this[i].open < this[i].close && (this[i].close - this[i].low) >  (this[i].close - this[i].open).multiply(BigDecimal(2))  ){
            // Turning from bearish to bullish
            this[i].tradingLabelsList.add(Indicator.BullishCandle)
        }
        if (this[i].open > this[i].close && (this[i].high - this[i].close) >  (this[i].open - this[i].close).multiply(BigDecimal(2))  ){
            // Turning from bearish to bullish
            this[i].tradingLabelsList.add(Indicator.BearishCandle)
        }
    }
}

fun List<TradingPeriod>.identifyReversals(){
    val localBearishReversals : MutableList<BigDecimal> = mutableListOf()
    val localBullishReversals : MutableList<BigDecimal> = mutableListOf()

    for (i in 1..this.lastIndex){
        val potentialBullishReversalPont = listOfBullishReversals.intersect(this[i].tradingLabelsList).size
        val potentialBearishReversalPont = listOfBearishReversals.intersect(this[i].tradingLabelsList).size
        when {
            potentialBullishReversalPont > potentialBearishReversalPont -> localBullishReversals.add(this[i].close)
            potentialBullishReversalPont < potentialBearishReversalPont -> localBearishReversals.add(this[i].close)
            else -> println(">>> Both bullish & bearish signal, do nothing")
        }

        if (localBullishReversals.map { it.plusOnePercent() }.any { it < this[i].close }){
            this[i].bullishElectedFlag = true
            localBullishReversals.removeIf { it.plusOnePercent() < this[i].close}
        }
        if (localBearishReversals.map { it.minusOnePercent() }.any { it > this[i].close }){
            this[i].bearishElectedFlag = true
            localBearishReversals.removeIf { it.minusOnePercent() > this[i].close}
        }

        localBullishReversals.sorted()
        localBearishReversals.sortDescending()
        this[i].bearishReversals.addAll(localBearishReversals)
        this[i].bullishReversals.addAll(localBullishReversals)
    }
}


/**
 * converts any NUM to Double in range [0,1]
 */
fun sig(x : BigDecimal) = 1 / (1 + exp(-x.toDouble()))

/**
 * tanh converts any NUM to Double in range [-1,1]
 */
fun tanh(x : BigDecimal) : BigDecimal {
    val xD = x.toDouble()
    val resltDouble = (exp(xD) - exp(-xD)) / (exp(xD) + exp(-xD))
    return  BigDecimal(resltDouble)
        .divide(BigDecimal.ONE, BIGDECIMAL_SCALE, RoundingMode.HALF_UP) // divide Just to round
}

/**
 * get list of BigDecimal, returns list of deltas (x[i+1] - x[i])
 */
fun getDeltas(lst : List<BigDecimal>) : List<BigDecimal> = lst.windowed(2,1).map { it[1] - it[0] }.map { it.divide(BigDecimal.ONE, BIGDECIMAL_SCALE, RoundingMode.HALF_UP) }

/**
 * returns list of deltas (x[i+1] - x[i]) in Percentage terms
 */
fun getDeltasPercentage(lst : List<BigDecimal>) : List<BigDecimal> = lst.windowed(2,1).map { (it[1] - it[0]).multiply(BigDecimal(100)).divide(it[0], BIGDECIMAL_SCALE, RoundingMode.HALF_UP) }

fun LocalDate.subtractWorkingDays(daysToSubtract : Int) : LocalDate {
    var result: LocalDate = LocalDate.parse(this.toString()) // Just a copy of current LocalDate

    var subtractedDays = 0
    while (subtractedDays < daysToSubtract) {
        result = result.minus(1,DateTimeUnit.DAY)
        if (!(result.dayOfWeek === DayOfWeek.SATURDAY || result.dayOfWeek === DayOfWeek.SUNDAY)) {
            ++subtractedDays
        }
    }
    return result
}

fun LocalDate.addWorkingDays(daysToAdd : Int) : LocalDate {
    var result: LocalDate = LocalDate.parse(this.toString()) // Just a copy of current LocalDate

    var addedDays = 0
    while (addedDays < daysToAdd) {
        result = result.plus(1,DateTimeUnit.DAY)
        if (!(result.dayOfWeek === DayOfWeek.SATURDAY || result.dayOfWeek === DayOfWeek.SUNDAY)) {
            ++addedDays
        }
    }
    return result
}


/*
fun main() {
    val t1 = TradingDay(startingDate = java.time.LocalDate.now().toKotlinLocalDate(), close = BigDecimal(30.00), open = BigDecimal.ZERO, high = BigDecimal.ZERO, low = BigDecimal.ZERO)
    val t2 = TradingDay(startingDate = java.time.LocalDate.now().toKotlinLocalDate(), close = BigDecimal(28.16), open = BigDecimal.ZERO, high = BigDecimal.ZERO, low = BigDecimal.ZERO)
    val t3 = TradingDay(startingDate = java.time.LocalDate.now().toKotlinLocalDate(), close = BigDecimal(30.16), open = BigDecimal.ZERO, high = BigDecimal.ZERO, low = BigDecimal.ZERO)
    val t4 = TradingDay(startingDate = java.time.LocalDate.now().toKotlinLocalDate(), close = BigDecimal(29.85), open = BigDecimal.ZERO, high = BigDecimal.ZERO, low = BigDecimal.ZERO)
    val t5 = TradingDay(startingDate = java.time.LocalDate.now().toKotlinLocalDate(), close = BigDecimal(30.52), open = BigDecimal.ZERO, high = BigDecimal.ZERO, low = BigDecimal.ZERO)
    val lst = listOf<Double>(30.00, 28.16, 30.16, 29.85, 30.52	)

    println(getDeltasPercentage(lst.map { BigDecimal(it) }))
    println(getDeltas(lst.map { BigDecimal(it) }))
    println(t2.deltaPercentage(t1))
    println(t3.deltaPercentage(t2))
    println(t4.deltaPercentage(t3))
    println(t5.deltaPercentage(t4))
}*/
