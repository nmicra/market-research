package com.github.nmicra.marketresearch.ml.predictor.llhh

import com.github.nmicra.marketresearch.ml.predictor.RegressionPrecision
import com.github.nmicra.marketresearch.ml.predictor.evaluate
import com.github.nmicra.marketresearch.ml.predictor.train
import org.springframework.stereotype.Service
import org.tribuo.Model
import org.tribuo.MutableDataset
import org.tribuo.Prediction
import org.tribuo.data.columnar.ColumnarIterator
import org.tribuo.data.columnar.FieldProcessor
import org.tribuo.data.columnar.RowProcessor
import org.tribuo.data.columnar.processors.field.DoubleFieldProcessor
import org.tribuo.data.columnar.processors.field.TextFieldProcessor
import org.tribuo.data.columnar.processors.response.FieldResponseProcessor
import org.tribuo.data.csv.CSVDataSource
import org.tribuo.data.text.impl.BasicPipeline
import org.tribuo.evaluation.TrainTestSplitter
import org.tribuo.regression.RegressionFactory
import org.tribuo.regression.Regressor
import org.tribuo.regression.xgboost.XGBoostRegressionTrainer
import org.tribuo.util.tokens.impl.BreakIteratorTokenizer
import java.io.File
import java.nio.charset.Charset
import java.util.*


@Service
class LLHHDistancePredictor {

//    private val xgb = XGBoostRegressionTrainer(50)

    private val regressionFactory =  RegressionFactory()

    /**
     * column headers
     */
    private val reportHeaders = arrayOf("currentIndexClassification","distanceToHH","distanceToLL","distanceToLH","distanceToHL","nextLLHHClassification","nextLLHHDistance")
    private val reportHeadersMV = arrayOf("mv5Dev,mv7Dev,currentIndexClassification","distanceToHH","distanceToLL","distanceToLH","distanceToHL","nextLLHHClassification","nextLLHHDistance")

    /**
     * Target field
     */
    val responseProcessor =  FieldResponseProcessor("nextLLHHDistance","-1", regressionFactory)

    /**
     * first feature which is TEXT
     */
    val currentIndexClassifFieldProcessor = TextFieldProcessor("currentIndexClassification", BasicPipeline(
        BreakIteratorTokenizer(Locale.US),1)
    )

    /**
     * first feature which is TEXT
     */
    val nextLLHHClassification = TextFieldProcessor("nextLLHHClassification", BasicPipeline(
        BreakIteratorTokenizer(Locale.US),1)
    )

    /**
     * mapping of all feature fields to FieldProcessor
     */
    val fieldProcessors = mapOf<String, FieldProcessor>("currentIndexClassification" to currentIndexClassifFieldProcessor,
        "distanceToHH" to DoubleFieldProcessor("distanceToHH"),
        "distanceToLL" to DoubleFieldProcessor("distanceToLL"),
        "distanceToLH" to DoubleFieldProcessor("distanceToLH"),
        "distanceToHL" to DoubleFieldProcessor("distanceToHL"),
        "nextLLHHClassification" to nextLLHHClassification
    )

    val fieldProcessorsMV = mapOf<String, FieldProcessor>(
        "mv5Dev" to DoubleFieldProcessor("mv5Dev"),
        "mv7Dev" to DoubleFieldProcessor("mv7Dev"),
        "currentIndexClassification" to currentIndexClassifFieldProcessor,
        "distanceToHH" to DoubleFieldProcessor("distanceToHH"),
        "distanceToLL" to DoubleFieldProcessor("distanceToLL"),
        "distanceToLH" to DoubleFieldProcessor("distanceToLH"),
        "distanceToHL" to DoubleFieldProcessor("distanceToHL"),
        "nextLLHHClassification" to nextLLHHClassification
    )

    val rp = RowProcessor(responseProcessor,fieldProcessors)
    val rpMV = RowProcessor(responseProcessor,fieldProcessorsMV)


/*
    private var csvSaver = CSVSaver()

    private var csvLoader = CSVLoader<Regressor>(regressionFactory)

    lateinit var predictionExample: ArrayExample<Regressor> //= ArrayExample<Regressor>(trainData.outputs.first())*/

    fun createLLHHDistanceModel(recordsForCSV : List<String>, xgBoosterNumTrees : Int = 50, mvIncluded : Boolean = false) : Pair<Model<Regressor?>, RegressionPrecision> {
        val xgb = XGBoostRegressionTrainer(xgBoosterNumTrees)
        val tmpFile = File.createTempFile("llhh-distance-${System.currentTimeMillis()}", ".csv")
            .also { it.deleteOnExit() }

        println(">>> csvPath: ${tmpFile.absolutePath}")


        val seriesSource = when {
            mvIncluded -> {
                tmpFile.writeText(reportHeadersMV.joinToString(",").plus("\r\n"), Charset.defaultCharset())
                recordsForCSV.forEach { tmpFile.appendText(it, Charset.defaultCharset()) }
                CSVDataSource(tmpFile.toPath(),rpMV,true)
            }
            else -> {
                tmpFile.writeText(reportHeaders.joinToString(",").plus("\r\n"), Charset.defaultCharset())
                recordsForCSV.forEach { tmpFile.appendText(it, Charset.defaultCharset()) }
                CSVDataSource(tmpFile.toPath(),rp,true)
            }
        }
        val splitter = TrainTestSplitter(seriesSource, 0.8, 1L)
        val trainData = MutableDataset(splitter.train)
        val evalData = MutableDataset(splitter.test)



        var xgbModel = train("XGBoostLLHHDistance",xgb,trainData)
        evaluate(xgbModel.first,evalData)
        return xgbModel
    }



    fun predictWithModel(model : Model<Regressor?>, params : List<String>, mvIncluded : Boolean = false): Prediction<Regressor?> {
        val predictionQuery = when {
            mvIncluded -> mapOf<String, String>(
                "mv5Dev" to params[0],
                "mv7Dev" to params[1],
                "currentIndexClassification" to params[2],
                "distanceToHH" to params[3],
                "distanceToLL" to params[4],
                "distanceToLH" to params[5],
                "distanceToHL" to params[6],
                "nextLLHHClassification" to params[7]
            )
            else -> mapOf<String, String>("currentIndexClassification" to params[0],
                "distanceToHH" to params[1],
                "distanceToLL" to params[2],
                "distanceToLH" to params[3],
                "distanceToHL" to params[4],
                "nextLLHHClassification" to params[5]
            )
        }


        val headers: List<String> = predictionQuery.keys.toList()
        val row: ColumnarIterator.Row = ColumnarIterator.Row(0, headers, predictionQuery)
        val prediction = model.predict(rp.generateExample(row,false).get())
        println(">>> prediction => ${prediction.output} ${prediction.outputScores}")
        return prediction
    }
}