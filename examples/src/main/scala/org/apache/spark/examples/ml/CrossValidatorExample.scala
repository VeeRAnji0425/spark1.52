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

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.LogisticRegression

import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.feature.HashingTF
import org.apache.spark.ml.feature.Tokenizer
import org.apache.spark.ml.tuning.CrossValidator
import org.apache.spark.ml.tuning.ParamGridBuilder
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.sql.Row
import org.apache.spark.sql.SQLContext
import scala.beans.BeanInfo
/**
 * A simple example demonstrating model selection using CrossValidator.
 * ��ʾģ��ѡ��ʹ�ý�����֤һ���򵥵�����
 * This example also demonstrates how Pipelines are Estimators.
 * ������ӻ���ʾ����ιܵ�����
 * This example uses the [[LabeledDocument]] and [[Document]] case classes from
 * [[SimpleTextClassificationPipeline]].
 *
 * Run with
 * {{{
 * bin/run-example ml.CrossValidatorExample
 * }}}
 * 
 * CrossValidator�����ݼ�����Ϊ�����Ӽ��ֱ�ؽ���ѵ���Ͳ��ԡ�
 * �統k��3ʱ,CrossValidator����3��ѵ��������������ݶ�,ÿ�����ݶ�ʹ��2/3��������ѵ��,1/3�����������ԡ�
 */


