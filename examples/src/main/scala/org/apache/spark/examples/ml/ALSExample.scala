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

import scala.reflect.runtime.universe

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.recommendation.ALS
import org.apache.spark.sql.SQLContext
/**
 * An example demonstrating ALS.
 * Run with
 * {{{
 * bin/run-example ml.ALSExample
 * }}}
 * ��MovieLens dataset������������,ÿһ�а����û�����Ӱ�������Լ�ʱ���
 * 
 */
object ALSExample {
  // $example on$
  case class Rating(userId: Int, movieId: Int, rating: Float, timestamp: Long)
  
  def parseRating(str: String): Rating = {
    val fields = str.split("::")
    assert(fields.size == 4)
    Rating(fields(0).toInt, fields(1).toInt, fields(2).toFloat, fields(3).toLong)
  }
  // $example off$

  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("ALSExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
  
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    // $example on$
    val ratings = sc.textFile("../data/mllib/als/sample_movielens_ratings.txt")
      .map(parseRating)
      .toDF()
    val Array(training, test) = ratings.randomSplit(Array(0.8, 0.2))

    // Build the recommendation model using ALS on the training data
    val als = new ALS()
      //������������
      .setMaxIter(5)
      //���򻯲���
      .setRegParam(0.01)
      //�����û�����
      .setUserCol("userId")
      //������Ʒ�������
      .setItemCol("movieId")
      //������������
      .setRatingCol("rating")
    val model = als.fit(training)
     //import spark.implicits._
    // Evaluate the model by computing the RMSE on the test data
    //test[userId: int, movieId: int, rating: float, timestamp: bigint]
    val predictions = model.transform(test)
    predictions.collect()
    //ͨ��Ԥ�����ֵľ���������������Ƽ�ģ��
    val evaluator = new RegressionEvaluator()  
      //rmse���������˵����������ɢ�̶�
      .setMetricName("rmse")
      .setLabelCol("rating")
      .setPredictionCol("prediction")
      //predictions [userId: int, movieId: int, rating: float, timestamp: bigint, prediction: float]
    val rmse = evaluator.evaluate(predictions)
    //rmse���������˵����������ɢ�̶�
    println(s"Root-mean-square error = $rmse")
    // $example off$
   sc.stop()
   // spark.stop()
  }
}
// scalastyle:on println

