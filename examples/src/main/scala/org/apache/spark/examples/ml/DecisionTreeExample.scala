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
import org.apache.spark.ml.{Pipeline, PipelineStage, Transformer}
import org.apache.spark.ml.classification.{DecisionTreeClassificationModel, DecisionTreeClassifier}
import org.apache.spark.ml.feature.{VectorIndexer, StringIndexer}
import org.apache.spark.ml.regression.{DecisionTreeRegressionModel, DecisionTreeRegressor}
import org.apache.spark.ml.util.MetadataUtils
import org.apache.spark.mllib.evaluation.{RegressionMetrics, MulticlassMetrics}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{SQLContext, DataFrame}


/**
 * An example runner for decision trees. Run with
 * ��������һ������
 * {{{
 * ./bin/run-example ml.DecisionTreeExample [options]
 * }}}
 * Note that Decision Trees can take a large amount of memory.  If the run-example command above
 * ��ע��,���������Բ�ȡ�������ڴ�,������������ʾ������ʧ��,������ͨ��Spark�ύָ�����ڴ���Ϊ����1G
 * fails, try running via spark-submit and specifying the amount of memory as at least 1g.
 * For local mode, run ����ģʽ
 * {{{
 * ./bin/spark-submit --class org.apache.spark.examples.ml.DecisionTreeExample --driver-memory 1g
 *   [examples JAR path] [options]
 * }}}
 * If you use it as a template to create your own app, please use `spark-submit` to submit your app.
 */
object DecisionTreeExample {

  case class Params(
      input: String = "../data/mllib/rf_libsvm_data.txt",
      testInput: String = "",
      dataFormat: String = "libsvm",
      algo: String = "Classification",//"regression",�㷨����
      maxDepth: Int = 5,//������
      maxBins: Int = 32,//����������ɢ�����������,�Լ�ѡ��ÿ���ڵ���������ķ�ʽ
      minInstancesPerNode: Int = 1,//���Ѻ��Խڵ����ٰ�����ʵ������
      minInfoGain: Double = 0.0,//���ѽڵ�ʱ������С��Ϣ����
      fracTest: Double = 0.2,//��������
      cacheNodeIds: Boolean = false,
      checkpointDir: Option[String] = None,//����Ŀ¼
      //���ü�����(>=1),�����ü���(-1)
      checkpointInterval: Int = 10) extends AbstractParams[Params]

