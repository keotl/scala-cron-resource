import scala.io.StdIn.readLine
import scala.util.CommandLineParser
import play.api.libs.json.Json
import java.time.ZonedDateTime
import java.time.ZoneOffset

enum Command {
  case Check, In, Out
}

given CommandLineParser.FromString[Command] with
  def fromString(value: String): Command = Command.valueOf(value)

def inCommand(): Unit = {
  val args       = readLine()
  val parsedArgs = Json.parse(args)
  println(
    Json.stringify(
      Json.obj("version" -> parsedArgs("version"))
    )
  )
}

def checkCommand(): Unit = {
  val args       = readLine()
  val parsedArgs = Json.parse(args)
  val cronString = (parsedArgs \ "source" \ "pattern").as[String]

  val result = CronParser.parseCronString(cronString) match {
    case Some(expression) =>
      Some(
        CronIteration
          .latestOccurrence(
            expression,
            ZonedDateTime.now(ZoneOffset.UTC)
          )
          .toString()
      )

    case None => None
  }

  println(
    Json.stringify(
      Json.arr(
        Json.obj(
          "ref" -> result
        )
      )
    )
  )
}

@main def hello(command: Command): Unit = {
  command match {
    case Command.Check => checkCommand()
    case Command.In    => inCommand()
    case _             => println("{\"version\": {\"ref\": \"\"}}")
  }
}
