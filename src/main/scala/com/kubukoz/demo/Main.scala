package com.kubukoz.demo

import scalaz.zio._
import scalaz.zio.console._
import Messages.messages
import Connections.connections

object Main extends App {

  type R = Console with Messages with Connections

  val program: ZIO[R, Nothing, Unit] = {
    val msgProgram = for {
      before <- messages.findAll
      _      <- putStrLn("Before: " + before.toString)
      _      <- ZIO.foreachPar(List.tabulate(100)(n => "Hello " + n))(messages.add)
      after  <- messages.findAll
      _      <- putStrLn("After: " + after.toString)
    } yield ()

    val connectionProgram =
      ZIO.collectAll(List.fill(5)(connections.connection.use(conn => putStrLn(s"Using connection $conn")))).unit

    msgProgram *> putStrLn("\n\n") *> connectionProgram
  }

  val runtime: ZManaged[Console, Nothing, R] = for {
    ref <- ZManaged.fromEffect(Ref.make(List.empty[String]))
    cake <- Connections.createPool.map { pool =>
      new Messages.InMemory(ref) with Console.Live with pool.Instance
    }
  } yield cake

  def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    runtime.use(program.provide).const(0)
}
