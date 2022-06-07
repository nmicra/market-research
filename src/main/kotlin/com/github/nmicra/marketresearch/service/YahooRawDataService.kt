package com.github.nmicra.marketresearch.service

import com.github.nmicra.marketresearch.entity.MarketRaw
import com.github.nmicra.marketresearch.utils.JavaLocalDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.annotations.ColumnName
import org.jetbrains.kotlinx.dataframe.api.toListOf
import org.jetbrains.kotlinx.dataframe.io.read
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@Service
class YahooRawDataService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun tradingDataForPeriod(ticker : String, fromLD : JavaLocalDate, toLD : JavaLocalDate) : List<MarketRaw>{

        val from = fromLD.toEpochDay() * 24 *60 *60
        val to = toLD.toEpochDay() * 24 *60 *60

        val tmpFileCsv = File.createTempFile("yahoo-raw-${ticker}-${System.currentTimeMillis()}", ".csv")
            .also { it.deleteOnExit() }

        logger.info("retrieving rad data from Yahoo, ticker=$ticker from=$from, to=$to")
        val client = OkHttpClient()
        val request = Request.Builder().url("https://query1.finance.yahoo.com/v7/finance/download/${ticker}?period1=${from}&period2=${to}&interval=1d&events=history")
            .addHeader("Accept", "*/*")
            .addHeader("cache-Control", "no-cache")
            .addHeader("connection", "keep-alive")
            .addHeader("accept-encoding", "gzip,csv,deflate")
            .addHeader("Content-Type", "application/octet-stream")
            .addHeader("Content-Transfer-Encoding", "Binary")
            .build()
        val response =client.newCall(request).execute()
        tmpFileCsv.writeBytes(response.body!!.byteStream().readAllBytes())
        response.body?.close()

        return DataFrame.read(tmpFileCsv.absolutePath)
            .toListOf<YahooTradingDay>().map { it.toMarketRaw(ticker) }
    }
}
//MarketRaw
data class YahooTradingDay(
    @ColumnName("Date") val date: LocalDate,
    @ColumnName("Open") val open: Double,
    @ColumnName("High") val high: Double,
    @ColumnName("Low") val low: Double,
    @ColumnName("Close") val close: Double,
    @ColumnName("Adj Close") val adj: Double,
    val Volume: Long
)

fun YahooTradingDay.toMarketRaw(ticker : String) : MarketRaw = MarketRaw(label = ticker, date = date.toJavaLocalDate(), close = close, open = open, high = high, low = low, volume = Volume)