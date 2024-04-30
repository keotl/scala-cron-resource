import scala.io.StdIn.readLine
import scala.util.CommandLineParser

enum Command {
  case Check, In, Out
}

given CommandLineParser.FromString[Command] with
  def fromString(value: String): Command = Command.valueOf(value)

@main def hello(command: Command): Unit = {
  println(s"Running command ${command}.");
  val args = readLine()
  SelectorParser().parseSelector("a");
}
