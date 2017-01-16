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
import org.apache.spark.ml.classification.LogisticRegression
// $example off$
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{SQLContext, DataFrame}

object LogisticRegressionWithElasticNetExample {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("LogisticRegressionWithElasticNetExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
  
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    // $example on$
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
      .setMaxIter(10)//����������
      .setRegParam(0.3)//���򻯲���
      .setElasticNetParam(0.8)//���������ϲ���,0.0ΪL2���� 1.0ΪL1����

    // Fit the model
    //fit()������DataFrameת��Ϊһ��Transformer���㷨
    val lrModel = lr.fit(training)

    // Print the coefficients and intercept for logistic regression
    //println(s"Coefficients: ${lrModel.coefficients} Intercept: ${lrModel.intercept}")
    // $example off$

    sc.stop()
  }
}
// scalastyle:on println
