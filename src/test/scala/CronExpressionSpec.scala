class CronExpressionSpec extends munit.FunSuite {

  test("parse range") {
    val parsed = SelectorParser().parseSelector("10-20")

    assertEquals(parsed, Some(CronSelector.RangeSelector(10, 20)))
  }

  test("parse list of values") {
    val parsed = SelectorParser().parseSelector("10,20,30")

    assertEquals(parsed, Some(CronSelector.AbsoluteSelector(List(10, 20, 30))))
  }

  test("parse interval") {
    val parsed = SelectorParser().parseSelector("*/10")

    assertEquals(parsed, Some(CronSelector.IntervalSelector(10)))
  }

  test("pase range interval") {
    val parsed = SelectorParser().parseSelector("10-30/10")

    assertEquals(parsed, Some(CronSelector.RangeIntervalSelector(10, 30, 10)))
  }

}
