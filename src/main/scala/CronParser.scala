object CronParser {
  private val parser = SelectorParser()

  def parseCronString(cron: String): Option[CronExpression] = {
    print(cron.split(" ", 6).mkString("Array(", ", ", ")"))
    cron.split(" ", 6) match {
      case Array(a, b, c, d, e, command) => {
        (
          parser.parseSelector(a),
          parser.parseSelector(b),
          parser.parseSelector(c),
          parser.parseSelector(d, parser.MONTHS),
          parser.parseSelector(e, parser.WEEKDAYS)
        ) match {
          case (
                Some(minute),
                Some(hour),
                Some(dayOfMonth),
                Some(month),
                Some(dayOfWeek)
              ) => {
            Some(
              CronExpression(
                minute,
                hour,
                dayOfMonth,
                month,
                dayOfWeek,
                command
              )
            )
          }
          case _ => None
        }
      }
      case _ => None
    }
  }
}
