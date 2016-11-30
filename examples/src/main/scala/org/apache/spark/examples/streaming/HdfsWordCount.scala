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
package org.apache.spark.examples.streaming

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
 * Counts words in new text files created in the given directory
 * �ڸ���Ŀ¼�д��������ı��ļ��еĵ�����
 * Usage: HdfsWordCount <directory>
 *   <directory> is the directory that Spark Streaming will use to find and read new text files.
 *	��Spark�������ڲ��ҺͶ�ȡ�µ��ı��ļ���Ŀ¼
 * To run this on your local machine on directory `localdir`, run this example
 * �����ڱ��ػ����ϵ�Ŀ¼'localdir',���д�ʾ��
 *    $ bin/run-example \
 *       org.apache.spark.examples.streaming.HdfsWordCount localdir
 *
 * Then create a text file in `localdir` and the words in the file will get counted.
 * Ȼ�󴴽�һ���ı��ļ��е�` localdir `���ļ��Ļ������
 */
object HdfsWordCount {
  def main(args: Array[String]) {
 /*   if (args.length < 1) {
      System.err.println("Usage: HdfsWordCount <directory>")
      System.exit(1)
    }*/

    StreamingExamples.setStreamingLogLevels()
    
    val mast="spark://dept3:8088"
    val hdfs="hdfs://xcsq:8089/cookbook/input/"
     //����SparkConf����
    val sparkConf = new SparkConf().setAppName("HdfsWordCount").setMaster(mast)
    // Create the context����������
    val ssc = new StreamingContext(sparkConf, Seconds(2))

    // Create the FileInputDStream on the directory and use the
    // stream to count words in new files created
    //����Ŀ¼��fileinputdstream��ʹ�����������ʴ������ļ�
    //val lines = ssc.textFileStream(args(0))
     //���Ŀ¼�����´������ļ�,���ȡ
    val lines = ssc.textFileStream(hdfs)
   //�ָ�Ϊ����
    val words = lines.flatMap(_.split(" ")) 
    //ͳ�Ƶ��ʳ��ִ���
    val wordCounts = words.map(x => (x, 1)).reduceByKey(_ + _)
    //��ӡ���
    wordCounts.print()
    //����Spark Streaming
    ssc.start()
    //һֱ����,������Ϊ��Ԥ��ֹͣ
    ssc.awaitTermination()
  }
}
// scalastyle:on println
