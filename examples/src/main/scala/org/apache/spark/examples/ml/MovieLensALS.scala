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

import scopt.OptionParser

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.examples.mllib.AbstractParams
import org.apache.spark.ml.recommendation.ALS
import org.apache.spark.sql.{Row, SQLContext}

/**
 * Эͬ����,������С����(ALS)��ʽ��������ʽ����
 * An example app for ALS on MovieLens data (http://grouplens.org/datasets/movielens/).
 * ALS��MovieLens�����ϵ�һ��ʾ��Ӧ�ó���
 * Run with
 * {{{
 * bin/run-example ml.MovieLensALS
 * }}}
 */
object MovieLensALS {
  //����һ��������,�û�ID,��ӰID,����,����ʱ��
  case class Rating(userId: Int, movieId: Int, rating: Float, timestamp: Long)

  object Rating {
    def parseRating(str: String): Rating = {
      val fields = str.split("::")//��::�ָ�
      assert(fields.size == 4)//�ָ���
      Rating(fields(0).toInt, fields(1).toInt, fields(2).toFloat, fields(3).toLong)
    }
  }
  //�����Ӱ��,��ӰID,����,��Ӱ����
  case class Movie(movieId: Int, title: String, genres: Seq[String])

  object Movie {
    //������Ӱ
    def parseMovie(str: String): Movie = {
      val fields = str.split("::")
      assert(fields.size == 3)//\\|�ָ�
      Movie(fields(0).toInt, fields(1), fields(2).split("\\|"))
    }
  }

  case class Params(
      ratings: String = "../data/mllib/als/sample_movielens_ratings.txt",
      movies: String = "../data/mllib/als/sample_movielens_movies.txt",
      maxIter: Int = 10,//��������
      regParam: Double = 0.1,//���򻯲���
      rank: Int = 10,////��ģ�������������ӵĸ���,�ֽ���������
      numBlocks: Int = 10) extends AbstractParams[Params]

  def main(args: Array[String]) {
    val defaultParams = Params()

    val parser = new OptionParser[Params]("MovieLensALS") {
      head("MovieLensALS: an example app for ALS on MovieLens data.")
      opt[String]("ratings")
        //.required()
        .text("path to a MovieLens dataset of ratings")
        .action((x, c) => c.copy(ratings = x))
      opt[String]("movies")
        //.required()
        .text("path to a MovieLens dataset of movies")
        .action((x, c) => c.copy(movies = x))
      opt[Int]("rank")
        .text(s"rank, default: ${defaultParams.rank}")
        .action((x, c) => c.copy(rank = x))
      opt[Int]("maxIter")
        .text(s"max number of iterations, default: ${defaultParams.maxIter}")
        .action((x, c) => c.copy(maxIter = x))
      opt[Double]("regParam")
        .text(s"regularization parameter, default: ${defaultParams.regParam}")
        .action((x, c) => c.copy(regParam = x))
      opt[Int]("numBlocks")
        .text(s"number of blocks, default: ${defaultParams.numBlocks}")
        .action((x, c) => c.copy(numBlocks = x))
      note(
        """
          |Example command line to run this app:
          |
          | bin/spark-submit --class org.apache.spark.examples.ml.MovieLensALS \
          |  examples/target/scala-*/spark-examples-*.jar \
          |  --rank 10 --maxIter 15 --regParam 0.1 \
          |  --movies data/mllib/als/sample_movielens_movies.txt \
          |  --ratings data/mllib/als/sample_movielens_ratings.txt
        """.stripMargin)
    }

    parser.parse(args, defaultParams).map { params =>
      run(params)
    } getOrElse {
      System.exit(1)
    }
  }

