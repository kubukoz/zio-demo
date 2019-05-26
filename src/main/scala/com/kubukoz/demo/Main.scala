package com.kubukoz.demo

import scalaz.zio._
import scalaz.zio.console._
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import Messages.messages
import Connections.connections
import java.{util => ju}

object Main extends App {

  val pool = Managed.apply(
    putStrLn("Creating reservation").const(
      Reservation(putStrLn("Acquiring reservation"), putStrLn("Releasing reservation"))
    )
  )

  val program = for {
    before <- messages.findAll
    _      <- putStrLn("Before: " + before.toString)
    _      <- ZIO.foreachPar(List.tabulate(100)(n => "Hello " + n))(messages.add)
    after  <- messages.findAll
    _      <- putStrLn("After: " + after.toString)

    _ <- putStrLn("\n\n")
    _ <- connections.connection.use(conn => putStrLn(s"Using connection $conn"))
    _ <- connections.connection.use(conn => putStrLn(s"Using connection $conn"))
  } yield ()

  def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    for {
      ref <- Ref.make(List.empty[String])
      _ <- {
        Connections.createPool.use { pool =>
          program.provide {
            new Messages.InMemory(ref) with Console.Live with Connections {
              val connections: Connections.Service = pool.connections
            }
          }
        }

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

trait Connections {
  def connections: Connections.Service
}

object Connections {

  trait Service {
    def connection: Managed[Nothing, String]
  }

  val createPool: ZManaged[Console, Nothing, Connections] = Managed.unwrap {
    ZIO.access[Console](_.console).map { console =>
      val uuid = ZIO.effectTotal(ju.UUID.randomUUID().toString())

      ZManaged.make(putStrLn("Creating pool") *> uuid)(_ => putStrLn("Closing pool")).map { poolId =>
        new Connections {
          val connections: Service = new Connections.Service {
            val connection: Managed[Nothing, String] =
              Managed.make {
                uuid.tap { id =>
                  console.putStrLn(s"Creating connection $id from pool $poolId")
                }
              }(id => console.putStrLn(s"Closing connection $id"))
          }
        }
      }
    }
  }

  object connections {
    val connection: ZManaged[Connections, Nothing, String] = Managed.unwrap(ZIO.access(_.connections.connection))
  }
}
