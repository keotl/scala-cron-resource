import java.time.ZonedDateTime
import java.time.YearMonth
import java.util.{Calendar, GregorianCalendar}
import scala.annotation.tailrec
object CronIteration {

  def latestOccurrence(
      expression: CronExpression,
      now: ZonedDateTime
  ): ZonedDateTime = {
    val currentMonth      = now.getMonthValue()
    val currentYear       = now.getYear()
    val currentDayOfMonth = now.getDayOfMonth()
    val currentHour       = now.getHour()

    val date =
      HourDescriptor(
        DayDescriptor(
          MonthDescriptor(currentYear, currentMonth),
          currentDayOfMonth
        ),
        currentHour
      )

    val nextDay = findNextHour(expression, date)

    ZonedDateTime.of(
      nextDay.day.month.year,
      nextDay.day.month.month,
      nextDay.day.day,
      nextDay.hour,
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

  private def findNextDayCombined(
      expression: CronExpression,
      date: DayDescriptor
  ): DayDescriptor = {
    val dayCandidates: List[Option[DayDescriptor]] = List(
      if (expression.dayOfWeek != CronSelector.AnySelector()) {
        Some(findNextWeekday(expression, date))
      } else { None },
      if (expression.dayOfMonth != CronSelector.AnySelector()) {
        Some(findNextDayOfMonth(expression, date))
      } else {
        None
      }
    )

    return dayCandidates.flatten.maxOption.getOrElse(
      findNextDayOfMonth(expression, date)
    )
  }

  @tailrec private def findNextWeekday(
      expression: CronExpression,
      currentDay: DayDescriptor
  ): DayDescriptor = {
    val nextWeekday =
      latestWithinRange(expression.dayOfWeek, currentDay.dayOfWeek())
    if (nextWeekday == currentDay.dayOfWeek()) {
      currentDay
    } else {
      findNextWeekday(expression, currentDay.minus(currentDay.dayOfWeek() + 1))
    }
  }

  @tailrec private def findNextDayOfMonth(
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
      findNextDayOfMonth(expression, currentDay.minus(currentDay.day))
    }
  }

  @tailrec private def findNextHour(
      expression: CronExpression,
      currentHour: HourDescriptor
  ): HourDescriptor = {
    val nextDay = findNextDayCombined(expression, currentHour.day)
    val nextCandidateHour = if (nextDay == currentHour.day) { currentHour.hour }
    else { 23 }
    val nextHour = latestWithinRange(expression.hour, nextCandidateHour)
    if (nextHour >= 0) {
      HourDescriptor(nextDay, nextHour)
    } else {
      findNextHour(expression, currentHour.minus(currentHour.hour + 1))
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
  private case class MonthDescriptor(year: Int, month: Int)
      extends Ordered[MonthDescriptor] {
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

    def compare(that: MonthDescriptor): Int = {
      if (year < that.year) {
        -1
      } else if (year > that.year) {
        1
      } else if (month < that.month) {
        -1
      } else if (month > that.month) {
        1
      } else {
        0
      }
    }

  }
  private case class DayDescriptor(month: MonthDescriptor, day: Int)
      extends Ordered[DayDescriptor] {
    def dayOfWeek(): Int = {
      val cal = new GregorianCalendar()
      cal.set(month.year, month.month - 1, day, 0, 0, 0)
      cal.get(Calendar.DAY_OF_WEEK) - 1;
    }

    def prev(): DayDescriptor = {
      minus(1)
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

    def compare(that: DayDescriptor): Int = {
      if (month < that.month) {
        -1
      } else if (month > that.month) {
        1
      } else if (day < that.day) {
        -1
      } else if (day > that.day) {
        1
      } else {
        0
      }
    }
  }

  private case class HourDescriptor(day: DayDescriptor, hour: Int) {
    def minus(hours: Int): HourDescriptor = {
      val newHour = hour - hours
      if (newHour < 0) {
        val newDay = day.prev()
        HourDescriptor(newDay, 23)
      } else {
        HourDescriptor(day, newHour)
      }
    }
  };
  private case class MinuteDescriptor(hour: HourDescriptor, minute: Int);
}
