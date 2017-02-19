package ru.bigcash.common.dto

import spray.json.DefaultJsonProtocol

/**
  * @author Artem Vedernikov
  */
case class ClientMessage(userId: String,
                         accept: Boolean,
                         isInitialMessage: Boolean = false
                        )

object ClientMessageJsonProtocol extends DefaultJsonProtocol {
  implicit val clientMessageJsonProtocol = jsonFormat3(ClientMessage.apply)
}
