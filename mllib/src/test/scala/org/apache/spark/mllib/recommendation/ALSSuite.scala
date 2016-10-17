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

package org.apache.spark.mllib.recommendation

import scala.collection.JavaConversions._
import scala.math.abs
import scala.util.Random

import org.jblas.DoubleMatrix

import org.apache.spark.SparkFunSuite
import org.apache.spark.mllib.util.MLlibTestSparkContext
import org.apache.spark.storage.StorageLevel
/**
 * 协同过滤
 */
object ALSSuite {
/**
 * Llib中对每个解决最小二乘问题的正则化参数lambda做了扩展：
 * 一个是在更新用户因素时用户产生的评分数量；另一个是在更新产品因素时产品被评分的数量。
 */
  def generateRatingsAsJavaList(
      users: Int,
      products: Int,
      features: Int,
      samplingRate: Double,
      implicitPrefs: Boolean,
      negativeWeights: Boolean): (java.util.List[Rating], DoubleMatrix, DoubleMatrix) = {
    val (sampledRatings, trueRatings, truePrefs) =
      generateRatings(users, products, features, samplingRate, implicitPrefs)
    (seqAsJavaList(sampledRatings), trueRatings, truePrefs)
  }

  def generateRatings(
      users: Int,
      products: Int,
      features: Int,
      samplingRate: Double,
      implicitPrefs: Boolean = false,
      negativeWeights: Boolean = false,
      negativeFactors: Boolean = true): (Seq[Rating], DoubleMatrix, DoubleMatrix) = {
    val rand = new Random(42)

    // Create a random matrix with uniform values from -1 to 1
    //创建一个具有均匀值的随机矩阵，从-1到1
    def randomMatrix(m: Int, n: Int) = {
      if (negativeFactors) {
        new DoubleMatrix(m, n, Array.fill(m * n)(rand.nextDouble() * 2 - 1): _*)
      } else {
        new DoubleMatrix(m, n, Array.fill(m * n)(rand.nextDouble()): _*)
      }
    }

    val userMatrix = randomMatrix(users, features)
    val productMatrix = randomMatrix(features, products)
    val (trueRatings, truePrefs) = implicitPrefs match {
      case true =>
        // Generate raw values from [0,9], or if negativeWeights, from [-2,7]
        //从[0，]生成原始值,或者如果negativeweights，从[ 2,7 ]
        val raw = new DoubleMatrix(users, products,
          Array.fill(users * products)(
            (if (negativeWeights) -2 else 0) + rand.nextInt(10).toDouble): _*)
        val prefs =
          new DoubleMatrix(users, products, raw.data.map(v => if (v > 0) 1.0 else 0.0): _*)
        (raw, prefs)
      case false => (userMatrix.mmul(productMatrix), null)
    }

    val sampledRatings = {
      for (u <- 0 until users; p <- 0 until products if rand.nextDouble() < samplingRate)
        yield Rating(u, p, trueRatings.get(u, p))
    }

    (sampledRatings, trueRatings, truePrefs)
  }
}


class ALSSuite extends SparkFunSuite with MLlibTestSparkContext {

  test("rank-1 matrices") {//秩1矩阵
    testALS(50, 100, 1, 15, 0.7, 0.3)
  }

  test("rank-1 matrices bulk") {//秩1矩阵体积
    testALS(50, 100, 1, 15, 0.7, 0.3, false, true)
  }

  test("rank-2 matrices") {//矩阵的秩
    testALS(100, 200, 2, 15, 0.7, 0.3)
  }

  test("rank-2 matrices bulk") {//秩矩阵体积
    testALS(100, 200, 2, 15, 0.7, 0.3, false, true)
  }

  test("rank-1 matrices implicit") {//秩1隐式矩阵
    testALS(80, 160, 1, 15, 0.7, 0.4, true)
  }

