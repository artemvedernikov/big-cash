package ru.bigcash.server.actor

import akka.actor.{Actor, ActorLogging, Props}
import ru.bigcash.server.dao.StatisticDAO

import scala.util.control.NonFatal

/**
  * @author Artem Vedernikov
  */
object MongoWriter {
  def props(dao: StatisticDAO): Props = {
    Props(classOf[MongoWriter], dao)
  }
}

/**
  * Updates user statistics in MongoDB
  * @param dao - Dato Access Object for statistics collection in DB
  */
class MongoWriter(dao: StatisticDAO) extends Actor with ActorLogging {

  private val MaxRetries = 1

  def receive = {
    case WriteStatistic(userId, moneyWon) =>
      if (updateStatisticsWithRetry(userId, moneyWon, MaxRetries)) {
        sender ! StatisticsWriteSuccess(userId, moneyWon)
      } else {
        log.error(s"Unable to update statistics for user $userId money won $moneyWon. Out of retries")
        sender ! StatisticsWriteError(userId, moneyWon)
      }

    case x =>
      log.error(s"Unknown message $x")
  }

  private def updateStatisticsWithRetry(userId: String, moneyWon: Int, maxRetries: Int): Boolean = {

    def updateStatistics(userId: String, moneyWon: Int, retriesLeft: Int): Boolean = {
      log.debug(s"Updating statistics for user $userId money won $moneyWon. Retries left $retriesLeft")
      try {
        val docsUpdated = dao.updateStatistic(userId, moneyWon).getN
        if (docsUpdated > 0) {
          log.debug(s"Successfully Updated statistics for user $userId with moneyWon $moneyWon")
          true
        } else {
          log.error(s"Exception while saving statistics for user $userId, money won $moneyWon - no Docs updated")
          if (retriesLeft > 0) {
            updateStatistics(userId, moneyWon, retriesLeft - 1)
          } else {
            false
          }
        }
      } catch {
        case NonFatal(ex) =>
          log.error(s"Exception while saving statistics for user $userId, money won $moneyWon", ex)
          if (retriesLeft > 0) {
            updateStatistics(userId, moneyWon, retriesLeft - 1)
          } else {
            false
          }
      }
    }

    updateStatistics(userId, moneyWon, maxRetries)
  }

}

case class WriteStatistic(userId: String, moneyWon: Int)
case class StatisticsWriteSuccess(userId: String, moneyWon: Int)
case class StatisticsWriteError(userId: String, moneyWon: Int)