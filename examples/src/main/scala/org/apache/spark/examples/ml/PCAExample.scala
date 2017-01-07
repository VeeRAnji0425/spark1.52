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
import org.apache.spark.ml.feature.PCA
import org.apache.spark.mllib.linalg.Vector
// $example off$
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{SQLContext, DataFrame}
import org.apache.spark.mllib.linalg.Vectors
/**
 * PCA���ɷַ�����һ��ͳ��ѧ����,��ʹ������ת����һϵ�п�����صı�������ȡ�����޹ر�����,
 * ��ȡ���ı������е�Ԫ�س�Ϊ���ɷ�,ʹ��PCA�������ԶԱ������Ͻ��н�ά
 */
object PCAExample {
  def main(args: Array[String]): Unit = {
  val conf = new SparkConf().setAppName("CrossValidatorExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
  
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    // $example on$
    /**
     * ʾ������չʾ��ν�5ά��������ת��Ϊ3ά���ɷ�����
     */
    val data = Array(
      Vectors.sparse(5, Seq((1, 1.0), (3, 7.0))),
      Vectors.dense(2.0, 0.0, 3.0, 4.0, 5.0),
      Vectors.dense(4.0, 0.0, 0.0, 6.0, 7.0)
    )
    val df = sqlContext.createDataFrame(data.map(Tuple1.apply)).toDF("features")
    val pca = new PCA()
      .setInputCol("features")
      .setOutputCol("pcaFeatures")
      .setK(3)
      .fit(df)
    val pcaDF = pca.transform(df)
    val result = pcaDF.select("pcaFeatures")
    /**
     * +--------------------+
     * |         pcaFeatures|
     * +--------------------+
     * |[1.64857282308838...|
     * |[-4.6451043317815...|
     * |[-6.4288805356764...|
     * +--------------------+
     */
    result.show()
    // $example off$

    sc.stop()
  }
}
// scalastyle:on println
