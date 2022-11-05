import cats.effect.{ExitCode, IO}
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp

object Main extends CommandIOApp(
  name = "native-cli",
  header = "Scala Native CLI"
) {

  def main: Opts[IO[ExitCode]] =
    Opts.subcommand("now", "Print the current time")(
      Opts(IO.realTimeInstant.flatMap(now => IO.println(s"Now is $now").as(ExitCode.Success)))
    )
}

