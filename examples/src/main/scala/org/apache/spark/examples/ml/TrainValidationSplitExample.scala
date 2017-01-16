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

import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.regression.LinearRegression
import org.apache.spark.ml.tuning.{ParamGridBuilder, TrainValidationSplit}
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}

/**
 * A simple example demonstrating model selection using TrainValidationSplit.
 * һ���򵥵�������ʾģ��ѡ��ʹ��ѵ����֤����
 * ������С��ʱ�������CrossValidator���н�����֤,���������ʱ�����ֱ����trainValidationSplit
 * The example is based on [[SimpleParamsExample]] using linear regression.
 * Run with
 * {{{
 * bin/run-example ml.TrainValidationSplitExample
 * }}}
 * Spark���ṩѵ����֤�������Գ������������ͽ�����֤����K�β�ͬ,ѵ����֤����ֻ��ÿ���������һ��
 * �뽻����֤��ͬ,ȷ����Ѳ������,ѵ����֤�������ʹ����Ѳ��������ȫ��������������Ϲ�����
 */
object TrainValidationSplitExample {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("TrainValidationSplitExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    // Prepare training and test data.
    // ׼����ѵ�Ͳ�������
    val data = MLUtils.loadLibSVMFile(sc, "../data/mllib/sample_libsvm_data.txt").toDF()
   // �������������Ϊ����,һ������ѵ��,һ�����ڲ���
    val Array(training, test) = data.randomSplit(Array(0.9, 0.1), seed = 12345)
   //���Իع�
    val lr = new LinearRegression()

    // We use a ParamGridBuilder to construct a grid of parameters to search over.
    //������һ��paramgridbuilder���������������
    // TrainValidationSplit will try all combinations of values and determine best model using
    // the evaluator.
    //trainvalidationsplit���������е���Ϻ�ʹ������ֵȷ�����ģ��
    //ParamGridBuilder������ѡ����(��:logistic regression��regParam)
    val paramGrid = new ParamGridBuilder()
      .addGrid(lr.regParam, Array(0.1, 0.01))
      //�Ƿ�ѵ�����ض���
      .addGrid(lr.fitIntercept, Array(true, false))
      //���������ϲ���,0.0ΪL2���� 1.0ΪL1����
      .addGrid(lr.elasticNetParam, Array(0.0, 0.5, 1.0))
      .build()

    // In this case the estimator is simply the linear regression.
    //�����������,�����Ǽ򵥵����Իع�
    // A TrainValidationSplit requires an Estimator, a set of Estimator ParamMaps, and an Evaluator.
    //һ��trainvalidationsplit��Ҫ����,һ�׹���parammaps,������
    val trainValidationSplit = new TrainValidationSplit()//����ģ�ͳ������Ż�
      .setEstimator(lr)//��������������
      .setEvaluator(new RegressionEvaluator)//��������������
      .setEstimatorParamMaps(paramGrid)

    // 80% of the data will be used for training and the remaining 20% for validation.
    //80%�����ݽ�����ѵ����ʣ���20%������֤
    trainValidationSplit.setTrainRatio(0.8)//

    // Run train validation split, and choose the best set of parameters.
    //����ѵ����֤���,��ѡ����ѵĲ�����
    //fit()������DataFrameת��Ϊһ��Transformer���㷨
    val model = trainValidationSplit.fit(training)

    // Make predictions on test data. model is the model with combination of parameters
    //�Բ������ݽ���Ԥ��,ģ�������ŵĲ�����ϵ�ģ��
    // that performed best.
    //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    /**
      +--------------------+-----+--------------------+
      |            features|label|          prediction|
      +--------------------+-----+--------------------+
      |(692,[154,155,156...|  1.0|  1.0638236485622758|
      |(692,[153,154,155...|  0.0| 0.07337831421003782|
      |(692,[156,157,158...|  1.0|  0.9377417246384833|
      |(692,[126,127,128...|  0.0|6.372102815044061E-4|
      |(692,[127,128,154...|  1.0|  0.6554640392935814|
      |(692,[124,125,126...|  0.0|-0.03226781932439382|
      +--------------------+-----+--------------------+*/
    model.transform(test)
      .select("features", "label", "prediction")
      .show(5)

    sc.stop()
  }
}
