package ru.bigcash.server.dao

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.Imports._
import org.slf4j.LoggerFactory

/**
  * @author Artem Vedernikov
  */
object StatisticDAO {
  private val log = LoggerFactory.getLogger(getClass)
  val dbName = "big_cash"
  val collectionName = "statistic"

  def apply(mongoHosts: String,
            dbName: String = dbName,
            collectionName: String = collectionName,
            user: Option[String] = None,
            password: Option[String] = None): StatisticDAO = {
    val mongoCollection = MongoUtils.getCollection(mongoHosts, dbName, collectionName, user, password)
    new StatisticDAO(mongoCollection)
  }
}

class StatisticDAO(mongoCollection: MongoCollection) {

  import StatisticDAO._

  def updateStatistic(userId: String, moneyWon: Int): WriteResult = {
    log.info(s"Incrementing user $userId budget with $moneyWon")
    mongoCollection.update(MongoDBObject("_id" -> userId), $inc("money" -> moneyWon, "games" -> 1), upsert = true)
  }

}
