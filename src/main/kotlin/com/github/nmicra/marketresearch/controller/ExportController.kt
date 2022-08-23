package com.github.nmicra.marketresearch.controller

import com.github.nmicra.marketresearch.analysis.*
import com.github.nmicra.marketresearch.entity.toTradingDay
import com.github.nmicra.marketresearch.repository.MarketRawDataRepository
import com.github.nmicra.marketresearch.service.TradingPeriodService
import com.github.nmicra.marketresearch.service.report.LLHHReportService
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

@RestController
class ExportController {

    @Autowired
    lateinit var marketRawDataRepository: MarketRawDataRepository

    @Autowired
    lateinit var LLHHReportService: LLHHReportService

    @Autowired
    lateinit var tradingPeriodService : TradingPeriodService

    /**
     * exports raw data to csv file
     * for a given label
     */
    @GetMapping("/export/raw/{label}")
    suspend fun exportRawToCsv(@PathVariable label: String): ResponseEntity<StreamingResponseBody> {
        val header = HttpHeaders().also {
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
    suspend fun exportReversalsToCsv(
        @PathVariable label: String,
        @PathVariable interval: String
    ): ResponseEntity<StreamingResponseBody> {

        val tradingData = runBlocking { tradingPeriodService.tradingPeriodByInterval(interval, label) }

        tradingData.calculateDelta()
        tradingData.scaleVolume()
        tradingData.calculateTrend()
        tradingData.scaleTrend()
        tradingData.calculateMomentum()
        tradingData.calculateStochastic()
        tradingData.calculateMA(5)
        tradingData.calculateMA(7)
        tradingData.identifyOutsideReversals()
        tradingData.identifyCandlePattern()
        tradingData.identifyEveningStarReversals()
        tradingData.identifyReversals()
        val dailyList = tradingData.filter { it.hasStochastic() } // Stochastic is for 14 days

        val header = HttpHeaders().also {
            it.add(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=$label-reversals-${System.currentTimeMillis()}.csv"
            )
            it.add("Cache-Control", "no-cache, no-store, must-revalidate")
            it.add("Pragma", "no-cache")
            it.add("Expires", "0")
        }


        val stream = StreamingResponseBody { out: OutputStream? ->
            runBlocking {
                dailyList.asFlow()
                    .onStart {
                        val csvHeader =
                            "year,weekNr,open,high,low,close,volume,delta,intraVolatility,trend,scaledTrend,momentum," +
                                    "stochastic,bullishIndicators,bearishIndicators,bullishReversals,bullishElectedFlag," +
                                    "bearishReversals,bearishElectedFlag,mavg5,mavg7\n"
                        IOUtils.copy(csvHeader.byteInputStream(), out)
                    }
                    .map {
                        ("${it.year},${it.weekNr},${it.open},${it.high},${it.low},${it.close},${it.volume},${it.delta},${it.intraVolatility}" +
                                ",${it.trend},${it.scaledTrend},${it.momentum},${it.stochastic}" +
                                ",\"[${
                                    it.tradingLabelsList.filter { ind -> ind.isBullishIndicator() }.joinToString(",")
                                }]\"" +
                                ",\"[${
                                    it.tradingLabelsList.filter { ind -> ind.isBearishIndicator() }.joinToString(",")
                                }]\"" +
                                ",${it.bullishReversals.reversed().joinToString(">")},${it.bullishElectedFlag}" +
                                ",${it.bearishReversals.joinToString(">")},${it.bearishElectedFlag}" +
                                ",${it.movingAvg[5]},${it.movingAvg[7]}\n").byteInputStream()
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

    @GetMapping("/export/llhh/{label}/{interval}")
    suspend fun exportLLHHReportToCsv(
        @PathVariable label: String,
        @PathVariable interval: String
    ): ResponseEntity<StreamingResponseBody> {

        val tradingData = runBlocking { tradingPeriodService.tradingPeriodByInterval(interval, label) }

//        val report = LLHHReportService.createLLHHReport(tradingData)
        val report = LLHHReportService.createLLHHReportWithMV(tradingData)

        val header = HttpHeaders().also {
            it.add(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=$label-LLHH-${System.currentTimeMillis()}.csv"
            )
            it.add("Cache-Control", "no-cache, no-store, must-revalidate")
            it.add("Pragma", "no-cache")
            it.add("Expires", "0")
        }


        val stream = StreamingResponseBody { out: OutputStream? ->
            runBlocking {
                report.asFlow()
                    .onStart {
                        val csvHeader =
                            "date,close,mv5Dev,mv7Dev,currentIndexClassification,distanceToHH,distanceToLL,distanceToLH,distanceToHL,nextLLHH,nextLLHHDistance\n"
                        IOUtils.copy(csvHeader.byteInputStream(), out)
                    }
                    .map {
//                        it.plus("\n").byteInputStream()
                        it.byteInputStream()
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