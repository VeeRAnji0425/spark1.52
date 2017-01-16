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

import scala.collection.mutable
import scala.language.reflectiveCalls

import scopt.OptionParser

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.examples.mllib.AbstractParams
import org.apache.spark.ml.{Pipeline, PipelineStage}
import org.apache.spark.ml.classification.{RandomForestClassificationModel, RandomForestClassifier}
import org.apache.spark.ml.feature.{StringIndexer, VectorIndexer}
import org.apache.spark.ml.regression.{RandomForestRegressionModel, RandomForestRegressor}
import org.apache.spark.sql.DataFrame


/**
 * An example runner for decision trees. Run with
 * ��������һ������
 * {{{
 * ./bin/run-example ml.RandomForestExample [options]
 * }}}
 * Decision Trees and ensembles can take a large amount of memory.  If the run-example command
 * above fails, try running via spark-submit and specifying the amount of memory as at least 1g.
 * For local mode, run
 * {{{
 * ./bin/spark-submit --class org.apache.spark.examples.ml.RandomForestExample --driver-memory 1g
 *   [examples JAR path] [options]
 * }}}
 * If you use it as a template to create your own app, please use `spark-submit` to submit your app.
 */
object RandomForestExample {

  case class Params(
      input: String = "../data/mllib/sample_multiclass_classification_data.txt",
      testInput: String = "",
      /**
 *  libSVM�����ݸ�ʽ
 *  <label> <index1>:<value1> <index2>:<value2> ...
 *  ����<label>��ѵ�����ݼ���Ŀ��ֵ,���ڷ���,���Ǳ�ʶĳ�������(֧�ֶ����);���ڻع�,������ʵ��
 *  <index>����1��ʼ������,�����ǲ�����
 *  <value>Ϊʵ��,Ҳ�������ǳ�˵���Ա���
 */
      dataFormat: String = "libsvm",
      algo: String = "classification",//����
      maxDepth: Int = 5,//����������
      maxBins: Int = 32,//��ɢ�����Ա���ʱ���ķ�������Ĭ���� 32
      minInstancesPerNode: Int = 1,//���Ѻ��Խڵ����ٰ�����ʵ������
      minInfoGain: Double = 0.0,//���ѽڵ�ʱ������С��Ϣ����
      numTrees: Int = 10,//���ɭ����Ҫѵ�������ĸ�����Ĭ��ֵ�� 20
      featureSubsetStrategy: String = "auto",
      fracTest: Double = 0.2,
      cacheNodeIds: Boolean = false,
      checkpointDir: Option[String] = None,
      //���ü�����(>=1),�����ü���(-1)
      checkpointInterval: Int = 10) extends AbstractParams[Params]

