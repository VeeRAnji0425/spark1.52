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

import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.stat.MultivariateOnlineSummarizer
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.{SparkConf, SparkContext}


/**
 * An example app for summarizing multivariate data from a file. Run with
 * 一个用于汇总多元数据的文件中示例应用程序
 * {{{
 * bin/run-example org.apache.spark.examples.mllib.MultivariateSummarizer
 * }}}
 * By default, this loads a synthetic dataset from `data/mllib/sample_linear_regression_data.txt`.
 * 默认,这个加载一个合成数据集来自`data/mllib/sample_linear_regression_data.txt`
 * If you use it as a template to create your own app, please use `spark-submit` to submit your app.
 */
object MultivariateSummarizer {
/**
 *  libSVM的数据格式
 *  <label> <index1>:<value1> <index2>:<value2> ...
 *  其中<label>是训练数据集的目标值,对于分类,它是标识某类的整数(支持多个类);对于回归,是任意实数
 *  <index>是以1开始的整数,可以是不连续
 *  <value>为实数,也就是我们常说的自变量
 */
  case class Params(input: String = "../data/mllib/sample_linear_regression_data.txt")
    extends AbstractParams[Params]

  def main(args: Array[String]) {

    val defaultParams = Params()

    val parser = new OptionParser[Params]("MultivariateSummarizer") {
      head("MultivariateSummarizer: an example app for MultivariateOnlineSummarizer")
      opt[String]("input")
        .text(s"Input path to labeled examples in LIBSVM format, default: ${defaultParams.input}")
        .action((x, c) => c.copy(input = x))
      note(
        """
        |For example, the following command runs this app on a synthetic dataset:
        |
        | bin/spark-submit --class org.apache.spark.examples.mllib.MultivariateSummarizer \
        |  examples/target/scala-*/spark-examples-*.jar \
        |  --input data/mllib/sample_linear_regression_data.txt
        """.stripMargin)
    }

    parser.parse(args, defaultParams).map { params =>
      run(params)
    } getOrElse {
        sys.exit(1)
    }
  }

  def run(params: Params) {
    val conf = new SparkConf().setAppName(s"MultivariateSummarizer with $params").setMaster("local")
    val sc = new SparkContext(conf)
    /**
     *  libSVM的数据格式
     *  <label> <index1>:<value1> <index2>:<value2> ...
     *  其中<label>是训练数据集的目标值,对于分类,它是标识某类的整数(支持多个类);对于回归,是任意实数
     *  <index>是以1开始的整数,可以是不连续
     *  <value>为实数,也就是我们常说的自变量
     */
    val examples = MLUtils.loadLibSVMFile(sc, params.input).cache()

    println(s"Summary of data file: ${params.input}")
    println(s"${examples.count()} data points")

    // Summarize labels
    //总结标签
    val labelSummary = examples.aggregate(new MultivariateOnlineSummarizer())(
      (summary, lp) => summary.add(Vectors.dense(lp.label)),
      (sum1, sum2) => sum1.merge(sum2))

    // Summarize features
    //总结特点
    val featureSummary = examples.aggregate(new MultivariateOnlineSummarizer())(
      (summary, lp) => summary.add(lp.features),
      (sum1, sum2) => sum1.merge(sum2))

    println()
    println(s"Summary statistics")
    println(s"\tLabel\tFeatures")
    println(s"mean\t${labelSummary.mean(0)}\t${featureSummary.mean.toArray.mkString("\t")}")
    println(s"var\t${labelSummary.variance(0)}\t${featureSummary.variance.toArray.mkString("\t")}")
    println(
      s"nnz\t${labelSummary.numNonzeros(0)}\t${featureSummary.numNonzeros.toArray.mkString("\t")}")
    println(s"max\t${labelSummary.max(0)}\t${featureSummary.max.toArray.mkString("\t")}")
    println(s"min\t${labelSummary.min(0)}\t${featureSummary.min.toArray.mkString("\t")}")
    println()

    sc.stop()
  }
}
// scalastyle:on println
