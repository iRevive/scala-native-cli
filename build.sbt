ThisBuild / scalaVersion := "3.2.0"

ThisBuild / githubWorkflowOSes           := Seq("ubuntu-latest", "macos-latest")
ThisBuild / githubWorkflowTargetBranches := Seq("main")
ThisBuild / githubWorkflowTargetTags    ++= Seq("v*")

ThisBuild / githubWorkflowBuildPostamble ++= Seq(
  WorkflowStep.Sbt(
    List("cliNative/generateNativeBinary"),
    name = Some("Generate native binary"),
    cond = Some("startsWith(github.ref, 'refs/tags/v')")
  ),
  WorkflowStep.Run(
    List("mv native-cli native-cli-linux-x86_64"),
    name = Some("Rename Linux binary"),
    cond = Some("startsWith(github.ref, 'refs/tags/v') && matrix.os == 'ubuntu-latest'")
  ),
  WorkflowStep.Use(
    UseRef.Public("softprops", "action-gh-release", "v1"),
    name = Some("Release Linux"),
    params = Map("files" -> "./native-cli-linux-x86_64"),
    cond = Some("startsWith(github.ref, 'refs/tags/v') && matrix.os == 'ubuntu-latest'")
  ),
  WorkflowStep.Run(
    List("mv native-cli native-cli-macos-x86_64"),
    name = Some("Rename MacOS binary"),
    cond = Some("startsWith(github.ref, 'refs/tags/v') && matrix.os == 'macos-latest'")
  ),
  WorkflowStep.Use(
    UseRef.Public("softprops", "action-gh-release", "v1"),
    name = Some("Release MacOS"),
    params = Map("files" -> "./native-cli-macos-x86_64"),
    cond = Some("startsWith(github.ref, 'refs/tags/v') && matrix.os == 'macos-latest'")
  )
)

ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))


lazy val cli = crossProject(JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("."))
  .nativeSettings(generateBinarySettings)
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
      val binary = (Compile / nativeLink).value
      val output = file("./native-cli")

      IO.delete(output)
      IO.copyFile(binary, output)
    }
  )
}

