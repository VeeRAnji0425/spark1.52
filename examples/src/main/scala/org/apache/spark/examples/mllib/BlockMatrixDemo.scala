package org.apache.spark.examples.mllib

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.distributed.IndexedRowMatrix
import org.apache.spark.mllib.linalg.distributed.BlockMatrix
import org.apache.spark.mllib.linalg.distributed.IndexedRow
import org.apache.spark.mllib.linalg.Vectors
/**
* �ֿ����(BlockMatrix)����RDD֧�ŵķֲ�ʽ����,RDD�е�Ԫ��ΪMatrixBlock,
* MatrixBlock�Ƕ��((Int, Int),Matrix)��ɵ�Ԫ��,����(Int,Int)�Ƿֿ�����,Matriax��ָ�����������Ӿ���
*/
object BlockMatrixDemo {
  def main(args: Array[String]) {
    //val sparkConf = new SparkConf().setMast("local[2]").setAppName("SparkHdfsLR")

    val conf = new SparkConf().setAppName("test").setMaster("local")
    val sc = new SparkContext(conf)
    implicit def double2long(x: Double) = x.toLong
    val rdd1 = sc.parallelize(
      Array(
        Array(1.0, 20.0, 30.0, 40.0),
        Array(2.0, 50.0, 60.0, 70.0),
        Array(3.0, 80.0, 90.0, 100.0))).map(f =>{
          //takeȡǰn��Ԫ�� drop����ǰn��Ԫ��
          //1.0|||20.0,30.0,40.0
          println(f.take(1)(0)+"|||"+f.drop(1).mkString(","))
          IndexedRow(f.take(1)(0), Vectors.dense(f.drop(1)))
        })
    //�����о���(IndexedRowMatrix)���зֲ�ʽ�洢,��������,��ײ�֧�Žṹ������������ɵ�RDD,����ÿ�п���ͨ������(long)�;ֲ�������ʾ
    val indexRowMatrix = new IndexedRowMatrix(rdd1)
    //��IndexedRowMatrixת����BlockMatrix,ָ��ÿ���������
    val blockMatrix: BlockMatrix = indexRowMatrix.toBlockMatrix(2, 2)

    //ִ�к�Ĵ�ӡ���ݣ�
    //Index:(0,0)MatrixContent:2 x 2 CSCMatrix
    //(1,0) 20.0
    //(1,1) 30.0
    //Index:(1,1)MatrixContent:2 x 1 CSCMatrix
    //(0,0) 70.0
    //(1,0) 100.0
    //Index:(1,0)MatrixContent:2 x 2 CSCMatrix
    //(0,0) 50.0
    //(1,0) 80.0
    //(0,1) 60.0
    //(1,1) 90.0
    //Index:(0,1)MatrixContent:2 x 1 CSCMatrix
    //(1,0) 40.0
    //�Ӵ�ӡ���ݿ��Կ��������ֿ������õ���ϡ�����CSC��ʽ�洢
    blockMatrix.blocks.foreach(f => println("Index:" + f._1 + "MatrixContent:" + f._2))

    //ת���ɱ��ؾ���
    //0.0   0.0   0.0    
    //20.0  30.0  40.0   
    //50.0  60.0  70.0   
    //80.0  90.0  100.0 
    //��ת��������ݿ��Կ���,��indexRowMatrix.toBlockMatrix(2, 2)
    //����ʱ,ָ����������ʵ�ʾ������ݲ�ƥ��ʱ,�������Ӧ����ֵ���
    //LocalMatrix�ֲ�����ʹ���������������͸���(double)��ֵ,�洢�ڵ�����
    blockMatrix.toLocalMatrix()

    //��������
    blockMatrix.add(blockMatrix)

    //��������blockMatrix*blockMatrix^T��T��ʾת�ã�
    blockMatrix.multiply(blockMatrix.transpose)

    //ת����CoordinateMatrix
    //CoordinateMatrix������ϡ���ԱȽϸߵļ�����,MatrixEntry��һ�� Tuple���͵�Ԫ��,���а����С��к�Ԫ��ֵ
     blockMatrix.toCoordinateMatrix()

    //ת����IndexedRowMatrix
    blockMatrix.toIndexedRowMatrix()

    //��֤�ֿ����ĺϷ���
    blockMatrix.validate()
  }
}