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
}

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