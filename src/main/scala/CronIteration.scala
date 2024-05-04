import java.time.ZonedDateTime
import java.time.YearMonth
import java.util.{Calendar, GregorianCalendar}
import scala.annotation.tailrec
object CronIteration {

  def latestOccurrence(
      expression: CronExpression,
      now: ZonedDateTime
  ): ZonedDateTime = {
    val date =
      MinuteDescriptor(
        HourDescriptor(
          DayDescriptor(
            MonthDescriptor(now.getYear(), now.getMonthValue()),
            now.getDayOfMonth()
          ),
          now.getHour()
        ),
        now.getMinute()
      )

    val nextDay = findNextMinute(expression, date)

    ZonedDateTime.of(
      nextDay.hour.day.month.year,
      nextDay.hour.day.month.month,
      nextDay.hour.day.day,
      nextDay.hour.hour,
      nextDay.minute,
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

    val canSelectCurrentWeek = expression.dayOfWeek match {
      case CronSelector.WeekdayOrdinalSelector(dayOfWeek, weekNumber) =>
        currentDay.day <= weekNumber * 7 && currentDay.day >= (weekNumber - 1) * 7
      case _ => true
    }

    if (nextWeekday == currentDay.dayOfWeek() && canSelectCurrentWeek) {
      currentDay
    } else {
      findNextWeekday(
        expression,
        currentDay.minus(currentDay.dayOfWeek().max(1))
      )
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

  @tailrec private def findNextMinute(
      expression: CronExpression,
      currentMinute: MinuteDescriptor
  ): MinuteDescriptor = {
    val nextHour = findNextHour(expression, currentMinute.hour)
    val nextCandidateMinute = if (nextHour == currentMinute.hour) {
      currentMinute.minute
    } else { 59 }
    val nextMinute = latestWithinRange(expression.minute, nextCandidateMinute)
    if (nextMinute >= 0) {
      MinuteDescriptor(nextHour, nextMinute)
    } else {
      findNextMinute(expression, currentMinute.minus(currentMinute.minute + 1))
    }
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
      case CronSelector.WeekdayOrdinalSelector(dayOfWeek, weekNumber) => {
        // We have to check the weekNumber separately
        if (dayOfWeek <= value) {
          dayOfWeek
        } else {
          -1
        }
      }
      case CronSelector.AnySelector() => value
    }
  }

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
  private case class MinuteDescriptor(hour: HourDescriptor, minute: Int) {
    def minus(minutes: Int): MinuteDescriptor = {
      val newMinute = minute - minutes
      if (newMinute < 0) {
        val newHour = hour.minus(1)
        MinuteDescriptor(newHour, newMinute)
      } else {
        MinuteDescriptor(hour, newMinute)
      }
    }
  };
}
