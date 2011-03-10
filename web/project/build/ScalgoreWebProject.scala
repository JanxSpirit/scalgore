import sbt._

class ScalgoreWebProject(info: ProjectInfo) extends DefaultWebProject(info) {

  val scalatra = "org.scalatra" %% "scalatra" % "2.0.0.M3"

  // jetty
  val jetty6 = "org.mortbay.jetty" % "jetty" % "6.1.22" % "test"
  val servletApi = "org.mortbay.jetty" % "servlet-api" % "2.5-20081211"
}