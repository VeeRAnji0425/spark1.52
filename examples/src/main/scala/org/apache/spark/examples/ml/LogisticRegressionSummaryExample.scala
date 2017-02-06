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
import org.apache.spark.ml.classification.{BinaryLogisticRegressionSummary, LogisticRegression}
// $example off$
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{SQLContext, DataFrame}
import org.apache.spark.sql.functions.max
/**
 * �߼��ع�ժҪ������
 */
object LogisticRegressionSummaryExample {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("LinearRegressionWithElasticNetExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
  
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    // Load training data
    /**
 *  libSVM�����ݸ�ʽ
 *  <label> <index1>:<value1> <index2>:<value2> ...
 *  ����<label>��ѵ�����ݼ���Ŀ��ֵ,���ڷ���,���Ǳ�ʶĳ�������(֧�ֶ����);���ڻع�,������ʵ��
 *  <index>����1��ʼ������,�����ǲ�����
 *  <value>Ϊʵ��,Ҳ�������ǳ�˵���Ա���
 */
    //val training = sqlContext.read.format("libsvm").load("data/mllib/sample_libsvm_data.txt")
      import org.apache.spark.mllib.util.MLUtils
      val dataSVM=MLUtils.loadLibSVMFile(sc, "../data/mllib/sample_libsvm_data.txt")
      val training = sqlContext.createDataFrame(dataSVM)
    val lr = new LogisticRegression()
      .setMaxIter(10)//��������
      .setRegParam(0.3)//���򻯲���(>=0)
      .setElasticNetParam(0.8)//���������ϲ���,0.0ΪL2���� 1.0ΪL1����

    // Fit the model
    //fit()������DataFrameת��Ϊһ��Transformer���㷨
    val lrModel = lr.fit(training)

    // $example on$
    // Extract the summary from the returned LogisticRegressionModel instance trained in the earlier
    // example
    //����ʵ���߼��ع�ģ�͵�ѵ������ȡժҪ
    val trainingSummary = lrModel.summary

    // Obtain the objective per iteration.
    //���ÿ�ε�����Ŀ��
    val objectiveHistory = trainingSummary.objectiveHistory
    /**
     *0.6833149135741656
      0.6662875751473731
      0.6217068546034619
      0.6127265245887888
      0.606034798680287
      0.6031750687571562
      0.5969621534836276
      0.5940743031983124
      0.5906089243339021
      0.5894724576491039
      0.588218777572959
     */
    objectiveHistory.foreach(loss => println(loss))

    // Obtain the metrics useful to judge performance on test data.
    // ������õ�ָ�����жϲ������ݵ�����
    // We cast the summary to a BinaryLogisticRegressionSummary since the problem is a
    // binary classification problem.
    val binarySummary = trainingSummary.asInstanceOf[BinaryLogisticRegressionSummary]

    // Obtain the receiver-operating characteristic as a dataframe and areaUnderROC.
    //���һ�����ݼ�areaUnderROC
    //ROC���������,��һ��������������ģ�ͺû���һ����׼
    val roc = binarySummary.roc
    /**
   	+---+--------------------+
    |FPR|                 TPR|
    +---+--------------------+
    |0.0|                 0.0|
    |0.0|0.017543859649122806|
    |0.0| 0.03508771929824561|
    |0.0| 0.05263157894736842|
    |0.0| 0.07017543859649122|
    +---+--------------------+*/
    roc.show(5)
    //1
    //ROC���������,��һ��������������ģ�ͺû���һ����׼
    println(binarySummary.areaUnderROC)

    // Set the model threshold to maximize F-Measure
    //ģ�͵��趨��ֵ���ֵ
    val fMeasure = binarySummary.fMeasureByThreshold
    /**
     *+------------------+--------------------+
      |         threshold|           F-Measure|
      +------------------+--------------------+
      |0.7845860015371144|0.034482758620689655|
      |0.7843193344168924| 0.06779661016949151|
      |0.7842976092510133|                 0.1|
      |0.7842531051133194| 0.13114754098360656|
      |0.7835792429453299| 0.16129032258064516|
      +------------------+--------------------+*/
    fMeasure.show(5)
    val maxFMeasure = fMeasure.select(max("F-Measure")).head().getDouble(0)
    val bestThreshold = fMeasure.where($"F-Measure" === maxFMeasure)
      .select("threshold").head().getDouble(0)
     //�ڶ����Ʒ�����������ֵ,��ΧΪ[0,1],������ǩ1�Ĺ��Ƹ���>Threshold,��Ԥ��1,����0
    lrModel.setThreshold(bestThreshold)
    // $example off$

    sc.stop()
  }
}
// scalastyle:on println