object CrossValidatorExample {
/**
 * ������������,��������hashingTF.numFeatures��3��ֵ,lr.regParam��2��ֵ,
 * CrossValidatorʹ��2�۽�����֤,�����ͻ����(3*2)*2=12�в�ͬ��ģ����Ҫ����ѵ��
 */
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("CrossValidatorExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    // Prepare training documents, which are labeled.
    //׼��ѵ������,id ����,��ǩ
    val training = sc.parallelize(Seq(
      //id||����||�����ʶ
      LabeledDocument(0L, "a b c d e spark", 1.0),
      LabeledDocument(1L, "b d", 0.0),
      LabeledDocument(2L, "spark f g h", 1.0),
      LabeledDocument(3L, "hadoop mapreduce", 0.0),
      LabeledDocument(4L, "b spark who", 1.0),
      LabeledDocument(5L, "g d a y", 0.0),
      LabeledDocument(6L, "spark fly", 1.0),
      LabeledDocument(7L, "was mapreduce", 0.0),
      LabeledDocument(8L, "e spark program", 1.0),
      LabeledDocument(9L, "a e c l", 0.0),
      LabeledDocument(10L, "spark compile", 1.0),
      LabeledDocument(11L, "hadoop software", 0.0)))

    // Configure an ML pipeline, which consists of three stages: tokenizer, hashingTF, and lr.
      //���û���ѧϰ�ܵ�,��tokenizer, hashingTF, �� lr������ ���
      //Tokenizer ���ֺõĴ�ת��Ϊ����
    val tokenizer = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("words")
      //������ȡ��ת�� TF-IDF�㷨���ı��ִ��д�����������
      //HashingTF ��һ���ĵ��м����������С�Ĵ�Ƶ����,
      //"a a b b c d" HashingTF (262144,[97,98,99,100],[2.0,2.0,1.0,1.0])
    val hashingTF = new HashingTF()
      .setInputCol(tokenizer.getOutputCol)
      .setOutputCol("features")
    //�߼��ع�
    val lr = new LogisticRegression()
      .setMaxIter(10)
      //PipeLine:�����DataFrame��Estimator�㷨����һ���ض���ML Wolkflow
      //һ�� Pipeline�ڽṹ�ϻ����һ������ PipelineStage,ÿһ�� PipelineStage �������һ������
    val pipeline = new Pipeline()
      .setStages(Array(tokenizer, hashingTF, lr))

    // We now treat the Pipeline as an Estimator, wrapping it in a CrossValidator instance.
    //�������ǰѹܵ�������һ��Estimator,������װ��CrossValidatorʵ����
    // This will allow us to jointly choose parameters for all Pipeline stages.
    //�����������������Ϊ�ܵ�������stageѡ�����
    // A CrossValidator requires an Estimator, a set of Estimator ParamMaps, and an Evaluator.    
    //CrossValidator��Ҫһ��Estimator,һ����������������,��һ��Evaluator
    //ע�������evaluator�Ƕ�Ԫ�����BinaryClassificationEvaluator,��Ĭ�ϵĶ�����areaUnderROC.
    val crossval = new CrossValidator()//����
      .setEstimator(pipeline)//Estimator:��DataFrameת��Ϊһ��Transformer���㷨ͨ��ʵ��
      //����������
      .setEvaluator(new BinaryClassificationEvaluator)
    // We use a ParamGridBuilder to construct a grid of parameters to search over.
    //ʹ��ParamGridBuilder ����һ����������
    // With 3 values for hashingTF.numFeatures and 2 values for lr.regParam,
    //hashingTF.numFeatures��3��ֵ,lr.regParam��2��ֵ
    // this grid will have 3 x 2 = 6 parameter settings for CrossValidator to choose from.
     //���������6��������CrossValidator��ѡ��
     //ParamGridBuilder������ѡ����(��:logistic regression��regParam)
    val paramGrid = new ParamGridBuilder()//ͨ��addGrid���������ҪѰ�ҵ���Ѳ���  
      .addGrid(hashingTF.numFeatures, Array(10, 100, 1000))//����HashingTF.numFeaturesΪ����ֵ
      .addGrid(lr.regParam, Array(0.1, 0.01))//����LogisticRegression���򻯲�������ֵ
     .addGrid(lr.maxIter, Array(0, 10,50,100))//����LogisticRegression���򻯲�������ֵ
      .build()//�����ѡ����
    crossval.setEstimatorParamMaps(paramGrid)//���ù�����ѡ����
    crossval.setNumFolds(2) // Use 3+ in practice ��ʵ����ʹ��3 +

    // Run cross-validation, and choose the best set of parameters.
    //���н�����֤,��ѡ����õĲ�����
    //fit()������DataFrameת��Ϊһ��Transformer���㷨
    val cvModel = crossval.fit(training.toDF())

    // Prepare test documents, which are unlabeled.
    //׼�������ĵ�,��Щ�ļ���δ��ǵ�
    val test = sc.parallelize(Seq(
      Document(4L, "spark i j k"),
      Document(5L, "l m n"),
      Document(6L, "mapreduce spark"),
      Document(7L, "apache hadoop")))

    // Make predictions on test documents.cvModel uses the best model found (lrModel)
    //�ڲ����ĵ�����Ԥ��,cvModel��ѡ���������õ�ģ��
    //transform()������DataFrameת��Ϊ����һ��DataFrame���㷨
    //��õ�ģ�Ͳ���  
    val parent = cvModel.bestModel.parent.asInstanceOf[Pipeline]    
    val p=parent.getStages(1).asInstanceOf[HashingTF]
    import org.apache.spark.ml.param.ParamMap
    val lrp=parent.getStages(2).copy(ParamMap.empty).asInstanceOf[LogisticRegression]
    //��õ�ģ�Ͳ���
    println("numFeatures:"+p.getNumFeatures)    
    println(lrp.getMaxIter+"|||"+lrp.getRegParam)
    //println(parent.getRegParam)//���򻯲���
    //println(parent.getMaxIter)//����������
    cvModel.transform(test.toDF())
     //text�ı�||probability ����||predictionԤ��
      .select("id", "text", "probability", "prediction")
      .collect()
      .foreach { case Row(id: Long, text: String, prob: Vector, prediction: Double) =>
      /**
      *(4, spark i j k) --> prob=[0.24804795226775067,0.7519520477322493], prediction=1.0
      *(5, l m n) --> prob=[0.9647209186740322,0.03527908132596766], prediction=0.0
      *(6, mapreduce spark) --> prob=[0.4248344997494984,0.5751655002505017], prediction=1.0
      *(7, apache hadoop) --> prob=[0.6899594200690095,0.3100405799309906], prediction=0.0
       */
      println(s"($id, $text) --> prob=$prob, prediction=$prediction")
    }

    sc.stop()
  }
}
// scalastyle:on println
