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

import scala.language.reflectiveCalls

import scopt.OptionParser

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.{DecisionTree, RandomForest, impurity}
import org.apache.spark.mllib.tree.configuration.{Algo, Strategy}
import org.apache.spark.mllib.tree.configuration.Algo._
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.util.Utils

/**
 * ���е�һ�������������ɭ����������
 * An example runner for decision trees and random forests. Run with
 * {{{
 * ./bin/run-example org.apache.spark.examples.mllib.DecisionTreeRunner [options]
 * }}}
 * If you use it as a template to create your own app, please use `spark-submit` to submit your app.
 *
 * Note: This script treats all features as real-valued (not categorical).
 * 			 ����ű������еĹ��ܶ���Ϊ������ֵ(û�з���)
 *       To include categorical features, modify categoricalFeaturesInfo.
 *       ������������,�޸ķ���������Ϣ
 */
object DecisionTreeRunner {
  /**
   * ����һ��ö������
   */
  object ImpurityType extends Enumeration {
    type ImpurityType = Value
    //����,��,����
    val Gini, Entropy, Variance = Value
  }

  import ImpurityType._

  case class Params(
      input: String = "../data/mllib/sample_binary_classification_data.txt",
      testInput: String = "",
      /**
       *  libSVM�����ݸ�ʽ
       *  <label> <index1>:<value1> <index2>:<value2> ...
       *  ����<label>��ѵ�����ݼ���Ŀ��ֵ,���ڷ���,���Ǳ�ʶĳ�������(֧�ֶ����);���ڻع�,������ʵ��
       *  <index>����1��ʼ������,�����ǲ�����
       *  <value>Ϊʵ��,Ҳ�������ǳ�˵���Ա���
       */
      dataFormat: String = "libsvm",
      algo: Algo = Classification,//�㷨
      maxDepth: Int = 5,//���������ȣ�Ĭ��ֵ�� 5
      impurity: ImpurityType = Gini,//���ڵ�ѡ��Ĳ����ȵĺ���ָ��,ȡֵ�����ǡ�entroy����gini��,Ĭ���ǡ�gini��
      maxBins: Int = 32,//��ɢ�����Ա���ʱ���ķ�����,Ĭ���� 32
      minInstancesPerNode: Int = 1,//���Ѻ��Խڵ����ٰ�����ʵ������
      minInfoGain: Double = 0.0,//���ѽڵ�ʱ������С��Ϣ����
      numTrees: Int = 1,//���ɭ����Ҫѵ�������ĸ���,Ĭ��ֵ�� 20
      featureSubsetStrategy: String = "auto",
      fracTest: Double = 0.2,
      useNodeIdCache: Boolean = false,
      checkpointDir: Option[String] = None,
      //���ü�����(>=1),�����ü���(-1)
      checkpointInterval: Int = 10) extends AbstractParams[Params]

