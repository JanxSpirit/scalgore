import sbt._

class ScalgoreProject (info: ProjectInfo) extends ParentProject(info) {
  lazy val irclogger = project("irclogger", "Scalgore Irc Logger")
  lazy val web = project("web", "Scalgore Web UI")
}
