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

import java.io.File

import scala.io.Source._

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.SparkContext._

/**
  * Simple test for reading and writing to a distributed
  * �ֲ�ʽ�ļ�ϵͳ��д�ļ򵥲���,���������������
  * file system.  This example does the following:
  *
  *   1. Reads local file ��ȡ�����ļ�
  *   2. Computes word count on local file ���㱾���ļ��ϵ��ּ���
  *   3. Writes local file to a DFS д�����ļ���DFS
  *   4. Reads the file back from the DFS ���ļ���DFS������
  *   5. Computes word count on the file using Spark ��Spark�����ļ��ϵ��ּ���
  *   6. Compares the word count results �Ƚ��ּ������
  */
object DFSReadWriteTest {

  private var localFilePath: File = new File(".")
  private var dfsDirPath: String = ""

  private val NPARAMS = 2
  /**
   * ��ȡ�ļ�
   */
  private def readFile(filename: String): List[String] = {
    val lineIter: Iterator[String] = fromFile(filename).getLines()
    val lineList: List[String] = lineIter.toList //Iteratorת��List
    lineList
  }
 /**
  * ��ӡʹ��
  */
  private def printUsage(): Unit = {
    val usage: String = "DFS Read-Write Test\n" +
    "\n" +
    "Usage: localFile dfsDir\n" +
    "\n" +
    "localFile - (string) local file to use in test\n" +
    "dfsDir - (string) DFS directory for read/write tests\n"

    println(usage)
  }
  /**
   * ���������ж��Ƿ��ļ�
   */
  private def parseArgs(args: Array[String]): Unit = {
    if (args.length != NPARAMS) {//�������������2���˳�
      printUsage()
      System.exit(1)
    }

    var i = 0

    localFilePath = new File(args(i))//ȡ����һ�������ļ�
    if (!localFilePath.exists) {//����ļ�������,�������·���ļ�������
      System.err.println("Given path (" + args(i) + ") does not exist.\n")
      printUsage()
      System.exit(1)
    }

    if (!localFilePath.isFile) {//����ļ������ļ�,�������·�������ļ�
      System.err.println("Given path (" + args(i) + ") is not a file.\n")
      printUsage()
      System.exit(1)
    }

    i += 1
    dfsDirPath = args(i)//����ļ�λ�õ�Ŀ¼
  }
/**
 * ���б��ص��ʼ���
 */
  def runLocalWordCount(fileContents: List[String]): Int = {
    fileContents.flatMap(_.split(" "))//�Կո�ָ�
      .flatMap(_.split("\t"))//ˮƽ�Ʊ�(HT)(������һ��TABλ��)
      .filter(_.size > 0)//���˵����ȴ�С��0
      .groupBy(w => w)//����
      .mapValues(_.size)//��С
      .values//ת��ֵ
      .sum//���
  }

  def main(args: Array[String]): Unit = {//��һ������ȡ�ļ�,�ڶ������Ǵ��ļ�Ŀ¼
    val args1=Array("D:\\spark\\spark-1.5.0-hadoop2.6\\README.md","D:\\spark\\spark-1.5.0-hadoop2.6\\")
    //parseArgs(args)
    parseArgs(args1)

    println("Performing local word count")//ִ�б����ּ���
    val fileContents = readFile(localFilePath.toString())//��ȡ�ļ�
    val localWordCount = runLocalWordCount(fileContents)

    println("Creating SparkConf")//����Spark�����ļ�
    val conf = new SparkConf().setAppName("DFS Read Write Test").setMaster("local")

    println("Creating SparkContext")//����Spark������
    val sc = new SparkContext(conf)

    println("Writing local file to DFS")//д�����ļ���DFS
    val dfsFilename = dfsDirPath + "/dfs_read_write_test"//��Ŷ�ȡ�ļ���λ��
    val fileRDD = sc.parallelize(fileContents)//ת��RDD�ļ�
    fileRDD.saveAsTextFile(dfsFilename)//�����ļ�
    //��DFS�Ķ��ļ�����������
    println("Reading file from DFS and running Word Count")
    val readFileRDD = sc.textFile(dfsFilename)

    val dfsWordCount = readFileRDD
      .flatMap(_.split(" "))
      .flatMap(_.split("\t"))
      .filter(_.size > 0)
      .map(w => (w, 1))
      .countByKey()
      .values
      .sum

    sc.stop()

    if (localWordCount == dfsWordCount) {//������ص���ͳ������dfs����ͳ������ͬ,���ʾ���ݶ�ȡ�ɹ�
      println(s"Success! Local Word Count ($localWordCount) " +
        s"and DFS Word Count ($dfsWordCount) agree.")
    } else {
      println(s"Failure! Local Word Count ($localWordCount) " +
        s"and DFS Word Count ($dfsWordCount) disagree.")
    }

  }
}
// scalastyle:on println
