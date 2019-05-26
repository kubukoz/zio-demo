package com.kubukoz.demo

import scalaz.zio._
import scalaz.zio.console._

object Main extends App {

  import Messages.messages

  val program = for {
    before <- messages.findAll
    _      <- putStrLn("Before: " + before.toString)
    _      <- ZIO.foreachPar(List.tabulate(100)(n => "Hello " + n))(messages.add)
    after  <- messages.findAll
    _      <- putStrLn("After: " + after.toString)
  } yield ()

  def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    for {
      service <- Ref.make(List.empty[String]).map[Messages.Service](ref => new Messages.InMemory(ref))
      _ <- {
        val cake = new Console.Live with Messages {
          val messages: Messages.Service = service
        }

        program.provide(cake)
      }
    } yield 0
}

trait Messages {
  def messages: Messages.Service
}

object Messages {

  trait Service {
    def findAll: UIO[List[String]]
    def add(message: String): UIO[Unit]
  }

  class InMemory(storage: Ref[List[String]]) extends Messages.Service {
    val findAll: UIO[List[String]]                 = storage.get
    def add(message: String): scalaz.zio.UIO[Unit] = storage.update(message :: _).unit
  }

  object messages {
    val findAll: ZIO[Messages, Nothing, List[String]]      = ZIO.accessM(_.messages.findAll)
    def add(message: String): ZIO[Messages, Nothing, Unit] = ZIO.accessM(_.messages.add(message))
  }
}
