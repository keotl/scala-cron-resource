import java.time.ZonedDateTime
import CronParser.parseCronString

class CronIterationSpec extends munit.FunSuite {
  // test("finds latest occurrence on start of previous minute") {
  //   val now = ZonedDateTime.parse("2024-04-29T08:59:57Z")
  //   val cron = parseCronString("* * * * * echo")

  //   val occurrence = CronIteration.latestOccurrence(cron.get, now)

  //   assertEquals(occurrence, ZonedDateTime.parse("2024-04-29T08:59:00Z"))
  // }

  test("month iterator") {
    val now = ZonedDateTime.parse("2024-04-29T08:59:57Z")
    val cron = parseCronString("0 0 1 * * echo")

    val occurrence = CronIteration.latestOccurrence(cron.get, now)

    assertEquals(occurrence, ZonedDateTime.parse("2024-04-01T00:00:00Z"))
  }
}
