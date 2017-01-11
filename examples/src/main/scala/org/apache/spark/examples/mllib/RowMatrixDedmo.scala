package org.apache.spark.examples.mllib

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.distributed.CoordinateMatrix
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.mllib.stat.MultivariateStatisticalSummary
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.SingularValueDecomposition
import org.apache.spark.mllib.linalg.Matrix
import org.apache.spark.mllib.linalg.Matrices

object RowMatrixDedmo {
  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("SparkHdfsLR")
    val sc = new SparkContext(sparkConf)
    // ����RDD[Vector]
    val rdd1 = sc.parallelize(
      Array(
        Array(1.0, 2.0, 3.0, 4.0),
        Array(2.0, 3.0, 4.0, 5.0),
        Array(3.0, 4.0, 5.0, 6.0))).map(f => Vectors.dense(f))
    //�����ֲ�ʽ���� RowMatrix
    val rowMatirx = new RowMatrix(rdd1)
    //������֮������ƶȣ����ص���CoordinateMatrix������
    //case class MatrixEntry(i: Long, j: Long, value: Double)�洢ֵ
    //columnSimilarities���������ÿ����֮����������ƶ�
    //����:ʹ�ý����㷨����ֵ,ֵԽ���������ٶ�Խ������Խ��,Ĭ��Ϊ0
    var coordinateMatrix: CoordinateMatrix = rowMatirx.columnSimilarities()
    //���ؾ�������������
    println(coordinateMatrix.numCols())
    println(coordinateMatrix.numRows())
    //�鿴����ֵ���鿴������֮������ƶ�
    //Array[org.apache.spark.mllib.linalg.distributed.MatrixEntry] 
    //= Array(MatrixEntry(2,3,0.9992204753914715), 
    //MatrixEntry(0,1,0.9925833339709303), 
    //MatrixEntry(1,2,0.9979288897338914), 
    //MatrixEntry(0,3,0.9746318461970762), 
    //MatrixEntry(1,3,0.9946115458726394), 
    //MatrixEntry(0,2,0.9827076298239907))
    println(coordinateMatrix.entries.collect())

    //ת�ɺ�������һ������ϸ����
    coordinateMatrix.toBlockMatrix()
    //ת���������о�����һ������ϸ����
    coordinateMatrix.toIndexedRowMatrix()
    //ת����RowMatrix
    coordinateMatrix.toRowMatrix()

    //������ͳ����Ϣ
    var mss: MultivariateStatisticalSummary = rowMatirx.computeColumnSummaryStatistics()
    //ÿ�еľ�ֵ, org.apache.spark.mllib.linalg.Vector = [2.0,3.0,4.0,5.0]
    mss.mean
    // ÿ�е����ֵorg.apache.spark.mllib.linalg.Vector = [3.0,4.0,5.0,6.0]
    mss.max
    // ÿ�е���Сֵ org.apache.spark.mllib.linalg.Vector = [1.0,2.0,3.0,4.0]
    mss.min
    //ÿ�з���Ԫ�صĸ���org.apache.spark.mllib.linalg.Vector = [3.0,3.0,3.0,3.0]
    mss.numNonzeros
    //�����е�1-����,||x||1 = sum��abs(xi)����
    //org.apache.spark.mllib.linalg.Vector = [6.0,9.0,12.0,15.0]
    mss.normL1
    //�����е�2-����,||x||2 = sqrt(sum(xi.^2))��
    // org.apache.spark.mllib.linalg.Vector = [3.7416573867739413,5.385164807134504,7.0710678118654755,8.774964387392123]
    mss.normL2
    //�����еķ���
    //org.apache.spark.mllib.linalg.Vector = [1.0,1.0,1.0,1.0]
    mss.variance
    //����Э����
    //covariance: org.apache.spark.mllib.linalg.Matrix = 
    //1.0  1.0  1.0  1.0  
    //1.0  1.0  1.0  1.0  
    //1.0  1.0  1.0  1.0  
    //1.0  1.0  1.0  1.0  
    //computeCovariance �����������������Э����
    var covariance: Matrix = rowMatirx.computeCovariance()
    //������ķ����rowMatirx^T*rowMatirx��T��ʾת�ò���
    //gramianMatrix: org.apache.spark.mllib.linalg.Matrix = 
    //14.0  20.0  26.0  32.0  
    //20.0  29.0  38.0  47.0  
    //26.0  38.0  50.0  62.0  
    //32.0  47.0  62.0  77.0  
    var gramianMatrix: Matrix = rowMatirx.computeGramianMatrix()
    //�Ծ���������ɷַ���������ָ�����ص������������ֳɸ���
    //PCA�㷨��һ�־���Ľ�ά�㷨
    //principalComponents: org.apache.spark.mllib.linalg.Matrix = 
    //-0.5000000000000002  0.8660254037844388    
    //-0.5000000000000002  -0.28867513459481275  
    //-0.5000000000000002  -0.28867513459481287  
    //-0.5000000000000002  -0.28867513459481287  
    var principalComponents = rowMatirx.computePrincipalComponents(2)

    /**
     * �Ծ����������ֵ�ֽ⣬�����ΪA(m x n). ����ֵ�ֽ⽫�����������󣬷ֱ���U,S,V
     * �������� A ~= U * S * V', S�������趨��k������ֵ��U��VΪ��Ӧ������ֵ����
     */
    //   svd: org.apache.spark.mllib.linalg.SingularValueDecomposition[org.apache.spark.mllib.linalg.distributed.RowMatrix,org.apache.spark.mllib.linalg.Matrix] = 
    //SingularValueDecomposition(org.apache.spark.mllib.linalg.distributed.RowMatrix@688884e,[13.011193721236575,0.8419251442105343,7.793650306633694E-8],-0.2830233037672786  -0.7873358937103356  -0.5230588083704528  
    //-0.4132328277901395  -0.3594977469144485  0.5762839813994667   
    //-0.5434423518130005  0.06834039988143598  0.4166084623124157   
    //-0.6736518758358616  0.4961785466773299   -0.4698336353414313  )
    var svd: SingularValueDecomposition[RowMatrix, Matrix] = rowMatirx.computeSVD(3, true)

    //������˻�����
    var multiplyMatrix: RowMatrix = rowMatirx.multiply(Matrices.dense(4, 1, Array(1, 2, 3, 4)))
  }
}