import java.time.ZonedDateTime
object CronIteration {

  def latestOccurrence(
      expression: CronExpression,
      now: ZonedDateTime
  ): ZonedDateTime = {
    val nextMonth = monthIterator(expression, now).next()

    now
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

  private case class ValidRange(
      rangeStart: ZonedDateTime,
      rangeEnd: ZonedDateTime
  )
  private case class MonthDescriptor(year: Int, month: Int)
  private case class DayDescriptor(month: MonthDescriptor, day: Int)
}