  def main(args: Array[String]) {
    val defaultParams = Params()

    val parser = new OptionParser[Params]("DecisionTreeRunner") {
      head("DecisionTreeRunner: an example decision tree app.")
      opt[String]("algo")
        .text(s"algorithm (${Algo.values.mkString(",")}), default: ${defaultParams.algo}")
        .action((x, c) => c.copy(algo = Algo.withName(x)))
      opt[String]("impurity")
        .text(s"impurity type (${ImpurityType.values.mkString(",")}), " +
          s"default: ${defaultParams.impurity}")
        .action((x, c) => c.copy(impurity = ImpurityType.withName(x)))
      opt[Int]("maxDepth")
        .text(s"max depth of the tree, default: ${defaultParams.maxDepth}")
        .action((x, c) => c.copy(maxDepth = x))
      opt[Int]("maxBins")
        .text(s"max number of bins, default: ${defaultParams.maxBins}")
        .action((x, c) => c.copy(maxBins = x))
      opt[Int]("minInstancesPerNode")
        .text(s"min number of instances required at child nodes to create the parent split," +
          s" default: ${defaultParams.minInstancesPerNode}")
        .action((x, c) => c.copy(minInstancesPerNode = x))//���Ѻ��Խڵ����ٰ�����ʵ������
      opt[Double]("minInfoGain")//���ѽڵ�ʱ������С��Ϣ����
        .text(s"min info gain required to create a split, default: ${defaultParams.minInfoGain}")
        .action((x, c) => c.copy(minInfoGain = x))//���ѽڵ�ʱ������С��Ϣ����
      opt[Int]("numTrees")
        .text(s"number of trees (1 = decision tree, 2+ = random forest)," +
          s" default: ${defaultParams.numTrees}")
        .action((x, c) => c.copy(numTrees = x))
      /*opt[String]("featureSubsetStrategy")
        .text(s"feature subset sampling strategy" +
          s" (${RandomForest.supportedFeatureSubsetStrategies.mkString(", ")}), " +
          s"default: ${defaultParams.featureSubsetStrategy}")
        .action((x, c) => c.copy(featureSubsetStrategy = x))*/
      opt[Double]("fracTest")
        .text(s"fraction of data to hold out for testing.  If given option testInput, " +
          s"this option is ignored. default: ${defaultParams.fracTest}")
        .action((x, c) => c.copy(fracTest = x))
      opt[Boolean]("useNodeIdCache")
        .text(s"whether to use node Id cache during training, " +
          s"default: ${defaultParams.useNodeIdCache}")
        .action((x, c) => c.copy(useNodeIdCache = x))
      opt[String]("checkpointDir")
        .text(s"checkpoint directory where intermediate node Id caches will be stored, " +
         s"default: ${defaultParams.checkpointDir match {
           case Some(strVal) => strVal
           case None => "None"
         }}")
        .action((x, c) => c.copy(checkpointDir = Some(x)))
      opt[Int]("checkpointInterval")
        .text(s"how often to checkpoint the node Id cache, " +
         s"default: ${defaultParams.checkpointInterval}")
        .action((x, c) => c.copy(checkpointInterval = x))
      opt[String]("testInput")
        .text(s"input path to test dataset.  If given, option fracTest is ignored." +
          s" default: ${defaultParams.testInput}")
        .action((x, c) => c.copy(testInput = x))
      opt[String]("dataFormat")
        .text("data format: libsvm (default), dense (deprecated in Spark v1.1)")
        .action((x, c) => c.copy(dataFormat = x))
      /*arg[String]("<input>")
        .text("input path to labeled examples")
        .required()
        .action((x, c) => c.copy(input = x))*/
      checkConfig { params =>
        if (params.fracTest < 0 || params.fracTest > 1) {
          failure(s"fracTest ${params.fracTest} value incorrect; should be in [0,1].")
        } else {
          if (params.algo == Classification &&
            (params.impurity == Gini || params.impurity == Entropy)) {
            success
          } else if (params.algo == Regression && params.impurity == Variance) {
            success
          } else {
            failure(s"Algo ${params.algo} is not compatible with impurity ${params.impurity}.")
          }
        }
      }
    }

