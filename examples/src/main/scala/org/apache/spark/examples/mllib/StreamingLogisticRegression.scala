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
package org.apache.spark.examples.mllib

import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.classification.StreamingLogisticRegressionWithSGD
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
 * Train a logistic regression model on one stream of data and make predictions
 * ��һ����������ѵ��һ���߼��ع�ģ��,������һ���������Ͻ���Ԥ��
 * on another stream, where the data streams arrive as text files
 * into two different directories.
 * �������������ı��ļ���������ͬ��Ŀ¼ʱ
 * 
 * The rows of the text files must be labeled data points in the form
 * �ı��ļ����б����ڴ����б�����ݵ�
 * `(y,[x1,x2,x3,...,xn])`
 * Where n is the number of features, y is a binary label,
 * ����n�������ĸ�����Y��һ�������Ʊ�ǩ,��n��������ͬ��ѵ���Ͳ���
 * and n must be the same for train and test.
 *
 * Usage: StreamingLogisticRegression <trainingDir> <testDir> <batchDuration> <numFeatures>
 *
 * To run on your local machine using the two directories `trainingDir` and `testDir`,
 * �����ڱ��ػ�����ʹ������Ŀ¼` trainingdir `��` testdir `
 * with updates every 5 seconds, and 2 features per data point, call:
 * ÿ5�����һ��,ÿ�����ݵ��2������
 *    $ bin/run-example mllib.StreamingLogisticRegression trainingDir testDir 5 2
 *
 * As you add text files to `trainingDir` the model will continuously update.
 * ��������ı��ļ�` trainingdir `ģ�ͽ����ϸ���
 * Anytime you add text files to `testDir`, you'll see predictions from the current model.
 * �κ�ʱ��������ı��ļ�` testdir `,�㽫��Ŀǰ������Ԥ��ģ��
 *
 */
object StreamingLogisticRegression {

  def main(args: Array[String]) {

    if (args.length != 4) {
      System.err.println(
        "Usage: StreamingLogisticRegression <trainingDir> <testDir> <batchDuration> <numFeatures>")
      System.exit(1)
    }

    val conf = new SparkConf().setMaster("local").setAppName("StreamingLogisticRegression")
    //���μ��
    val ssc = new StreamingContext(conf, Seconds(args(2).toLong))

    val trainingData = ssc.textFileStream(args(0)).map(LabeledPoint.parse)
    val testData = ssc.textFileStream(args(1)).map(LabeledPoint.parse)

    val model = new StreamingLogisticRegressionWithSGD()
    //initialWeights��ʼȡֵ,Ĭ����0����
      .setInitialWeights(Vectors.zeros(args(3).toInt))

    model.trainOn(trainingData)
    model.predictOnValues(testData.map(lp => (lp.label, lp.features))).print()

    ssc.start()
    ssc.awaitTermination()

  }

}
// scalastyle:on println
