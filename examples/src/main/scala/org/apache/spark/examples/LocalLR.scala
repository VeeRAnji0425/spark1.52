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
package org.apache.spark.examples

import java.util.Random

import breeze.linalg.{Vector, DenseVector}

/**
 * Logistic regression based classification.
 * �����߼��ع�ķ���,
 * This is an example implementation for learning how to use Spark. For more conventional use,
 * ����һ��ѧϰ���ʹ��Spark������ʵ��,Ϊ����ͳ��ʹ��(SGD����ݶ��½�)
 * please refer to either(���������Ҫ�����) org.apache.spark.mllib.classification.LogisticRegressionWithSGD or
 * org.apache.spark.mllib.classification.LogisticRegressionWithLBFGS(BFGS������2��ţ�ٷ�) based on your needs.
 */
object LocalLR {
  val N = 10000  // Number of data points ���ݵ�
  val D = 10   // Number of dimensions ά��
  val R = 0.7  // Scaling factor ��������
  val ITERATIONS = 5 //��������
  val rand = new Random(42)

  case class DataPoint(x: Vector[Double], y: Double)

  def generateData: Array[DataPoint] = {
    def generatePoint(i: Int): DataPoint = {
      val y = if (i % 2 == 0) -1 else 1
      //println(rand.nextGaussian)
      val x = DenseVector.fill(D){rand.nextGaussian + y * R}
      DataPoint(x, y)
    }
    Array.tabulate(N)(generatePoint)
  }

  def showWarning() {
    System.err.println(
      """WARN: This is a naive implementation of Logistic Regression and is given as an example!
        |Please use either org.apache.spark.mllib.classification.LogisticRegressionWithSGD(SGD����ݶ��½�) or 
        |org.apache.spark.mllib.classification.LogisticRegressionWithLBFGS(BFGS������2��ţ�ٷ�)
        |for more conventional use.
      """.stripMargin)
  }

  def main(args: Array[String]) {

    showWarning()

    val data = generateData
    //Initialize w to a random value
    //��ʼ��W��һ�����ֵ
    var w = DenseVector.fill(D){2 * rand.nextDouble - 1}
    /**
     * Initial w: 
     * DenseVector(-0.8066603352924779, -0.5488747509304204, -0.7351625370864459, 
     * 						 0.8228539509375878, -0.6662446067860872, -0.33245457898921527, 
     *             0.9664202269036932, -0.20407887461434115, 0.4120993933386614, 
     *             -0.8125908063470539)
     */
    println("Initial w: " + w)

    for (i <- 1 to ITERATIONS) {
      println("On iteration " + i)//��������
      var gradient = DenseVector.zeros[Double](D)//�ܼ�����ά��10
      for (p <- data) {
        val scale = (1 / (1 + math.exp(-p.y * (w.dot(p.x)))) - 1) * p.y
        gradient +=  p.x * scale
      }
      w -= gradient
    }
    /**
     * Final w: DenseVector(5816.075967498844, 5222.008066011373, 5754.751978607454, 
     * 											3853.1772062206874, 5593.565827145935, 5282.38787420105, 
     * 											3662.9216051953567, 4890.782103406075, 4223.371512250295, 
     * 											5767.368579668877)
     */
    println("Final w: " + w)
  }
}
// scalastyle:on println
