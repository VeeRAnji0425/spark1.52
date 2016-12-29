package org.apache.spark.examples.mllib

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Matrices
import org.apache.spark.mllib.linalg.Matrix
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.distributed.IndexedRow
import org.apache.spark.mllib.linalg.distributed.IndexedRowMatrix
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.distributed.CoordinateMatrix
import org.apache.spark.mllib.linalg.distributed.MatrixEntry
import org.apache.spark.mllib.linalg.distributed.BlockMatrix

/**
 * �������Ͳ���
 */
object DataTypes {
/**
 * ϡ�����:�ھ����У�����ֵΪ0��Ԫ����ĿԶԶ���ڷ�0Ԫ�ص���Ŀʱ����Ƹþ���Ϊϡ�����
 * �ܼ�����:�ھ����У�����0Ԫ����Ŀռ�����ʱ����Ƹþ���Ϊ�ܼ�����
 */
  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("SparkHdfsLR")
    val sc = new SparkContext(sparkConf)
    /**������������**/
    //����������Local Vector���洢�ڵ�̨�����ϣ���������0��ʼ�����ͱ�ʾ��ֵ����Double���͵�ֵ��ʾ
    // Create a dense vector (1.0, 0.0, 3.0).
    //�ܶȾ�����ֵҲ�洢
    val dv: Vector = Vectors.dense(1.0, 0.0, 3.0)
    // Create a sparse vector (1.0, 0.0, 3.0) by specifying its indices and values corresponding to nonzero entries.
   //����ϡ�����ָ��Ԫ�صĸ���������������ֵ�����鷽ʽ
    val sv1: Vector = Vectors.sparse(3, Array(0, 2), Array(1.0, 3.0))
    // Create a sparse vector (1.0, 0.0, 3.0) by specifying its nonzero entries.
    // ����ϡ�����ָ��Ԫ�صĸ���������������ֵ���������з�ʽ
    val sv2: Vector = Vectors.sparse(3, Seq((0, 1.0), (2, 3.0)))

    /**����ǩ��**/

    // Create a labeled point with a positive label and a dense feature vector.
    val pos = LabeledPoint(1.0, Vectors.dense(1.0, 0.0, 3.0))

    // Create a labeled point with a negative label and a sparse feature vector.
    val neg = LabeledPoint(0.0, Vectors.sparse(3, Array(0, 2), Array(1.0, 3.0)))
    //ϡ������,MLlib���Զ�ȡ��LibSVM��ʽ�洢��ѵ��ʵ��,ÿ�д���һ�������ǩ��ϡ����������
    //������1��ʼ���ҵ���,���ر�ת��Ϊ��0��ʼ   
    /**
 *  libSVM�����ݸ�ʽ
 *  <label> <index1>:<value1> <index2>:<value2> ...
 *  ����<label>��ѵ�����ݼ���Ŀ��ֵ,���ڷ���,���Ǳ�ʶĳ�������(֧�ֶ����);���ڻع�,������ʵ��
 *  <index>����1��ʼ������,�����ǲ�����
 *  <value>Ϊʵ��,Ҳ�������ǳ�˵���Ա���
 */
    val examples: RDD[LabeledPoint] = MLUtils.loadLibSVMFile(sc, "data/mllib/sample_libsvm_data.txt")
    /**�����ܼ�����***/
    // Create a dense matrix ((1.0, 2.0), (3.0, 4.0), (5.0, 6.0))
    val dm: Matrix = Matrices.dense(3, 2, Array(1.0, 3.0, 5.0, 2.0, 4.0, 6.0))
    /**����ϡ�����***/
    /**���о���
    1.0 0.0 4.0

    0.0 3.0 5.0

    2.0 0.0 6.0
		�������ϡ�����洢�Ļ�����洢��Ϣ�������е���ʽ��
		ʵ�ʴ洢ֵ�� [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]`,
		����Ԫ�ض�Ӧ����������rowIndices=[0, 2, 1, 0, 1, 2]`
		����ʼλ�������� `colPointers=[0, 2, 3, 6]**/
    // Create a sparse matrix ((9.0, 0.0), (0.0, 8.0), (0.0, 6.0))
    val sm: Matrix = Matrices.sparse(3, 2, Array(0, 1, 3), Array(0, 2, 1), Array(9, 6, 8))
    /**�ֲ�ʽ����**/
    val rows: RDD[Vector] = null // an RDD of local vectors
    // Create a RowMatrix from an RDD[Vector].
    //�ֲ�ʽ������
    /**�зֲ�ʽ����**/
    val mat: RowMatrix = new RowMatrix(rows)

    // Get its size.
    val m = mat.numRows()
    val n = mat.numCols()

    // QR decomposition 
    val qrResult = mat.tallSkinnyQR(true)
    /**�����зֲ�ʽ����**/
    //�������������ݼ���Ϣ
    val rowsIndex: RDD[IndexedRow] = null // an RDD of indexed rows �����е�RDD
    // Create an IndexedRowMatrix from an RDD[IndexedRow].
    val matIndex: IndexedRowMatrix = new IndexedRowMatrix(rowsIndex)

    // Get its size. �õ����Ĵ�С
    val mIndex = matIndex.numRows()
    val nIndex = matIndex.numCols()

    // Drop its row indices. �½�������
    val rowMat: RowMatrix = matIndex.toRowMatrix()
    /***��Ԫ�����*/
    val entries: RDD[MatrixEntry] = null // an RDD of matrix entries ����Ԫ�ص�RDD
    // Create a CoordinateMatrix from an RDD[MatrixEntry].
    //����һ���������ΪRDD
    val matCoordinate: CoordinateMatrix = new CoordinateMatrix(entries)

    // Get its size.
    //�õ����Ĵ�С
    val mCoordinate = mat.numRows()
    val nCoordinate = mat.numCols()

    // Convert it to an IndexRowMatrix whose rows are sparse vectors.
    //����ת��Ϊһ������ϡ�������������о���
    val indexedRowMatrix = matCoordinate.toIndexedRowMatrix()
    /**BlockMatrix�����**/

    val coordMat: CoordinateMatrix = new CoordinateMatrix(entries)

    val matA: BlockMatrix = coordMat.toBlockMatrix().cache()

    // Validate whether the BlockMatrix is set up properly. Throws an Exception when it is not valid.
    //��֤�Ƿ���ȷ��������,������Чʱ�׳�һ���쳣
    // Nothing happens if it is valid.���������Ч��,ʲô�����ᷢ��
    matA.validate()

    // Calculate A^T A.
    val ata = matA.transpose.multiply(matA)

  }
}