package com.github.nmicra.marketresearch.controller

import com.github.nmicra.marketresearch.analysis.*
import com.github.nmicra.marketresearch.entity.toTradingDay
import com.github.nmicra.marketresearch.ml.predictor.RegressionPrecision
import com.github.nmicra.marketresearch.ml.predictor.llhh.LLHHDistancePredictor
import com.github.nmicra.marketresearch.ml.predictor.llhh.LLHHMVPredictor
import com.github.nmicra.marketresearch.ml.predictor.llhh.LLHHPredictor
import com.github.nmicra.marketresearch.repository.MarketRawDataRepository
import com.github.nmicra.marketresearch.service.TradingPeriodService
import com.github.nmicra.marketresearch.service.report.LLHHReportService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.tribuo.Model
import org.tribuo.regression.Regressor

@RestController
class PredictController {

    @Autowired
    lateinit var marketRawDataRepository: MarketRawDataRepository

    @Autowired
    lateinit var LLHHReportService: LLHHReportService

    @Autowired
    lateinit var LLHHPredictor: LLHHPredictor

    @Autowired
    lateinit var LLHHMVPredictor: LLHHMVPredictor

    @Autowired
    lateinit var LLHHDistancePredictor: LLHHDistancePredictor

    @Autowired
    lateinit var tradingPeriodService : TradingPeriodService


    @GetMapping("/predict/llhh/{label}/{interval}")
    suspend fun exportLLHHReportToCsv(
        @PathVariable label: String,
        @PathVariable interval: String
    ): String {
        val tradingData = runBlocking { tradingPeriodService.tradingPeriodByInterval(interval,label) }

                // 1. Predict Classification
        val classificationReport = LLHHReportService.createLLHHReport(tradingData, false)
                                .map { it.split(",").drop(2).joinToString(",") }//drop first 2 columns which is date & closePrice

        val modelLLHH = LLHHPredictor.createClassificationModelLLHH(classificationReport.dropLast(1))
        val classificationQueryParams = classificationReport.last().split(",").map { it.removeSuffix("\r\n") }
        val classificationPrediction = LLHHPredictor.predictWithModel(modelLLHH, classificationQueryParams)

                // 2. Predict Distance / Regression
        val distanceReport = LLHHReportService.createLLHHReport(tradingData, true)
            .map { it.split(",").drop(2).joinToString(",") }//drop first 2 columns which is date

        val distanceLst = mutableListOf<Pair<Model<Regressor?>, RegressionPrecision>>()
        for (i in 10..100 step 10){
            distanceLst.add(LLHHDistancePredictor.createLLHHDistanceModel(distanceReport.dropLast(1),i))
        }

        // debug purposes
//        distanceLst.forEachIndexed { index, pair ->
//            println(">>> model $index >>> eval ${pair.second}") // debug purposes
//        }

        // lets choose the model with the lowest RMSE value
        val distanceModel = distanceLst.reduce {model1,model2 -> if (model1.second.rmse < model2.second.rmse) model1 else model2}

        val distanceQueryParam = (classificationQueryParams + listOf<String>(classificationPrediction.output.label.toString())).map { it.removeSuffix("\r\n") }
        val distancePrediction = LLHHDistancePredictor.predictWithModel(distanceModel.first, distanceQueryParam)
        return "Classification -> ${classificationPrediction.output.label} : ${classificationPrediction.output.score}, Distance ->  ${distancePrediction.output?.values!![0]}"
    }

    @GetMapping("/predict/llhhmv/{label}/{interval}")
    suspend fun exportLLHHMVReportToCsv(
        @PathVariable label: String,
        @PathVariable interval: String
    ): String {
        val tradingData = runBlocking { tradingPeriodService.tradingPeriodByInterval(interval,label) }

        // 1. Predict Classification
        val classificationReport = LLHHReportService.createLLHHReportWithMV(tradingData, false)
            .filterNot { it.contains("-99999") }
            .map { it.split(",").drop(2).joinToString(",") }//drop first 2 columns which is date & closePrice

        val modelLLHH = LLHHMVPredictor.createClassificationModelLLHH(classificationReport.dropLast(1))
        val classificationQueryParams = classificationReport.last().split(",").map { it.removeSuffix("\r\n") }
        val classificationPrediction = LLHHMVPredictor.predictWithModel(modelLLHH, classificationQueryParams)

        // 2. Predict Distance / Regression
        val distanceReport = LLHHReportService.createLLHHReportWithMV(tradingData, true)
            .map { it.split(",").drop(2).joinToString(",") }//drop first 2 columns which is date

        val distanceLst = mutableListOf<Pair<Model<Regressor?>, RegressionPrecision>>()
        for (i in 10..100 step 10){
            distanceLst.add(LLHHDistancePredictor.createLLHHDistanceModel(distanceReport.dropLast(1),i, mvIncluded = true))
        }

        // debug purposes
//        distanceLst.forEachIndexed { index, pair ->
//            println(">>> model $index >>> eval ${pair.second}") // debug purposes
//        }

        // lets choose the model with the lowest RMSE value
        val distanceModel = distanceLst.reduce {model1,model2 -> if (model1.second.rmse < model2.second.rmse) model1 else model2}

        val distanceQueryParam = (classificationQueryParams + listOf<String>(classificationPrediction.output.label.toString())).map { it.removeSuffix("\r\n") }
        val distancePrediction = LLHHDistancePredictor.predictWithModel(distanceModel.first, distanceQueryParam, true)
        return "Classification -> ${classificationPrediction.output.label} : ${classificationPrediction.output.score}, Distance ->  ${distancePrediction.output?.values!![0]}"
    }
}
