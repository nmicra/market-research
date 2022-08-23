package com.github.nmicra.marketresearch.ml.predictor

import org.tribuo.Dataset
import org.tribuo.Model
import org.tribuo.Trainer
import org.tribuo.regression.Regressor
import org.tribuo.regression.evaluation.RegressionEvaluator
import org.tribuo.util.Util

data class RegressionPrecision(val rmse: Double, val mae: Double, val r2: Double)

fun train(name: String, trainer: Trainer<Regressor?>, trainData: Dataset<Regressor?>): Pair<Model<Regressor?>,RegressionPrecision> {
    // Train the model
    val startTime = System.currentTimeMillis()
    val model: Model<Regressor?> = trainer.train(trainData)
    val endTime = System.currentTimeMillis()
    println("Training " + name + " took " + Util.formatDuration(startTime, endTime))
    // Evaluate the model on the training data
    // This is a useful debugging tool to check the model actually learned something
    val eval = RegressionEvaluator()
    val evaluation = eval.evaluate(model, trainData)
    // We create a dimension here to aid pulling out the appropriate statistics.
    // You can also produce the String directly by calling "evaluation.toString()"
    val dimension = Regressor("DIM-0", Double.NaN)
    System.out.printf(
        "Evaluation (train):%n  RMSE %f%n  MAE %f%n  R^2 %f%n",
        evaluation.rmse(dimension), evaluation.mae(dimension), evaluation.r2(dimension)
    )
    val rp = RegressionPrecision(evaluation.rmse(dimension), evaluation.mae(dimension), evaluation.r2(dimension))
    return Pair(model,rp)
}

fun evaluate(model: Model<Regressor?>, testData: Dataset<Regressor>) {
    // Evaluate the model on the test data
    val eval = RegressionEvaluator()
    val evaluation = eval.evaluate(model, testData)
    // We create a dimension here to aid pulling out the appropriate statistics.
    // You can also produce the String directly by calling "evaluation.toString()"
    val dimension = Regressor("DIM-0", Double.NaN)
    System.out.printf(
        "Evaluation (test):%n  RMSE %f%n  MAE %f%n  R^2 %f%n",
        evaluation.rmse(dimension), evaluation.mae(dimension), evaluation.r2(dimension)
    )
}