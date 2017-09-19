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

package org.apache.spark.sql.execution

import scala.language.implicitConversions
import scala.reflect.runtime.universe.TypeTag
import scala.util.control.NonFatal

import org.apache.spark.SparkFunSuite
import org.apache.spark.sql.{DataFrame, DataFrameHolder, Row, SQLContext}
import org.apache.spark.sql.catalyst.analysis.UnresolvedAttribute
import org.apache.spark.sql.catalyst.util._

/**
 * Base class for writing tests for individual physical operators. For an example of how this
 * class's test helper methods can be used, see [[SortSuite]].
 */
private[sql] abstract class SparkPlanTest extends SparkFunSuite {
  protected def _sqlContext: SQLContext

  /**
   * Creates a DataFrame from a local Seq of Product.
   * 从创建产品局部序列的一个数据框。
   */
  implicit def localSeqToDataFrameHolder[A <: Product : TypeTag](data: Seq[A]): DataFrameHolder = {
    _sqlContext.implicits.localSeqToDataFrameHolder(data)
  }

  /**
   * Runs the plan and makes sure the answer matches the expected result.
   * 运行计划,确保答案符合预期的结果
   * @param input the input data to be used.使用的输入数据
   * @param planFunction a function which accepts the input SparkPlan and uses it to instantiate
   *                     the physical operator that's being tested.
   *                      一个函数接受输入的Spark计划和使用它来实例化正在测试的物理运算符
   * @param expectedAnswer the expected result in a [[Seq]] of [[Row]]s.
   * 											预期的结果在一个[序列]的的[行]
   * @param sortAnswers if true, the answers will be sorted by their toString representations prior
   *                    to being compared.
   */
  protected def checkAnswer(
      input: DataFrame,
      planFunction: SparkPlan => SparkPlan,
      expectedAnswer: Seq[Row],
      sortAnswers: Boolean = true): Unit = {
    doCheckAnswer(
      input :: Nil,
      (plans: Seq[SparkPlan]) => planFunction(plans.head),
      expectedAnswer,
      sortAnswers)
  }

  /**
   * Runs the plan and makes sure the answer matches the expected result.
   * 运行计划,确保答案符合预期的结果
   * @param left the left input data to be used. 使用的左输入数据
   * @param right the right input data to be used.使用的右输入数据
   * @param planFunction a function which accepts the input SparkPlan and uses it to instantiate
   *                     the physical operator that's being tested.
   * @param expectedAnswer the expected result in a [[Seq]] of [[Row]]s.
   * @param sortAnswers if true, the answers will be sorted by their toString representations prior
   *                    to being compared.
   */
  protected def checkAnswer2(
      left: DataFrame,
      right: DataFrame,
      planFunction: (SparkPlan, SparkPlan) => SparkPlan,
      expectedAnswer: Seq[Row],
      sortAnswers: Boolean = true): Unit = {
    doCheckAnswer(
      left :: right :: Nil,
      (plans: Seq[SparkPlan]) => planFunction(plans(0), plans(1)),
      expectedAnswer,
      sortAnswers)
  }

  /**
   * Runs the plan and makes sure the answer matches the expected result.
   * 运行计划,确保答案符合预期的结果
   * @param input the input data to be used. 使用的输入数据
   * @param planFunction a function which accepts a sequence of input SparkPlans and uses them to
   *                     instantiate the physical operator that's being tested.
   *                     一个函数接受一个输入序列Spark计划并使用它们来实例化物理运算符被检测
   * @param expectedAnswer the expected result in a [[Seq]] of [[Row]]s.
   * @param sortAnswers if true, the answers will be sorted by their toString representations prior
   *                    to being compared.
   */
  protected def doCheckAnswer(
      input: Seq[DataFrame],
      planFunction: Seq[SparkPlan] => SparkPlan,
      expectedAnswer: Seq[Row],
      sortAnswers: Boolean = true): Unit = {
    SparkPlanTest.checkAnswer(input, planFunction, expectedAnswer, sortAnswers, _sqlContext) match {
      case Some(errorMessage) => fail(errorMessage)
      case None =>
    }
  }

  /**
   * Runs the plan and makes sure the answer matches the result produced by a reference plan.
   * 运行计划,确保答案匹配参考计划的结果
   * @param input the input data to be used. 使用的输入数据
   * @param planFunction a function which accepts the input SparkPlan and uses it to instantiate
   *                     the physical operator that's being tested.
   *                     一个函数接受输入的Spark计划和使用它来实例化正在测试的物理运算符
   * @param expectedPlanFunction a function which accepts the input SparkPlan and uses it to
   *                             instantiate a reference implementation of the physical operator
   *                             that's being tested. The result of executing this plan will be
   *                             treated as the source-of-truth for the test.
   *                             一个函数接受输入sparkplan和用它来实例化一个物理运算符被检测的参考实现。
   *                             执行此计划的结果将被视为测试的真实来源。
   * @param sortAnswers if true, the answers will be sorted by their toString representations prior
   *                    to being compared. 如果为true,答案将由他们的toString表示,相比之前的排序
   */
  protected def checkThatPlansAgree(
      input: DataFrame,
      planFunction: SparkPlan => SparkPlan,
      expectedPlanFunction: SparkPlan => SparkPlan,
      sortAnswers: Boolean = true): Unit = {
    SparkPlanTest.checkAnswer(
        input, planFunction, expectedPlanFunction, sortAnswers, _sqlContext) match {
      case Some(errorMessage) => fail(errorMessage)
      case None =>
    }
  }
}

