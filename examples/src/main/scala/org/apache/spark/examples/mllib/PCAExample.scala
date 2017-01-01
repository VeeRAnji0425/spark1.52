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

import java.text.BreakIterator

import scala.collection.mutable

import scopt.OptionParser

import org.apache.log4j.{ Level, Logger }
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.{ SparkContext, SparkConf }
import org.apache.spark.mllib.linalg.{ Vector, Vectors }
import org.apache.spark.rdd.RDD

/**
 * ���ɷ����Ľ�ά
 * ��ά:����ά�Ȼ��߽������������Ĺ���.
 * ΪʲôҪ����������:û�б�Ҫ�������ŵ�ͬһ��ƽ�ȵļ���(����ֵ��ȥƽ��ֵ,Ȼ����Ա�׼��)
 * �������	ռ����� 		���ŵķ������ 		���ŵ�ռ����� 		���ݼ۸�
 * 2524,		12839,		-0.025,					-0.231,					2405
 * 2937,		10000,			0.323,				-0.4,						2200
 * 1778,		8040,			-0.654,					-0.517,					1400
 * 1242,		13104,		-1.105,					-0.215,					1800
 * 2900,		10000,			0.291,				-0.4,						2351
 * 1218,		3049,			 -1.126,				-0.814,					795
 * 2722,		38768,			0.142,				 1.312,					2725
 */
object PCAExample {
  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("KMeansClustering")
    val sc = new SparkContext(sparkConf)
    //�������ݵ�RDD
    val data = sc.textFile("../data/mllib/scaledhousedata.csv")
    //����ת�����ܼ�������RDD
    val parsedData = data.map(line => Vectors.dense(line.
      split(',').map(_.toDouble)))
    //����parsedData����һ��RowMatrix(�о���)
    val mat = new RowMatrix(parsedData)
    //����һ����Ҫ(��)����
    val pc = mat.computePrincipalComponents(1)
    /**
     * -0.9995710763570875
		 * -0.02928588926998105
     */
    //ͨ��������Ҫ�ɷְ���ͶӰ�����Կռ�
    val projected = mat.multiply(pc)
    //��ͶӰ����RowMatrixת����RDD
    val projectedRDD = projected.rows
    
    projectedRDD.saveAsTextFile("phdata")

  }
}
// scalastyle:on println
