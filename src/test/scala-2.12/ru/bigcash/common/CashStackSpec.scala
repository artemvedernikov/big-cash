package ru.bigcash.common

import org.scalatest.{FunSuite, Matchers}

/**
  * @author Artem Vedernikov
  */
class CashStackSpec extends FunSuite with Matchers {

  test("Exception should be thrown if trying to create CashStack with empty banknotes") {
    intercept[AssertionError] {
      new CashStack(List.empty)
    }
  }

}
