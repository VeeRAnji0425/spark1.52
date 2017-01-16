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

package org.apache.spark.examples.ml

// $example on$
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.regression.LinearRegression
import org.apache.spark.ml.tuning.{ParamGridBuilder, TrainValidationSplit}
// $example off$
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.sql.Row
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{SQLContext, DataFrame}


/**
 * A simple example demonstrating model selection using TrainValidationSplit.
 * ��ʾһ���򵥵�����,ѡ��ʹ��trainvalidationsplitģ��
 *������С��ʱ�������CrossValidator���н�����֤,���������ʱ�����ֱ����trainValidationSplit
 * Run with
 * {{{
 * bin/run-example ml.ModelSelectionViaTrainValidationSplitExample
 * }}}
 */
object ModelSelectionViaTrainValidationSplitExample {

  def main(args: Array[String]): Unit = {
     val conf = new SparkConf().setAppName("ModelSelectionViaTrainValidationSplitExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
  
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    

    // $example on$
    // Prepare training and test data.
    /**
 *  libSVM�����ݸ�ʽ
 *  <label> <index1>:<value1> <index2>:<value2> ...
 *  ����<label>��ѵ�����ݼ���Ŀ��ֵ,���ڷ���,���Ǳ�ʶĳ�������(֧�ֶ����);���ڻع�,������ʵ��
 *  <index>����1��ʼ������,�����ǲ�����
 *  <value>Ϊʵ��,Ҳ�������ǳ�˵���Ա���
 */
   // val data = sqlContext.read.format("libsvm").load("data/mllib/sample_linear_regression_data.txt")
    import org.apache.spark.mllib.util.MLUtils
      val dataSVM=MLUtils.loadLibSVMFile(sc, "../data/mllib/sample_linear_regression_data.txt")
      val data = sqlContext.createDataFrame(dataSVM)
    val Array(training, test) = data.randomSplit(Array(0.9, 0.1), seed = 12345)

    val lr = new LinearRegression()

    // We use a ParamGridBuilder to construct a grid of parameters to search over.
    // TrainValidationSplit will try all combinations of values and determine best model using
    // the evaluator.
    //ParamGridBuilder������ѡ����(��:logistic regression��regParam)
    val paramGrid = new ParamGridBuilder()
      .addGrid(lr.regParam, Array(0.1, 0.01))
      .addGrid(lr.fitIntercept)//�Ƿ�ѵ�����ض���
      //���������ϲ���,0.0ΪL2���� 1.0ΪL1����
      .addGrid(lr.elasticNetParam, Array(0.0, 0.5, 1.0))
      .build()

    // In this case the estimator is simply the linear regression.
    // A TrainValidationSplit requires an Estimator, a set of Estimator ParamMaps, and an Evaluator.
    //������С��ʱ�������CrossValidator���н�����֤,���������ʱ�����ֱ����trainValidationSplit
    val trainValidationSplit = new TrainValidationSplit()
      .setEstimator(lr)
      .setEvaluator(new RegressionEvaluator)//�ع�����
      .setEstimatorParamMaps(paramGrid)
      // 80% of the data will be used for training and the remaining 20% for validation.
      //80%�����ݽ�����ѵ��,����20%������֤
      .setTrainRatio(0.8)

    // Run train validation split, and choose the best set of parameters.
    //fit()������DataFrameת��Ϊһ��Transformer���㷨
    val model = trainValidationSplit.fit(training)

    // Make predictions on test data. model is the model with combination of parameters
    // that performed best.
    //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    /**
      +--------------------+------------------+--------------------+
      |            features|             label|          prediction|
      +--------------------+------------------+--------------------+
      |(10,[0,1,2,3,4,5,...|-5.082010756207233| -1.0519586530518505|
      |(10,[0,1,2,3,4,5,...| 7.887786536531237|  1.3995823450701195|
      |(10,[0,1,2,3,4,5,...| 6.082192787194888|-0.07337670647585792|
      |(10,[0,1,2,3,4,5,...|-7.481405271455238| -0.9318480964325699|
      |(10,[0,1,2,3,4,5,...| 4.564039393483412|-0.26155859898173217|
      +--------------------+------------------+--------------------+
     */
    model.transform(test)
      .select("features", "label", "prediction")
      .show(5)
    // $example off$

    sc.stop()
  }
}
