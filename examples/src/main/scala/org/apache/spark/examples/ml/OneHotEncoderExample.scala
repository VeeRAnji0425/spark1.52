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
import org.apache.spark.ml.feature.{OneHotEncoder, StringIndexer}
// $example off$
import org.apache.spark.sql.Row
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{SQLContext, DataFrame}
/**
 * oneHotEncoder ��ɢ<->����������Label�໥ת��
 * ���ȱ��뽫�������(��ɢ��,�Ѿ�ת��Ϊ���ֱ����ʽ),ӳ��ɶ��ȱ��롣
 * ������Logistic�ع�������Ҫ������ֵֵ��Ϊ��������ķ�������Ҳ����ʹ�����(��ɢ)����
 * ����˷��������ô����������ݵ�����
 */
object OneHotEncoderExample {
  def main(args: Array[String]): Unit = {
    
    val conf = new SparkConf().setAppName("OneHotEncoderExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
  
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
   

    // $example on$
    val df = sqlContext.createDataFrame(Seq(
      (0, "a"),
      (1, "b"),
      (2, "c"),
      (3, "a"),
      (4, "a"),
      (5, "c")
    )).toDF("id", "category")
  //onehotencoderǰ��Ҫת��Ϊstring->numerical
    val indexer = new StringIndexer()
      .setInputCol("category")
      .setOutputCol("categoryIndex")
      .fit(df)
    val indexed = indexer.transform(df)
  //������ֲ���������OneHotEncoder��ת������Ե���������ֵ����
    val encoder = new OneHotEncoder()
      .setInputCol("categoryIndex")
      .setOutputCol("categoryVec")
    //ע�ⲻ��Ҫfit 
    val encoded = encoder.transform(indexed)
    /**
     * ���ݻ���ϡ���
     * +---+-------------+
     * | id|  categoryVec|
     * +---+-------------+
     * |  0|(2,[0],[1.0])|
     * |  1|    (2,[],[])|
     * |  2|(2,[1],[1.0])|
     * |  3|(2,[0],[1.0])|
     * |  4|(2,[0],[1.0])|
     * |  5|(2,[1],[1.0])|
     * +---+-------------+
     */
    encoded.select("id", "categoryVec").show()
    // $example off$

    sc.stop()
  }
}
// scalastyle:on println
