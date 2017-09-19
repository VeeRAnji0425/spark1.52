/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// scalastyle:off println
package org.apache.spark.examples.mllib

import org.apache.log4j.{Level, Logger}
import scopt.OptionParser

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.regression.LinearRegressionWithSGD
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.mllib.optimization.{SimpleUpdater, SquaredL2Updater, L1Updater}

/**
 * 一个线性回归的例子
 * An example app for linear regression. Run with
 * {{{
 * bin/run-example org.apache.spark.examples.mllib.LinearRegression
 * }}}
 * A synthetic dataset can be found at `data/mllib/sample_linear_regression_data.txt`.
 * If you use it as a template to create your own app, please use `spark-submit` to submit your app.
 */
object LinearRegression {

  object RegType extends Enumeration {
    type RegType = Value
    val NONE, L1, L2 = Value
  }

  import RegType._

  case class Params(
      //input: String = null,
      input: String = "../data/mllib/sample_linear_regression_data.txt",
      numIterations: Int = 100,
      stepSize: Double = 1.0,//每次迭代优化步长
      regType: RegType = L2,
      regParam: Double = 0.01) extends AbstractParams[Params]

  def main(args: Array[String]) {
    val defaultParams = Params()

    val parser = new OptionParser[Params]("LinearRegression") {
      head("LinearRegression: an example app for linear regression.")
      opt[Int]("numIterations")
        .text("number of iterations")
        .action((x, c) => c.copy(numIterations = x))
      opt[Double]("stepSize")//每次迭代优化步长
        .text(s"initial step size, default: ${defaultParams.stepSize}")
        .action((x, c) => c.copy(stepSize = x))
      opt[String]("regType")
        .text(s"regularization type (${RegType.values.mkString(",")}), " +
        s"default: ${defaultParams.regType}")
        .action((x, c) => c.copy(regType = RegType.withName(x)))
      opt[Double]("regParam")
        .text(s"regularization parameter, default: ${defaultParams.regParam}")
      opt[String]("<input>")
        //.required()
	/**
   *  libSVM的数据格式
   *  <label> <index1>:<value1> <index2>:<value2> ...
   *  其中<label>是训练数据集的目标值,对于分类,它是标识某类的整数(支持多个类);对于回归,是任意实数
   *  <index>是以1开始的整数,可以是不连续
   *  <value>为实数,也就是我们常说的自变量
   */
        .text("input paths to labeled examples in LIBSVM format")
        .action((x, c) => c.copy(input = x))
      note(
        """
          |For example, the following command runs this app on a synthetic dataset:
          |
          | bin/spark-submit --class org.apache.spark.examples.mllib.LinearRegression \
          |  examples/target/scala-*/spark-examples-*.jar \
          |  data/mllib/sample_linear_regression_data.txt
        """.stripMargin)
    }

    parser.parse(args, defaultParams).map { params =>
      run(params)
    } getOrElse {
      sys.exit(1)
    }
  }

  def run(params: Params) {
    val conf = new SparkConf().setAppName(s"LinearRegression with $params").setMaster("local[*]")
    val sc = new SparkContext(conf)

    Logger.getRootLogger.setLevel(Level.WARN)
  /**
   *  libSVM的数据格式
   *  <label> <index1>:<value1> <index2>:<value2> ...
   *  其中<label>是训练数据集的目标值,对于分类,它是标识某类的整数(支持多个类);对于回归,是任意实数
   *  <index>是以1开始的整数,可以是不连续
   *  <value>为实数,也就是我们常说的自变量
   */
    val examples = MLUtils.loadLibSVMFile(sc, params.input).cache()

    val splits = examples.randomSplit(Array(0.8, 0.2))
    val training = splits(0).cache()
    val test = splits(1).cache()

    val numTraining = training.count()
    val numTest = test.count()
    println(s"Training: $numTraining, test: $numTest.")

    examples.unpersist(blocking = false)

    val updater = params.regType match {
      case NONE => new SimpleUpdater()
      case L1 => new L1Updater()
      case L2 => new SquaredL2Updater()
    }

    val algorithm = new LinearRegressionWithSGD()//(SGD随机梯度下降)
    algorithm.optimizer
      .setNumIterations(params.numIterations)//迭代次数
      .setStepSize(params.stepSize)//每次迭代优化步长
      .setUpdater(updater)
      .setRegParam(params.regParam)//正则化

    val model = algorithm.run(training)

    val prediction = model.predict(test.map(_.features))
    val predictionAndLabel = prediction.zip(test.map(_.label))

    val loss = predictionAndLabel.map { case (p, l) =>
      val err = p - l
      err * err
    }.reduce(_ + _)
    //rmse均方根误差说明样本的离散程度
    val rmse = math.sqrt(loss / numTest)
    //rmse均方根误差说明样本的离散程度
    println(s"Test RMSE = $rmse.")

    sc.stop()
  }
}
// scalastyle:on println
