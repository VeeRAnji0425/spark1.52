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

package org.apache.spark.ml.feature

import org.apache.spark.SparkFunSuite
import org.apache.spark.ml.attribute._
import org.apache.spark.ml.param.ParamsSuite
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.util.MLlibTestSparkContext

class RFormulaSuite extends SparkFunSuite with MLlibTestSparkContext {
  test("params") {
    ParamsSuite.checkParams(new RFormula())
  }

  test("transform numeric data") {//转换的数字数据
    val formula = new RFormula().setFormula("id ~ v1 + v2")
    val original = sqlContext.createDataFrame(
      Seq((0, 1.0, 3.0), (2, 2.0, 5.0))).toDF("id", "v1", "v2")
      //fit()方法将DataFrame转化为一个Transformer的算法
    val model = formula.fit(original)
     //transform()方法将DataFrame转化为另外一个DataFrame的算法
    val result = model.transform(original)
    val resultSchema = model.transformSchema(original.schema)
    val expected = sqlContext.createDataFrame(
      Seq(
        (0, 1.0, 3.0, Vectors.dense(1.0, 3.0), 0.0),
        (2, 2.0, 5.0, Vectors.dense(2.0, 5.0), 2.0))
      ).toDF("id", "v1", "v2", "features", "label")
    // TODO(ekl) make schema comparisons ignore metadata, to avoid .toString
    assert(result.schema.toString == resultSchema.toString)
    assert(resultSchema == expected.schema)
    assert(result.collect() === expected.collect())
  }

  test("features column already exists") {//特征列已经存在
    val formula = new RFormula().setFormula("y ~ x").setFeaturesCol("x")
    val original = sqlContext.createDataFrame(Seq((0, 1.0), (2, 2.0))).toDF("x", "y")
    intercept[IllegalArgumentException] {
    //fit()方法将DataFrame转化为一个Transformer的算法
      formula.fit(original)
    }
    intercept[IllegalArgumentException] {
    //fit()方法将DataFrame转化为一个Transformer的算法
      formula.fit(original)
    }
  }

  test("label column already exists") {//标签列已经存在
    val formula = new RFormula().setFormula("y ~ x").setLabelCol("y")
    val original = sqlContext.createDataFrame(Seq((0, 1.0), (2, 2.0))).toDF("x", "y")
    val model = formula.fit(original)
    val resultSchema = model.transformSchema(original.schema)
    assert(resultSchema.length == 3)
     //transform()方法将DataFrame转化为另外一个DataFrame的算法
    assert(resultSchema.toString == model.transform(original).schema.toString)
  }

  test("label column already exists but is not double type") {//标签列已经存在但不是双类型
    val formula = new RFormula().setFormula("y ~ x").setLabelCol("y")
    val original = sqlContext.createDataFrame(Seq((0, 1), (2, 2))).toDF("x", "y")
    //fit()方法将DataFrame转化为一个Transformer的算法
    val model = formula.fit(original)
    intercept[IllegalArgumentException] {
     //transform()方法将DataFrame转化为另外一个DataFrame的算法
      model.transformSchema(original.schema)
    }
    intercept[IllegalArgumentException] {
      model.transform(original)
    }
  }

  test("allow missing label column for test datasets") {//允许测试数据集的丢失的标签列
    val formula = new RFormula().setFormula("y ~ x").setLabelCol("label")
    val original = sqlContext.createDataFrame(Seq((0, 1.0), (2, 2.0))).toDF("x", "_not_y")
    //fit()方法将DataFrame转化为一个Transformer的算法
    val model = formula.fit(original)
    val resultSchema = model.transformSchema(original.schema)
    assert(resultSchema.length == 3)
    assert(!resultSchema.exists(_.name == "label"))
    assert(resultSchema.toString == model.transform(original).schema.toString)
  }

  test("encodes string terms") {//编码字符串
    val formula = new RFormula().setFormula("id ~ a + b")
    val original = sqlContext.createDataFrame(
      Seq((1, "foo", 4), (2, "bar", 4), (3, "bar", 5), (4, "baz", 5))
    ).toDF("id", "a", "b")
    //fit()方法将DataFrame转化为一个Transformer的算法
    val model = formula.fit(original)
     //transform()方法将DataFrame转化为另外一个DataFrame的算法
    val result = model.transform(original)
    val resultSchema = model.transformSchema(original.schema)
    val expected = sqlContext.createDataFrame(
      Seq(
        (1, "foo", 4, Vectors.dense(0.0, 1.0, 4.0), 1.0),
        (2, "bar", 4, Vectors.dense(1.0, 0.0, 4.0), 2.0),
        (3, "bar", 5, Vectors.dense(1.0, 0.0, 5.0), 3.0),
        (4, "baz", 5, Vectors.dense(0.0, 0.0, 5.0), 4.0))
      ).toDF("id", "a", "b", "features", "label")
    assert(result.schema.toString == resultSchema.toString)
    assert(result.collect() === expected.collect())
  }

  test("attribute generation") {//属性生成
    val formula = new RFormula().setFormula("id ~ a + b")
    val original = sqlContext.createDataFrame(
      Seq((1, "foo", 4), (2, "bar", 4), (3, "bar", 5), (4, "baz", 5))
    ).toDF("id", "a", "b")
    //fit()方法将DataFrame转化为一个Transformer的算法
    val model = formula.fit(original)
    val result = model.transform(original)
    val attrs = AttributeGroup.fromStructField(result.schema("features"))
    val expectedAttrs = new AttributeGroup(
      "features",
      Array(
        new BinaryAttribute(Some("a__bar"), Some(1)),
        new BinaryAttribute(Some("a__foo"), Some(2)),
        new NumericAttribute(Some("b"), Some(3))))
    assert(attrs === expectedAttrs)
  }
}
