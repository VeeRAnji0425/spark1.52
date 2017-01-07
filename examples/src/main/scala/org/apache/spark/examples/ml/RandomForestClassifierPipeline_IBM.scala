package org.apache.spark.examples.ml
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification._
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.{ IndexToString, StringIndexer, VectorAssembler }
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.sql.SQLContext
import org.apache.spark.{ SparkConf, SparkContext }
/**
 * https://www.ibm.com/developerworks/cn/opensource/os-cn-spark-practice5/
 * ʹ�� ML Pipeline ��������ѧϰ������
 */
object ClassificationPipeline_IBM {
  def main(args: Array[String]) {
    /*if (args.length < 1) {
      println("Usage:ClassificationPipeline inputDataFile")
      sys.exit(1)
    }*/
    val conf = new SparkConf().setMaster("local[2]").setAppName("Classification with ML Pipeline")
    val sc = new SparkContext(conf)
    val sqlCtx = new SQLContext(sc)

    /**
     * ����һ����ֽ�Ҽ�������е�ͼƬ����ȡ�����ݼ����ܹ���������У�ǰ 4 ����ָ��ֵ (������)�����һ������ٱ�ʶ
     * ����������С���任ͼ��ķ��С���任ͼ���ƫ̬��С���任ͼ��ķ�ȣ�ͼ���أ�����ǩ
     * Step 1
     * Read the source data file and convert it to be a dataframe with columns named.
     * 3.6216,8.6661,-2.8073,-0.44699,0
     * 4.5459,8.1674,-2.4586,-1.4621,0
     * 3.866,-2.6383,1.9242,0.10645,0
     * 3.4566,9.5228,-4.0112,-3.5944,0
     * 0.32924,-4.4552,4.5718,-0.9888,0
     * ... ...
     */
    
    
     /***=======spark-shell.cmdִ������===========
     val parsedRDD = sc.textFile("./ml-100k/data_banknote_authentication.txt").map(_.split(",")).map(eachRow => {
      val a = eachRow.map(x => x.toDouble)
      (a(0), a(1), a(2), a(3), a(4))
    })
 		 val df = sqlCtx.createDataFrame(parsedRDD).toDF(
      "f0", "f1", "f2", "f3", "label").cache()

      df.registerTempTable("data")

      sqlCtx.sql("select f0,f1,f2,f3,label from data ").show()
      **/
    
    val parsedRDD = sc.textFile("../data/mllib/data_banknote_authentication.txt").map(_.split(",")).map(eachRow => {
      val a = eachRow.map(x => x.toDouble)
      (a(0), a(1), a(2), a(3), a(4))
    })
    val df = sqlCtx.createDataFrame(parsedRDD).toDF(
      //����������С���任ͼ��ķ��С���任ͼ���ƫ̬��С���任ͼ��ķ�ȣ�ͼ���أ�����ǩ,���һ������ٱ�ʶ
      "f0", "f1", "f2", "f3", "label").cache()
     df.registerTempTable("data")
   val queryCaseWhen = sqlCtx.sql("select f0,f1,f2,f3,label from data ").show()
   
   
  
    /**
     * *
     * Step 2
     * ʹ�� StringIndexer ȥ��Դ��������ַ� Label������ Label ���ֵ�Ƶ�ζ���������б���, �磬0,1,2������
     * �ڱ����������У����������������ò������ԣ���Ϊ���ǵ����ݸ�ʽ���ã�Label ����Ҳֻ�����֣�
     * �����Ѿ��������б���ġ�0���͡�1����ʽ�����Ƕ��ڶ������������� Label �������ַ����ı��뷽ʽ��
     * �硱High��,��Low��,��Medium���ȣ���ô�������ͺ����ã�ת����ĸ�ʽ�����ܱ� Spark ���õĴ���
     */
    val labelIndexer = new StringIndexer()
      .setInputCol("label") //
      .setOutputCol("indexedLabel")
      .fit(df)// fit ������ƺ�ʵ����ʵ�����ǲ�����ģ�巽�������ģʽ����������ʵ����� train ����

    /**
     * Step 3
     * ʹ�� VectorAssembler ��Դ��������ȡ����ָ������,����һ���Ƚϵ�����ͨ�õĲ��裬
     * ��Ϊ���ǵ�ԭʼ���ݼ�����������һЩ��ָ�����ݣ��� ID��Description ��
     * VectorAssembler��һ��ת����,���������������кϲ�Ϊһ������
     */
    val vectorAssembler = new VectorAssembler()
      .setInputCols(Array("f0", "f1", "f2", "f3"))
      .setOutputCol("featureVector")

    /**
     * Step 4
     * ����һ�����ɭ�ַ����� RandomForestClassifier ʵ�������趨��ز�����
     * ��Ҫ�Ǹ������ɭ���㷨���� DataFrame �������ĸ����������������ĸ�������ʶ.
     */
    val rfClassifier = new RandomForestClassifier()
      .setLabelCol("indexedLabel")//��ǩ�е�����
      .setFeaturesCol("featureVector")//ѵ�����ݼ� DataFrame �д洢�������ݵ�����
      .setProbabilityCol("probability")//���Ԥ��������������ֵ�洢�е�����, Ĭ��ֵ�ǡ�probability��
      .setPredictionCol("prediction")//�㷨Ԥ�����Ĵ洢�е�����, Ĭ���ǡ�prediction��
      .setNumTrees(5)//���������ɭ�ַ�����ѵ�� 5 �ö���������

    /**
     * Step 5
     *����ʹ�� IndexToString Transformer ȥ��֮ǰ�����б����� Label ת����ԭʼ�� Label���ָ�֮ǰ�Ŀɶ��ԱȽϸߵ� Label��
     *���������Ǵ洢������ʾģ�͵Ĳ��Խ�����ɶ��Զ���Ƚϸ�
     */
    val labelConverter = new IndexToString()
      .setInputCol("prediction")
      .setOutputCol("predictedLabel")
      .setLabels(labelIndexer.labels)

    //Step 6
    //Randomly split the input data by 8:2, while 80% is for training, the rest is for testing.
     //������������ݰ�8:2����80%������ѵ������������ڲ���
    val Array(trainingData, testData) = df.randomSplit(Array(0.8, 0.2))

    /**
     * Step 7
     * ���� Pipeline ʵ�������һᰴ��˳��ִ�У��������Ǹ��ݵõ��� PipelineModel ʵ����
     * ��һ�������� transform ������ȥ��ѵ���õ�ģ��Ԥ��������ݼ��ķ���,
     * Ҫ����һ�� Pipeline������������Ҫ���� Pipeline �еĸ��� PipelineStage����ָ����ȡ��ת��ģ��ѵ����
     * ����:al pipeline = new Pipeline().setStages(Array(stage1,stage2,stage3,��))
     * Ȼ��Ϳ��԰�ѵ�����ݼ���Ϊ��β����� Pipelin ʵ���� fit ��������ʼ�����ķ�ʽ������Դѵ�����ݣ�
     * ������û᷵��һ�� PipelineModel ��ʵ���� ����������Ԥ��������ݵı�ǩ������һ�� Transformer
     */
    val pipeline = new Pipeline().setStages(Array(labelIndexer, vectorAssembler, rfClassifier, labelConverter))
    val model = pipeline.fit(trainingData)

    /**
     * Step 8
     * Perform predictions about testing data. This transform method will return a result DataFrame
     * with new prediction column appended towards previous DataFrame.
     * ��Ҫ�������� һ�� DataFrame ת������һ�� DataFrame,����һ��ģ�;���һ�� Transformer��
     * ��Ϊ�����԰� һ��������Ԥ���ǩ�Ĳ������ݼ� DataFrame ���ϱ�ǩת������һ������Ԥ���ǩ�� DataFrame��
     * ��Ȼ�����Ľ�������Ա���������������Ŀ��ӻ�
     */
    val predictionResultDF = model.transform(testData) //��Ҫ�������� һ�� DataFrame ת������һ�� DataFrame

    /**
     * Step 9
     * Select features,label,and predicted label from the DataFrame to display.
     * ����ѡ��,��ǩ,��Ԥ���֡��ʾ��ǩ
     * We only show 20 rows, it is just for reference.
     * ����ֻ��ʾ20��,��ֻ�ǹ��ο�
      +--------+--------+--------+---------+-----+--------------+
      |      f0|      f1|      f2|       f3|label|predictedLabel|
      +--------+--------+--------+---------+-----+--------------+
      |  4.3684|  9.6718| -3.9606|  -3.1625|  0.0|           0.0|
      |  1.1432| -3.7413|  5.5777| -0.63578|  0.0|           0.0|
      |-0.38214|  8.3909|  2.1624|  -3.7405|  0.0|           0.0|
      |-0.96511|  9.4111|  1.7305|  -4.8629|  0.0|           0.0|
      |  4.3239| -4.8835|  3.4356|  -0.5776|  0.0|           0.0|
      |  4.8265| 0.80287|  1.6371|   1.1875|  0.0|           0.0|
      |  2.5635|  6.7769|-0.61979|  0.38576|  0.0|           0.0|
      |   5.807|  5.0097| -2.2384|  0.43878|  0.0|           0.0|
      |  3.1377| -4.1096|  4.5701|  0.98963|  0.0|           0.0|
      |  4.2586| 11.2962| -4.0943|  -4.3457|  0.0|           0.0|
      |  1.7939| -1.1174|  1.5454| -0.26079|  0.0|           0.0|
      |  2.5367|   2.599|  2.0938|  0.20085|  0.0|           0.0|
      |  4.7181| 10.0153| -3.9486|  -3.8582|  0.0|           0.0|
      |  4.1654| -3.4495|   3.643|   1.0879|  0.0|           0.0|
      |  4.4069| 10.9072| -4.5775|  -4.4271|  0.0|           0.0|
      |  1.8664|  7.7763|-0.23849|  -2.9634|  0.0|           0.0|
      |  2.1616| -6.8804|  8.1517|-0.081048|  0.0|           0.0|
      |  5.1129|-0.49871| 0.62863|   1.1189|  0.0|           0.0|
      |  3.5438|  1.2395|   1.997|   2.1547|  0.0|           0.0|
      |  3.2351|   9.647| -3.2074|  -2.5948|  0.0|           0.0|
      +--------+--------+--------+---------+-----+--------------+
     */
    predictionResultDF.select("f0", "f1", "f2", "f3", "featureVector","label", "predictedLabel").show(20)

    /**
     * Step 10
     * The evaluator code is used to compute the prediction accuracy, this is
     * �����������������Ԥ�⾫��,
     * usually a valuable feature to estimate prediction accuracy the trained model.
     * ��ͨ����һ���м�ֵ������������Ԥ���׼ȷ��ѵ��ģ��
     */
    val evaluator = new MulticlassClassificationEvaluator()
      .setLabelCol("label")//��ǩ�е�����
      .setPredictionCol("prediction")//�㷨Ԥ�����Ĵ洢�е�����, Ĭ���ǡ�prediction��
      .setMetricName("precision")//��������
    //predictionAccuracy: Double = 0.9825783972125436
    val predictionAccuracy = evaluator.evaluate(predictionResultDF)   
    //Testing Error = 0.017421602787456414
    println("Testing Error = " + (1.0 - predictionAccuracy))
    /**
     * Step 11(Optional)
     * You can choose to print or save the the model structure.
     * ������ѡ���ӡ�򱣴�ģ�ͽṹ
     */
    val randomForestModel = model.stages(2).asInstanceOf[RandomForestClassificationModel]
    println("Trained Random Forest Model is:\n" + randomForestModel.toDebugString)
  }
}