  def run(params: Params) {
    val conf = new SparkConf().setAppName(s"MovieLensALS with $params").setMaster("local[*]")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    //��������sample_movielens_ratings.txt����,����������
    val ratings = sc.textFile(params.ratings).map(Rating.parseRating).cache()
    //������
    val numRatings = ratings.count()
    //�û���
    val numUsers = ratings.map(_.userId).distinct().count()
    //��Ӱ��
    val numMovies = ratings.map(_.movieId).distinct().count()
    //Got 1501 ratings(����) from 30 users(�û�) on 100 movies(��Ӱ).
    println(s"Got $numRatings ratings from $numUsers users on $numMovies movies.")
    //�ָ�����
    val splits = ratings.randomSplit(Array(0.8, 0.2), 0L)
    //ѵ������
    val training = splits(0).cache()
    //��������
    val test = splits(1).cache()
    //ѵ����
    val numTraining = training.count()
    //������
    val numTest = test.count()
    //Training: 1168, test: 333.
    println(s"Training: $numTraining, test: $numTest.")
    
    ratings.unpersist(blocking = false)

    val als = new ALS()
      .setUserCol("userId")//�����û�ID
      .setItemCol("movieId")//��ƷID
      .setRank(params.rank)//��ģ�������������ӵĸ���,�ֽ���������
      .setMaxIter(params.maxIter)//��������
      .setRegParam(params.regParam)//���򻯲���
      .setNumBlocks(params.numBlocks)
    //fit()������DataFrameת��Ϊһ��Transformer���㷨
    val model = als.fit(training.toDF())
	  //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    val predictions = model.transform(test.toDF()).cache()
    /**
      +------+-------+------+----------+----------+
      |userId|movieId|rating| timestamp|prediction|
      +------+-------+------+----------+----------+
      |     5|     31|   1.0|1424380312| 1.2984413|
      |    14|     31|   3.0|1424380312|  2.152092|
      |     9|     32|   4.0|1424380312| 2.6434822|
      |    20|     32|   2.0|1424380312| 3.4105005|
      |    23|     33|   2.0|1424380312| 2.3467884|
      +------+-------+------+----------+----------+
     */
      predictions.show(5)
    // Evaluate the model.  rmse���������˵����������ɢ�̶�
    // TODO: Create an evaluator to compute RMSE. ����һ����������������������
    /**
      +------+----------+
      |rating|prediction|
      +------+----------+
      |   1.0| 1.2984413|
      |   3.0|  2.152092|
      |   4.0| 2.6434822|
      |   2.0| 3.4105005|
      |   2.0| 2.3467884|
      +------+----------+*/
    predictions.select("rating", "prediction").show(5)
    //ratingԭ������,prediction�����ȼ� mse ������
    val mse = predictions.select("rating", "prediction").rdd
      .flatMap { case Row(rating: Float, prediction: Float) =>
        val err = rating.toDouble - prediction
        val err2 = err * err 
        if (err2.isNaN) {
          None
        } else {
          Some(err2)
        }
      }.mean()
   //rmse���������˵����������ɢ�̶�
    val rmse = math.sqrt(mse)
    //Test RMSE = 1.135149010495338.
    println(s"Test RMSE = $rmse.")

    // Inspect false positives. ��������
    // Note: We reference columns in 2 ways: ���ǲο���2�ַ�ʽ
    //  (1) predictions("movieId") lets us specify the movieId column in the predictions
    //      DataFrame, rather than the movieId column in the movies DataFrame.
    //  (2) $"userId" specifies the userId column in the predictions DataFrame.
    //      We could also write predictions("userId") but do not have to since
    //      the movies DataFrame does not have a column "userId."
    //���ص�Ӱ����,��������
    val movies = sc.textFile(params.movies).map(Movie.parseMovie).toDF()
    predictions.join(movies).show(5)
    /**
     *+------+-------+------+----------+----------+-------+-------+-------------------+
      |userId|movieId|rating| timestamp|prediction|movieId|  title|             genres|
      +------+-------+------+----------+----------+-------+-------+-------------------+
      |     5|     31|   1.0|1424380312| 1.2984413|      0|Movie 0|  [Romance, Comedy]|
      |     5|     31|   1.0|1424380312| 1.2984413|      1|Movie 1|    [Action, Anime]|
      |     5|     31|   1.0|1424380312| 1.2984413|      2|Movie 2|[Romance, Thriller]|
      |     5|     31|   1.0|1424380312| 1.2984413|      3|Movie 3|  [Action, Romance]|
      |     5|     31|   1.0|1424380312| 1.2984413|      4|Movie 4|    [Anime, Comedy]|
      +------+-------+------+----------+----------+-------+-------+-------------------+
     */
    val falsePositives = predictions.join(movies)
      .where((predictions("movieId") === movies("movieId"))//��Ӱ���
        && ($"rating" <= 1) && ($"prediction" >= 4))//�ȼ�<=1���� Ԥ����>=4
      .select($"userId", predictions("movieId"), $"title", $"rating", $"prediction")      
      /**
       *+------+-------+-----+------+----------+
        |userId|movieId|title|rating|prediction|
        +------+-------+-----+------+----------+
        +------+-------+-----+------+----------+
       */
      falsePositives.show(5)
    val numFalsePositives = falsePositives.count()
    //���Ҳ���ȷ������
    println(s"Found $numFalsePositives false positives")
    if (numFalsePositives > 0) {
      println(s"Example false positives:")
      falsePositives.limit(100).collect().foreach(println)
    }

    sc.stop()
  }
}
// scalastyle:on println