  def main(args: Array[String]) {
    val defaultParams = Params()

    val parser = new OptionParser[Params]("DecisionTreeExample") {
      head("DecisionTreeExample: an example decision tree app.")
      opt[String]("algo")
        .text(s"algorithm (classification, regression), default: ${defaultParams.algo}")
        .action((x, c) => c.copy(algo = x))
      opt[Int]("maxDepth")
        .text(s"max depth of the tree, default: ${defaultParams.maxDepth}")
        .action((x, c) => c.copy(maxDepth = x))
      opt[Int]("maxBins")
        .text(s"max number of bins, default: ${defaultParams.maxBins}")
        .action((x, c) => c.copy(maxBins = x))
      opt[Int]("minInstancesPerNode")
        .text(s"min number of instances required at child nodes to create the parent split," +
          s" default: ${defaultParams.minInstancesPerNode}")
        .action((x, c) => c.copy(minInstancesPerNode = x))
      opt[Double]("minInfoGain")//���ѽڵ�ʱ������С��Ϣ����
        .text(s"min info gain required to create a split, default: ${defaultParams.minInfoGain}")
        .action((x, c) => c.copy(minInfoGain = x))
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
         s"default: ${defaultParams.checkpointDir match {
           case Some(strVal) => strVal
           case None => "None"
         }}")
        .action((x, c) => c.copy(checkpointDir = Some(x)))
      opt[Int]("checkpointInterval")//���ü�����(>=1),�����ü���(-1)
        .text(s"how often to checkpoint the node Id cache, " +
         s"default: ${defaultParams.checkpointInterval}")//���ü�����(>=1),�����ü���(-1)
        .action((x, c) => c.copy(checkpointInterval = x))//���ü�����(>=1),�����ü���(-1)
      opt[String]("testInput")
        .text(s"input path to test dataset.  If given, option fracTest is ignored." +
          s" default: ${defaultParams.testInput}")
        .action((x, c) => c.copy(testInput = x))
      opt[String]("dataFormat")
        .text("data format: libsvm (default), dense (deprecated in Spark v1.1)")
        .action((x, c) => c.copy(dataFormat = x))
      //arg[String]("<input>")
       // .text("input path to labeled examples")
        //.required()
       // .action((x, c) => c.copy(input = x))
      checkConfig { params =>
        if (params.fracTest < 0 || params.fracTest >= 1) {
          failure(s"fracTest ${params.fracTest} value incorrect; should be in [0,1).")
        } else {
          success
        }
      }
    }

    parser.parse(args, defaultParams).map { params =>{
      //println(">>>>>>>>>")
      run(params)
      }
    }.getOrElse {
      sys.exit(1)
    }
  }

  /** 
   *  Load a dataset from the given path, using the given format
   *  �Ӹ�����·���������ݼ�,ʹ�ø����ĸ�ʽ
   *   */
  private[ml] def loadData(
      sc: SparkContext,
      path: String,
      format: String,
      expectedNumFeatures: Option[Int] = None): RDD[LabeledPoint] = {
    format match {
    //LabeledPoint��ǵ��Ǿֲ�����,�����������ܼ��ͻ���ϡ����,ÿ�������������һ����ǩ(label)
      case "dense" => MLUtils.loadLabeledPoints(sc, path)
      case "libsvm" => expectedNumFeatures match {
        case Some(numFeatures) => MLUtils.loadLibSVMFile(sc, path, numFeatures)
      	/**
       	*  libSVM�����ݸ�ʽ
       	*  <label> <index1>:<value1> <index2>:<value2> ...
       	*  ����<label>��ѵ�����ݼ���Ŀ��ֵ,���ڷ���,���Ǳ�ʶĳ�������(֧�ֶ����);���ڻع�,������ʵ��
       	*  <index>����1��ʼ������,�����ǲ�����
       	*  <value>Ϊʵ��,Ҳ�������ǳ�˵���Ա���
       	*/
        case None => MLUtils.loadLibSVMFile(sc, path)
      }
      case _ => throw new IllegalArgumentException(s"Bad data format: $format")
    }
  }

  /**
   * Load training and test data from files.
   * ���ļ��м���ѵ���Ͳ�������
   * @param input  Path to input dataset. �������ݼ���·��
   * @param dataFormat  "libsvm" or "dense"
   * @param testInput  Path to test dataset. �������ݼ���·��
   * @param algo  Classification or Regression �����ع�
   * @param fracTest  Fraction of input data to hold out for testing.  Ignored if testInput given.
   * 				���ڲ��Ե��������ݵķ���,���testinput��������
   * @return  (training dataset, test dataset)
   */
  private[ml] def loadDatasets(
      sc: SparkContext,
      input: String,
      dataFormat: String,
      testInput: String,
      algo: String,
      fracTest: Double): (DataFrame, DataFrame) = {
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    // Load training data ���� ѵ������
    val origExamples: RDD[LabeledPoint] = loadData(sc, input, dataFormat)

    // Load or create test set ���ػ򴴽����Լ�
    val splits: Array[RDD[LabeledPoint]] = if (testInput != "") {
      // Load testInput. 
      val numFeatures = origExamples.take(1)(0).features.size
     
      val origTestExamples: RDD[LabeledPoint] =
        loadData(sc, testInput, dataFormat, Some(numFeatures))
      Array(origExamples, origTestExamples)
    } else {
      // Split input into training, test. ��������Ϊѵ��,����
      origExamples.randomSplit(Array(1.0 - fracTest, fracTest), seed = 12345)
    }

    // For classification, convert labels to Strings since we will index them later with    
    // StringIndexer.
    //���з���,��Ϊ���ǽ����������Ժ�,����ǩת��Ϊ�ַ���
    def labelsToStrings(data: DataFrame): DataFrame = {
      algo.toLowerCase match {
        case "classification" =>//����㷨����,��������ֶ�
          //withColumn��������ʵ�ֶ�dataframe���е����,data��ȡ�ֶ�"label"����,ǿ��ת���ַ��Ŵ�����
          data.withColumn("labelString", data("label").cast(StringType))
        case "regression" =>
          data
        case _ =>
          throw new IllegalArgumentException("Algo ${params.algo} not supported.")
      }
    }
    val dataframes = splits.map(_.toDF()).map(labelsToStrings)   
    val training = dataframes(0).cache()
    val test = dataframes(1).cache()

    val numTraining = training.count()//ѵ����������
    val numTest = test.count()//������������
   /** 
    +-----+--------------------+-----------+
    |label|            features|labelString|
    +-----+--------------------+-----------+
    |  0.0| (6,[4,5],[1.0,1.0])|        0.0|
    |  0.0|(6,[2,4,5],[1.0,1...|        0.0|
    |  0.0| (6,[4,5],[1.0,1.0])|        0.0|
    |  1.0|(6,[0,1,2,3],[1.0...|        1.0|
    |  1.0|(6,[1,2,3],[1.0,1...|        1.0|
    +-----+--------------------+-----------+**/
    training.show()
    //��ȡ������
    val numFeatures = training.select("features").first().getAs[Vector](0).size
    //(6,[4,5],[1.0,1.0])
    val tset1=training.select("features").first().getAs[Vector](0)//(0)ȡ1������
     println("==="+tset1)
    println("Loaded data:")
    //ѵ������: numTraining = 5, ��������:numTest = 4
    println(s"  numTraining = $numTraining, numTest = $numTest")
    //������:numFeatures = 6
    println(s"  numFeatures = $numFeatures")
    (training, test)
  }

  def run(params: Params) {
    val conf = new SparkConf().setAppName(s"DecisionTreeExample with $params").setMaster("local[*]")
    val sc = new SparkContext(conf)
    params.checkpointDir.foreach(sc.setCheckpointDir)
    val algo = params.algo.toLowerCase

    println(s"DecisionTreeExample with parameters:\n$params")

    // Load training and test data and cache it. ����ѵ���Ͳ������ݲ����仺��
    val (training: DataFrame, test: DataFrame) =
      loadDatasets(sc, params.input, params.dataFormat, params.testInput, algo, params.fracTest)

    // Set up Pipeline �����ܵ�
    //������ת��,�����ۺ�,ģ�͵����һ���ܵ�,����������fit������ϳ�ģ��*/  
    //һ�� Pipeline �ڽṹ�ϻ����һ������ PipelineStage,ÿһ�� PipelineStage �������һ������
    val stages = new mutable.ArrayBuffer[PipelineStage]()
    // (1) For classification, re-index classes. ���ڷ���ģ�ͱ�ǩ����:indexedLabel,�ع�ģ�ͱ�ǩ����:label
    val labelColName = if (algo == "classification") "indexedLabel" else "label"
    if (algo == "classification") {
      val labelIndexer = new StringIndexer()
        .setInputCol("labelString")
        .setOutputCol(labelColName)
      stages += labelIndexer
    }
    // (2) Identify categorical features using VectorIndexer.
    //     ȷ��ʹ��vectorindexer��������
    //     Features with more than maxCategories values will be treated as continuous.
    //    VectorIndexer�Ƕ����ݼ����������е����(��ɢֵ)�������б��
    val featuresIndexer = new VectorIndexer()
      .setInputCol("features")
      .setOutputCol("indexedFeatures")
      .setMaxCategories(10)
    stages += featuresIndexer
    // (3) Learn Decision Tree ѧϰ������
    val dt = algo match {
      case "classification" =>//����
        new DecisionTreeClassifier()
	 //ѵ�����ݼ�DataFrame�д洢�������ݵ�����
          .setFeaturesCol("indexedFeatures")//������
          .setLabelCol(labelColName)//��ǩ��
          .setMaxDepth(params.maxDepth)//������
          .setMaxBins(params.maxBins)//����������ɢ��������������Լ�ѡ��ÿ���ڵ���������ķ�ʽ
          .setMinInstancesPerNode(params.minInstancesPerNode)//���Ѻ��Խڵ����ٰ�����ʵ������
          .setMinInfoGain(params.minInfoGain)//���ѽڵ�ʱ������С��Ϣ����
          .setCacheNodeIds(params.cacheNodeIds)//
          .setCheckpointInterval(params.checkpointInterval)//���ü�����(>=1)
      case "regression" =>//�ع�
        new DecisionTreeRegressor()
          .setFeaturesCol("indexedFeatures")//������
          .setLabelCol(labelColName)//��ǩ��
          .setMaxDepth(params.maxDepth)//������
          .setMaxBins(params.maxBins)//����������ɢ�����������
          .setMinInstancesPerNode(params.minInstancesPerNode)//���Ѻ��Խڵ����ٰ�����ʵ������
          .setMinInfoGain(params.minInfoGain)//���ѽڵ�ʱ������С��Ϣ����
          .setCacheNodeIds(params.cacheNodeIds)
          .setCheckpointInterval(params.checkpointInterval)//���ü�����(>=1)
      case _ => throw new IllegalArgumentException("Algo ${params.algo} not supported.")
    }
    stages += dt
     //PipeLine:�����DataFrame��Estimator�㷨����һ���ض���ML Wolkflow
    //һ�� Pipeline�ڽṹ�ϻ����һ������ PipelineStage,ÿһ�� PipelineStage �������һ������
    val pipeline = new Pipeline().setStages(stages.toArray)

    // Fit the Pipeline ��װ�ܵ�
    // ϵͳ��ʱ���ĵ�ǰֵ,�Ժ�΢��Ϊ��λ
    val startTime = System.nanoTime()
    //fit()������DataFrameת��Ϊһ��Transformer���㷨
    val pipelineModel = pipeline.fit(training)
    //1e9��Ϊ1*(10�ľŴη�),Ҳ����ʮ��
    val elapsedTime = (System.nanoTime() - startTime) / 1e9
    //��ӡѵ��ʹ�õ�ʱ��    
    println(s"Training time: $elapsedTime seconds")

    // Get the trained Decision Tree from the fitted PipelineModel
    //����ϵĹܵ�ģ���еõ�ѵ���ľ�����
    algo match {
      case "classification" =>//����
        //��ùܵ���ѵ�������ģ�;�����
        val treeModel = pipelineModel.stages.last.asInstanceOf[DecisionTreeClassificationModel]
        if (treeModel.numNodes < 20) {//�ڵ���
          println(treeModel.toDebugString) // Print full model. ��ӡ������ģ��
        } else {
          println(treeModel) // Print model summary. ��ӡģ������
        }
      case "regression" =>//�ع�
        //��ùܵ�ģ����ѵ���ľ�����
        val treeModel = pipelineModel.stages.last.asInstanceOf[DecisionTreeRegressionModel]
        if (treeModel.numNodes < 20) {
          println(treeModel.toDebugString) // Print full model.
        } else {
          println(treeModel) // Print model summary.
        }
      case _ => throw new IllegalArgumentException("Algo ${params.algo} not supported.")
    }

    // Evaluate model on training, test data ѵ������ģ�ͣ���������
    algo match {
      case "classification" =>//����
        println("Training data results:")
        evaluateClassificationModel(pipelineModel, training, labelColName)
        println("Test data results:")
        evaluateClassificationModel(pipelineModel, test, labelColName)
      case "regression" =>//�ع�
        println("Training data results:")
        evaluateRegressionModel(pipelineModel, training, labelColName)
        println("Test data results:")
        evaluateRegressionModel(pipelineModel, test, labelColName)
      case _ =>
        throw new IllegalArgumentException("Algo ${params.algo} not supported.")
    }

    sc.stop()
  }

  /**
   * Evaluate the given ClassificationModel on data.  Print the results.
   * ���������ķ���ģ�����ݲ���ӡ���
   * @param model  Must fit ClassificationModel abstraction �����ʺϷ���ģ�ͳ���
   * @param data  DataFrame with "prediction" and labelColName columns ��Ԥ�⡱��labelcolname�����ݿ�
   * @param labelColName  Name of the labelCol parameter for the model ��ģ�͵�labelcol��������
   *
   * TODO: Change model type to ClassificationModel once that API is public. SPARK-5995
   */
  private[ml] def evaluateClassificationModel(
      model: Transformer,
      data: DataFrame,
      labelColName: String): Unit = {
      //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    val fullPredictions = model.transform(data).cache()
    //���Ԥ����
    /**
    +-----+--------------------+-----------+------------+--------------------+-------------+-----------+----------+
    |label|            features|labelString|indexedLabel|     indexedFeatures|rawPrediction|probability|prediction|
    +-----+--------------------+-----------+------------+--------------------+-------------+-----------+----------+
    |  1.0|(6,[0,1,2,3],[1.0...|        1.0|         1.0|(6,[0,1,2,3],[1.0...|    [0.0,2.0]|  [0.0,1.0]|       1.0|
    |  1.0|(6,[0,1,3],[1.0,1...|        1.0|         1.0|(6,[0,1,3],[1.0,1...|    [0.0,2.0]|  [0.0,1.0]|       1.0|
    |  0.0|(6,[0,4,5],[1.0,1...|        0.0|         0.0|(6,[0,4,5],[1.0,1...|    [3.0,0.0]|  [1.0,0.0]|       0.0|
    |  0.0|(6,[0,4,5],[1.0,1...|        0.0|         0.0|(6,[0,4,5],[1.0,1...|    [3.0,0.0]|  [1.0,0.0]|       0.0|
    +-----+--------------------+-----------+------------+--------------------+-------------+-----------+----------+**/
    fullPredictions.show(5)
    val predictions = fullPredictions.select("prediction").map(_.getDouble(0))
    //���label�е�ֵ
    val labels = fullPredictions.select(labelColName).map(_.getDouble(0))
    // Print number of classes for reference
    /**
     * fullPredictions.schema("indexedLabel")
     * StructField(indexedLabel,DoubleType,true)||||indexedLabel
     */
    println(fullPredictions.schema(labelColName)+"||||"+labelColName)
    //getNumClasses ���һ��schema��ʶ��ǩ���з������Ŀ
    val numClasses = MetadataUtils.getNumClasses(fullPredictions.schema(labelColName)) match {
      case Some(n) => n
      case None => throw new RuntimeException(
        //�����ǩ����ʱ����δ֪����
        "Unknown failure when indexing labels for classification.")
    }
    //(1.0,1.0)
    //(1.0,1.0)
    predictions.zip(labels).foreach(println _)
    //����ָ��-�����
    val accuracy = new MulticlassMetrics(predictions.zip(labels)).precision
    println(s"  Accuracy ($numClasses classes): $accuracy")
  }

  /**
   * Evaluate the given RegressionModel on data.  Print the results.
   * ���������ع�ģ�����ݲ���ӡ���
   * @param model  Must fit RegressionModel abstraction ����ع�ģ�͵ĳ���
   * @param data  DataFrame with "prediction" and labelColName columns ���ݼ�����prediction����
   * @param labelColName  Name of the labelCol parameter for the model ��ģ�͵ı�ǩ�еĲ�������
   *
   * TODO: Change model type to RegressionModel once that API is public. SPARK-5995
   */
  private[ml] def evaluateRegressionModel(
      model: Transformer,
      data: DataFrame,
      labelColName: String): Unit = {
      //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    val fullPredictions = model.transform(data).cache()
    /***
     * 
     *+-----+--------------------+--------------------+----------+
      |label|            features|     indexedFeatures|prediction|
      +-----+--------------------+--------------------+----------+
      |  1.0|(6,[0,1,2,3],[1.0...|(6,[0,1,2,3],[1.0...|       1.0|
      |  1.0|(6,[0,1,3],[1.0,1...|(6,[0,1,3],[1.0,1...|       1.0|
      |  0.0|(6,[0,4,5],[1.0,1...|(6,[0,4,5],[1.0,1...|       0.0|
      |  0.0|(6,[0,4,5],[1.0,1...|(6,[0,4,5],[1.0,1...|       0.0|
      +-----+--------------------+--------------------+----------+
     */
    fullPredictions.show()    
    val predictions = fullPredictions.select("prediction").map(_.getDouble(0))
    val labels = fullPredictions.select(labelColName).map(_.getDouble(0))    
    //(1.0,1.0)
    //(1.0,1.0)
    val zip=predictions.zip(labels)    
   // println("zip:"+zip.)
    //RMSE ���������
    val RMSE = new RegressionMetrics(predictions.zip(labels)).rootMeanSquaredError
    //Root mean squared error (RMSE): 0.0
    println(s"  Root mean squared error (RMSE): $RMSE")
  }
}
// scalastyle:on println
