package com.kubukoz.demo

import java.util.UUID

import scalaz.zio.console.{putStrLn, Console}
import scalaz.zio.{Managed, ZIO, ZManaged}

trait Connections {
  def connections: Connections.Service
}

object Connections {

  trait Service {
    def connection: Managed[Nothing, String]
  }

  private val uuid = ZIO.effectTotal(UUID.randomUUID().toString)

  class Pooled private[Connections] (poolId: String, console: Console.Service[Any]) {

    trait Instance extends Connections {

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

  val createPool: ZManaged[Console, Nothing, Pooled] = Managed.unwrap {
    ZIO.access[Console](_.console).map { console =>
      ZManaged.make(putStrLn("Creating pool") *> uuid)(_ => putStrLn("Closing pool")).map { poolId =>
        new Pooled(poolId, console)
      }
    }
  }

  object connections {
    val connection: ZManaged[Connections, Nothing, String] = Managed.unwrap(ZIO.access(_.connections.connection))
  }
}
