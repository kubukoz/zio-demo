package com.kubukoz.demo

import scalaz.zio.{Ref, UIO, ZIO}

trait Messages {
  def messages: Messages.Service
}

object Messages {

  trait Service {
    def findAll: UIO[List[String]]
    def add(message: String): UIO[Unit]
  }

  class InMemory(storage: Ref[List[String]]) extends Messages {

    val messages: Service = new Service {
      val findAll: UIO[List[String]]                 = storage.get
      def add(message: String): scalaz.zio.UIO[Unit] = storage.update(message :: _).unit
    }
  }

  object messages {
    val findAll: ZIO[Messages, Nothing, List[String]]      = ZIO.accessM(_.messages.findAll)
    def add(message: String): ZIO[Messages, Nothing, Unit] = ZIO.accessM(_.messages.add(message))
  }
}
