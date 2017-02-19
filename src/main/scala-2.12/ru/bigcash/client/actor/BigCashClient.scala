package ru.bigcash.client.actor

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString
import ru.bigcash.common.dto.{ClientMessage, ServerMessage}
import spray.json._

import scala.util.{Failure, Success, Try}

/**
  * @author Artem Vedernikov
  */

object BigCashClient {

  def props(serverAddress: InetSocketAddress, clientId: String, playerBehaviour: ServerMessage => Boolean): Props = {
    Props(classOf[BigCashClient], serverAddress, clientId, playerBehaviour)
  }
}

/**
  * Actor responsible for creating client tcp connection to BigCash game server
  *
  * @param serverAddress - address of game server
  * @param userId - id of user currently playing
  * @param playerBehaviour - bot logic to choose which pack to accept
  */
class BigCashClient(serverAddress: InetSocketAddress, userId: String, playerBehaviour: ServerMessage => Boolean) extends Actor with ActorLogging {

  import context.system
  import ru.bigcash.common.dto.ServerMessageJsonProtocol._
  import ru.bigcash.common.dto.ClientMessageJsonProtocol._

  IO(Tcp) ! Connect(serverAddress)

  def receive = {
    case CommandFailed(_: Connect) =>
      log.error("Connection failed")
      context stop self

    case Connected(remote, local) =>
      import ru.bigcash.common.dto.ClientMessageJsonProtocol._

      log.info("Connection successfully established")
      val connection = sender()

      connection ! Register(self)

      connection ! Write(ByteString(ClientMessage(userId, true, true).toJson.toString()))

      context become gameAwaiting(connection)
  }


  def gameAwaiting(connection: ActorRef): Receive = handleTcpErrors.orElse {
    case Received(data) =>
      withDecodedServerMessage(data) { serverMessage =>
        if (!serverMessage.isError) {
          serverMessage.stack match {
            case Some(stackInfo) =>
              log.info(s"BigCash game with stack $stackInfo is ready to start. Accept?")
              val accept = playerBehaviour(serverMessage)
              log.debug(s"Will accept: $accept")
              connection ! Write(ByteString(ClientMessage(userId, accept).toJson.toString()))
              context become gameInProgress(connection)

            case None =>
              log.error("Invalid message from Server - should have stack info. Aborting")
              context stop self
          }
        } else {
          log.error("Received error message from server. Aborting")
          context stop self
        }
      }
  }

  def gameInProgress(connection: ActorRef): Receive = handleTcpErrors.orElse {
    case Received(data) =>
      withDecodedServerMessage(data) { serverMessage =>
        if (!serverMessage.isError) {
          serverMessage.moneyWon match {
            case Some(moneyWon) =>
              log.info(s"Game finished, money won $moneyWon. ")
              context stop self

            case None =>
              serverMessage.currentOffer match {
                case Some(currentOffer) =>
                  log.info(s"Got offer $currentOffer")
                  val accept = playerBehaviour(serverMessage)
                  log.debug(s"Will accept: $accept")

                  connection ! Write(ByteString(ClientMessage(userId, accept).toJson.toString()))

                case None =>
                  log.error("Incorrect server message: should have currentOffer while game in progress. Aborting")
                  context stop self
              }
          }
        } else {
          log.error("Received error message from server. Aborting")
          context stop self
        }
      }
  }

  private def withDecodedServerMessage(data: ByteString)(process: ServerMessage => Any): Unit = {
    Try(data.decodeString("UTF-8").parseJson.convertTo[ServerMessage]) match {
      case Success(serverMessage) =>
        log.debug(s"Got message $serverMessage")
        process(serverMessage)
      case Failure(ex) =>
        log.error("Exception while decoding message from server. Aborting", ex)
        context stop self
    }
  }

  private def handleTcpErrors: Receive = {
    case CommandFailed(w: Write) =>
      log.error("Failed to write to TCP. Aborting")
      context stop self

    case _: ConnectionClosed =>
      log.error("Connection closed by server.")
      context stop self
  }
}
