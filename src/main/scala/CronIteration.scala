import java.time.ZonedDateTime
import java.time.YearMonth
object CronIteration {

  def latestOccurrence(
      expression: CronExpression,
      now: ZonedDateTime
  ): ZonedDateTime = {
    val currentMonth = now.getMonthValue()
    val currentYear  = now.getYear()
    val currentDay   = now.getDayOfMonth()

    val nextDay = findNextDay(
      expression,
      DayDescriptor(MonthDescriptor(currentYear, currentMonth), currentDay)
    )

    ZonedDateTime.of(
      nextDay.month.year,
      nextDay.month.month,
      nextDay.day,
      0,
      0,
      0,
      0,
      now.getZone()
    )
  }

  private def findNextMonth(
      expression: CronExpression,
      currentMonth: MonthDescriptor
  ): MonthDescriptor = {
    val nextMonth = latestWithinRange(expression.month, currentMonth.month)
    val nextYear = if (nextMonth > currentMonth.month) { currentMonth.year - 1 }
    else { currentMonth.year }
    MonthDescriptor(nextYear, nextMonth)
  }

  private def findNextDay(
      expression: CronExpression,
      currentDay: DayDescriptor
  ): DayDescriptor = {
    val nextMonth = findNextMonth(expression, currentDay.month)

    val firstCandidateDay = if (nextMonth == currentDay.month) {
      currentDay.day
    } else { lastDayOfMonth(nextMonth) }

    val nextDay = latestWithinRange(expression.dayOfMonth, firstCandidateDay)

    DayDescriptor(nextMonth, nextDay)
  }

  private def monthIterator(
      expression: CronExpression,
      now: ZonedDateTime
  ): Iterator[MonthDescriptor] = {
    val currentMonth = now.getMonthValue()
    val currentYear  = now.getYear()

    Iterator
      .from(0)
      .scanLeft(MonthDescriptor(currentYear, currentMonth))((acc, e) => {
        val MonthDescriptor(year, month) = acc
        val nextValidMonth = latestWithinRange(expression.month, month)
        val nextValidYear = if (nextValidMonth > month) { year - 1 }
        else { year }
        MonthDescriptor(nextValidYear, nextValidMonth)
      })
  }

  private def dayIterator(
      expression: CronExpression,
      month: MonthDescriptor,
      day: Int
  ): Iterator[DayDescriptor] = {
    // latestWithinRange(expression.dayOfMonth, day)

    Iterator
      .range(day, 0, -1)
      .scanLeft(DayDescriptor(month, day))((acc, e) => {
        val nextDay = latestWithinRange(expression.dayOfMonth, day)

        DayDescriptor(acc.month, nextDay)
      })
  }

  private def latestWithinRange(selector: CronSelector, value: Int): Int = {
    selector match {
      case CronSelector.AbsoluteSelector(values) => {
        values.findLast(_ <= value).getOrElse(-1)
      }
      case CronSelector.IntervalSelector(period) => {
        (value / period) * period
      }
      case CronSelector.RangeSelector(rangeStart, rangeEnd) => {
        if (value >= rangeStart && value <= rangeEnd) {
          return value
        } else {
          return rangeEnd
        }
      }
      case CronSelector.RangeIntervalSelector(rangeStart, rangeEnd, period) => {
        if (value >= rangeStart && value <= rangeEnd) {
          return (value / period) * period
        } else {
          return (rangeEnd / period) * period
        }
      }
      case CronSelector.AnySelector() => value

      case _ => throw new Error()
    }
  }

  private def lastDayOfMonth(month: MonthDescriptor): Int = {
    YearMonth.of(month.year, month.month).lengthOfMonth()
  }

  private case class ValidRange(
      rangeStart: ZonedDateTime,
      rangeEnd: ZonedDateTime
  )
  private case class MonthDescriptor(year: Int, month: Int) {
    def prev(): MonthDescriptor = {
      if (month > 1) {
        MonthDescriptor(year, month - 1)
      } else {
        MonthDescriptor(year - 1, 12)
      }
    }
  }
  private case class DayDescriptor(month: MonthDescriptor, day: Int)
}
