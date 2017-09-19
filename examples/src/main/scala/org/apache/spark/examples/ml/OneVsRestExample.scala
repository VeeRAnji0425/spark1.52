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
package org.apache.spark.examples.ml

import java.util.concurrent.TimeUnit.{NANOSECONDS => NANO}

import scopt.OptionParser

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.examples.mllib.AbstractParams
import org.apache.spark.ml.classification.{OneVsRest, LogisticRegression}
import org.apache.spark.ml.util.MetadataUtils
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext

/**
 * An example runner for Multiclass to Binary Reduction with One Vs Rest.
 * The example uses Logistic Regression as the base classifier. All parameters that
 * 这个例子使用逻辑回归为基础的分类,
 * OneVsRest将一个给定的二分类算法有效地扩展到多分类问题应用中
 * can be specified on the base classifier can be passed in to the runner options.
 * 所有的参数都可以在指定的基分类器,可以通过运行选项
 * Run with
 * {{{
 * ./bin/run-example ml.OneVsRestExample [options]
 * }}}
 * For local mode, run
 * {{{
 * ./bin/spark-submit --class org.apache.spark.examples.ml.OneVsRestExample --driver-memory 1g
 *   [examples JAR path] [options]
 * }}}
 * If you use it as a template to create your own app, please use `spark-submit` to submit your app.
 */
object OneVsRestExample {

  case class Params private[ml] (
      input: String = "../data/mllib/sample_libsvm_data.txt",
      testInput: Option[String] = None,
      maxIter: Int = 100,//迭代次数
      tol: Double = 1E-6,//迭代算法的收敛性
      fitIntercept: Boolean = true,//是否训练拦截对象
      regParam: Option[Double] = None,//正则化参数(>=0)
      elasticNetParam: Option[Double] = None,//弹性网络混合参数,0.0为L2正则化 1.0为L1正则化
      fracTest: Double = 0.2) extends AbstractParams[Params]