  test("rank-1 matrices implicit bulk") {//秩1隐式大块
    testALS(80, 160, 1, 15, 0.7, 0.4, true, true)
  }

  test("rank-2 matrices implicit") {//秩2隐式矩阵
    testALS(100, 200, 2, 15, 0.7, 0.4, true)
  }

  test("rank-2 matrices implicit bulk") {//秩2隐式矩阵大块
    testALS(100, 200, 2, 15, 0.7, 0.4, true, true)
  }

  test("rank-2 matrices implicit negative") {//秩2隐式矩阵负数
    testALS(100, 200, 2, 15, 0.7, 0.4, true, false, true)
  }

  test("rank-2 matrices with different user and product blocks") {
    testALS(100, 200, 2, 15, 0.7, 0.4, numUserBlocks = 4, numProductBlocks = 2)
  }

  test("pseudorandomness") {//伪随机性
    val ratings = sc.parallelize(ALSSuite.generateRatings(10, 20, 5, 0.5, false, false)._1, 2)
    val model11 = ALS.train(ratings, 5, 1, 1.0, 2, 1)//训练
    val model12 = ALS.train(ratings, 5, 1, 1.0, 2, 1)//训练
    val u11 = model11.userFeatures.values.flatMap(_.toList).collect().toList
    val u12 = model12.userFeatures.values.flatMap(_.toList).collect().toList
    val model2 = ALS.train(ratings, 5, 1, 1.0, 2, 2)
    val u2 = model2.userFeatures.values.flatMap(_.toList).collect().toList
    assert(u11 == u12)
    assert(u11 != u2)
  }

  test("Storage Level for RDDs in model") {//存储级别RDD模型
    val ratings = sc.parallelize(ALSSuite.generateRatings(10, 20, 5, 0.5, false, false)._1, 2)
    var storageLevel = StorageLevel.MEMORY_ONLY
    var model = new ALS()
      .setRank(5)
      .setIterations(1)
      .setLambda(1.0)
      .setBlocks(2)
      .setSeed(1)
      .setFinalRDDStorageLevel(storageLevel)
      .run(ratings)
    assert(model.productFeatures.getStorageLevel == storageLevel);
    assert(model.userFeatures.getStorageLevel == storageLevel);
    storageLevel = StorageLevel.DISK_ONLY
    model = new ALS()
      .setRank(5)//模型中潜在因素的数量
      .setIterations(1)//迭代次数
      .setLambda(1.0)
      .setBlocks(2)
      .setSeed(1)
      .setFinalRDDStorageLevel(storageLevel)
      .run(ratings)
    assert(model.productFeatures.getStorageLevel == storageLevel);
    assert(model.userFeatures.getStorageLevel == storageLevel);
  }

  test("negative ids") {//负ID
    val data = ALSSuite.generateRatings(50, 50, 2, 0.7, false, false)
    val ratings = sc.parallelize(data._1.map { case Rating(u, p, r) =>
      Rating(u - 25, p - 25, r)
    })
    val correct = data._2
    val model = ALS.train(ratings, 5, 15)

    val pairs = Array.tabulate(50, 50)((u, p) => (u - 25, p - 25)).flatten
    val ans = model.predict(sc.parallelize(pairs)).collect()
    ans.foreach { r =>
      val u = r.user + 25
      val p = r.product + 25
      val v = r.rating
      val error = v - correct.get(u, p)
      assert(math.abs(error) < 0.4)
    }
  }

  test("NNALS, rank 2") {
    testALS(100, 200, 2, 15, 0.7, 0.4, false, false, false, -1, -1, false)
  }

