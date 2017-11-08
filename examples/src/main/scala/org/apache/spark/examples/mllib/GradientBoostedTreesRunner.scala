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

import scopt.OptionParser

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.tree.GradientBoostedTrees
import org.apache.spark.mllib.tree.configuration.{BoostingStrategy, Algo}
import org.apache.spark.util.Utils


/**
 * 使用梯度提升树,以决策树为弱学习的梯度提升的一个例子
 * GBT的训练是每次训练一颗树,然后利用这颗树对每个实例进行预测,通过一个损失函数,计算损失函数的负梯度值作为残差,
 * 利用这个残差更新样本实例的label,然后再次训练一颗树去拟合残差,如此进行迭代,直到满足模型参数需求。
 * GBT只适用于二分类和回归,不支持多分类,在预测的时候,不像随机森林那样求平均值,GBT是将所有树的预测值相加求和。
 * An example runner for Gradient Boosting using decision trees as weak learners. Run with
 * {{{
 * ./bin/run-example mllib.GradientBoostedTreesRunner [options]
 * }}}
 * If you use it as a template to create your own app, please use `spark-submit` to submit your app.
 *
 * Note: This script treats all features as real-valued (not categorical).
 *       To include categorical features, modify categoricalFeaturesInfo.
  *       注意：此脚本将所有功能视为实值(而不是分类)
  *       要包含分类特征,请修改categoricalFeaturesInfo
 */
object GradientBoostedTreesRunner {

  case class Params(
      input: String = null,
      testInput: String = "",
      dataFormat: String = "libsvm",
      algo: String = "Classification",
      maxDepth: Int = 5,//树的最大深度,为了防止过拟合,设定划分的终止条件
      numIterations: Int = 10,
      fracTest: Double = 0.2) extends AbstractParams[Params]

  def main(args: Array[String]) {
    val defaultParams = Params()

    val parser = new OptionParser[Params]("GradientBoostedTrees") {
      head("GradientBoostedTrees: an example decision tree app.")
      opt[String]("algo")
        .text(s"algorithm (${Algo.values.mkString(",")}), default: ${defaultParams.algo}")
        .action((x, c) => c.copy(algo = x))
      opt[Int]("maxDepth")//树的最大深度,为了防止过拟合,设定划分的终止条件
        .text(s"max depth of the tree, default: ${defaultParams.maxDepth}")
        .action((x, c) => c.copy(maxDepth = x))
      opt[Int]("numIterations")
        .text(s"number of iterations of boosting," + s" default: ${defaultParams.numIterations}")
        .action((x, c) => c.copy(numIterations = x))
      opt[Double]("fracTest")
        .text(s"fraction of data to hold out for testing.  If given option testInput, " +
          s"this option is ignored. default: ${defaultParams.fracTest}")
        .action((x, c) => c.copy(fracTest = x))
      opt[String]("testInput")
        .text(s"input path to test dataset.  If given, option fracTest is ignored." +
          s" default: ${defaultParams.testInput}")
        .action((x, c) => c.copy(testInput = x))
      opt[String]("dataFormat")
      /**
       *  libSVM的数据格式
       *  <label> <index1>:<value1> <index2>:<value2> ...
       *  其中<label>是训练数据集的目标值,对于分类,它是标识某类的整数(支持多个类);对于回归,是任意实数
       *  <index>是以1开始的整数,可以是不连续
       *  <value>为实数,也就是我们常说的自变量
       */
        .text("data format: libsvm (default), dense (deprecated in Spark v1.1)")
        .action((x, c) => c.copy(dataFormat = x))
      arg[String]("<input>")
        .text("input path to labeled examples")
        .required()
        .action((x, c) => c.copy(input = x))
      checkConfig { params =>
        if (params.fracTest < 0 || params.fracTest > 1) {
          failure(s"fracTest ${params.fracTest} value incorrect; should be in [0,1].")
        } else {
          success
        }
      }
    }

    parser.parse(args, defaultParams).map { params =>
      run(params)
    }.getOrElse {
      sys.exit(1)
    }
  }

  def run(params: Params) {

    val conf = new SparkConf().setAppName(s"GradientBoostedTreesRunner with $params").setMaster("local")
    val sc = new SparkContext(conf)

    println(s"GradientBoostedTreesRunner with parameters:\n$params")

    // Load training and test data and cache it.
    //加载训练和测试数据并将其缓存
    val (training, test, numClasses) = DecisionTreeRunner.loadDatasets(sc, params.input,
      params.dataFormat, params.testInput, Algo.withName(params.algo), params.fracTest)

    val boostingStrategy = BoostingStrategy.defaultParams(params.algo)
    boostingStrategy.treeStrategy.numClasses = numClasses
    boostingStrategy.numIterations = params.numIterations
    boostingStrategy.treeStrategy.maxDepth = params.maxDepth

    val randomSeed = Utils.random.nextInt()
    if (params.algo == "Classification") {
     //系统计时器的当前值,以毫微秒为单位
      val startTime = System.nanoTime()
      //梯度提升决策树:综合多个决策树,消除噪声,避免过拟合
      val model = GradientBoostedTrees.train(training, boostingStrategy)
      //1e9就为1*(10的九次方),也就是十亿
      val elapsedTime = (System.nanoTime() - startTime) / 1e9
      println(s"Training time: $elapsedTime seconds")
      if (model.totalNumNodes < 30) {
        println(model.toDebugString) // Print full model.
      } else {
        println(model) // Print model summary.
      }
       //评估指标-多分类
      val trainAccuracy =
        new MulticlassMetrics(training.map(lp => (model.predict(lp.features), lp.label)))
          .precision
      println(s"Train accuracy = $trainAccuracy")
       //评估指标-多分类
      val testAccuracy =
        new MulticlassMetrics(test.map(lp => (model.predict(lp.features), lp.label))).precision
      println(s"Test accuracy = $testAccuracy")
    } else if (params.algo == "Regression") {
     //系统计时器的当前值,以毫微秒为单位
      val startTime = System.nanoTime()
      //梯度提升决策树:综合多个决策树,消除噪声,避免过拟合
      val model = GradientBoostedTrees.train(training, boostingStrategy)
       //1e9就为1*(10的九次方),也就是十亿
      val elapsedTime = (System.nanoTime() - startTime) / 1e9
      println(s"Training time: $elapsedTime seconds")
      if (model.totalNumNodes < 30) {
        println(model.toDebugString) // Print full model.
      } else {
        println(model) // Print model summary.
      }
      val trainMSE = DecisionTreeRunner.meanSquaredError(model, training)
      println(s"Train mean squared error = $trainMSE")
      val testMSE = DecisionTreeRunner.meanSquaredError(model, test)
      println(s"Test mean squared error = $testMSE")
    }

    sc.stop()
  }
}
// scalastyle:on println
