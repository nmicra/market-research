package com.github.nmicra.marketresearch.service.report

import com.github.nmicra.marketresearch.analysis.*
import com.github.nmicra.marketresearch.general.BIGDECIMAL_SCALE
import kotlinx.datetime.toJavaLocalDate
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class LLHHReportService {

    val BIG_DECIMAL_NOT_FOUND = BigDecimal(-99999)

    fun createLLHHReport(rawData : List<TradingPeriod>, includeNextLLHHDistance : Boolean = true) : List<String> {
        val resultsList = mutableListOf<String>()
//        val criticals = identifyAndLabelCriticals(rawData.map { it.close.toDouble() }).sortedBy { it.index }
        val criticals = identifyAndLabelCriticals2(rawData.map { Pair(it.close.toDouble(), it.startingDate.toJavaLocalDate()) })
            .filterNot { it.date == null }
            .sortedBy { it.index }
        var ind = firstIndexContainsAllHHLLs(criticals) + 1
        val lastIndex = criticals.maxOf { it.index }/*NOTE do not use criticals.lastIndex */

        while (ind <=lastIndex) {
            val criticalDate : LocalDate = criticals.firstOrNull { it.index == ind }?.date ?: run {
                                                                            val lastKnownIndex = criticals.filter { it.index < ind }.maxOf { it.index }
                                                                            val delta = (ind - lastKnownIndex).toLong()
                                                                            val calculatedDate = when(rawData[0]){
                                                                              is TradingDay -> criticals.first { it.index==lastKnownIndex }.date!!.plusDays(delta)
                                                                              is TradingWeek -> criticals.first { it.index==lastKnownIndex }.date!!.plusWeeks(delta)
                                                                              is TradingMonth -> criticals.first { it.index==lastKnownIndex }.date!!.plusMonths(delta)
                                                                              is TradingYear -> criticals.first { it.index==lastKnownIndex }.date!!.plusYears(delta)
                                                                            }
                                                                            calculatedDate
                                                                        }
            /* in rare cases we might get price -99999, when calculated date is an official holiday, then I should take the next trading DAY, but currently it's not that important */
            val criticalClosePrice = rawData.firstOrNull { it.startingDate.toJavaLocalDate() == criticalDate }?.close ?: -99999
            val currentIndexClassification = criticals.filter { it.index == ind }
                .map { it.hhll.toString() }.firstOrNull()
                ?: "NONE"
            val distanceToHH = ind - criticals.filter { it.index <= ind }
                .filter { it.hhll == HHLLClassification.HH }
                .maxOf { it.index }
            val distanceToLL = ind - criticals.filter { it.index <= ind }
                .filter { it.hhll == HHLLClassification.LL }
                .maxOf { it.index }

            val distanceToLH = ind - criticals.filter { it.index <= ind }
                .filter { it.hhll == HHLLClassification.LH }
                .maxOf { it.index }

            val distanceToHL = ind - criticals.filter { it.index <= ind }
                .filter { it.hhll == HHLLClassification.HL }
                .maxOf { it.index }

            val (nextLLHHClassification,nextLLHHDistance)= when {
                ind == lastIndex -> Pair(HHLLClassification.QUERY, -999)
                else -> {
                    val nextLLHH = criticals.first { it.index > ind }
                    Pair(nextLLHH.hhll, nextLLHH.index - ind)
                }
            }

//            val nextLLHH = criticals.first { it.index > ind }
//            val nextLLHHClassification = nextLLHH.hhll
//            val nextLLHHDistance = nextLLHH.index - ind
            //println("currentIndexClassification=$currentIndexClassification,distanceToHH=$distanceToHH,distanceToLL=$distanceToLL,distanceToLH=$distanceToLH,distanceToHL=$distanceToHL,nextLLHHClassification=$nextLLHHClassification,nextLLHHDistance=$nextLLHHDistance")
            when(includeNextLLHHDistance){
                true -> resultsList.add("${criticalDate},${criticalClosePrice},$currentIndexClassification,$distanceToHH,$distanceToLL,$distanceToLH,$distanceToHL,$nextLLHHClassification,$nextLLHHDistance\r\n")
                else -> resultsList.add("${criticalDate},${criticalClosePrice},$currentIndexClassification,$distanceToHH,$distanceToLL,$distanceToLH,$distanceToHL,$nextLLHHClassification\r\n")
            }

            ind++
        }
        return resultsList
    }

    fun createLLHHReportWithMV(rawData : List<TradingPeriod>, includeNextLLHHDistance : Boolean = true) : List<String> {
        rawData.calculateMA(5)
        rawData.calculateMA(7)
        val workingData = rawData.filter { it.movingAvg.contains(7) }
        val resultsList = mutableListOf<String>()
//        val criticals = identifyAndLabelCriticals(rawData.map { it.close.toDouble() }).sortedBy { it.index }
        val criticals = identifyAndLabelCriticals2(workingData.map { Pair(it.close.toDouble(), it.startingDate.toJavaLocalDate()) })
            .filterNot { it.date == null }
            .sortedBy { it.index }
        var ind = firstIndexContainsAllHHLLs(criticals) + 1

        val lastIndex = criticals.maxOf { it.index }/*NOTE do not use criticals.lastIndex */
        while (ind <= lastIndex) {
            val criticalDate : LocalDate = criticals.firstOrNull { it.index == ind }?.date ?: run {
                val lastKnownIndex = criticals.filter { it.index < ind }.maxOf { it.index }
                val delta = (ind - lastKnownIndex).toLong()
                val calculatedDate = when(workingData[0]){
                    is TradingDay -> criticals.first { it.index==lastKnownIndex }.date!!.plusDays(delta)
                    is TradingWeek -> criticals.first { it.index==lastKnownIndex }.date!!.plusWeeks(delta)
                    is TradingMonth -> criticals.first { it.index==lastKnownIndex }.date!!.plusMonths(delta)
                    is TradingYear -> criticals.first { it.index==lastKnownIndex }.date!!.plusYears(delta)
                }
                calculatedDate
            }
            /* in rare cases we might get price -99999, when calculated date is an official holiday, then I should take the next trading DAY, but currently it's not that important */
            val rawTradingPeriod = workingData.firstOrNull { it.startingDate.toJavaLocalDate() == criticalDate }
            val criticalClosePrice = rawTradingPeriod?.close ?: BIG_DECIMAL_NOT_FOUND
            val mv5Dev = if (criticalClosePrice == BIG_DECIMAL_NOT_FOUND) BIG_DECIMAL_NOT_FOUND
                                else criticalClosePrice.divide(rawTradingPeriod!!.movingAvg[5], BIGDECIMAL_SCALE, RoundingMode.HALF_UP)
            val mv7Dev = if (criticalClosePrice == BIG_DECIMAL_NOT_FOUND) BIG_DECIMAL_NOT_FOUND
            else criticalClosePrice.divide(rawTradingPeriod!!.movingAvg[7], BIGDECIMAL_SCALE, RoundingMode.HALF_UP)

            val currentIndexClassification = criticals.filter { it.index == ind }
                .map { it.hhll.toString() }.firstOrNull()
                ?: "NONE"
            val distanceToHH = ind - criticals.filter { it.index <= ind }
                .filter { it.hhll == HHLLClassification.HH }
                .maxOf { it.index }
            val distanceToLL = ind - criticals.filter { it.index <= ind }
                .filter { it.hhll == HHLLClassification.LL }
                .maxOf { it.index }

            val distanceToLH = ind - criticals.filter { it.index <= ind }
                .filter { it.hhll == HHLLClassification.LH }
                .maxOf { it.index }

            val distanceToHL = ind - criticals.filter { it.index <= ind }
                .filter { it.hhll == HHLLClassification.HL }
                .maxOf { it.index }

            val (nextLLHHClassification,nextLLHHDistance)= when {
                ind == lastIndex -> Pair(HHLLClassification.QUERY, -999)
                else -> {
                    val nextLLHH = criticals.first { it.index > ind }
                    Pair(nextLLHH.hhll, nextLLHH.index - ind)
                }
            }


            when(includeNextLLHHDistance){
                true -> resultsList.add("${criticalDate},${criticalClosePrice},${mv5Dev},${mv7Dev},$currentIndexClassification,$distanceToHH,$distanceToLL,$distanceToLH,$distanceToHL,$nextLLHHClassification,$nextLLHHDistance\r\n")
                else -> resultsList.add("${criticalDate},${criticalClosePrice},${mv5Dev},${mv7Dev},$currentIndexClassification,$distanceToHH,$distanceToLL,$distanceToLH,$distanceToHL,$nextLLHHClassification\r\n")
            }

            ind++
        }
        return resultsList
    }

    private fun identifyAndLabelCriticals2(closePriceParam: List<Pair<Double,LocalDate>>) : List<TradingHHLLClassification> {
        check(closePriceParam.size > 10) {"list is too shrt"}
        val lst = closePriceParam.map { it.toHHLL() }
        val closePrice = closePriceParam.map { it.first }

        val criticals  = when { // initial setup of first 2 nodes, either (HH,HL or LL,LH)
            closePrice[0] > closePrice[1] -> mutableListOf(TradingHHLLClassification(closePrice[0],0,HHLLClassification.HH), TradingHHLLClassification(closePrice[1],1,HHLLClassification.HL))
            else -> mutableListOf(TradingHHLLClassification(closePrice[0],0,HHLLClassification.LL), TradingHHLLClassification(closePrice[1],1,HHLLClassification.LH))
        }

        for ((ind,trading) in lst.drop(2).withIndex() ){
            when(criticals.last().hhll){
                HHLLClassification.HL -> {
                    if (trading.close < criticals.last().close){
                        criticals.add(trading.copy(hhll = HHLLClassification.LL, index = ind+2/* drop 2 in the loop*/))
                    } else if(trading.close > criticals[criticals.lastIndex-1].close) {
                        criticals.add(trading.copy(hhll = HHLLClassification.LH, index = ind+2/* drop 2 in the loop*/))
                    }
                }
                HHLLClassification.LH -> {
                    if (trading.close > criticals.last().close){
                        criticals.add(trading.copy(hhll = HHLLClassification.HH, index = ind+2/* drop 2 in the loop*/))
                    } else if(trading.close < criticals[criticals.lastIndex-1].close) {
                        criticals.add(trading.copy(hhll = HHLLClassification.HL, index = ind+2/* drop 2 in the loop*/))
                    }
                }
                HHLLClassification.LL -> {
                    if (trading.close < criticals.last().close){
                        criticals.add(trading.copy(hhll = HHLLClassification.LL, index = ind+2/* drop 2 in the loop*/))
                    } else if(trading.close > criticals[criticals.lastIndex-1].close) {
                        criticals.add(trading.copy(hhll = HHLLClassification.LH, index = ind+2/* drop 2 in the loop*/))
                    }
                }
                HHLLClassification.HH -> {
                    if (trading.close > criticals.last().close){
                        criticals.add(trading.copy(hhll = HHLLClassification.HH, index = ind+2/* drop 2 in the loop*/))
                    } else if(trading.close < criticals[criticals.lastIndex-1].close) {
                        criticals.add(trading.copy(hhll = HHLLClassification.HL, index = ind+2/* drop 2 in the loop*/))
                    }
                }
                else -> error("not supported state $trading")
            }
        }
        return criticals
    }

    private fun identifyAndLabelCriticals(closePrice: List<Double>) : List<TradingHHLLClassification> {
        check(closePrice.size > 10) {"list is too shrt"}
        val lst = closePrice.map { it.toHHLL() }
        val criticals  = when {
            closePrice[0] > closePrice[1] -> mutableListOf(TradingHHLLClassification(closePrice[0],0,HHLLClassification.HH), TradingHHLLClassification(closePrice[1],1,HHLLClassification.HL))
                else -> mutableListOf(TradingHHLLClassification(closePrice[0],0,HHLLClassification.LL), TradingHHLLClassification(closePrice[1],1,HHLLClassification.LH))
        }

        for ((ind,trading) in lst.drop(2).withIndex() ){
            when(criticals.last().hhll){
                HHLLClassification.HL -> {
                    if (trading.close < criticals.last().close){
                        criticals.add(trading.copy(hhll = HHLLClassification.LL, index = ind+2/* drop 2 in the loop*/))
                    } else if(trading.close > criticals[criticals.lastIndex-1].close) {
                        criticals.add(trading.copy(hhll = HHLLClassification.LH, index = ind+2/* drop 2 in the loop*/))
                    }
                }
                HHLLClassification.LH -> {
                    if (trading.close > criticals.last().close){
                        criticals.add(trading.copy(hhll = HHLLClassification.HH, index = ind+2/* drop 2 in the loop*/))
                    } else if(trading.close < criticals[criticals.lastIndex-1].close) {
                        criticals.add(trading.copy(hhll = HHLLClassification.HL, index = ind+2/* drop 2 in the loop*/))
                    }
                }
                HHLLClassification.LL -> {
                    if (trading.close < criticals.last().close){
                        criticals.add(trading.copy(hhll = HHLLClassification.LL, index = ind+2/* drop 2 in the loop*/))
                    } else if(trading.close > criticals[criticals.lastIndex-1].close) {
                        criticals.add(trading.copy(hhll = HHLLClassification.LH, index = ind+2/* drop 2 in the loop*/))
                    }
                }
                HHLLClassification.HH -> {
                    if (trading.close > criticals.last().close){
                        criticals.add(trading.copy(hhll = HHLLClassification.HH, index = ind+2/* drop 2 in the loop*/))
                    } else if(trading.close < criticals[criticals.lastIndex-1].close) {
                        criticals.add(trading.copy(hhll = HHLLClassification.HL, index = ind+2/* drop 2 in the loop*/))
                    }
                }
                else -> error("not supported state $trading")
            }
        }
        return criticals
    }

    private fun firstIndexContainsAllHHLLs(criticalsLst: List<TradingHHLLClassification>): Int {
        check(criticalsLst.size >= 4) {"list is too short to contain all HH,HL,LL,LH values"}
        val criticlasMapped =  criticalsLst.map { it.hhll }
        var ind = 3
        while (ind < criticlasMapped.size) {
            if (criticlasMapped.subList(0,ind).containsAll(listOf(
                    HHLLClassification.HH,
                    HHLLClassification.HL, HHLLClassification.LL, HHLLClassification.LH))){
                return criticalsLst[ind].index
            }
            ind++
        }
        error("index not found")
    }
}


enum class HHLLClassification{HH,HL,LL,LH,NONE,QUERY}
data class TradingHHLLClassification(val close : Double, var index : Int = -1, var hhll : HHLLClassification = HHLLClassification.NONE, var date: LocalDate? = null)
fun Double.toHHLL() : TradingHHLLClassification = TradingHHLLClassification(this)
fun Pair<Double,LocalDate>.toHHLL() : TradingHHLLClassification = TradingHHLLClassification(close = this.first, date = this.second)