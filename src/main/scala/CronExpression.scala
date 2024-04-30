enum CronSelector {
  case AbsoluteSelector(values: List[Int])
  case RangeSelector(rangeStart: Int, rangeEnd: Int)
  case RangeIntervalSelector(rangeStart: Int, rangeEnd: Int, period: Int)
  case IntervalSelector(period: Int)
}

case class CronExpression(
    minute: CronSelector,
    hour: CronSelector,
    dayOfMonth: CronSelector,
    month: CronSelector,
    dayOfWeek: CronSelector,
    command: String
)

class SelectorParser {
  import CronSelector._

  private val rangePattern         = "^(\\w+)-(\\w+)$".r
  private val absolutePattern      = "^(\\w+,)*(\\w+)$".r
  private val intervalPattern      = "^\\*/([0-9]+)$".r
  private val rangeIntervalPattern = "^(\\w+)-(\\w+)/([0-9]+)$".r

  private val numberPattern = "[0-9]+".r

  def parseSelector(
      text: String,
      allowedNames: Map[String, Int] = NONE
  ): Option[CronSelector] = {

    text match {
      case rangePattern(rangeStart: String, rangeEnd: String) => {
        (
          parseNamed(rangeStart, allowedNames),
          parseNamed(rangeEnd, allowedNames)
        ) match {
          case (Some(a), Some(b)) => Some(RangeSelector(a, b))
          case _                  => None
        }
      }

      case intervalPattern(period: String) => {
        Some(IntervalSelector(period.toInt))
      }

      case rangeIntervalPattern(
            rangeStart: String,
            rangeEnd: String,
            period: String
          ) => {
        (
          parseNamed(rangeStart, allowedNames),
          parseNamed(rangeEnd, allowedNames)
        ) match {
          case (Some(a), Some(b)) =>
            Some(RangeIntervalSelector(a, b, period.toInt))
          case _ => None
        }
      }

      case absolutePattern(args @ _*) => {
        val elements = text.split(",")
        val parsed   = elements.map(x => parseNamed(x, allowedNames))

        if (parsed.contains(Option.empty)) {
          return None
        } else {
          return Some(AbsoluteSelector(parsed.flatten[Int].toList))
        }
      }

      case _ => None
    }

  }

  private def parseNamed(
      text: String,
      nameMapping: Map[String, Int]
  ): Option[Int] = {
    text match {
      case x if numberPattern.matches(x) => Some(text.toInt)
      case x                             => nameMapping.get(text)
    }
  }

  val NONE: Map[String, Int] = Map()

  val WEEKDAYS = Map(
    ("sun", 0),
    ("mon", 1),
    ("tue", 2),
    ("wed", 3),
    ("thu", 4),
    ("fri", 5),
    ("sat", 6)
  )
  val MONTHS = Map(
    ("jan", 1),
    ("feb", 2),
    ("mar", 3),
    ("apr", 4),
    ("may", 5),
    ("jun", 6),
    ("jul", 7),
    ("aug", 8),
    ("sep", 9),
    ("oct", 10),
    ("nov", 11),
    ("dec", 12)
  )

}
