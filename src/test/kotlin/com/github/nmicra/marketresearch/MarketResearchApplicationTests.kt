package com.github.nmicra.marketresearch

import org.junit.jupiter.api.Test
import org.tribuo.MutableDataset
import org.tribuo.anomaly.evaluation.AnomalyEvaluator
import org.tribuo.anomaly.example.GaussianAnomalyDataSource
import org.tribuo.anomaly.libsvm.LibSVMAnomalyModel
import org.tribuo.anomaly.libsvm.LibSVMAnomalyTrainer
import org.tribuo.anomaly.libsvm.SVMAnomalyType
import org.tribuo.common.libsvm.KernelType
import org.tribuo.common.libsvm.SVMParameters
import org.tribuo.regression.evaluation.RegressionEvaluator
import org.tribuo.util.Util


//@ActiveProfiles(profiles = ["simpletest"])
//@SpringBootTest
class MarketResearchApplicationTests {

	var data = MutableDataset(
		GaussianAnomalyDataSource(
			2000,  /* number of examples */
			0.0f,  /*fraction anomalous */
			1L /* RNG seed */
		)
	)
	var test = MutableDataset(GaussianAnomalyDataSource(2000, 0.2f, 2L))

	@Test
	fun bla() {
		println(">>> dataSize = ${data.size()}")
		println(">>> testSize = ${test.size()}")

		val params = SVMParameters(SVMAnomalyType(SVMAnomalyType.SVMMode.ONE_CLASS), KernelType.RBF)
		params.gamma = 1.0
		params.setNu(0.1)
		val trainer = LibSVMAnomalyTrainer(params)

		var startTime = System.currentTimeMillis();
		var model = trainer.train(data);
		var endTime = System.currentTimeMillis();
		println();
		println("Training took " + Util.formatDuration(startTime,endTime));

		println("numberOfSupportVectors >>> " + (model as LibSVMAnomalyModel).numberOfSupportVectors.toString())

		val eval = AnomalyEvaluator()
		var testEvaluation = eval.evaluate(model,test);
		println(testEvaluation.toString());
		println(testEvaluation.confusionString());
	}

}