/**
 * Helper methods for writing tests of individual physical operators.
 * 用于编写单个物理运算符的测试的辅助方法
 */
object SparkPlanTest {

  /**
   * Runs the plan and makes sure the answer matches the result produced by a reference plan.
   * 运行计划,并确保答案匹配的结果所产生的参考计划
   * @param input the input data to be used. 使用的输入数据
   * @param planFunction a function which accepts the input SparkPlan and uses it to instantiate
   *                     the physical operator that's being tested.
   *                     一个函数接受输入sparkplan和用它来实例化物理运算符被检测
   * @param expectedPlanFunction a function which accepts the input SparkPlan and uses it to
   *                             instantiate a reference implementation of the physical operator
   *                             that's being tested. The result of executing this plan will be
   *                             treated as the source-of-truth for the test.
   */
  def checkAnswer(
      input: DataFrame,
      planFunction: SparkPlan => SparkPlan,
      expectedPlanFunction: SparkPlan => SparkPlan,
      sortAnswers: Boolean,
      _sqlContext: SQLContext): Option[String] = {

    val outputPlan = planFunction(input.queryExecution.sparkPlan)
    val expectedOutputPlan = expectedPlanFunction(input.queryExecution.sparkPlan)

    val expectedAnswer: Seq[Row] = try {
      executePlan(expectedOutputPlan, _sqlContext)
    } catch {
      case NonFatal(e) =>
        val errorMessage =
          s"""
             | Exception thrown while executing Spark plan to calculate expected answer:
             | $expectedOutputPlan
             | == Exception ==
             | $e
             | ${org.apache.spark.sql.catalyst.util.stackTraceToString(e)}
          """.stripMargin
        return Some(errorMessage)
    }

    val actualAnswer: Seq[Row] = try {
      executePlan(outputPlan, _sqlContext)
    } catch {
      case NonFatal(e) =>
        val errorMessage =
          s"""
             | Exception thrown while executing Spark plan:
             | $outputPlan
             | == Exception ==
             | $e
             | ${org.apache.spark.sql.catalyst.util.stackTraceToString(e)}
          """.stripMargin
        return Some(errorMessage)
    }

    compareAnswers(actualAnswer, expectedAnswer, sortAnswers).map { errorMessage =>
      s"""
         | Results do not match.
         | Actual result Spark plan:
         | $outputPlan
         | Expected result Spark plan:
         | $expectedOutputPlan
         | $errorMessage
       """.stripMargin
    }
  }

