#!/usr/bin/env -S scala shebang

//> using dep "org.zeromq:jeromq:0.5.2"

/*
  Subscribes to a ZMQ publisher and prints the messages to stdout
*/

import scala.util.control.NonFatal
import org.zeromq.SocketType
import org.zeromq.ZMQ
import org.zeromq.ZMQ.Socket
import org.zeromq.ZContext

val port = Try(args(0).toInt).getOrElse(5563) // The ZMQ publisher port
val host = Try(args(1)).getOrElse("localhost") // The ZMQ publisher host
val topic = Try(args(2)).getOrElse("vars") // The ZMQ topic to listen to

try {
  val context = new ZContext
  val socket = context.createSocket(SocketType.SUB)
  socket.connect(s"tcp://$host:$port")
  socket.subscribe(topic.getBytes(ZMQ.CHARSET))
  println(s"Listening to tcp://$host:$port")
  while (!Thread.currentThread().isInterrupted()) {
    val address = socket.recvStr()
    val contents = socket.recvStr()
    println(s"$address : $contents")
  }

} catch {
  case NonFatal(e) => println("Boom! An exception occurred :-(")
}