  /**
   * Test if we can correctly factorize R = U * P where U and P are of known rank.
   * 如果我们能正确地分解试验
   * @param users number of users
   * @param products number of products
   * @param features number of features (rank of problem)
   * @param iterations number of iterations to run
   * @param samplingRate what fraction of the user-product pairs are known
   * @param matchThreshold max difference allowed to consider a predicted rating correct
   * @param implicitPrefs flag to test implicit feedback
   * @param bulkPredict flag to test bulk predicition
   * @param negativeWeights whether the generated data can contain negative values
   * @param numUserBlocks number of user blocks to partition users into
   * @param numProductBlocks number of product blocks to partition products into
   * @param negativeFactors whether the generated user/product factors can have negative entries
   */
  // scalastyle:off
  def testALS(
      users: Int,
      products: Int,
      features: Int,
      iterations: Int,//迭代次数
      samplingRate: Double,
      matchThreshold: Double,
      implicitPrefs: Boolean = false,//制定是否使用显示反馈ALS变体（或者说是对隐式反馈数据的一种适应）
      bulkPredict: Boolean = false,
      negativeWeights: Boolean = false,
      numUserBlocks: Int = -1,//并行计算的块数量,（默认值为-1，表示自动配置）
      numProductBlocks: Int = -1,
      negativeFactors: Boolean = true) {
    // scalastyle:on

    val (sampledRatings, trueRatings, truePrefs) = ALSSuite.generateRatings(users, products,
      features, samplingRate, implicitPrefs, negativeWeights, negativeFactors)

    val model = new ALS()
      .setUserBlocks(numUserBlocks)
      .setProductBlocks(numProductBlocks)
      .setRank(features)//模型中潜在因素的数量
      .setIterations(iterations)
      .setAlpha(1.0)//应用于隐式数据的ALS变体，它控制的是观察到偏好的基本置信度
      .setImplicitPrefs(implicitPrefs)
      .setLambda(0.01)
      .setSeed(0L)
      .setNonnegative(!negativeFactors)
      .run(sc.parallelize(sampledRatings))

    val predictedU = new DoubleMatrix(users, features)
    for ((u, vec) <- model.userFeatures.collect(); i <- 0 until features) {
      predictedU.put(u, i, vec(i))
    }
    val predictedP = new DoubleMatrix(products, features)
    for ((p, vec) <- model.productFeatures.collect(); i <- 0 until features) {
      predictedP.put(p, i, vec(i))
    }
    val predictedRatings = bulkPredict match {
      case false => predictedU.mmul(predictedP.transpose)
      case true =>
        val allRatings = new DoubleMatrix(users, products)
        val usersProducts = for (u <- 0 until users; p <- 0 until products) yield (u, p)
        val userProductsRDD = sc.parallelize(usersProducts)
        model.predict(userProductsRDD).collect().foreach { elem =>
          allRatings.put(elem.user, elem.product, elem.rating)
        }
        allRatings
    }

    if (!implicitPrefs) {
      for (u <- 0 until users; p <- 0 until products) {
        val prediction = predictedRatings.get(u, p)
        val correct = trueRatings.get(u, p)
        if (math.abs(prediction - correct) > matchThreshold) {
          fail(("Model failed to predict (%d, %d): %f vs %f\ncorr: %s\npred: %s\nU: %s\n P: %s")
            .format(u, p, correct, prediction, trueRatings, predictedRatings, predictedU,
              predictedP))
        }
      }
    } else {
      // For implicit prefs we use the confidence-weighted RMSE to test (ref Mahout's tests)
      var sqErr = 0.0
      var denom = 0.0
      for (u <- 0 until users; p <- 0 until products) {
        val prediction = predictedRatings.get(u, p)
        val truePref = truePrefs.get(u, p)
        val confidence = 1 + 1.0 * abs(trueRatings.get(u, p))
        val err = confidence * (truePref - prediction) * (truePref - prediction)
        sqErr += err
        denom += confidence
      }
      val rmse = math.sqrt(sqErr / denom)
      if (rmse > matchThreshold) {
        fail("Model failed to predict RMSE: %f\ncorr: %s\npred: %s\nU: %s\n P: %s".format(
          rmse, truePrefs, predictedRatings, predictedU, predictedP))
      }
    }
  }
}

