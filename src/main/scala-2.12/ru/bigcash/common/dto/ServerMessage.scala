package ru.bigcash.common.dto

import spray.json.DefaultJsonProtocol

/**
  * @author Artem Vedernikov
  */
case class ServerMessage(stack: Option[List[Int]] = None,
                         currentOffer: Option[Offer] = None,
                         moneyWon: Option[Int] = None,
                         isError: Boolean = false)

case class Offer(banknotes: List[Int],
                 offerNumber: Int)

object ServerMessageJsonProtocol extends DefaultJsonProtocol {
  implicit val offerJsonProtocol = jsonFormat2(Offer.apply)
  implicit val serverMessageJsonProtocol = jsonFormat4(ServerMessage.apply)
}