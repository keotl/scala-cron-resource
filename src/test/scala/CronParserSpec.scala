class CronParserSpec extends munit.FunSuite {
  import CronSelector._

  test("parse simple cron string") {
    val parsed = CronParser.parseCronString("0 1 2 3 4 echo 'hello'")

    assertEquals(
      parsed,
      Some(
        CronExpression(
          AbsoluteSelector(List(0)),
          AbsoluteSelector(List(1)),
          AbsoluteSelector(List(2)),
          AbsoluteSelector(List(3)),
          AbsoluteSelector(List(4)),
          "echo 'hello'"
        )
      )
    )
  }

  test("parses named months") {
    val parsed = CronParser.parseCronString("* * * JAN-MAR SUN echo 'hello'")

    assertEquals(
      parsed,
      Some(
        CronExpression(
          AnySelector(),
          AnySelector(),
          AnySelector(),
          RangeSelector(1, 3),
          AbsoluteSelector(List(0)),
          "echo 'hello'"
        )
      )
    )
  }
}
