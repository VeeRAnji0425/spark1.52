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

package org.apache.spark.sql.hive.execution

import org.apache.spark.sql.AnalysisException
import org.apache.spark.sql.hive.test.TestHive.{read, sparkContext, jsonRDD, sql}
import org.apache.spark.sql.hive.test.TestHive.implicits._

case class Nested(a: Int, B: Int)
case class Data(a: Int, B: Int, n: Nested, nestedArray: Seq[Nested])

/**
 * A set of test cases expressed in Hive QL that are not covered by the tests
 * included in the hive distribution.
  * Hive QL中表示的一组测试用例,不包括在hive分布中的测试中。
  * HIVE解析测试套件
  */
class HiveResolutionSuite extends HiveComparisonTest {
  //嵌套数据不区分大小写测试
  test("SPARK-3698: case insensitive test for nested data") {
    read.json(sparkContext.makeRDD(
      """{"a": [{"a": {"a": 1}}]}""" :: Nil)).registerTempTable("nested")
    // This should be successfully analyzed
    //应该成功分析
    sql("SELECT a[0].A.A from nested").queryExecution.analyzed
  }
  //检查对字段的引用
  test("SPARK-5278: check ambiguous reference to fields") {
    read.json(sparkContext.makeRDD(
      """{"a": [{"b": 1, "B": 2}]}""" :: Nil)).registerTempTable("nested")

    // there are 2 filed matching field name "b", we should report Ambiguous reference error
    //有2个提交的匹配字段名称“b”,我们应该报告模糊参考错误
    val exception = intercept[AnalysisException] {
      sql("SELECT a[0].b from nested").queryExecution.analyzed
    }
    assert(exception.getMessage.contains("Ambiguous reference to fields"))
  }

  createQueryTest("table.attr",
    "SELECT src.key FROM src ORDER BY key LIMIT 1")

  createQueryTest("database.table",
    "SELECT key FROM default.src ORDER BY key LIMIT 1")

  createQueryTest("database.table table.attr",
    "SELECT src.key FROM default.src ORDER BY key LIMIT 1")
  //database.table table.attr不区分大小写
  createQueryTest("database.table table.attr case insensitive",
    "SELECT SRC.Key FROM Default.Src ORDER BY key LIMIT 1")

  createQueryTest("alias.attr",
    "SELECT a.key FROM src a ORDER BY key LIMIT 1")

  createQueryTest("subquery-alias.attr",
    "SELECT a.key FROM (SELECT * FROM src ORDER BY key LIMIT 1) a")

  createQueryTest("quoted alias.attr",
    "SELECT `a`.`key` FROM src a ORDER BY key LIMIT 1")

  createQueryTest("attr",
    "SELECT key FROM src a ORDER BY key LIMIT 1")

  createQueryTest("alias.star",
    "SELECT a.* FROM src a ORDER BY key LIMIT 1")
  //与scala反射的情况不敏感
  test("case insensitivity with scala reflection") {
    // Test resolution with Scala Reflection
    sparkContext.parallelize(Data(1, 2, Nested(1, 2), Seq(Nested(1, 2))) :: Nil)
      .toDF().registerTempTable("caseSensitivityTest")

    val query = sql("SELECT a, b, A, B, n.a, n.b, n.A, n.B FROM caseSensitivityTest")
    //获得字段方式
    assert(query.schema.fields.map(_.name) === Seq("a", "b", "A", "B", "a", "b", "A", "B"),
      "The output schema did not preserve the case of the query.")
    query.collect()
  }
  //与scala反射连接的情况不敏感
  ignore("case insensitivity with scala reflection joins") {
    // Test resolution with Scala Reflection
    sparkContext.parallelize(Data(1, 2, Nested(1, 2), Seq(Nested(1, 2))) :: Nil)
      .toDF().registerTempTable("caseSensitivityTest")

    sql("SELECT * FROM casesensitivitytest a JOIN casesensitivitytest b ON a.a = b.a").collect()
  }
  //嵌套重复解析
  test("nested repeated resolution") {
    sparkContext.parallelize(Data(1, 2, Nested(1, 2), Seq(Nested(1, 2))) :: Nil)
      .toDF().registerTempTable("nestedRepeatedTest")
    assert(sql("SELECT nestedArray[0].a FROM nestedRepeatedTest").collect().head(0) === 1)
  }
  //测试ambiguousReferences解决为HIVE
  createQueryTest("test ambiguousReferences resolved as hive",
    """
      |CREATE TABLE t1(x INT);
      |CREATE TABLE t2(a STRUCT<x: INT>, k INT);
      |INSERT OVERWRITE TABLE t1 SELECT 1 FROM src LIMIT 1;
      |INSERT OVERWRITE TABLE t2 SELECT named_struct("x",1),1 FROM src LIMIT 1;
      |SELECT a.x FROM t1 a JOIN t2 b ON a.x = b.k;
    """.stripMargin)

  /**
   * Negative examples.  Currently only left here for documentation purposes.
   * TODO(marmbrus): Test that catalyst fails on these queries.
   */

  /* SemanticException [Error 10009]: Line 1:7 Invalid table alias 'src'
  createQueryTest("table.*",
    "SELECT src.* FROM src a ORDER BY key LIMIT 1") */

  /* Invalid table alias or column reference 'src': (possible column names are: key, value)
  createQueryTest("tableName.attr from aliased subquery",
    "SELECT src.key FROM (SELECT * FROM src ORDER BY key LIMIT 1) a") */

}
