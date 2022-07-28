package com.github.nmicra.marketresearch.integration

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.annotations.ColumnName
import org.jetbrains.kotlinx.dataframe.api.filter
import org.jetbrains.kotlinx.dataframe.api.filterBy
import org.jetbrains.kotlinx.dataframe.api.head
import org.jetbrains.kotlinx.dataframe.api.toListOf
import org.jetbrains.kotlinx.dataframe.io.read
import org.jetbrains.kotlinx.dataframe.size
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.okhttp3.OkHttpClient
import org.testcontainers.shaded.okhttp3.Request
import java.io.File
//import java.time.LocalDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus


class Temp {


//    @Test
//    fun test(){
//        val from = JavaLocalDate.of(2022,1,1).toEpochDay() * 24 *60 *60
//        val to = JavaLocalDate.now().toEpochDay() * 24 *60 *60
//        println("from=$from, to=$to")
//        val client = OkHttpClient()
//        val request = Request.Builder().url("https://query1.finance.yahoo.com/v7/finance/download/GOLD?period1=${from}&period2=${to}&interval=1d&events=history")
//            .addHeader("Accept", "*/*")
//            .addHeader("cache-Control", "no-cache")
//            .addHeader("connection", "keep-alive")
//            .addHeader("accept-encoding", "gzip,csv,deflate")
//            .addHeader("Content-Type", "application/octet-stream")
//            .addHeader("Content-Transfer-Encoding", "Binary")
//            .build()
//        val response =client.newCall(request).execute()
//        File("kuku.csv").writeBytes(response.body()!!.byteStream().readAllBytes())
//        response.body()?.close()
//    }

    @Test
    fun test2(){
        val df = DataFrame.read("kuku.csv")
//            .filter { it.get(Day::date).isAfter(LocalDate.of(2022,2,2)) }
        println(df.columnNames())
        println(df.columnTypes())
        println(df.size())
        println(df.head(5))

        df.filterBy("Date")
    }

    @Test
    fun test3(){
        val df = DataFrame.read("kuku.csv")
            .filter { it.get(Day::date).minus(LocalDate(2022,2,10)).days >0 }
            .toListOf<Day>()
        println(">>> ${df[0]}")
    }


    @Test
    fun test4(){
        /*
HH -> HH
HH -> LH
LH -> LL
LH -> HL
LL -> LL
LL -> HL
HL -> HH
HL -> LH
         */
        // Int in criticals is the DISTANCE to previous critical
       val criticals  = mutableListOf(TradingHHLLClassification(3.5,0,HHLLClassification.HH), TradingHHLLClassification(2.3,1,HHLLClassification.HL))

        val lst = listOf<Double>(3.5,2.3,3.4,3.3,3.6,3.4,3.3,5.1,4.3,4.5,3.3,3.0,3.12,3.15,3.1,2.9,2.8,2.7).map { it.toHHLL() }
        for ((ind,trading) in lst.drop(2).withIndex() ){
//            println("ind=$ind trd=$trading")
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
        criticals.subList(0,3).map { it.hhll }

        println("criticals are: $criticals")
        println("first index contains all: ${firstIndexContainsAllHHLLs(criticals)}")
    }


    fun Double.toHHLL() : TradingHHLLClassification = TradingHHLLClassification(this)

    // returns first index, which contains all HH,HL,LL,LH

      fun firstIndexContainsAllHHLLs(criticalsLst: List<TradingHHLLClassification>): Int{
//    fun firstIndexContainsAllHHLLs(criticalsLst : List<HHLLClassification>) : Int {
        check(criticalsLst.size >= 4) {"list is too short to contain all HH,HL,LL,LH values"}
        val criticlasMapped =  criticalsLst.map { it.hhll }
        var ind = 3
        while (ind < criticlasMapped.size) {
            if (criticlasMapped.subList(0,ind).containsAll(listOf(
                    HHLLClassification.HH,
                    HHLLClassification.HL, HHLLClassification.LL, HHLLClassification.LH))){
//                return criticalsLst.subList(0,ind).map { it.index }.sum()
                return criticalsLst[ind].index
            }
            ind++
        }
        error("index not found")
    }
}
//TODO move over list, make csv currentStatus(NONE,LL,LH), numPrevLL,numPrevHH,numPrevHL,numPrevLH, Y=>nextLLorHH
//TODO move over list, make csv currentStatus(NONE,LL,LH), numPrevLL,numPrevHH,numPrevHL,numPrevLH, nextLLorHH, Y=>distanceTo
enum class HHLLClassification{HH,HL,LL,LH,NONE}
data class TradingHHLLClassification(val close : Double, var index : Int = -1, var hhll : HHLLClassification = HHLLClassification.NONE)


data class Day(
    @ColumnName("Date") val date: LocalDate,
    @ColumnName("Open") val open: Double,
    @ColumnName("High") val high: Double,
    @ColumnName("Low") val low: Double,
    @ColumnName("Close") val close: Double,
    @ColumnName("Adj Close") val adj: Double,
    val Volume: Long
)

/*
val passengers = DataFrame.read("titanic.csv")
    .filter { it.get(Passenger::city).endsWith("NY") }
    .toListOf<Passenger>()
 */