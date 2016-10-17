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

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.ml.classification.{ClassificationModel, Classifier, ClassifierParams}
import org.apache.spark.ml.param.{IntParam, ParamMap}
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.mllib.linalg.{BLAS, Vector, Vectors}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.sql.{DataFrame, Row, SQLContext}

/**
 * A simple example demonstrating how to write your own learning algorithm using Estimator,
 * һ���򵥵�������ʾ����ù���д�Լ���ѧϰ�㷨
 * Transformer, and other abstractions.ת������������
 * This mimics [[org.apache.spark.ml.classification.LogisticRegression]].
 * Run with
 * {{{
 * bin/run-example ml.DeveloperApiExample
 * }}}
 */
object DeveloperApiExample {

  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("DeveloperApiExample")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    // Prepare training data. ׼��ѵ������
    val training = sc.parallelize(Seq(
      LabeledPoint(1.0, Vectors.dense(0.0, 1.1, 0.1)),
      LabeledPoint(0.0, Vectors.dense(2.0, 1.0, -1.0)),
      LabeledPoint(0.0, Vectors.dense(2.0, 1.3, 1.0)),
      LabeledPoint(1.0, Vectors.dense(0.0, 1.2, -0.5))))

    // Create a LogisticRegression instance.  This instance is an Estimator.
      //����һ���߼��ع�ʵ��,���ʵ����һ��������
    val lr = new MyLogisticRegression()
    // Print out the parameters, documentation, and any default values.
    //��ӡ�������ĵ����κ�Ĭ��ֵ
    println("MyLogisticRegression parameters:\n" + lr.explainParams() + "\n")

    // We may set parameters using setter methods.
    //���ǿ���ʹ��setter�������ò���
    lr.setMaxIter(10)

    // Learn a LogisticRegression model.  This uses the parameters stored in lr.
    //ѧϰһ���߼��ع�ģ��,��ʹ�ô洢�Ĳ���
    val model = lr.fit(training.toDF())

    // Prepare test data. ׼����������
    val test = sc.parallelize(Seq(
      LabeledPoint(1.0, Vectors.dense(-1.0, 1.5, 1.3)),
      LabeledPoint(0.0, Vectors.dense(3.0, 2.0, -0.1)),
      LabeledPoint(1.0, Vectors.dense(0.0, 2.2, -1.5))))

    // Make predictions on test data. �Բ������ݽ���Ԥ��
    val sumPredictions: Double = model.transform(test.toDF())
      .select("features", "label", "prediction")
      .collect()
      .map { case Row(features: Vector, label: Double, prediction: Double) =>
        prediction
      }.sum
    assert(sumPredictions == 0.0,
      "MyLogisticRegression predicted something other than 0, even though all weights are 0!")

    sc.stop()
  }
}

/**
 * Example of defining a parameter trait for a user-defined type of [[Classifier]].
 * ����һ���û��������͵Ĳ�������������
 * NOTE: This is private since it is an example.  In practice, you may not want it to be private.
 */
private trait MyLogisticRegressionParams extends ClassifierParams {

  /**
   * Param for max number of iterations
   * ��������ĵ�������
   * NOTE: The usual way to add a parameter to a model or algorithm is to include:
   * ͨ���ķ��������һ��ģ�ͻ��㷨�Ĳ����ǰ���
   *   - val myParamName: ParamType
   *   - def getMyParamName
   *   - def setMyParamName
   * Here, we have a trait to be mixed in with the Estimator and Model (MyLogisticRegression
   * ��һ����������ƺ�ģ�ͻ����һ��
   * and MyLogisticRegressionModel).  We place the setter (setMaxIter) method in the Estimator
   * class since the maxIter parameter is only used during training (not in the Model).
   */
  val maxIter: IntParam = new IntParam(this, "maxIter", "max number of iterations")
  def getMaxIter: Int = $(maxIter)
}

