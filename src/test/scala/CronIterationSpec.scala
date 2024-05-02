import java.time.ZonedDateTime
import CronParser.parseCronString

class CronIterationSpec extends munit.FunSuite {
  // test("finds latest occurrence on start of previous minute") {
  //   val now = ZonedDateTime.parse("2024-04-29T08:59:57Z")
  //   val cron = parseCronString("* * * * * echo")

  //   val occurrence = CronIteration.latestOccurrence(cron.get, now)

  //   assertEquals(occurrence, ZonedDateTime.parse("2024-04-29T08:59:00Z"))
  // }

  test("find next matching month") {
    val now = ZonedDateTime.parse("2024-04-29T08:59:57Z")
    val cron = parseCronString("0 0 1 * * echo")

    val occurrence = CronIteration.latestOccurrence(cron.get, now)

    assertEquals(occurrence, ZonedDateTime.parse("2024-04-01T00:00:00Z"))
  }

  test("find next matching weekday") {
    val now = ZonedDateTime.parse("2024-04-29T08:59:57Z")
    val cron = parseCronString("0 0 * * SAT echo")

    val occurrence = CronIteration.latestOccurrence(cron.get, now)

    assertEquals(occurrence, ZonedDateTime.parse("2024-04-27T00:00:00Z"))
  }

  test("combined day of month and weekday") {
    val now = ZonedDateTime.parse("2024-04-29T08:59:57Z")
    val cron = parseCronString("0 0 9 * SAT echo")

    val occurrence = CronIteration.latestOccurrence(cron.get, now)

    assertEquals(occurrence, ZonedDateTime.parse("2024-03-09T00:00:00Z"))
  }

  test("combined day of month and weekday (2)") {
    val now = ZonedDateTime.parse("2024-02-29T08:59:57Z")
    val cron = parseCronString("0 0 9 * SAT echo")

    val occurrence = CronIteration.latestOccurrence(cron.get, now)

    assertEquals(occurrence, ZonedDateTime.parse("2023-12-09T00:00:00Z"))
  }

}
