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
import org.apache.spark.ml.classification.MultilayerPerceptronClassifier
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
// $example off$
import org.apache.spark.sql.Row
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{SQLContext, DataFrame}

/**
 * An example for Multilayer Perceptron Classification.
 * ����֪���ǻ��ڷ����˹�������,
 * ����֪�����ж��ڵ�,ÿ��ڵ����������һ��ڵ���ȫ����
 */
object MultilayerPerceptronClassifierExample {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("MultilayerPerceptronClassifierExample").setMaster("local[4]")
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
   // val data = sqlContext.read.format("libsvm")
    //  .load("data/mllib/sample_multiclass_classification_data.txt")
   import org.apache.spark.mllib.util.MLUtils
      val dataSVM=MLUtils.loadLibSVMFile(sc, "../data/mllib/sample_multiclass_classification_data.txt")
      val data = sqlContext.createDataFrame(dataSVM)
      // Split the data into train and test
      //�ָ����Ժ�ѵ������
    val splits = data.randomSplit(Array(0.6, 0.4), seed = 1234L)
    val train = splits(0)
    val test = splits(1)
    // specify layers for the neural network:
    //ָ��������Ĳ㣺
    // input layer of size 4 (features), two intermediate �м� of size 5 and 4
    // and output of size 3 (classes)
    //
    val layers = Array[Int](4, 5, 4, 3)
    // create the trainer and set its parameters
    //����ѵ���������ò���
    val trainer = new MultilayerPerceptronClassifier()
      .setLayers(layers)//���ģ,���������ģ�Լ������ģ
      .setBlockSize(128)//
      .setSeed(1234L)//�������
      .setMaxIter(100)//��������
    // train the model
    //fit()������DataFrameת��Ϊһ��Transformer���㷨
    val model = trainer.fit(train)
    // compute accuracy on the test set
    //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    val result = model.transform(test)
    /**
     *+-----+--------------------+----------+
      |label|            features|prediction|
      +-----+--------------------+----------+
      |  2.0|(4,[0,1,2,3],[0.3...|       0.0|
      |  1.0|(4,[0,1,2,3],[-0....|       1.0|
      |  0.0|(4,[0,1,2,3],[0.0...|       0.0|
      |  1.0|(4,[0,1,2,3],[-0....|       1.0|
      |  1.0|(4,[0,1,2,3],[-0....|       1.0|
      |  0.0|(4,[0,1,2,3],[0.0...|       0.0|
      |  2.0|(4,[0,1,2,3],[-0....|       2.0|
      |  1.0|(4,[0,1,2,3],[-0....|       1.0|
      |  0.0|(4,[0,1,2,3],[0.1...|       0.0|
      |  0.0|(4,[0,2,3],[0.444...|       0.0|
      |  2.0|(4,[0,1,2,3],[0.2...|       2.0|
      +-----+--------------------+----------+*/
    result.show(5)
    val predictionAndLabels = result.select("prediction", "label")
    //���������
    val evaluator = new MulticlassClassificationEvaluator()
      .setMetricName("precision")
    //׼ȷ�� Accuracy: 0.9636363636363636
    println("Accuracy: " + evaluator.evaluate(predictionAndLabels))
    // $example off$

    sc.stop()
  }
}
// scalastyle:on println
