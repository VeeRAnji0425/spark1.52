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
import org.apache.spark.ml.feature.{RegexTokenizer, Tokenizer}
// $example off$
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{SQLContext, DataFrame}
/**
 * Tokenizer(�ִ���)���ı�����Ϊ��������(ͨ��Ϊ����)
 */
object TokenizerExample {
  def main(args: Array[String]): Unit = {
    
     val conf = new SparkConf().setAppName("TokenizerExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
  
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    // $example on$
    val sentenceDataFrame = sqlContext.createDataFrame(Seq(
      (0, "Hi I heard about Spark"),
      (1, "I wish Java could use case classes"),
      (2, "Logistic,regression,models,are,neat")
    )).toDF("label", "sentence")

    val tokenizer = new Tokenizer().setInputCol("sentence").setOutputCol("words")
    val regexTokenizer = new RegexTokenizer()
      .setInputCol("sentence")
      .setOutputCol("words")
      /**
       * ����������ʽ�ṩ����Ļ���ѡ��,
       * Ĭ�������,������pattern��Ϊ�����ı��ķָ���,
       * �û�����ָ��������gaps����ָ������patten����ʾ��tokens�������Ƿָ���,
       * ������Ϊ�ִʽ���ҵ����п���ƥ������
       */
      .setPattern("\\W") // alternatively .setPattern("\\w+").setGaps(false)
	//transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    val tokenized = tokenizer.transform(sentenceDataFrame)
    /**
      +-----+--------------------+--------------------+
      |label|            sentence|               words|
      +-----+--------------------+--------------------+
      |    0|Hi I heard about ...|[hi, i, heard, ab...|
      |    1|I wish Java could...|[i, wish, java, c...|
      |    2|Logistic,regressi...|[logistic,regress...|
      +-----+--------------------+--------------------+*/
    tokenized.show()
    tokenized.select("words", "label").take(3).foreach(println)
    //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    val regexTokenized = regexTokenizer.transform(sentenceDataFrame)
    /**
    +-----+--------------------+--------------------+
    |label|            sentence|               words|
    +-----+--------------------+--------------------+
    |    0|Hi I heard about ...|[Hi, I, heard, ab...|
    |    1|I wish Java could...|[I, wish, Java, c...|
    |    2|Logistic,regressi...|[Logistic, regres...|
    +-----+--------------------+--------------------+*/
    regexTokenized.show()
    regexTokenized.select("words", "label").take(3).foreach(println)
    // $example off$

    sc.stop()
  }
}
// scalastyle:on println
