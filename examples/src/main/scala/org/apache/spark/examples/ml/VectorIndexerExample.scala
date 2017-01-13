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
import org.apache.spark.ml.feature.VectorIndexer
// $example off$
import org.apache.spark.mllib.linalg.Vectors
// $example off$
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{SQLContext, DataFrame}
import org.apache.spark.mllib.util._
/**
 * VectorIndexer��Ҫ����:��߾����������ɭ�ֵ�ML�����ķ���Ч����
 */
object VectorIndexerExample {
  def main(args: Array[String]): Unit = {
      val conf = new SparkConf().setAppName("TfIdfExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
  
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    // $example on$
    /**
 *  libSVM�����ݸ�ʽ
 *  <label> <index1>:<value1> <index2>:<value2> ...
 *  ����<label>��ѵ�����ݼ���Ŀ��ֵ,���ڷ���,���Ǳ�ʶĳ�������(֧�ֶ����);���ڻع�,������ʵ��
 *  <index>����1��ʼ������,�����ǲ�����
 *  <value>Ϊʵ��,Ҳ�������ǳ�˵���Ա���
 */
   val datasvm=MLUtils.loadLibSVMFile(sc,"../data/mllib/sample_libsvm_data.txt")
   val data =sqlContext.createDataFrame(datasvm)
    //val data = sqlContext.read.format("libsvm").load("../data/mllib/sample_libsvm_data.txt")
    /**
     * VectorIndexer�Ƕ����ݼ����������е����(��ɢֵ)�������б��
     * ���ܹ��Զ��ж���Щ��������ɢֵ�͵�����,�������ǽ��б��
     */
    val indexer = new VectorIndexer()
      .setInputCol("features")//������
      .setOutputCol("indexed")//�����
      .setMaxCategories(5)//��������Ϊ5,(��ĳһ��)�ж���5��ȡֵ��Ϊ����ֵ
     //fit()������DataFrameת��Ϊһ��Transformer���㷨
    val indexerModel = indexer.fit(data)

    val categoricalFeatures: Set[Int] = indexerModel.categoryMaps.keys.toSet
    println(s"Chose ${categoricalFeatures.size} categorical features: " +
      categoricalFeatures.mkString(", "))

    // Create new column "indexed" with categorical values transformed to indices
    //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    val indexedData = indexerModel.transform(data)
    /**
    +-------------------------+-------------------------+
    |features                 |indexedFeatures          |
    +-------------------------+-------------------------+
    |(3,[0,1,2],[2.0,5.0,7.0])|(3,[0,1,2],[2.0,1.0,1.0])|
    |(3,[0,1,2],[3.0,5.0,9.0])|(3,[0,1,2],[3.0,1.0,2.0])|
    |(3,[0,1,2],[8.0,4.0,9.0])|(3,[0,1,2],[8.0,0.0,2.0])|
    |(3,[0,1,2],[3.0,6.0,2.0])|(3,[0,1,2],[3.0,2.0,0.0])|
    |(3,[0,1,2],[5.0,9.0,2.0])|(3,[0,1,2],[5.0,4.0,0.0])|
    +-------------------------+-------------------------+
    *�������:������������3������,������0,����1,����2,
    *��Row=1,��Ӧ�������ֱ���2.0,5.0,7.0.��ת��Ϊ2.0,1.0,1.0
    *���Ƿ���ֻ������1,����2��ת����,����0û�б�ת��,������Ϊ����0��6��ȡֵ(2,3,4,5,8,,9),
    *����ǰ�������setMaxCategories(5),��˱���Ϊ����ֵ��,���ᱻת����
    * ����1��,(4,5,6,7,9)-->(0,1,2,3,4,5)
    * ����2��,(2,7,9)-->(0,1,2)
    */
    indexedData.show()
    // $example off$

    sc.stop()
  }
}
// scalastyle:on println