  def main(args: Array[String]) {
    val defaultParams = Params()

    val parser = new OptionParser[Params]("RandomForestExample") {
      head("RandomForestExample: an example random forest app.")
      opt[String]("algo")
        .text(s"algorithm (classification, regression), default: ${defaultParams.algo}")
        .action((x, c) => c.copy(algo = x))
      opt[Int]("maxDepth")
        .text(s"max depth of the tree, default: ${defaultParams.maxDepth}")
        .action((x, c) => c.copy(maxDepth = x))
      opt[Int]("maxBins")
        .text(s"max number of bins, default: ${defaultParams.maxBins}")
        .action((x, c) => c.copy(maxBins = x))
      opt[Int]("minInstancesPerNode")//���Ѻ��Խڵ����ٰ�����ʵ������
        .text(s"min number of instances required at child nodes to create the parent split," +
        s" default: ${defaultParams.minInstancesPerNode}")//���Ѻ��Խڵ����ٰ�����ʵ������
        .action((x, c) => c.copy(minInstancesPerNode = x))//���Ѻ��Խڵ����ٰ�����ʵ������
      opt[Double]("minInfoGain")//���ѽڵ�ʱ������С��Ϣ����
        .text(s"min info gain required to create a split, default: ${defaultParams.minInfoGain}")
        .action((x, c) => c.copy(minInfoGain = x))
      opt[Int]("numTrees")
        .text(s"number of trees in ensemble, default: ${defaultParams.numTrees}")
        .action((x, c) => c.copy(numTrees = x))
      opt[String]("featureSubsetStrategy")
        .text(s"number of features to use per node (supported:" +
        s" ${RandomForestClassifier.supportedFeatureSubsetStrategies.mkString(",")})," +
        s" default: ${defaultParams.numTrees}")
        .action((x, c) => c.copy(featureSubsetStrategy = x))
      opt[Double]("fracTest")
        .text(s"fraction of data to hold out for testing.  If given option testInput, " +
        s"this option is ignored. default: ${defaultParams.fracTest}")
        .action((x, c) => c.copy(fracTest = x))
      opt[Boolean]("cacheNodeIds")
        .text(s"whether to use node Id cache during training, " +
        s"default: ${defaultParams.cacheNodeIds}")
        .action((x, c) => c.copy(cacheNodeIds = x))
      opt[String]("checkpointDir")
        .text(s"checkpoint directory where intermediate node Id caches will be stored, " +
        s"default: ${
          defaultParams.checkpointDir match {
            case Some(strVal) => strVal
            case None => "None"
          }
        }")
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
      /**
 *  libSVM�����ݸ�ʽ
 *  <label> <index1>:<value1> <index2>:<value2> ...
 *  ����<label>��ѵ�����ݼ���Ŀ��ֵ,���ڷ���,���Ǳ�ʶĳ�������(֧�ֶ����);���ڻع�,������ʵ��
 *  <index>����1��ʼ������,�����ǲ�����
 *  <value>Ϊʵ��,Ҳ�������ǳ�˵���Ա���
 */
        .text("data format: libsvm (default), dense (deprecated in Spark v1.1)")
        .action((x, c) => c.copy(dataFormat = x))
      /*arg[String]("<input>")
        .text("input path to labeled examples")
        //.required()
        .action((x, c) => c.copy(input = x))*/
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

  def run(params: Params) {
    val conf = new SparkConf().setAppName(s"RandomForestExample with $params").setMaster("local[*]")
    val sc = new SparkContext(conf)
    params.checkpointDir.foreach(sc.setCheckpointDir)
    val algo = params.algo.toLowerCase

    println(s"RandomForestExample with parameters:\n$params")

    // Load training and test data and cache it.����ѵ���Ͳ������ݲ����仺��
    val (training: DataFrame, test: DataFrame) = DecisionTreeExample.loadDatasets(sc, params.input,
      params.dataFormat, params.testInput, algo, params.fracTest)

    // Set up Pipeline �����ܵ�
     //������ת��,�����ۺ�,ģ�͵����һ���ܵ�,����������fit������ϳ�ģ��
     //һ�� Pipeline �ڽṹ�ϻ����һ������ PipelineStage,ÿһ�� PipelineStage �������һ������
    val stages = new mutable.ArrayBuffer[PipelineStage]()
    // (1) For classification, re-index classes.���ڷ���,����������
    val labelColName = if (algo == "classification") "indexedLabel" else "label"
    if (algo == "classification") {
      val labelIndexer = new StringIndexer()
        .setInputCol("labelString")
        .setOutputCol(labelColName)
      stages += labelIndexer
    }
    // (2) Identify categorical features using VectorIndexer.ȷ��ʹ��vectorindexer��������
    //     Features with more than maxCategories values will be treated as continuous.
    //����maxcategoriesֵ������Ϊ�������ص�
    //VectorIndexer�Ƕ����ݼ����������е����(��ɢֵ)�������б��
    val featuresIndexer = new VectorIndexer()
      .setInputCol("features")
      .setOutputCol("indexedFeatures")
      .setMaxCategories(10)
    stages += featuresIndexer 
    // (3) Learn Random Forest ѧϰ���ɭ��
    val dt = algo match {
      case "classification" =>
        new RandomForestClassifier()
          .setFeaturesCol("indexedFeatures")//ѵ�����ݼ� DataFrame �д洢�������ݵ�����
          .setLabelCol(labelColName)//��ǩ�е�����
          .setMaxDepth(params.maxDepth)//���������ȣ�Ĭ��ֵ�� 5
          .setMaxBins(params.maxBins)//��ɢ�����Ա���ʱ���ķ�������Ĭ���� 32
          .setMinInstancesPerNode(params.minInstancesPerNode)
          .setMinInfoGain(params.minInfoGain)//���ѽڵ�ʱ������С��Ϣ����
          .setCacheNodeIds(params.cacheNodeIds)
          .setCheckpointInterval(params.checkpointInterval)//
          .setFeatureSubsetStrategy(params.featureSubsetStrategy)
          .setNumTrees(params.numTrees)//���ɭ����Ҫѵ�������ĸ�����Ĭ��ֵ�� 20
      case "regression" =>
        new RandomForestRegressor()
          .setFeaturesCol("indexedFeatures")//ѵ�����ݼ� DataFrame �д洢�������ݵ�����
          .setLabelCol(labelColName)//��ǩ�е�����
          .setMaxDepth(params.maxDepth)//���������ȣ�Ĭ��ֵ�� 5
          .setMaxBins(params.maxBins)//��ɢ�����Ա���ʱ���ķ�������Ĭ���� 32
          .setMinInstancesPerNode(params.minInstancesPerNode)//���Ѻ��Խڵ����ٰ�����ʵ������
          .setMinInfoGain(params.minInfoGain)//���ѽڵ�ʱ������С��Ϣ����
          .setCacheNodeIds(params.cacheNodeIds)
          .setCheckpointInterval(params.checkpointInterval)//���ü�����(>=1),�����ü���(-1)
          .setFeatureSubsetStrategy(params.featureSubsetStrategy)
          .setNumTrees(params.numTrees)//���ɭ����Ҫѵ�������ĸ�����Ĭ��ֵ�� 20
      case _ => throw new IllegalArgumentException("Algo ${params.algo} not supported.")
    }
    stages += dt
     //PipeLine:�����DataFrame��Estimator�㷨����һ���ض���ML Wolkflow
     //һ�� Pipeline�ڽṹ�ϻ����һ������ PipelineStage,ÿһ�� PipelineStage �������һ������
    val pipeline = new Pipeline().setStages(stages.toArray)

    // Fit the Pipeline ��װ�ܵ�
    //ϵͳ��ʱ���ĵ�ǰֵ,�Ժ�΢��Ϊ��λ
    val startTime = System.nanoTime()
    //fit()������DataFrameת��Ϊһ��Transformer���㷨
    val pipelineModel = pipeline.fit(training)
    //1e9��Ϊ1*(10�ľŴη�),Ҳ����ʮ��
    val elapsedTime = (System.nanoTime() - startTime) / 1e9
    println(s"Training time: $elapsedTime seconds")

    // Get the trained Random Forest from the fitted PipelineModel
    //�Ӱ�װ�ܵ�ģ�ͣ��õ�ѵ�����ɭ��
    algo match {
      case "classification" =>
        val rfModel = pipelineModel.stages.last.asInstanceOf[RandomForestClassificationModel]
        if (rfModel.totalNumNodes < 30) {
          println(rfModel.toDebugString) // Print full model.
        } else {
          println(rfModel) // Print model summary.
        }
      case "regression" =>
        val rfModel = pipelineModel.stages.last.asInstanceOf[RandomForestRegressionModel]
        if (rfModel.totalNumNodes < 30) {
          println(rfModel.toDebugString) // Print full model.
        } else {
          println(rfModel) // Print model summary.
        }
      case _ => throw new IllegalArgumentException("Algo ${params.algo} not supported.")
    }

    // Evaluate model on training, test data
    //ѵ������ģ��,��������
    algo match {
      case "classification" =>//����
        println("Training data results:")
        DecisionTreeExample.evaluateClassificationModel(pipelineModel, training, labelColName)
        println("Test data results:")
        DecisionTreeExample.evaluateClassificationModel(pipelineModel, test, labelColName)
      case "regression" =>//�ع�
        println("Training data results:")
        DecisionTreeExample.evaluateRegressionModel(pipelineModel, training, labelColName)
        println("Test data results:")
        DecisionTreeExample.evaluateRegressionModel(pipelineModel, test, labelColName)
      case _ =>
        throw new IllegalArgumentException("Algo ${params.algo} not supported.")
    }

    sc.stop()
  }
}
// scalastyle:on println
