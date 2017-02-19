package ru.bigcash.server.actor

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, Props}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.routing.RoundRobinPool
import ru.bigcash.common.CashStack
import ru.bigcash.server.dao.StatisticDAO

/**
  * @author Artem Vedernikov
  */

object BigCashServer {
  def props(port: Int, cashStack: CashStack, dao: StatisticDAO, dbWritersNum: Int): Props = {
    Props(classOf[BigCashServer], port, cashStack, dao, dbWritersNum)
  }
}

/**
  * Actor responsible for handling tcp requests
  * For every new TCP connection new BigCashSession Actor is invoked
  * @param port - port BigCashServer listens to
  * @param cashStack - object containg possible bankones for BigCash game
  * @param dao - Dato Access Object for statistics collection in DB
  * @param dbWritersNum - number of actors responisble for updating information in database
  */
class BigCashServer(port: Int, cashStack: CashStack, dao: StatisticDAO, dbWritersNum: Int) extends Actor with ActorLogging {

  import context.system
  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", port))

  val dbWriters = context.actorOf(MongoWriter.props(dao).withRouter(RoundRobinPool(dbWritersNum)))

  override def receive: Receive = {

    case Bound(localAddress) =>
      log.info(s"BigCash server started at port $port")

    case CommandFailed(_: Bind) =>
      log.error(s"Unable to start BigCash server at port $port. Shutting down")
      context stop self

    case Connected(remote, local) =>
      log.info(s"Accepted new connection from ${remote.getAddress}")
      val connection = sender

      val sessionHandler = context.actorOf(BigCashSession.props(connection, cashStack, dbWriters))
      connection ! Register(sessionHandler)
  }

}
