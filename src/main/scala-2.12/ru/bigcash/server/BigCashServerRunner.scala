package ru.bigcash.server

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import ru.bigcash.common.CashStack
import ru.bigcash.server.actor.BigCashServer
import ru.bigcash.server.dao.StatisticDAO

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * @author Artem Vedernikov
  */
object BigCashServerRunner extends App {

  private val log = LoggerFactory.getLogger(getClass)

  val config = ConfigFactory.load("server.conf")

  val mongoHosts = config.getString("mongo.hosts")
  val mongoUser = Try(config.getString("mongo.user")).toOption
  val mongoPassword = Try(config.getString("mongo.password")).toOption

  val statisticDAO = StatisticDAO(mongoHosts, user = mongoUser, password = mongoPassword)

  val port = config.getInt("bigcash.server.port")

  val banknotes = config.getIntList("bigcash.stack").asScala.map(_.toInt).toList
  val dbWritersNum = config.getInt("mongo.writers")

  log.info(s"Starting BigCash server $port with banknotes $banknotes")

  val actorSystem = ActorSystem("ServerSystem", config)

  val stack = new CashStack(banknotes)
  val server = actorSystem.actorOf(BigCashServer.props(port, stack, statisticDAO, dbWritersNum), "BigCashServer")

}
