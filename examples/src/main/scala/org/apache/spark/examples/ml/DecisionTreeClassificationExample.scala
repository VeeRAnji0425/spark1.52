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
import org.apache.spark.ml.classification.DecisionTreeClassificationModel
import org.apache.spark.ml.classification.DecisionTreeClassifier
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.{IndexToString, StringIndexer, VectorIndexer}
// $example off$
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{SQLContext, DataFrame}
import org.apache.spark.mllib.util.MLUtils
object DecisionTreeClassificationExample {
  def main(args: Array[String]): Unit = {
  val conf = new SparkConf().setAppName("DecisionTreeClassificationExample").setMaster("local[4]")
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
   // val data = sqlContext.read.format("libsvm").load("../data/mllib/sample_libsvm_data.txt")
   val data = sqlContext.createDataFrame(dataSVM)
    // Index labels, adding metadata to the label column.
    // Fit on whole dataset to include all labels in index.
    val labelIndexer = new StringIndexer()
      .setInputCol("label")
      .setOutputCol("indexedLabel")
      .fit(data)//fit()������DataFrameת��Ϊһ��Transformer���㷨
    // Automatically identify categorical features, and index them.
    //VectorIndexer�Ƕ����ݼ����������е����(��ɢֵ)�������б��
    val featureIndexer = new VectorIndexer()
      .setInputCol("features")
      .setOutputCol("indexedFeatures")
      //����4����ֵͬ��������Ϊ����
      .setMaxCategories(4) // features with > 4 distinct values are treated as continuous.
      .fit(data)//fit()������DataFrameת��Ϊһ��Transformer���㷨

    // Split the data into training and test sets (30% held out for testing).
    //�����ݷֳ�ѵ���Ͳ��Լ�(30%����)
    val Array(trainingData, testData) = data.randomSplit(Array(0.7, 0.3))

    // Train a DecisionTree model.
    //ѵ��һ��������ģ��
    val dt = new DecisionTreeClassifier()
      .setLabelCol("indexedLabel")//��ǩ����
      //ѵ�����ݼ� DataFrame �д洢�������ݵ�����
      .setFeaturesCol("indexedFeatures")

    // Convert indexed labels back to original labels.
    //ת��������ǩ�ص�ԭ���ı�ǩ
    val labelConverter = new IndexToString()
      .setInputCol("prediction")
      .setOutputCol("predictedLabel")
      .setLabels(labelIndexer.labels)

    // Chain indexers and tree in a Pipeline.
     //PipeLine:�����DataFrame��Estimator�㷨����һ���ض���ML Wolkflow
     //һ�� Pipeline�ڽṹ�ϻ����һ������ PipelineStage,ÿһ�� PipelineStage �������һ������
    val pipeline = new Pipeline()
      .setStages(Array(labelIndexer, featureIndexer, dt, labelConverter))

    // Train model. This also runs the indexers.
    //fit()������DataFrameת��Ϊһ��Transformer���㷨
    val model = pipeline.fit(trainingData)

    // Make predictions.
    //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    val predictions = model.transform(testData)

    // Select example rows to display.
    //ѡ��Ҫ��ʾ��ʾ����
    /**
     * +--------------+-----+--------------------+
     * |predictedLabel|label|            features|
     * +--------------+-----+--------------------+
     * |           1.0|  1.0|(692,[151,152,153...|
     * |           0.0|  0.0|(692,[129,130,131...|
     * |           0.0|  0.0|(692,[154,155,156...|
     * |           0.0|  0.0|(692,[127,128,129...|
     * |           0.0|  0.0|(692,[151,152,153...|
     * +--------------+-----+--------------------+
     */
    predictions.select("predictedLabel", "label", "features").show(5)

    // Select (prediction, true label) and compute test error.
    //ѡ��(Ԥ��,��ʵ��ǩ)�ͼ�����Դ���
    val evaluator = new MulticlassClassificationEvaluator()
    //��ǩ�е�����
      .setLabelCol("indexedLabel")
      //�㷨Ԥ�����Ĵ洢�е�����, Ĭ���ǡ�prediction��
      .setPredictionCol("prediction")
      //F1-Measure�Ǹ���׼ȷ��Precision���ٻ���Recall���߸�����һ���ۺϵ�����ָ��
      //���������в���(f1,precision,recall,weightedPrecision,weightedRecall)
      //f1        Test Error = 0.04660856384994316
      //precision Test Error = 0.030303030303030276
      //recall    Test Error = 0.0
      .setMetricName("precision")//׼ȷ��
      //����
    val accuracy = evaluator.evaluate(predictions)
    //println("==="+accuracy)
    println("Test Error = " + (1.0 - accuracy))

    val treeModel = model.stages(2).asInstanceOf[DecisionTreeClassificationModel]
    println("Learned classification tree model:\n" + treeModel.toDebugString)
    // $example off$

    sc.stop()
  }
}
// scalastyle:on println
