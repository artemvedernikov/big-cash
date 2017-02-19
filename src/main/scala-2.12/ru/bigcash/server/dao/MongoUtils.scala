package ru.bigcash.server.dao

import com.mongodb.ServerAddress
import com.mongodb.casbah.{MongoClient, MongoClientOptions, MongoCollection, MongoCredential}

/**
  * @author Artem Vedernikov
  */
object MongoUtils {

  val Comma = ","
  val Colon = ":"

  def getCollection(mongoUrl: String, db: String, collection: String, user: Option[String], password: Option[String]): MongoCollection = {
    val options = MongoClientOptions.Defaults
    val host = getHostsAddresses(mongoUrl)
    (user, password) match {
      case (Some(u), Some(p)) =>
        val credentials = MongoCredential.createMongoCRCredential(u, db, p.toCharArray)
        MongoClient(host, List(credentials), options)(db)(collection)
      case _ => MongoClient(host)(db)(collection)
    }
  }

  private def getHostsAddresses(hosts: String): List[ServerAddress] = {
    hosts.split(Comma).map({
      host =>
        val tokens = host.split(Colon)
        if (tokens.length == 2) {
          new ServerAddress(tokens(0), Integer.parseInt(tokens(1)))
        } else {
          new ServerAddress(host)
        }
    }).toList
  }

}