  /**
   * Runs the plan and makes sure the answer matches the expected result.
   * 运行计划,确保答案符合预期的结果
   * @param input the input data to be used.使用的输入数据
   * @param planFunction a function which accepts the input SparkPlan and uses it to instantiate
   *                     the physical operator that's being tested.
   *                     一个函数接受输入sparkplan和用它来实例化正在测试的物理运算符
   * @param expectedAnswer the expected result in a [[Seq]] of [[Row]]s.
   * 										预期的结果在一个[序列]的的[行]
   * @param sortAnswers if true, the answers will be sorted by their toString representations prior
   *                    to being compared.
   *                    如果为true,答案将由他们的toString表示相比之前的排序
   */
  def checkAnswer(
      input: Seq[DataFrame],
      planFunction: Seq[SparkPlan] => SparkPlan,
      expectedAnswer: Seq[Row],
      sortAnswers: Boolean,
      _sqlContext: SQLContext): Option[String] = {

    val outputPlan = planFunction(input.map(_.queryExecution.sparkPlan))

    val sparkAnswer: Seq[Row] = try {
      executePlan(outputPlan, _sqlContext)
    } catch {
      case NonFatal(e) =>
        val errorMessage =
          s"""
             | Exception thrown while executing Spark plan:
             | $outputPlan
             | == Exception ==
             | $e
             | ${org.apache.spark.sql.catalyst.util.stackTraceToString(e)}
          """.stripMargin
        return Some(errorMessage)
    }

    compareAnswers(sparkAnswer, expectedAnswer, sortAnswers).map { errorMessage =>
      s"""
         | Results do not match for Spark plan:
         | $outputPlan
         | $errorMessage
       """.stripMargin
    }
  }

  private def compareAnswers(
      sparkAnswer: Seq[Row],
      expectedAnswer: Seq[Row],
      sort: Boolean): Option[String] = {
    def prepareAnswer(answer: Seq[Row]): Seq[Row] = {
      // Converts data to types that we can do equality comparison using Scala collections.
      // For BigDecimal type, the Scala type has a better definition of equality test (similar to
      // Java's java.math.BigDecimal.compareTo).
      // For binary arrays, we convert it to Seq to avoid of calling java.util.Arrays.equals for
      // equality test.
      // This function is copied from Catalyst's QueryTest
      val converted: Seq[Row] = answer.map { s =>
        Row.fromSeq(s.toSeq.map {
          case d: java.math.BigDecimal => BigDecimal(d)
          case b: Array[Byte] => b.toSeq
          case o => o
        })
      }
      if (sort) {
        converted.sortBy(_.toString())
      } else {
        converted
      }
    }
    if (prepareAnswer(expectedAnswer) != prepareAnswer(sparkAnswer)) {
      val errorMessage =
        s"""
           | == Results ==
           | ${sideBySide(
              s"== Expected Answer - ${expectedAnswer.size} ==" +:
              prepareAnswer(expectedAnswer).map(_.toString()),
              s"== Actual Answer - ${sparkAnswer.size} ==" +:
              prepareAnswer(sparkAnswer).map(_.toString())).mkString("\n")}
      """.stripMargin
      Some(errorMessage)
    } else {
      None
    }
  }

  private def executePlan(outputPlan: SparkPlan, _sqlContext: SQLContext): Seq[Row] = {
    // A very simple resolver to make writing tests easier. In contrast to the real resolver
    // this is always case sensitive and does not try to handle scoping or complex type resolution.
    //一个非常简单的解析器,使写测试更容易, 与真正的解析器相反这总是区分大小写,并且不尝试处理范围或复杂的类型解析。
    val resolvedPlan = _sqlContext.prepareForExecution.execute(
      outputPlan transform {
        case plan: SparkPlan =>
          val inputMap = plan.children.flatMap(_.output).map(a => (a.name, a)).toMap
          plan.transformExpressions {
            case UnresolvedAttribute(Seq(u)) =>
              inputMap.getOrElse(u,
                sys.error(s"Invalid Test: Cannot resolve $u given input $inputMap"))
          }
      }
    )
    resolvedPlan.executeCollect().toSeq
  }
}

