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
import org.apache.spark.ml.classification.{GBTClassificationModel, GBTClassifier}
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.{IndexToString, StringIndexer, VectorIndexer}
// $example off$
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{SQLContext, DataFrame}

/**
 * �ݶ�����������:�ۺ϶��������,��������,��������
 * GBT��ѵ����ÿ��ѵ��һ����,Ȼ�������������ÿ��ʵ������Ԥ��,ͨ��һ����ʧ����,������ʧ�����ĸ��ݶ�ֵ��Ϊ�в�,
 * ��������в��������ʵ����label,Ȼ���ٴ�ѵ��һ����ȥ��ϲв�,��˽��е���,ֱ������ģ�Ͳ�������
 * GBTֻ�����ڶ�����ͻع�,��֧�ֶ����,��Ԥ���ʱ��,�������ɭ��������ƽ��ֵ,GBT�ǽ���������Ԥ��ֵ�����͡�
 */
object GradientBoostedTreeClassifierExample {
  def main(args: Array[String]): Unit = {
   val conf = new SparkConf().setAppName("GradientBoostedTreeClassifierExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
  
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    // $example on$
    // Load and parse the data file, converting it to a DataFrame.
    /**
 *  libSVM�����ݸ�ʽ
 *  <label> <index1>:<value1> <index2>:<value2> ...
 *  ����<label>��ѵ�����ݼ���Ŀ��ֵ,���ڷ���,���Ǳ�ʶĳ�������(֧�ֶ����);���ڻع�,������ʵ��
 *  <index>����1��ʼ������,�����ǲ�����
 *  <value>Ϊʵ��,Ҳ�������ǳ�˵���Ա���
 */
    import org.apache.spark.mllib.util.MLUtils
    val dataSVM=MLUtils.loadLibSVMFile(sc, "../data/mllib/sample_libsvm_data.txt")
    val data=sqlContext.createDataFrame(dataSVM)
    //val data = sqlContext.read.format("libsvm").load("../data/mllib/sample_libsvm_data.txt")
  

    // Index labels, adding metadata to the label column.
    // ������ǩ,��Ԫ������ӵ���ǩ��.
    // Fit on whole dataset to include all labels in index.
    val labelIndexer = new StringIndexer()
      .setInputCol("label")
      .setOutputCol("indexedLabel")
      .fit(data)//fit()������DataFrameת��Ϊһ��Transformer���㷨
    // Automatically identify categorical features, and index them.
      //�Զ�ʶ���������,�������ǽ�������,
    // Set maxCategories so features with > 4 distinct values are treated as continuous.
     //VectorIndexer�Ƕ����ݼ����������е����(��ɢֵ)�������б��
    val featureIndexer = new VectorIndexer()
      .setInputCol("features")
      .setOutputCol("indexedFeatures")
      .setMaxCategories(4)//��������Ϊ5,(��ĳһ��)�ж���4��ȡֵ��Ϊ����ֵ,������ת��
      .fit(data)//fit()������DataFrameת��Ϊһ��Transformer���㷨

    // Split the data into training and test sets (30% held out for testing).
    //�����ݷֳ�ѵ���Ͳ��Լ�(30%���в���)
    val Array(trainingData, testData) = data.randomSplit(Array(0.7, 0.3))

    // Train a GBT model.
    //ѵ��GBT����ģ��
    val gbt = new GBTClassifier()
      .setLabelCol("indexedLabel")
       //ѵ�����ݼ�DataFrame�д洢�������ݵ�����
      .setFeaturesCol("indexedFeatures")
      .setMaxIter(10)

    // Convert indexed labels back to original labels.
    //ת��������ǩ�ص�ԭ���ı�ǩ
    val labelConverter = new IndexToString()
      .setInputCol("prediction")
      .setOutputCol("predictedLabel")
      .setLabels(labelIndexer.labels)

    // Chain indexers and GBT in a Pipeline.
     //PipeLine:�����DataFrame��Estimator�㷨����һ���ض���ML Wolkflow
     //һ�� Pipeline�ڽṹ�ϻ����һ������ PipelineStage,ÿһ�� PipelineStage �������һ������
    val pipeline = new Pipeline()
      .setStages(Array(labelIndexer, featureIndexer, gbt, labelConverter))

    // Train model. This also runs the indexers.
    //fit()������DataFrameת��Ϊһ��Transformer���㷨
    val model = pipeline.fit(trainingData)

    // Make predictions.
    //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    val predictions = model.transform(testData)

    // Select example rows to display.
    /**
     *+--------------+-----+--------------------+
      |predictedLabel|label|            features|
      +--------------+-----+--------------------+
      |           0.0|  0.0|(692,[127,128,129...|
      |           1.0|  1.0|(692,[151,152,153...|
      |           1.0|  1.0|(692,[158,159,160...|
      |           0.0|  1.0|(692,[99,100,101,...|
      |           1.0|  1.0|(692,[97,98,99,12...|
      +--------------+-----+--------------------+
     */
    predictions.select("predictedLabel", "label", "features").show(5)

    // Select (prediction, true label) and compute test error.
    val evaluator = new MulticlassClassificationEvaluator()
      .setLabelCol("indexedLabel")
      //�㷨Ԥ�����Ĵ洢�е�����, Ĭ���ǡ�prediction��
      .setPredictionCol("prediction")
      .setMetricName("precision")
     //Ԥ��׼ȷ��
    val accuracy = evaluator.evaluate(predictions)
    //Test Error = 0.03448275862068961
    println("Test Error = " + (1.0 - accuracy))
    //�ܵ�ģ�ͻ��GBTClassificationModelģ��
    val gbtModel = model.stages(2).asInstanceOf[GBTClassificationModel]
    println("Learned classification GBT model:\n" + gbtModel.toDebugString)
    // $example off$

    sc.stop()
  }
}
// scalastyle:on println
