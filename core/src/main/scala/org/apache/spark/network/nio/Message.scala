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

package org.apache.spark.network.nio

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import scala.collection.mutable.ArrayBuffer

import com.google.common.base.Charsets.UTF_8

import org.apache.spark.util.Utils

private[nio] abstract class Message(val typ: Long, val id: Int) {
  var senderAddress: InetSocketAddress = null
  var started = false
  var startTime = -1L
  var finishTime = -1L
  var isSecurityNeg = false
  var hasError = false

  def size: Int

  def getChunkForSending(maxChunkSize: Int): Option[MessageChunk]

  def getChunkForReceiving(chunkSize: Int): Option[MessageChunk]

  def timeTaken(): String = (finishTime - startTime).toString + " ms"

  override def toString: String = {
    this.getClass.getSimpleName + "(id = " + id + ", size = " + size + ")"
  }
}


private[nio] object Message {
  val BUFFER_MESSAGE = 1111111111L

  var lastId = 1

  def getNewId(): Int = synchronized {
    lastId += 1
    if (lastId == 0) {
      lastId += 1
    }
    lastId
  }

  def createBufferMessage(dataBuffers: Seq[ByteBuffer], ackId: Int): BufferMessage = {
    if (dataBuffers == null) {
      return new BufferMessage(getNewId(), new ArrayBuffer[ByteBuffer], ackId)
    }
    if (dataBuffers.exists(_ == null)) {
      throw new Exception("Attempting to create buffer message with null buffer")
    }
    new BufferMessage(getNewId(), new ArrayBuffer[ByteBuffer] ++= dataBuffers, ackId)
  }

  def createBufferMessage(dataBuffers: Seq[ByteBuffer]): BufferMessage =
    createBufferMessage(dataBuffers, 0)

  def createBufferMessage(dataBuffer: ByteBuffer, ackId: Int): BufferMessage = {
    if (dataBuffer == null) {
      //ByteBuffer.allocate在能够读和写之前,必须有一个缓冲区,用静态方法 allocate() 来分配缓冲区
      createBufferMessage(Array(ByteBuffer.allocate(0)), ackId)
    } else {
      createBufferMessage(Array(dataBuffer), ackId)
    }
  }

  def createBufferMessage(dataBuffer: ByteBuffer): BufferMessage =
    createBufferMessage(dataBuffer, 0)

  def createBufferMessage(ackId: Int): BufferMessage = {
    createBufferMessage(new Array[ByteBuffer](0), ackId)
  }

  /**
   * Create a "negative acknowledgment" to notify a sender that an error occurred
   * while processing its message.  The exception's stacktrace will be formatted
   * as a string, serialized into a byte array, and sent as the message payload.
    * 创建一个“否定确认”来通知发件人在处理其消息时发生错误,异常的堆栈跟踪将被格式化为字符串,序列化为字节数组,并作为消息有效载荷发送。
   */
  def createErrorMessage(exception: Exception, ackId: Int): BufferMessage = {
    val exceptionString = Utils.exceptionString(exception)
    val serializedExceptionString = ByteBuffer.wrap(exceptionString.getBytes(UTF_8))
    val errorMessage = createBufferMessage(serializedExceptionString, ackId)
    errorMessage.hasError = true
    errorMessage
  }

  def create(header: MessageChunkHeader): Message = {
    val newMessage: Message = header.typ match {
      case BUFFER_MESSAGE => new BufferMessage(header.id,
        //ByteBuffer.allocate在能够读和写之前,必须有一个缓冲区,用静态方法 allocate() 来分配缓冲区
        ArrayBuffer(ByteBuffer.allocate(header.totalSize)), header.other)
    }
    newMessage.hasError = header.hasError
    newMessage.senderAddress = header.address
    newMessage
  }
}
