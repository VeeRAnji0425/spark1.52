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
import org.apache.spark.ml.feature.RFormula
// $example off$
import org.apache.spark.sql.Row
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{SQLContext, DataFrame}
/**
 * RFormulaͨ��Rģ�͹�ʽ��ѡ����,֧��R�����еĲ��ֲ���
 * 1. ~�ָ�Ŀ��Ͷ���
 * 2. +�ϲ����󣬡�+ 0����ζ��ɾ���ո�
 * 3. :��������ֵ��ˣ�����ֵ����
 * 4. . ����Ŀ�����ȫ����
 */
object RFormulaExample {
  def main(args: Array[String]): Unit = {
    
   val conf = new SparkConf().setAppName("RFormulaExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
  
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    // $example on$
    //����������һ��DataFrame����id,country, hour��clicked���У�
    val dataset = sqlContext.createDataFrame(Seq(
      (7, "US", 18, 1.0),
      (8, "CA", 12, 0.0),
      (9, "NZ", 15, 0.0)
    )).toDF("id", "country", "hour", "clicked")
    //�������ʹ��RFormula��ʽclicked ~ country+ hour��
    //���������ϣ������country��hourԤ��clicked
    val formula = new RFormula()
      .setFormula("clicked ~ country + hour")//��ʽ
      .setFeaturesCol("features")//������
      .setLabelCol("label")//��ǩ����
      //fit()������DataFrameת��Ϊһ��Transformer���㷨
      //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    val output = formula.fit(dataset).transform(dataset)
    /**
    id | country |hour | clicked | features         | label
    ---|---------|-----|---------|------------------|-------
     7 | "US"    | 18  | 1.0     | [0.0, 0.0, 18.0] | 1.0
     8 | "CA"    | 12  | 0.0     | [0.0, 1.0, 12.0] | 0.0
     9 | "NZ"    | 15  | 0.0     | [1.0, 0.0, 15.0] | 0.0
    */
    output.select("features", "label").show()
    // $example off$

    sc.stop()
  }
}
// scalastyle:on println