  def main(args: Array[String]) {
    val defaultParams = Params()

    val parser = new OptionParser[Params]("OneVsRest Example") {
      head("OneVsRest Example: multiclass to binary reduction using OneVsRest")
      opt[String]("input")
        .text("input path to labeled examples. This path must be specified")
        //.required()
        .action((x, c) => c.copy(input = x))
      opt[Double]("fracTest")
        .text(s"fraction of data to hold out for testing.  If given option testInput, " +
        s"this option is ignored. default: ${defaultParams.fracTest}")
        .action((x, c) => c.copy(fracTest = x))
      opt[String]("testInput")
        .text("input path to test dataset.  If given, option fracTest is ignored")
        .action((x, c) => c.copy(testInput = Some(x)))
      opt[Int]("maxIter")
        .text(s"maximum number of iterations for Logistic Regression." +
          s" default: ${defaultParams.maxIter}")
        .action((x, c) => c.copy(maxIter = x))
      opt[Double]("tol")//迭代算法的收敛性
        .text(s"the convergence tolerance of iterations for Logistic Regression." +
          s" default: ${defaultParams.tol}")
        .action((x, c) => c.copy(tol = x))
      opt[Boolean]("fitIntercept")//是否训练拦截对象
        .text(s"fit intercept for Logistic Regression." +
        s" default: ${defaultParams.fitIntercept}")//是否训练拦截对象
        .action((x, c) => c.copy(fitIntercept = x))
      opt[Double]("regParam")
        .text(s"the regularization parameter for Logistic Regression.")
        .action((x, c) => c.copy(regParam = Some(x)))
      opt[Double]("elasticNetParam")//弹性网络混合参数,0.0为L2正则化 1.0为L1正则化
        .text(s"the ElasticNet mixing parameter for Logistic Regression.")
        .action((x, c) => c.copy(elasticNetParam = Some(x)))
      checkConfig { params =>
        if (params.fracTest < 0 || params.fracTest >= 1) {
          failure(s"fracTest ${params.fracTest} value incorrect; should be in [0,1).")
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

  private def run(params: Params) {
    val conf = new SparkConf().setAppName(s"OneVsRestExample with $params").setMaster("local[*]")
    val sc = new SparkContext(conf)
    /**
 *  libSVM的数据格式
 *  <label> <index1>:<value1> <index2>:<value2> ...
 *  其中<label>是训练数据集的目标值,对于分类,它是标识某类的整数(支持多个类);对于回归,是任意实数
 *  <index>是以1开始的整数,可以是不连续
 *  <value>为实数,也就是我们常说的自变量
 */
    val inputData = MLUtils.loadLibSVMFile(sc, params.input)
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    // compute the train/test split: if testInput is not provided use part of input.
    //计算训练/测试分离,如果testinput不提供使用部分输入
    val data = params.testInput match {
      case Some(t) => {
        // compute the number of features in the training set.
        //在训练集中计算功能的数量
        val numFeatures = inputData.first().features.size
	/**
 *  libSVM的数据格式
 *  <label> <index1>:<value1> <index2>:<value2> ...
 *  其中<label>是训练数据集的目标值,对于分类,它是标识某类的整数(支持多个类);对于回归,是任意实数
 *  <index>是以1开始的整数,可以是不连续
 *  <value>为实数,也就是我们常说的自变量
 */
        val testData = MLUtils.loadLibSVMFile(sc, t, numFeatures)
        Array[RDD[LabeledPoint]](inputData, testData)
      }
      case None => {
        val f = params.fracTest
        inputData.randomSplit(Array(1 - f, f), seed = 12345)
      }
    }
    val Array(train, test) = data.map(_.toDF().cache())

    // instantiate the base classifier
    //实例化的基分类器
    val classifier = new LogisticRegression()
      .setMaxIter(params.maxIter)//迭代次数
      .setTol(params.tol)//迭代算法的收敛
      .setFitIntercept(params.fitIntercept)//是否训练拦截对象

    // Set regParam, elasticNetParam if specified in params
    //设置参数,弹性网参数如果指定参数
    params.regParam.foreach(classifier.setRegParam)
    //弹性网络混合参数,0.0为L2正则化 1.0为L1正则化
    params.elasticNetParam.foreach(classifier.setElasticNetParam)

    // instantiate the One Vs Rest Classifier.
    //实例化的一对多分类器
    val ovr = new OneVsRest()
    //设置分类器
    ovr.setClassifier(classifier)

    // train the multiclass model.
    //训练多类模型
    val (trainingDuration, ovrModel) = time(ovr.fit(train))

    // score the model on test data.
    //评分测试数据模型
    //transform()方法将DataFrame转化为另外一个DataFrame的算法
    val (predictionDuration, predictions) = time(ovrModel.transform(test))
    /**
     *+-----+--------------------+----------+
      |label|            features|prediction|
      +-----+--------------------+----------+
      |  1.0|(692,[158,159,160...|       1.0|
      |  1.0|(692,[127,128,154...|       1.0|
      |  1.0|(692,[128,129,155...|       1.0|
      |  0.0|(692,[123,124,125...|       0.0|
      |  1.0|(692,[129,130,131...|       1.0|
      +-----+--------------------+----------+
     */
    predictions.show()
    // evaluate the model 评估模型
    val predictionsAndLabels = predictions.select("prediction", "label")
    //获取每一行数据
      .map(row => (row.getDouble(0), row.getDouble(1)))
    //评估指标-多分类
    val metrics = new MulticlassMetrics(predictionsAndLabels)
    
    val confusionMatrix = metrics.confusionMatrix //匹配矩阵

    // compute the false positive rate per label 计算每个标签的假阳性率
    val predictionColSchema = predictions.schema("prediction")
    //获得元数据分类数
    val numClasses = MetadataUtils.getNumClasses(predictionColSchema).get
    val fprs = Range(0, numClasses).map(p => (p, metrics.falsePositiveRate(p.toDouble)))
   //Training Time 9 sec
    println(s" Training Time ${trainingDuration} sec\n")
    //Prediction Time 0 sec
    println(s" Prediction Time ${predictionDuration} sec\n")
   /**Confusion Matrix
    9.0  1.0   
    0.0  15.0**/  
    println(s" Confusion Matrix\n ${confusionMatrix.toString}\n")

    
    /**
     * label	fpr
     *   0		0.0
     *   1		0.1
     */
    println("label\tfpr")
    println(fprs.map {case (label, fpr) => label + "\t" + fpr}.mkString("\n"))

    sc.stop()
  }
  /**
   * 柯里化函数,返回类型(Long,R)
   * block:没有声明类型,默认方法
   */  
  private def time[R](block: => R): (Long, R) = {
    //系统计时器的当前值,以毫微秒为单位
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    (NANO.toSeconds(t1 - t0), result)
  }
}
// scalastyle:on println
