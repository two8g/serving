package org.apache.spark.ml.regression

import com.github.fommil.netlib.BLAS.{getInstance => blas}
import org.apache.spark.ml.classification.PredictionServingModel
import org.apache.spark.ml.data.{SCol, SDFrame, UDF}
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.ml.param.ParamMap

class GBTRegressionServingModel(stage: GBTRegressionModel)
  extends PredictionServingModel[Vector, GBTRegressionServingModel, GBTRegressionModel](stage) {

  override def copy(extra: ParamMap): GBTRegressionServingModel = {
    new GBTRegressionServingModel(stage.copy(extra))
  }

  override def predict(features: Vector): Double ={
    val treePredictions = stage.trees.map(_.rootNode.predictImpl(features).prediction)
    blas.ddot(stage.numTrees, treePredictions, 1, stage.treeWeights, 1)
  }

  override def transformImpl(dataset: SDFrame): SDFrame = {
    val predictUDF = UDF.make[Double, Vector](predict, false)
    dataset.withColum(predictUDF.apply(stage.getPredictionCol, SCol(stage.getFeaturesCol)))
  }

  override val uid: String = stage.uid
}

object GBTRegressionServingModel {
  def apply(stage: GBTRegressionModel): GBTRegressionServingModel = new GBTRegressionServingModel(stage)
}
