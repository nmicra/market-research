package com.github.nmicra.marketresearch.controller

import com.github.nmicra.marketresearch.analysis.*
import com.github.nmicra.marketresearch.entity.toTradingDay
import com.github.nmicra.marketresearch.repository.MarketRawDataRepository
import com.github.nmicra.marketresearch.utils.objectMapper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream
import java.math.BigDecimal

@RestController
class ExportController {

    @Autowired
    lateinit var marketRawDataRepository : MarketRawDataRepository

    /**
     * exports raw data to csv file
     * for a given label
     */
    @GetMapping("/export/raw/{label}")
    suspend fun exportRawToCsv(@PathVariable label : String) : ResponseEntity<StreamingResponseBody> {
        val header = HttpHeaders().also{
            it.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$label-${System.currentTimeMillis()}.csv")
            it.add("Cache-Control", "no-cache, no-store, must-revalidate")
            it.add("Pragma", "no-cache")
            it.add("Expires", "0")
        }

        val stream = StreamingResponseBody { out: OutputStream? ->
            runBlocking {
                marketRawDataRepository.findAllByLabel(label)
                    .map {
                      "${it.date},${it.close},${it.open},${it.low},${it.close}\n".byteInputStream()
                    }.onEach {
                    IOUtils.copy(it, out)
                }.collect()
            }
        }

        return ResponseEntity.ok()
            .headers(header)
            .contentType(MediaType.parseMediaType("application/octet-stream"))
            .body(stream)
    }

    @GetMapping("/export/reversals/{label}/{interval}")
    suspend fun exportReversalsToCsv(@PathVariable label : String, @PathVariable interval : String) : ResponseEntity<StreamingResponseBody> {
        println(">>> $interval")
        //TODO support interval: daily,weekly,monthly
        val daysData = marketRawDataRepository.findAllByLabel(label)
            .map { it.toTradingDay() }
            .toList()
            .sortedBy { it.startingDate }
            .toTradingWeeks()
        daysData.calculateDelta()
        daysData.scaleVolume()
        daysData.calculateTrend()
        daysData.scaleTrend()
        daysData.calculateMomentum()
        daysData.calculateStochastic()
        daysData.calculateMA(5)
        daysData.calculateMA(7)
        daysData.identifyOutsideReversals()
        daysData.identifyCandlePattern()
        daysData.identifyEveningStarReversals()
        val dailyList = daysData.filter { it.hasStochastic() } // Stochastic is for 14 days

        val header = HttpHeaders().also{
            it.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$label-reversals-${System.currentTimeMillis()}.csv")
            it.add("Cache-Control", "no-cache, no-store, must-revalidate")
            it.add("Pragma", "no-cache")
            it.add("Expires", "0")
        }


        val stream = StreamingResponseBody { out: OutputStream? ->
            runBlocking {
                dailyList.asFlow()
                    //date,open,high,low,close,volume,delta,intraVolatility
                    //trend,momentum,stochastic,bullishIndicators,bearishIndicators,neutralIndicators,additionalData
                    .map {
                        val mvgAvg = objectMapper.writeValueAsString(it.movingAvg).replace("\"","\"\"")
                        ("${it.year},${it.weekNr},${it.open},${it.high},${it.low},${it.close},${it.volume},${it.delta},${it.intraVolatility}" +
                          ",${it.trend},${it.momentum},${it.stochastic}" +
                                ",\"[${it.tradingLabelsList.filter { ind -> ind.isBullishIndicator()}.joinToString(",")}]\"" +
                                ",\"[${it.tradingLabelsList.filter { ind -> ind.isBearishIndicator()}.joinToString(",")}]\"" +
                                ",${it.movingAvg[5]},${it.movingAvg[7]}\n").byteInputStream()
//                                ",\"${mvgAvg}\"\n").byteInputStream()
                    }.onEach {
                        IOUtils.copy(it, out)
                    }.collect()
            }
        }

        return ResponseEntity.ok()
            .headers(header)
            .contentType(MediaType.parseMediaType("application/octet-stream"))
            .body(stream)
    }
}