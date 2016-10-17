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

import scala.beans.BeanInfo

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.feature.{HashingTF, Tokenizer}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.sql.{Row, SQLContext}

@BeanInfo
case class LabeledDocument(id: Long, text: String, label: Double)

@BeanInfo
case class Document(id: Long, text: String)

  /**
   * �򵥷������ʾ��
   * A simple text classification pipeline that recognizes "spark" from input text. This is to show
   * һ���򵥵��ı�����ܵ����ӡ�Spark�������ı���ʶ��
   * how to create and configure an ML pipeline. Run with
   * ������ʾ��δ���������һ��ML�Ĺܵ�
   * {{{
   * bin/run-example ml.SimpleTextClassificationPipeline
   * }}}
   */
object SimpleTextClassificationPipeline {

  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("SimpleTextClassificationPipeline").setMaster("local[2]")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    // Prepare training documents, which are labeled.
    //׼��ѵ���ĵ�,��Щ�ļ������б�ǩ
    val training = sc.parallelize(Seq(
      //�ĵ�ID,����,���
      LabeledDocument(0L, "a b c d e spark", 1.0),
      LabeledDocument(1L, "b d", 0.0),
      LabeledDocument(2L, "spark f g h", 1.0),
      LabeledDocument(3L, "hadoop mapreduce", 0.0)))

    // Configure an ML pipeline, which consists of three stages: tokenizer, hashingTF, and lr.
    //����ML����,���а��������׶�
    //ʹ�ý��ı���ֳɵ���
    val tokenizer = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("words")
    //������ȡ��ת�� TF-IDF
    val hashingTF = new HashingTF()
      .setNumFeatures(1000)//��������ֵ
      .setInputCol(tokenizer.getOutputCol)
      .setOutputCol("features")//text=spark hadoop spark,features=(1000,[269,365],[1.0,2.0]) //2����spark����2��
    //�Ѵ�Ƶ��Ϊ�������������߼��ع������
    val lr = new LogisticRegression()
      .setMaxIter(10)//����������
      .setRegParam(0.001)
    //����Щ�����ϲ���һ��pipeline��,��pipelineʵ��ִ�д�����ѵ�������й���ģ�͵Ĺ���
    val pipeline = new Pipeline()
      .setStages(Array(tokenizer, hashingTF, lr))

    // Fit the pipeline to training documents.
    //���ܵ���װ��ѵ���ĵ�
    //��ʽת��ΪschemaRDD
    val model = pipeline.fit(training.toDF())

    // Prepare test documents, which are unlabeled.
    //׼�������ĵ�,��Щ�ļ���δ��ǵ�
    //��ģ���������ĵ��ķ���,
    /**
      LabeledDocument(0L, "a b c d e spark", 1.0),
      LabeledDocument(1L, "b d",0.0),
      LabeledDocument(2L, "spark f g h", 1.0),
      LabeledDocument(3L, "hadoop mapreduce", 0.0)))
     */
    val test = sc.parallelize(Seq(
      Document(4L, "spark i j k"),
      Document(5L, "l m n  i k"),
      Document(6L, "spark hadoop spark"),
      Document(7L, "apache hadoop")))

    // Make predictions on test documents.
     //ע��Modelʵ������һ����������ת���߼���pipeline,������һ���Է���ĵ���
    model.transform(test.toDF())
      .select("id", "text","features", "probability", "prediction")
      .collect()
      .foreach { case Row(id: Long, text: String,  features: Vector, prob: Vector, prediction: Double) =>
        //�ĵ�ID,text�ı�,probability����,prediction Ԥ�����
        println(s"($id, $text) --> prob=$prob, prediction=$prediction,text=$text,features=$features")
      }
   
   /**
    * (4, spark i j k) --> prob=[0.1596407738787411,0.8403592261212589], prediction=1.0
    * (5, l m n) --> prob=[0.8378325685476612,0.16216743145233883], prediction=0.0
    * (6, spark hadoop spark) --> prob=[0.0692663313297627,0.9307336686702373], prediction=1.0
    * (7, apache hadoop) --> prob=[0.9821575333444208,0.01784246665557917], prediction=0.0
    */

    sc.stop()
  }
}
// scalastyle:on println
