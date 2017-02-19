package ru.bigcash.client

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import ru.bigcash.client.actor.BigCashClient
import ru.bigcash.common.dto.ServerMessage

import scala.util.Random

/**
  * @author Artem Vedernikov
  */
object BigCashClientRunner extends App {

  private val log = LoggerFactory.getLogger(getClass)

  val userId = args.headOption.getOrElse(System.currentTimeMillis().toString)

  val config = ConfigFactory.load("client.conf")
  val actorSystem = ActorSystem("ClientSystem", config)
  val serverHost = config.getString("bigcash.server.host")
  val serverPort = config.getInt("bigcash.server.port")

  log.info(s"Connecting to BigCash server $serverHost : $serverPort")
  val serverAddress = new InetSocketAddress(serverHost, serverPort)

  /**
    * Accepts every game, during the game chooses randomly
    */
  val randomBehaviour = (serverMessage: ServerMessage) =>
    serverMessage.stack.isDefined || Random.nextDouble() > 0.5

  actorSystem.actorOf(BigCashClient.props(serverAddress, userId, randomBehaviour))

}
