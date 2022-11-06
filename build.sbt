ThisBuild / scalaVersion := "3.2.0"

lazy val binariesMatrix = Map(
  "ubuntu-latest" -> "native-cli-linux-x84_64",
  "macos-latest"  -> "native-cli-macos-x86_64"
)

ThisBuild / githubWorkflowOSes           := binariesMatrix.keys.toSeq
ThisBuild / githubWorkflowTargetBranches := Seq("main")
ThisBuild / githubWorkflowTargetTags    ++= Seq("v*")
ThisBuild / githubWorkflowPublish        := Nil

ThisBuild / githubWorkflowBuildPostamble ++= {
  val tagsOnly = "startsWith(github.ref, 'refs/tags/v')"

  val generate = WorkflowStep.Sbt(
    List("generateNativeBinary"),
    name = Some("Generate native binary"),
    cond = Some(tagsOnly)
  )

  val uploads = binariesMatrix.flatMap { case (os, binaryName) =>
    val condition = s"$tagsOnly && matrix.os == '$os'"
    Seq(
      WorkflowStep.Run(
        List(s"mv native-cli $binaryName"),
	name = Some(s"Rename $os binary"),
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

  generate +: uploads.toSeq
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
  val generateNativeBinary = taskKey[Unit]("Generate native binary")

  Seq(
    generateNativeBinary := {
      val binary = (cli.native / Compile / nativeLink).value
      val output = file("./native-cli")

      IO.delete(output)
      IO.copyFile(binary, output)
    }
  )
}

