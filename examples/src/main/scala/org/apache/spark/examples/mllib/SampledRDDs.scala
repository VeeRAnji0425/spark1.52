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

import org.apache.spark.mllib.util.MLUtils
import scopt.OptionParser

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.SparkContext._

/**
 * An example app for randomly generated and sampled RDDs. Run with
 * 随机生成和采样RDDS一个示例应用程序
 * {{{
 * bin/run-example org.apache.spark.examples.mllib.SampledRDDs
 * }}}
 * If you use it as a template to create your own app, please use `spark-submit` to submit your app.
  * 如果您使用它作为模板来创建您自己的应用程序,请使用`spark-submit`提交您的应用程序
 */
object SampledRDDs {
/**
 *  libSVM的数据格式
 *  <label> <index1>:<value1> <index2>:<value2> ...
 *  其中<label>是训练数据集的目标值,对于分类,它是标识某类的整数(支持多个类);对于回归,是任意实数
 *  <index>是以1开始的整数,可以是不连续
 *  <value>为实数,也就是我们常说的自变量
 */
  case class Params(input: String = "../data/mllib/sample_binary_classification_data.txt")
    extends AbstractParams[Params]

  def main(args: Array[String]) {
    val defaultParams = Params()

    val parser = new OptionParser[Params]("SampledRDDs") {
      head("SampledRDDs: an example app for randomly generated and sampled RDDs.")
      opt[String]("input")
        .text(s"Input path to labeled examples in LIBSVM format, default: ${defaultParams.input}")
        .action((x, c) => c.copy(input = x))
      note(
        """
        |For example, the following command runs this app:
        |
        | bin/spark-submit --class org.apache.spark.examples.mllib.SampledRDDs \
        |  examples/target/scala-*/spark-examples-*.jar
        """.stripMargin)
    }

    parser.parse(args, defaultParams).map { params =>
      run(params)
    } getOrElse {
      sys.exit(1)
    }
  }

  def run(params: Params) {
    val conf = new SparkConf().setAppName(s"SampledRDDs with $params").setMaster("local")
    val sc = new SparkContext(conf)

    val fraction = 0.1 // fraction of data to sample 样本数据的分数
    /**
     *  libSVM的数据格式
     *  <label> <index1>:<value1> <index2>:<value2> ...
     *  其中<label>是训练数据集的目标值,对于分类,它是标识某类的整数(支持多个类);对于回归,是任意实数
     *  <index>是以1开始的整数,可以是不连续
     *  <value>为实数,也就是我们常说的自变量
     */
    val examples = MLUtils.loadLibSVMFile(sc, params.input)
    val numExamples = examples.count()
    if (numExamples == 0) {
      throw new RuntimeException("Error: Data file had no samples to load.")
    }
    println(s"Loaded data with $numExamples examples from file: ${params.input}")

    // Example: RDD.sample() and RDD.takeSample()
    val expectedSampleSize = (numExamples * fraction).toInt
    println(s"Sampling RDD using fraction $fraction.  Expected sample size = $expectedSampleSize.")
    val sampledRDD = examples.sample(withReplacement = true, fraction = fraction)
    println(s"  RDD.sample(): sample has ${sampledRDD.count()} examples")
    val sampledArray = examples.takeSample(withReplacement = true, num = expectedSampleSize)
    println(s"  RDD.takeSample(): sample has ${sampledArray.size} examples")

    println()
     //spark对于分层抽样支持两个版本sampleByKey和sampleByKeyExact。
     //它是一个根据RDD的Key-Value来抽样的功能,可以为每个key设置其被选中的概率
    // Example: RDD.sampleByKey() and RDD.sampleByKeyExact()
    val keyedRDD = examples.map { lp => (lp.label.toInt, lp.features) }
    println(s"  Keyed data using label (Int) as key ==> Orig")
    //  Count examples per label in original data.
    val keyCounts = keyedRDD.countByKey()

    //  Subsample, and count examples per label in sampled data. (approximate)
    //样本,并在采样数据的每个标签计数的例子,(近似的)
    val fractions = keyCounts.keys.map((_, fraction)).toMap
    val sampledByKeyRDD = keyedRDD.sampleByKey(withReplacement = true, fractions = fractions)
    val keyCountsB = sampledByKeyRDD.countByKey()
    val sizeB = keyCountsB.values.sum
    println(s"  Sampled $sizeB examples using approximate stratified sampling (by label)." +
      " ==> Approx Sample")

    //  Subsample, and count examples per label in sampled data. (approximate)
    //样本,并在采样数据的每个标签计数的例子(近似的)
    val sampledByKeyRDDExact =
      keyedRDD.sampleByKeyExact(withReplacement = true, fractions = fractions)
    val keyCountsBExact = sampledByKeyRDDExact.countByKey()
    val sizeBExact = keyCountsBExact.values.sum
    println(s"  Sampled $sizeBExact examples using exact stratified sampling (by label)." +
      " ==> Exact Sample")

    //  Compare samples比较样品
    println(s"   \tFractions of examples with key")
    println(s"Key\tOrig\tApprox Sample\tExact Sample")
    keyCounts.keys.toSeq.sorted.foreach { key =>
      val origFrac = keyCounts(key) / numExamples.toDouble
      val approxFrac = if (sizeB != 0) {
        keyCountsB.getOrElse(key, 0L) / sizeB.toDouble
      } else {
        0
      }
      val exactFrac = if (sizeBExact != 0) {
        keyCountsBExact.getOrElse(key, 0L) / sizeBExact.toDouble
      } else {
        0
      }
      println(s"$key\t$origFrac\t$approxFrac\t$exactFrac")
    }

    sc.stop()
  }
}
// scalastyle:on println
