package com.github.nmicra.marketresearch.ml.predictor.llhh

import org.springframework.stereotype.Service
import org.tribuo.*
import org.tribuo.classification.Label
import org.tribuo.classification.LabelFactory
import org.tribuo.classification.evaluation.LabelEvaluation
import org.tribuo.classification.evaluation.LabelEvaluator
import org.tribuo.classification.sgd.linear.LogisticRegressionTrainer
import org.tribuo.data.columnar.ColumnarIterator
import org.tribuo.data.columnar.FieldProcessor
import org.tribuo.data.columnar.RowProcessor
import org.tribuo.data.columnar.processors.field.DoubleFieldProcessor
import org.tribuo.data.columnar.processors.field.TextFieldProcessor
import org.tribuo.data.columnar.processors.response.FieldResponseProcessor
import org.tribuo.data.csv.CSVDataSource
import org.tribuo.data.csv.CSVLoader
import org.tribuo.data.text.impl.BasicPipeline
import org.tribuo.evaluation.TrainTestSplitter
import org.tribuo.util.tokens.impl.BreakIteratorTokenizer
import java.io.File
import java.nio.charset.Charset
import java.util.*


@Service
class LLHHMVPredictor {

    /**
     * column headers
     */
    private val reportHeaders = arrayOf("mv5Dev,mv7Dev,currentIndexClassification","distanceToHH","distanceToLL","distanceToLH","distanceToHL","nextLLHHClassification")


    /**
     * Target field
     */
    val responseProcessor =  FieldResponseProcessor("nextLLHHClassification","SOMEDEFAULT",LabelFactory())

    /**
     * first feature which is TEXT
     */
    val currentIndexClassifFieldProcessor = TextFieldProcessor("currentIndexClassification", BasicPipeline(BreakIteratorTokenizer(Locale.US),1))

    /**
     * mapping of all feature fields to FieldProcessor
     */
    val fieldProcessors = mapOf<String, FieldProcessor>("currentIndexClassification" to currentIndexClassifFieldProcessor,
    "mv5Dev" to DoubleFieldProcessor("mv5Dev"),
    "mv7Dev" to DoubleFieldProcessor("mv7Dev"),
    "distanceToHH" to DoubleFieldProcessor("distanceToHH"),
    "distanceToLL" to DoubleFieldProcessor("distanceToLL"),
    "distanceToLH" to DoubleFieldProcessor("distanceToLH"),
    "distanceToHL" to DoubleFieldProcessor("distanceToHL")
    )

    val rp = RowProcessor(responseProcessor,fieldProcessors)

    fun createClassificationModelLLHH(recordsForCSV : List<String>) : Model<Label> {
        val tmpFile = File.createTempFile("llhh-classification-${System.currentTimeMillis()}", ".csv")
            .also { it.deleteOnExit() }

        println(">>> csvPath: ${tmpFile.absolutePath}")
        tmpFile.writeText(reportHeaders.joinToString(",").plus("\r\n"), Charset.defaultCharset())
        recordsForCSV.forEach { tmpFile.appendText(it, Charset.defaultCharset()) }

        val seriesSource = CSVDataSource(tmpFile.toPath(),rp,true);
        val splitter = TrainTestSplitter(seriesSource, 0.8, 1L)
        val trainData = MutableDataset(splitter.train)
        val evalData = MutableDataset(splitter.test)

        val trainer: Trainer<Label> = LogisticRegressionTrainer()
        val llhhModel: Model<Label> = trainer.train(trainData)

        val evaluator = LabelEvaluator()
        val evaluation: LabelEvaluation = evaluator.evaluate(llhhModel, evalData)
        println(evaluation.toString())

        return llhhModel
    }

    fun predictWithModel(model : Model<Label>, params : List<String>): Prediction<Label> {

        val predictionQuery = mapOf<String, String>(
            "mv5Dev" to params[0],
            "mv7Dev" to params[1],
            "currentIndexClassification" to params[2],
            "distanceToHH" to params[3],
            "distanceToLL" to params[4],
            "distanceToLH" to params[5],
            "distanceToHL" to params[6]
        )
        val headers: List<String> = predictionQuery.keys.toList()
        val row: ColumnarIterator.Row = ColumnarIterator.Row(0, headers, predictionQuery)

        val prediction = model.predict(rp.generateExample(row,false).get())
        println(">>> prediction => ${prediction.output} ${prediction.outputScores}")
        return prediction
    }
}