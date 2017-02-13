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

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.mllib.linalg.Vectors

/**
 * ��ά���ɷַ���(PCA)
 * Compute the principal components of a tall-and-skinny matrix, whose rows are observations.
 * ����һ�����ɷ����ָ����ݵľ���,�۲���ֵ
 * The input matrix must be stored in row-oriented dense format, one line per row with its entries
 * ��������������Ϊ������ܼ���ʽ�洢,ÿ��һ�е���Ŀ�ɿո����
 * separated by space. For example,
 * {{{
 * 0.5 1.0
 * 2.0 3.0
 * 4.0 5.0
 * }}}
 * represents a 3-by-2 matrix, whose first row is (0.5, 1.0).
 * ����3��2�о���,��һ����(0.5,1)
 * PCA�㷨����:
 * ����m��nά����
 *   1)��ԭʼ���ݰ������n��m�о���X
 *   2)��X��ÿһ��(����һ�������ֶ�)�������ֵ��,����ȥ��һ�еľ�ֵ
 *   3)���Э�������C=\frac{1}{m}XX^\mathsf{T}
 *   4)���Э������������ֵ����Ӧ����������
 *   5)��������������Ӧ����ֵ��С���ϵ��°������гɾ���,ȡǰk����ɾ���P
 *   6)Y=PX��Ϊ��ά��kά�������
 */
object TallSkinnyPCA {
  def main(args: Array[String]) {
   /* if (args.length != 1) {
      System.err.println("Usage: TallSkinnyPCA <input>")
      System.exit(1)
    }*/

    val conf = new SparkConf().setAppName("TallSkinnyPCA").setMaster("local[*]")
    val sc = new SparkContext(conf)

    // Load and parse the data file.
    //���غͽ��������ļ�
    /**
     *{{{
     * 0.5 1.0
     * 2.0 3.0
     * 4.0 5.0
     * }}}
     */
    val rows = sc.textFile("../data/mllib/CosineSimilarity.txt").map { line =>
      val values = line.split(' ').map(_.toDouble)
      Vectors.dense(values)
    }
    //�о���(RowMatrix)���зֲ�ʽ�洢,��������,�ײ�֧�Žṹ�Ƕ���������ɵ�RDD,ÿ����һ���ֲ�����
    val mat = new RowMatrix(rows)
    ///numCols:2
    println("numCols:"+mat.numCols())
    // Compute principal components.
    //�������ɷ�
    val pc = mat.computePrincipalComponents(mat.numCols().toInt)
    /**  
      Principal components are:
      -0.6596045032796274  -0.7516128652792183  
      -0.7516128652792183  0.6596045032796274 
     */
    println("Principal components are:\n" + pc)

    sc.stop()
  }
}
// scalastyle:on println
