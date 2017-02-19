package ru.bigcash.server.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp
import akka.io.Tcp.{CommandFailed, ConnectionClosed, Received, Write}
import akka.util.ByteString
import ru.bigcash.common.CashStack
import ru.bigcash.common.dto.{ClientMessage, Offer, ServerMessage}
import spray.json._

import scala.util.{Failure, Success, Try}

/**
  * @author Artem Vedernikov
  */

case class Send(data: ByteString)

object BigCashSession {
  def props(connect: ActorRef, cashStack: CashStack, dbWriters: ActorRef): Props = {
    Props(classOf[BigCashSession], connect, cashStack, dbWriters)
  }
}

/**
  * Actor representing game session serving for one user
  * @param connection - tcp connection
  * @param cashStack - cash stack for current game session
  * @param dbWriters - actors responsible for statistics storing
  */
class BigCashSession(connection: ActorRef, cashStack: CashStack, dbWriters: ActorRef) extends Actor with ActorLogging {

  context watch connection

  import ru.bigcash.common.dto.ClientMessageJsonProtocol._
  import ru.bigcash.common.dto.ServerMessageJsonProtocol._

  private var offerNumber = 1
  private var currentOffer: List[Int] = cashStack.generateOffer

  private val MaxTries = 3

  override def receive: Receive = handleTcpErrors.orElse {
    case Received(data) =>
      withDecodedClientMessage(data) { clientMessage =>
        if (clientMessage.isInitialMessage) {
          log.info(s"Received client connection. Sending cash stack info to user ${clientMessage.userId}")
          val cashStackInfo = ServerMessage(Some(cashStack.banknotes))
          connection ! Write(ByteString(cashStackInfo.toJson.toString()))
          context become gameAwaiting

        } else {
          log.debug("Invalid message from client: initial message required. Aborting")
          context stop self
        }
      }
  }

  def gameAwaiting: Receive = handleTcpErrors.orElse {
    case Received(data) =>
      withDecodedClientMessage(data) { clientMessage =>
        if (clientMessage.accept) {
          log.info(s"Client ${clientMessage.userId} accepted BigCash game!")
          val firstOffer = ServerMessage(currentOffer = Some(Offer(currentOffer, offerNumber)))
          connection ! Write(ByteString(firstOffer.toJson.toString()))
          context become gameInProgress

        } else {
          log.info(s"Client ${clientMessage.userId} declined BigCash game. Stopping")
          context stop self
        }
      }
  }

  def gameInProgress: Receive = handleTcpErrors.orElse {
    case Received(data) =>
      withDecodedClientMessage(data) { clientMessage =>
        if (clientMessage.accept) {
          log.info(s"Client ${clientMessage.userId} accepts offer $currentOffer")
          dbWriters ! WriteStatistic(clientMessage.userId, currentOffer.sum)
          context become awaitDbWriterResponse

        } else if (offerNumber < MaxTries) {
          log.info(s"Client declined offer $offerNumber, sending next offer.")
          offerNumber += 1
          currentOffer = cashStack.generateOffer
          val offer = ServerMessage(currentOffer = Some(Offer(currentOffer, offerNumber)))
          connection ! Write(ByteString(offer.toJson.toString()))

        } else {
          log.info(s"Client declined $MaxTries offers, Using ${MaxTries + 1} offer")
          offerNumber += 1
          currentOffer = cashStack.generateOffer
          dbWriters ! WriteStatistic(clientMessage.userId, currentOffer.sum)
          context become awaitDbWriterResponse
        }
      }
  }

  def awaitDbWriterResponse: Receive = handleTcpErrors.orElse {
    case StatisticsWriteSuccess(userId, moneyWon) =>
      log.info("Received statistics message from db writer")
      val response = ServerMessage(currentOffer = Some(Offer(currentOffer, offerNumber)),
        moneyWon = Some(moneyWon))

      connection ! Write(ByteString(response.toJson.toString()))

    case StatisticsWriteError(userId, moneyWon) =>
      log.error("Error occurred when writing to database")
      val response = ServerMessage(isError = true)

      connection ! Write(ByteString(response.toJson.toString()))
  }

  private def withDecodedClientMessage(data: ByteString)(process: ClientMessage => Any): Unit = {
    Try(data.decodeString("UTF-8").parseJson.convertTo[ClientMessage]) match {
      case Success(clientMessage) =>
        log.debug(s"Got message $clientMessage")
        process(clientMessage)
      case Failure(ex) =>
        log.error("Exception while decoding message from client. Aborting", ex)
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
