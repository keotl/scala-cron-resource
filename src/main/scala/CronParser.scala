import scala.util.matching.Regex

object CronParser {
  val pattern: Regex = "^([0-9/*]+) ([0-9/*]+) ([0-9/*]+) ([0-9/*]+) ([0-9/*]+) (.+)$".r

  def parseCronString(cron: String): Option[CronExpression] = {
    cron match {
      case pattern(a, b, c, d, e, f, g) => None
      case _ => None
    }
  }
}

