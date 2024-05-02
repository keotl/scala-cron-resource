import java.time.ZonedDateTime
import java.time.YearMonth
import java.util.{Calendar, GregorianCalendar}
import scala.annotation.tailrec
object CronIteration {

  def latestOccurrence(
      expression: CronExpression,
      now: ZonedDateTime
  ): ZonedDateTime = {
    val currentMonth = now.getMonthValue()
    val currentYear  = now.getYear()
    val currentDay   = now.getDayOfMonth()

    val nextDay = findNextWeekday(
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

  @tailrec private def findNextWeekday(
      expression: CronExpression,
      currentDay: DayDescriptor
  ): DayDescriptor = {
    val nextDay = findNextDay(expression, currentDay)
    val nextWeekday =
      latestWithinRange(expression.dayOfWeek, nextDay.dayOfWeek())
    if (nextWeekday == nextDay.dayOfWeek()) {
      nextDay
    } else {
      findNextWeekday(expression, nextDay.minus(currentDay.dayOfWeek() + 1))
    }
  }

  @tailrec private def findNextDay(
      expression: CronExpression,
      currentDay: DayDescriptor
  ): DayDescriptor = {
    val nextMonth = findNextMonth(expression, currentDay.month)

    val firstCandidateDay = if (nextMonth == currentDay.month) {
      currentDay.day
    } else { nextMonth.lastDay() }

    val nextDay = latestWithinRange(expression.dayOfMonth, firstCandidateDay)
    if (nextDay > 0) {
      DayDescriptor(nextMonth, nextDay)
    } else {
      findNextDay(expression, currentDay.minus(currentDay.day))
    }
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
  private case class MonthDescriptor(year: Int, month: Int) {
    def prev(): MonthDescriptor = {
      if (month > 1) {
        MonthDescriptor(year, month - 1)
      } else {
        MonthDescriptor(year - 1, 12)
      }
    }
    def lastDay(): Int = {
      YearMonth.of(year, month).lengthOfMonth()
    }
  }
  private case class DayDescriptor(month: MonthDescriptor, day: Int) {
    def dayOfWeek(): Int = {
      val cal = new GregorianCalendar()
      cal.set(month.year, month.month - 1, day, 0, 0, 0)
      cal.get(Calendar.DAY_OF_WEEK) - 1;
    }

    def minus(days: Int): DayDescriptor = {
      val newDay = day - days
      if (newDay <= 0) {
        val newMonth = month.prev()
        DayDescriptor(newMonth, newMonth.lastDay() + newDay)
      } else {
        DayDescriptor(month, newDay)
      }
    }
  }
}
