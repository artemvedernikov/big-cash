package ru.bigcash.common

import scala.util.Random

/**
  * @author Artem Vedernikov
  */
class CashStack(val banknotes: List[Int]) {

  assert(banknotes.nonEmpty)

  val MaxPackSize = 3

  def generateOffer: List[Int] = {
    val banknotesInOffer = Random.nextInt(MaxPackSize) + 1
    Random.shuffle(banknotes).take(banknotesInOffer)
  }
}

