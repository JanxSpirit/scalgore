package net.evilmonkeylabs.scalgore.web

import org.scalatra._

class ScalgoreServlet extends ScalatraServlet {

  get("/") {
    <h1>Up and running</h1>
  }

}