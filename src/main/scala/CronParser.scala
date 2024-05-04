object CronParser {
  private val parser = SelectorParser()

  def parseCronString(cron: String): Option[CronExpression] = {
    cron.split(" ") match {
      case Array(a, b, c, d, e) => {
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
                dayOfWeek
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
