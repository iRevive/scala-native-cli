ThisBuild / scalaVersion := "3.2.0"

lazy val binariesMatrix = Map(
  "ubuntu-latest" -> "native-cli-linux-x84_64",
  "macos-latest"  -> "native-cli-macos-x86_64"
)

ThisBuild / githubWorkflowOSes                  := binariesMatrix.keys.toSeq
ThisBuild / githubWorkflowTargetBranches        := Seq("main")
ThisBuild / githubWorkflowTargetTags           ++= Seq("v*")
ThisBuild / githubWorkflowPublish               := Nil
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))


ThisBuild / githubWorkflowBuildPostamble ++=
  binariesMatrix.toSeq.flatMap { case (os, binaryName) =>
    val condition = s"startsWith(github.ref, 'refs/tags/v') && matrix.os == '$os'"
    Seq(
      WorkflowStep.Sbt(
        List(s"generateNativeBinary ./$binaryName"),
        name = Some(s"Generate $os native binary"),
        cond = Some(condition)
      ),
      WorkflowStep.Use(
        UseRef.Public("softprops", "action-gh-release", "v1"),
        name   = Some(s"Upload $binaryName"),
        params = Map("files" -> binaryName),
	cond   = Some(condition)
      )
    )
  }

lazy val root = project
  .in(file("."))
  .aggregate(cli.jvm, cli.native)
  .settings(name := "native-cli")
  .settings(generateBinarySettings)

lazy val cli = crossProject(JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("./modules/cli"))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect"    % "3.3.14",
      "com.monovore"  %%% "decline-effect" % "2.3.1"
    )
  )  

lazy val generateBinarySettings = {
  val generateNativeBinary = inputKey[Unit]("Generate native binary")

  Seq(
    generateNativeBinary := {
      val log    = streams.value.log
      val args   = sbt.complete.Parsers.spaceDelimited("<arg>").parsed
      val binary = (cli.native / Compile / nativeLink).value
      val output = file(args.headOption.getOrElse("./native-cli"))

      log.info(s"Writing binary to $output")
      IO.delete(output)
      IO.copyFile(binary, output)
    }
  )
}

