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

// $example on$
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.feature.VectorIndexer
import org.apache.spark.ml.regression.DecisionTreeRegressionModel
import org.apache.spark.ml.regression.DecisionTreeRegressor
// $example off$
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{SQLContext, DataFrame}
import org.apache.spark.mllib.util.MLUtils
/**
 * �ع����������
 */
object DecisionTreeRegressionExample {
  def main(args: Array[String]): Unit = {
      val conf = new SparkConf().setAppName("DecisionTreeRegressionExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
  
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
   

    // $example on$
    // Load the data stored in LIBSVM format as a DataFrame.
    /**
 *  libSVM�����ݸ�ʽ
 *  <label> <index1>:<value1> <index2>:<value2> ...
 *  ����<label>��ѵ�����ݼ���Ŀ��ֵ,���ڷ���,���Ǳ�ʶĳ�������(֧�ֶ����);���ڻع�,������ʵ��
 *  <index>����1��ʼ������,�����ǲ�����
 *  <value>Ϊʵ��,Ҳ�������ǳ�˵���Ա���
 */
    val dataSVM=MLUtils.loadLibSVMFile(sc, "../data/mllib/sample_libsvm_data.txt")
    val data = sqlContext.createDataFrame(dataSVM)
 
    /**
    +-----+--------------------+
    |label|            features|
    +-----+--------------------+
    |  0.0|(692,[127,128,129...|
    |  1.0|(692,[158,159,160...|
    |  1.0|(692,[124,125,126...|
    |  1.0|(692,[124,125,126...|
    +-----+--------------------+*/
     data.show()
    // Automatically identify categorical features, and index them.
     //�Զ�ʶ���������,������,������,����ѵ������ֵ>4��ֵͬ��Ϊ����
    // Here, we treat features with > 4 distinct values as continuous.
    //VectorIndexer�Ƕ����ݼ����������е����(��ɢֵ)�������б��
    val featureIndexer = new VectorIndexer()
      .setInputCol("features")
      .setOutputCol("indexedFeatures")
      .setMaxCategories(4)//������
      .fit(data)//fit()������DataFrameת��Ϊһ��Transformer���㷨

    // Split the data into training and test sets (30% held out for testing).
    // �����ݷֳ�ѵ���Ͳ��Լ�(30%���в���)
    val Array(trainingData, testData) = data.randomSplit(Array(0.7, 0.3))

    // Train a DecisionTree model.
    // ѵ��һ������ģ��
    val dt = new DecisionTreeRegressor()
      .setLabelCol("label")
       //ѵ�����ݼ�DataFrame�д洢�������ݵ�����
      .setFeaturesCol("indexedFeatures")

    // Chain indexer and tree in a Pipeline.
     //PipeLine:�����DataFrame��Estimator�㷨����һ���ض���ML Wolkflow
     //һ�� Pipeline�ڽṹ�ϻ����һ������ PipelineStage,ÿһ�� PipelineStage �������һ������
    val pipeline = new Pipeline()
      .setStages(Array(featureIndexer, dt))

    // Train model. This also runs the indexer.
    //fit()������DataFrameת��Ϊһ��Transformer���㷨
    val model = pipeline.fit(trainingData)

    // Make predictions.
    //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    val predictions = model.transform(testData)

    // Select example rows to display.
    /**
     *+----------+-----+--------------------+
      |prediction|label|            features|
      +----------+-----+--------------------+
      |       1.0|  1.0|(692,[158,159,160...|
      |       1.0|  1.0|(692,[124,125,126...|
      |       0.0|  1.0|(692,[99,100,101,...|
      |       0.0|  0.0|(692,[127,128,129...|
      |       0.0|  0.0|(692,[153,154,155...|
      +----------+-----+--------------------+
     **/
    predictions.select("prediction", "label", "features").show(5)

    // Select (prediction, true label) and compute test error.
    //�ع�����,ѡ��(Ԥ��,��ʵ��ǩ)�ͼ�����Դ���
    val evaluator = new RegressionEvaluator()
      //��ǩ�е�����
      .setLabelCol("label")
      //�㷨Ԥ�����Ĵ洢�е�����, Ĭ���ǡ�prediction��
      .setPredictionCol("prediction")
       //rmse���������˵����������ɢ�̶�
      .setMetricName("rmse")//���������
    val rmse = evaluator.evaluate(predictions)
     //rmse���������˵����������ɢ�̶�
    //Root Mean Squared Error (RMSE) on test data = 0.25819888974716115
    println("Root Mean Squared Error (RMSE) on test data = " + rmse)
    //�ӹܵ�ģ�ͻ�þ������ع�ģ��
    val treeModel = model.stages(1).asInstanceOf[DecisionTreeRegressionModel]
    println("Learned regression tree model:\n" + treeModel.toDebugString)
    // $example off$

    sc.stop()
  }
}
// scalastyle:on println