/**
 * Example of defining a type of [[Classifier]].
 * ����һ�����͵�����[ [����] ]
 * NOTE: This is private since it is an example.  In practice, you may not want it to be private.
 */
private class MyLogisticRegression(override val uid: String)
  extends Classifier[Vector, MyLogisticRegression, MyLogisticRegressionModel]
  with MyLogisticRegressionParams {

  def this() = this(Identifiable.randomUID("myLogReg"))

  setMaxIter(100) // Initialize

  // The parameter setter is in this class since it should return type MyLogisticRegression.
  //��������������Ϊ��Ӧ�÷�������mylogisticregression
  def setMaxIter(value: Int): this.type = set(maxIter, value)

  // This method is used by fit()���ַ�������fit()
  override protected def train(dataset: DataFrame): MyLogisticRegressionModel = {
    // Extract columns from data using helper method.
    //ʹ�ø�����������������ȡ��
    val oldDataset = extractLabeledPoints(dataset)

    // Do learning to estimate the weight vector.
    //��ѧϰ����Ȩ������
    val numFeatures = oldDataset.take(1)(0).features.size
    val weights = Vectors.zeros(numFeatures) // Learning would happen here. ѧϰ�ᷢ��������

    // Create a model, and return it. ����һ��ģ�ͣ���������
    new MyLogisticRegressionModel(uid, weights).setParent(this)
  }

  override def copy(extra: ParamMap): MyLogisticRegression = defaultCopy(extra)
}

/**
 * Example of defining a type of [[ClassificationModel]].
 * ����һ�����͵�[����ģ��]������
 * NOTE: This is private since it is an example.  In practice, you may not want it to be private.
 */
private class MyLogisticRegressionModel(
    override val uid: String,
    val weights: Vector)
  extends ClassificationModel[Vector, MyLogisticRegressionModel]
  with MyLogisticRegressionParams {

  // This uses the default implementation of transform(), which reads column "features" and outputs
  //������transform()Ĭ��ʵ�֣���ȡ������еġ��ص㡱
  // columns "prediction" and "rawPrediction."
  //�С�Ԥ�⡱�͡�rawprediction����

  // This uses the default implementation of predict(), which chooses the label corresponding to
  //������predict()Ĭ��ʵ��
  // the maximum value returned by [[predictRaw()]].
  //ѡ���ǩ��Ӧ���ֵ���ص�predictraw() 

  /**
   * Raw prediction for each possible label. ��ÿ�����ܵı�ǩ��ԭʼԤ��
   * The meaning of a "raw" prediction may vary between algorithms, but it intuitively gives
   * ԭʼ��Ԥ��ĺ�����ܻ�������ͬ���㷨
   * a measure of confidence in each possible label (where larger = more confident).
   * ����ֱ�۵ظ�����һ������ÿ�����ܵı�ǩ����
   * This internal method is used to implement [[transform()]] and output [[rawPredictionCol]].
   *
   * @return  vector where element i is the raw prediction for label i.
   *          This raw prediction may be any real number, where a larger value indicates greater
   *          confidence for that label.
   */
  override protected def predictRaw(features: Vector): Vector = {
    val margin = BLAS.dot(features, weights)
    // There are 2 classes (binary classification), so we return a length-2 vector,
    // where index i corresponds to class i (i = 0, 1).
    Vectors.dense(-margin, margin)
  }

  /** 
   *  Number of classes the label can take.  2 indicates binary classification.
   *  ��ǩ���Բ�ȡ���������, 2��ʾ�����Ʒ���
   *  */
  override val numClasses: Int = 2

  /**
   * Create a copy of the model.����ģ�͵ĸ���
   * The copy is shallow, except for the embedded paramMap, which gets a deep copy.
   * ������ǳ������Ƕ��ʽparammap���õ����
   * This is used for the default implementation of [[transform()]].
   */
  override def copy(extra: ParamMap): MyLogisticRegressionModel = {
    copyValues(new MyLogisticRegressionModel(uid, weights), extra).setParent(parent)
  }
}
// scalastyle:on println
