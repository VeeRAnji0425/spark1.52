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

import org.apache.spark.SparkConf
import org.apache.spark.mllib.clustering.StreamingKMeans
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
 * ���� ��ʽK��ֵ
 * Estimate clusters on one stream of data and make predictions
 * ��һ���������Ϲ��Ƽ�Ⱥ��������һ�����Ͻ���Ԥ��,
 * on another stream, where the data streams arrive as text files
 * into two different directories.
 * �������������ı��ļ���������ͬ��Ŀ¼ʱ
 *
 * The rows of the training text files must be vector data in the form
 * ѵ���ı��ļ����б����Ǵ����е�ʸ������,����n��ά������Ŀ
 * `[x1,x2,x3,...,xn]`
 * Where n is the number of dimensions.
 *
 * The rows of the test text files must be labeled data in the form
 * �����ı��ļ����б����ڴ����б������
 * `(y,[x1,x2,x3,...,xn])`
 * Where y is some identifier. n must be the same for train and test.
 * ������Y��һЩ��ʶ��,ѵ���Ͳ��Ա�������ͬ��
 *
 * Usage:
 *   StreamingKMeansExample <trainingDir> <testDir> <batchDuration> <numClusters> <numDimensions>
 *
 * To run on your local machine using the two directories `trainingDir` and `testDir`,
 * ʹ������Ŀ¼�ڱ��ػ���������`trainingDir` �� `testDir`,
 * with updates every 5 seconds, 2 dimensions per data point, and 3 clusters, call:
 * ÿ5�����һ��,ÿһ�����ݵ��2��ά��,��3����
 *    $ bin/run-example mllib.StreamingKMeansExample trainingDir testDir 5 3 2
 *
 * As you add text files to `trainingDir` the clusters will continuously update.
 * Anytime you add text files to `testDir`, you'll see predicted labels using the current model.
 *
 */
object StreamingKMeansExample {

  def main(args: Array[String]) {
    if (args.length != 5) {
      System.err.println(
        "Usage: StreamingKMeansExample " +
          "<trainingDir> <testDir> <batchDuration> <numClusters> <numDimensions>")
      System.exit(1)
    }

    val conf = new SparkConf().setMaster("local").setAppName("StreamingKMeansExample")
    //������
    val ssc = new StreamingContext(conf, Seconds(args(2).toLong))
    //�ļ���,ѵ��Ŀ¼,��������
    val trainingData = ssc.textFileStream(args(0)).map(Vectors.parse)
    //����Ŀ¼
    val testData = ssc.textFileStream(args(1)).map(LabeledPoint.parse)

    val model = new StreamingKMeans()
      //�������ĵ�
      .setK(args(3).toInt)
      .setDecayFactor(1.0)
      //���������
      .setRandomCenters(args(4).toInt, 0.0)

    model.trainOn(trainingData)
    model.predictOnValues(testData.map(lp => (lp.label, lp.features))).print()

    ssc.start()
    ssc.awaitTermination()
  }
}
// scalastyle:on println
