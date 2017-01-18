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
import org.apache.spark.ml.classification.{RandomForestClassificationModel, RandomForestClassifier}
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.{IndexToString, StringIndexer, VectorIndexer}
// $example off$
import org.apache.spark.sql.Row
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{SQLContext, DataFrame}
/**
 * ���ɭ��(Random Forests)��ʵ���Ƕ��������,ÿ����������һ��Ȩ��,��δ֪���ݽ���Ԥ��ʱ,
 * ���ö���������ֱ�Ԥ��һ��ֵ,Ȼ��������Ȩ��,������Ԥ��ֵ�ۺ�����,
 * ���ڷ�������,���ö������,���ڻع�����,ֱ����ƽ����
 * RandomForestClassifier ���ɭ�ַ�����
 */
object RandomForestClassifierExample {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("RandomForestClassifierExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
  
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    // $example on$
    // Load and parse the data file, converting it to a DataFrame.
   // val data = sqlContext.read.format("libsvm").load("data/mllib/sample_libsvm_data.txt")
      import org.apache.spark.mllib.util.MLUtils
      val dataSVM=MLUtils.loadLibSVMFile(sc, "../data/mllib/sample_libsvm_data.txt")
      val data = sqlContext.createDataFrame(dataSVM)
    // Index labels, adding metadata to the label column.
    // Fit on whole dataset to include all labels in index.
    //ѵ��֮ǰ����ʹ������������Ԥ������������������ת��
    val labelIndexer = new StringIndexer()
      .setInputCol("label")
      .setOutputCol("indexedLabel")
      .fit(data)//fit()������DataFrameת��Ϊһ��Transformer���㷨
    // Automatically identify categorical features, and index them.
    // Set maxCategories so features with > 4 distinct values are treated as continuous.
    //VectorIndexer�Ƕ����ݼ����������е����(��ɢֵ)�������б��
    val featureIndexer = new VectorIndexer()
      .setInputCol("features")
      .setOutputCol("indexedFeatures")
      .setMaxCategories(4)//��������Ϊ4,(��ĳһ��)�ж���4��ȡֵ��Ϊ����ֵ
      .fit(data)//fit()������DataFrameת��Ϊһ��Transformer���㷨

    // Split the data into training and test sets (30% held out for testing).
    //ʹ�õ�һ�������ݽ���ѵ��,ʣ������������
    val Array(trainingData, testData) = data.randomSplit(Array(0.7, 0.3))

    // Train a RandomForest model.
    val rf = new RandomForestClassifier()
      .setLabelCol("indexedLabel")//��ǩ����
      .setFeaturesCol("indexedFeatures")//��������
      .setNumTrees(10)//ѵ������������

    // Convert indexed labels back to original labels.
    //ת��������ǩ�ص�ԭ���ı�ǩ
    val labelConverter = new IndexToString()
      .setInputCol("prediction")
      .setOutputCol("predictedLabel")
      .setLabels(labelIndexer.labels)

    // Chain indexers and forest in a Pipeline.
     //PipeLine:�����DataFrame��Estimator�㷨����һ���ض���ML Wolkflow
     //һ�� Pipeline�ڽṹ�ϻ����һ������ PipelineStage,ÿһ�� PipelineStage �������һ������
    val pipeline = new Pipeline()
      .setStages(Array(labelIndexer, featureIndexer, rf, labelConverter))

    // Train model. This also runs the indexers.
    //fit()������DataFrameת��Ϊһ��Transformer���㷨
    val model = pipeline.fit(trainingData)

    // Make predictions.
    //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    val predictions = model.transform(testData)
    /**
      +-----+--------------------+------------+--------------------+-------------+-----------+----------+--------------+
      |label|            features|indexedLabel|     indexedFeatures|rawPrediction|probability|prediction|predictedLabel|
      +-----+--------------------+------------+--------------------+-------------+-----------+----------+--------------+
      |  0.0|(692,[127,128,129...|         1.0|(692,[127,128,129...|   [0.0,10.0]|  [0.0,1.0]|       1.0|           0.0|
      |  1.0|(692,[152,153,154...|         0.0|(692,[152,153,154...|   [10.0,0.0]|  [1.0,0.0]|       0.0|           1.0|
      |  0.0|(692,[154,155,156...|         1.0|(692,[154,155,156...|    [6.0,4.0]|  [0.6,0.4]|       0.0|           1.0|
      |  0.0|(692,[124,125,126...|         1.0|(692,[124,125,126...|   [0.0,10.0]|  [0.0,1.0]|       1.0|           0.0|
      |  1.0|(692,[124,125,126...|         0.0|(692,[124,125,126...|   [10.0,0.0]|  [1.0,0.0]|       0.0|           1.0|
      +-----+--------------------+------------+--------------------+-------------+-----------+----------+--------------+*/
    predictions.show(5)
    // Select example rows to display.
    /**
      +--------------+-----+--------------------+
      |predictedLabel|label|            features|
      +--------------+-----+--------------------+
      |           1.0|  1.0|(692,[158,159,160...|
      |           1.0|  1.0|(692,[124,125,126...|
      |           0.0|  0.0|(692,[129,130,131...|
      |           1.0|  1.0|(692,[158,159,160...|
      |           0.0|  1.0|(692,[99,100,101,...|
      +--------------+-----+--------------------+*/
    predictions.select("predictedLabel", "label", "features").show(5)

    // Select (prediction, true label) and compute test error.
    // ѡ��(Ԥ��,��ǩ)������Դ���
    val evaluator = new MulticlassClassificationEvaluator()
      .setLabelCol("indexedLabel")//��ǩ����
      //�㷨Ԥ�����Ĵ洢�е�����, Ĭ���ǡ�prediction��
      .setPredictionCol("prediction")
      .setMetricName("precision")//׼ȷ��
    val accuracy = evaluator.evaluate(predictions)
    //Test Error = 0.025000000000000022
    println("Test Error = " + (1.0 - accuracy))

    val rfModel = model.stages(2).asInstanceOf[RandomForestClassificationModel]
    //Learned classification forest model:
    println("Learned classification forest model:\n" + rfModel.toDebugString)
    // $example off$

    sc.stop()
  }
}
// scalastyle:on println
