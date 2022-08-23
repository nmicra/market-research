package com.github.nmicra.marketresearch.ml.predictor

import java.io.File
import java.nio.charset.Charset
import org.tribuo.Dataset
import org.tribuo.Feature
import org.tribuo.MutableDataset
import org.tribuo.data.csv.CSVLoader
import org.tribuo.data.csv.CSVSaver
import org.tribuo.evaluation.TrainTestSplitter
import org.tribuo.impl.ArrayExample
import org.tribuo.regression.RegressionFactory
import org.tribuo.regression.Regressor
import org.tribuo.regression.xgboost.XGBoostRegressionTrainer

class XGBoostTimeSeriesPredictor {

    val xgb = XGBoostRegressionTrainer(50)

    val regressionFactory =  RegressionFactory()

    var csvSaver = CSVSaver()

    var csvLoader = CSVLoader<Regressor>(regressionFactory)

    fun predict(dataList: List<Double>, timeUnitsForModelling : Int = 10, timeUnitsToPredict : Int = 5 ) : List<Double>{
        if ((timeUnitsForModelling+1) > dataList.size) error("invalid size")
        val trainData: MutableDataset<Regressor> = MutableDataset(null, regressionFactory)

        for (i in 0..dataList.size-(timeUnitsForModelling+1)) {
//            val regressionLst = dataList.subList(i,timeUnitsForModelling).mapIndexed { index, dVal -> com.oracle.labs.mlrg.olcut.util.Pair("elm${timeUnitsForModelling-index}",dVal)}.toMutableList()
//            val example1 = ArrayExample<Regressor>(Regressor.createFromPairList(regressionLst))

            val exampleLst = dataList.subList(i,timeUnitsForModelling+i+1)
            val example1 = ArrayExample<Regressor>(Regressor("next", exampleLst.last()))
            for (j in 0 until exampleLst.lastIndex){
                example1.add(Feature("elm${j}",exampleLst[j]))
            }
            trainData.add(example1)
        }


        var xgbModel = train("XGBoost",xgb,trainData).first


        val regressionLst2 = dataList.dropLast(timeUnitsForModelling).mapIndexed { index, dVal -> com.oracle.labs.mlrg.olcut.util.Pair("elm${timeUnitsForModelling-index}",dVal)}.toMutableList()
        val example2 = ArrayExample<Regressor>(Regressor.createFromPairList(regressionLst2))
//        predictDS.add(example2)

//        evaluate(xgbModel,evalData);

//        val example1 =  ArrayExample<Regressor>(Regressor("DIM-0",Double.NaN))
        val prediction = xgbModel.predict(example2)
        return listOf(prediction.output.toString().toDouble())
    }






    fun predictWithCSV(dataList: List<Double>, timeUnitsForModelling : Int = 10, timeUnitsToPredict : Int = 5 ) : List<Double>{
        if ((timeUnitsForModelling+1) > dataList.size) error("invalid size")
        // CREATE TEMP CSV
        val tmpFile = File.createTempFile("tribuo-xboost-timeseries-${System.currentTimeMillis()}", ".csv")
            .also { it.deleteOnExit() }

        println(">>> csvPath: ${tmpFile.absolutePath}")
        val csvHeader = (0 until timeUnitsForModelling).joinToString(",") { "\"elm${it}\"" } + ",\"next\"\r\n"
        tmpFile.writeText(csvHeader, Charset.defaultCharset())


        for (i in 0..dataList.size-(timeUnitsForModelling+1)) {
            val exampleLst = dataList.subList(i,timeUnitsForModelling+i+1)
            tmpFile.appendText(exampleLst.joinToString(",") { it.toString() } + "\r\n")
        }


        val seriesSource = csvLoader.loadDataSource(tmpFile.toPath(),"next")
        val splitter = TrainTestSplitter(seriesSource, 0.9, 0L)
        val trainData: Dataset<Regressor?> = MutableDataset(splitter.train)
        val evalData: Dataset<Regressor> = MutableDataset(splitter.test)


        var xgbModel = train("XGBoost",xgb,trainData)
        evaluate(xgbModel.first,evalData)



        val predictions = mutableListOf<Double>()

        for (i in 0 until timeUnitsToPredict){
            val predictionExample: ArrayExample<Regressor> = ArrayExample<Regressor>(trainData.outputs.first())
            val exampleToRunOn = dataList.subList(dataList.lastIndex - (timeUnitsForModelling-i),dataList.lastIndex).toMutableList()
            exampleToRunOn.addAll(predictions)
            exampleToRunOn.forEachIndexed{index, d ->  predictionExample.add("elm${index}",d)}
            val prediction = xgbModel.first.predict(predictionExample)
            predictions.add(prediction.output!!.values[0])
        }

        return predictions
    }
}