    parser.parse(args, defaultParams).map { params =>
      run(params)
    }.getOrElse {
      sys.exit(1)
    }
  }

  /**
   * Load training and test data from files.
   * ���ļ��м���ѵ���Ͳ�������
   * @param input  Path to input dataset.
   * @param dataFormat  "libsvm" or "dense"
   * @param testInput  Path to test dataset.
   * @param algo  Classification or Regression �����ع�
   * @param fracTest  Fraction of input data to hold out for testing.  Ignored if testInput given.
   * @return  (training dataset, test dataset, number of classes),
   *          where the number of classes is inferred from data (and set to 0 for Regression)
   */
  private[mllib] def loadDatasets(
      sc: SparkContext,
      input: String,
      dataFormat: String,
      testInput: String,
      algo: Algo,
      fracTest: Double): (RDD[LabeledPoint], RDD[LabeledPoint], Int) = {
    // Load training data and cache it.
    //����ѵ�����ݲ����仺��
    val origExamples = dataFormat match {
    //LabeledPoint��ǵ��Ǿֲ�����,�����������ܼ��ͻ���ϡ����,ÿ�������������һ����ǩ(label)
      case "dense" => MLUtils.loadLabeledPoints(sc, input).cache()
      /**
       *  libSVM�����ݸ�ʽ
       *  <label> <index1>:<value1> <index2>:<value2> ...
       *  ����<label>��ѵ�����ݼ���Ŀ��ֵ,���ڷ���,���Ǳ�ʶĳ�������(֧�ֶ����);���ڻع�,������ʵ��
       *  <index>����1��ʼ������,�����ǲ�����
       *  <value>Ϊʵ��,Ҳ�������ǳ�˵���Ա���
       */
      case "libsvm" => MLUtils.loadLibSVMFile(sc, input).cache()
    }
    // For classification, re-index classes if needed.
    //���ڷ���,�����Ҫ�Ļ�,����������
    val (examples, classIndexMap, numClasses) = algo match {
      case Classification => {
        // classCounts: class --> # examples in class
        val classCounts = origExamples.map(_.label).countByValue()
        val sortedClasses = classCounts.keys.toList.sorted
        val numClasses = classCounts.size
        // classIndexMap: class --> index in 0,...,numClasses-1
        val classIndexMap = {
          if (classCounts.keySet != Set(0.0, 1.0)) {
            sortedClasses.zipWithIndex.toMap
          } else {
            Map[Double, Int]()
          }
        }
        val examples = {
          if (classIndexMap.isEmpty) {
            origExamples
          } else {
	  //LabeledPoint��ǵ��Ǿֲ�����,�����������ܼ��ͻ���ϡ����,ÿ�������������һ����ǩ(label)
            origExamples.map(lp => LabeledPoint(classIndexMap(lp.label), lp.features))
          }
        }
        val numExamples = examples.count()
        //numClasses = 2
        println(s"numClasses = $numClasses.")
        println(s"Per-class example fractions, counts:")
        println(s"Class\tFrac\tCount")
        /*Class	Frac	Count
            0.0	0.43	43
            1.0	0.57	57*/
        sortedClasses.foreach { c =>
          val frac = classCounts(c) / numExamples.toDouble
          println(s"$c\t$frac\t${classCounts(c)}")
        }
        (examples, classIndexMap, numClasses)
      }
      case Regression =>
        (origExamples, null, 0)
      case _ =>
        throw new IllegalArgumentException("Algo ${params.algo} not supported.")
    }

    // Create training, test sets.
    //����ѵ��,���Լ�
    val splits = if (testInput != "") {
      // Load testInput. ���ز�������
      val numFeatures = examples.take(1)(0).features.size
      val origTestExamples = dataFormat match {
        case "dense" => MLUtils.loadLabeledPoints(sc, testInput)
    	/**
       *  libSVM�����ݸ�ʽ
       *  <label> <index1>:<value1> <index2>:<value2> ...
       *  ����<label>��ѵ�����ݼ���Ŀ��ֵ,���ڷ���,���Ǳ�ʶĳ�������(֧�ֶ����);���ڻع�,������ʵ��
       *  <index>����1��ʼ������,�����ǲ�����
       *  <value>Ϊʵ��,Ҳ�������ǳ�˵���Ա���
       */
        case "libsvm" => MLUtils.loadLibSVMFile(sc, testInput, numFeatures)
      }
      algo match {
        case Classification => {
          // classCounts: class --> # examples in class
          val testExamples = {
            if (classIndexMap.isEmpty) {
              origTestExamples
            } else {
	    //LabeledPoint��ǵ��Ǿֲ�����,�����������ܼ��ͻ���ϡ����,ÿ�������������һ����ǩ(label)
              origTestExamples.map(lp => LabeledPoint(classIndexMap(lp.label), lp.features))
            }
          }
          Array(examples, testExamples)
        }
        case Regression =>
          Array(examples, origTestExamples)
      }
    } else {
      // Split input into training, test.
      //��������Ϊѵ��,����
      examples.randomSplit(Array(1.0 - fracTest, fracTest))
    }
    val training = splits(0).cache()
    val test = splits(1).cache()

    val numTraining = training.count()
    val numTest = test.count()
    //numTraining = 84, numTest = 16.
    println(s"numTraining = $numTraining, numTest = $numTest.")

    examples.unpersist(blocking = false)

    (training, test, numClasses)
  }

  def run(params: Params) {

    val conf = new SparkConf().setAppName(s"DecisionTreeRunner with $params").setMaster("local")
    val sc = new SparkContext(conf)
    /**
      {
        input:	../data/mllib/sample_binary_classification_data.txt,
        testInput:	,
        dataFormat:	libsvm,
        algo:	Classification,
        maxDepth:	5,
        impurity:	Gini,
        maxBins:	32,
        minInstancesPerNode:	1,
        minInfoGain:	0.0,
        numTrees:	1,
        featureSubsetStrategy:	auto,
        fracTest:	0.2,
        useNodeIdCache:	false,
        checkpointDir:	None,
        checkpointInterval:	10
      }*/
    println(s"DecisionTreeRunner with parameters:\n$params")

    // Load training and test data and cache it.
    //����ѵ���Ͳ������ݲ����仺��
    val (training, test, numClasses) = loadDatasets(sc, params.input, params.dataFormat,
      params.testInput, params.algo, params.fracTest)

    val impurityCalculator = params.impurity match {
      case Gini => impurity.Gini //����
      case Entropy => impurity.Entropy //��
      case Variance => impurity.Variance //����
    }

    params.checkpointDir.foreach(sc.setCheckpointDir)
  /**
         ��������maxDepth
  	��С��Ϣ����minInfoGain
          ��С�ӽڵ�ʵ����minInstancesPerNode*/
    val strategy
      = new Strategy(
          algo = params.algo,
          impurity = impurityCalculator,//������Ϣ�����׼��
          maxDepth = params.maxDepth,//����������(>=0)
          maxBins = params.maxBins,//����������ɢ�����������,�Լ�ѡ��ÿ���ڵ���������ķ�ʽ
          numClasses = numClasses,//ѵ������������
          minInstancesPerNode = params.minInstancesPerNode,//���Ѻ��Խڵ����ٰ�����ʵ������
          minInfoGain = params.minInfoGain,//���ѽڵ�ʱ������С��Ϣ����
          useNodeIdCache = params.useNodeIdCache,//ʹ��RDDÿ�еĽڵ�ID����
	        //���ü�����(>=1),�����ü���(-1)
          checkpointInterval = params.checkpointInterval)
    if (params.numTrees == 1) {//ѵ������������
      //ϵͳ��ʱ���ĵ�ǰֵ,�Ժ�΢��Ϊ��λ
      val startTime = System.nanoTime()
      val model = DecisionTree.train(training, strategy)
      //1e9��Ϊ1*(10�ľŴη�),Ҳ����ʮ��
      val elapsedTime = (System.nanoTime() - startTime) / 1e9
      println(s"Training time: $elapsedTime seconds")
      if (model.numNodes < 20) {
        println(model.toDebugString) // Print full model.
      } else {
        println(model) // Print model summary.
      }
      if (params.algo == Classification) {//����
       //����ָ��-�����
        val trainAccuracy =
          new MulticlassMetrics(training.map(lp => (model.predict(lp.features), lp.label)))
            .precision
        println(s"Train accuracy = $trainAccuracy")
	     //����ָ��-�����
        val testAccuracy =
          new MulticlassMetrics(test.map(lp => (model.predict(lp.features), lp.label))).precision
          //Test accuracy = 1.0
        println(s"Test accuracy = $testAccuracy")
      }
      if (params.algo == Regression) {//�ع�
        val trainMSE = meanSquaredError(model, training)
        println(s"Train mean squared error = $trainMSE")
        val testMSE = meanSquaredError(model, test)
        println(s"Test mean squared error = $testMSE")
      }
    } else {
      val randomSeed = Utils.random.nextInt()
      //������
      if (params.algo == Classification) {
       //ϵͳ��ʱ���ĵ�ǰֵ,�Ժ�΢��Ϊ��λ
        val startTime = System.nanoTime()
        val model = RandomForest.trainClassifier(training, strategy, params.numTrees,
          params.featureSubsetStrategy, randomSeed)
	  		//1e9��Ϊ1*(10�ľŴη�),Ҳ����ʮ��
        val elapsedTime = (System.nanoTime() - startTime) / 1e9
        //Training time: 5.574476103 seconds
        println(s"Training time: $elapsedTime seconds")
        if (model.totalNumNodes < 30) {
          /**
           DecisionTreeModel classifier of depth 2 with 5 nodes
            If (feature 434 <= 0.0)
             If (feature 100 <= 165.0)
              Predict: 0.0
             Else (feature 100 > 165.0)
              Predict: 1.0
            Else (feature 434 > 0.0)
             Predict: 1.0*/
          println(model.toDebugString) // Print full model.
        } else {
          println(model) // Print model summary.
        }
        
        val trainAccuracy =
          new MulticlassMetrics(training.map(lp => (model.predict(lp.features), lp.label)))
            .precision
        //Train accuracy = 1.0,ѵ��׼ȷ��
        println(s"Train accuracy = $trainAccuracy")
        val testAccuracy =
          new MulticlassMetrics(test.map(lp => (model.predict(lp.features), lp.label))).precision
        //����׼ȷ��
        println(s"Test accuracy = $testAccuracy")
      }
    if (params.algo == Regression) {
        //ϵͳ��ʱ���ĵ�ǰֵ,�Ժ�΢��Ϊ��λ
        val startTime = System.nanoTime()
        val model = RandomForest.trainRegressor(training, strategy, params.numTrees,
          params.featureSubsetStrategy, randomSeed)
				//1e9��Ϊ1*(10�ľŴη�),Ҳ����ʮ��
        val elapsedTime = (System.nanoTime() - startTime) / 1e9
        println(s"Training time: $elapsedTime seconds")
        if (model.totalNumNodes < 30) {
          println(model.toDebugString) // Print full model.
        } else {
          println(model) // Print model summary.
        }
        val trainMSE = meanSquaredError(model, training)
        println(s"Train mean squared error = $trainMSE")
        val testMSE = meanSquaredError(model, test)
        println(s"Test mean squared error = $testMSE")
      }
    }

    sc.stop()
  }

  /**
   * Calculates the mean squared error for regression.
   * ����ع��ƽ��ƽ�����
   * This is just for demo purpose. In general, don't copy this code because it is NOT efficient
   * due to the use of structural types, which leads to one reflection call per record.
   * ��ֻ��Ϊ����ʾ��Ŀ��,һ����˵,��Ҫ���ƴ˴���,����ʹ�õĲ�ͬ�ṹ����,����ÿ����¼��һ���������
   */
  // scalastyle:off structural.type
  private[mllib] def meanSquaredError(
      model: { def predict(features: Vector): Double },
      data: RDD[LabeledPoint]): Double = {
    data.map { y =>
      val predict=model.predict(y.features)
      val err = predict - y.label
      err * err
    }.mean()
  }
  // scalastyle:on structural.type
}
// scalastyle:on println
