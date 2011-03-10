/**
 * ScalGore simple #akka IRC Bot.
 *
 * Brendan W. McAdams <bwmcadams@evilmonkeylabs.com>
 */
package net.evilmonkeylabs.scalgore

import akka.actor._
import akka.util._
import com.mongodb.casbah.Imports._
import org.elasticsearch.client.transport._
import org.elasticsearch.common.transport._
import org.elasticsearch.client.Requests._
import org.elasticsearch.common.xcontent.XContentBuilder._
import org.elasticsearch.common.xcontent.XContentFactory._

trait IrcMessage {
  val sender: String
  val login: String
  val hostname: String
  val message: String
  val date = new java.util.Date
  // capture date as soon as object is created
}

case class IrcAction(val network: String,
                     val sender: String,
                     val login: String,
                     val hostname: String,
                     val message: String,
                     val target: String) extends IrcMessage

case class IrcPublicMessage(val network: String,
                            val channel: String,
                            val sender: String,
                            val login: String,
                            val hostname: String,
                            val message: String) extends IrcMessage

case class IrcPrivateMessage(val network: String,
                             val sender: String,
                             val login: String,
                             val hostname: String,
                             val message: String) extends IrcMessage

class IrcLogger extends Actor with Logging {
  val public_type = "publicMessage"
  val private_type = "privateMessage"
  val action_type = "action"

  val mongo = MongoConnection()("ircLogs")
  val es = new TransportClient().addTransportAddress(new InetSocketTransportAddress("localhost", 9300))

  def parseMessage(msg: String) = msg.split(" ")

  def fixChanName(network: String, name: String) = network ++ name.replaceAll("#", "HASH")

  def receive = {
    case pub@IrcPublicMessage(network, channel, sender, login, hostname, message) => {
      val name = fixChanName(network, channel)
      val kws = parseMessage(message)

      val idx = MongoDBObject("sender" -> 1, "keywords" -> 1)
      mongo(name).ensureIndex(idx)

      val obj = MongoDBObject(
        "type" -> public_type,
        "channel" -> channel,
        "sender" -> sender,
        "login" -> login,
        "hostname" -> hostname,
        "message" -> message,
        "keywords" -> kws,
        "date" -> pub.date)

      log.info(obj.toString)

      mongo(name) += (obj)

      // index in elasticsearch
      val source = jsonBuilder()
        .startObject()
        .field("type", public_type)
        .field("channel", channel)
        .field("sender", sender)
        .field("login", login)
        .field("hostname", hostname)
        .field("message", message)
        .field("date", pub.date)
        .startArray("keywords")
      kws.foreach(source.value(_))
      source.endArray.endObject

      val response = es.prepareIndex("irclogs", "message", obj.get("_id").toString)
        .setSource(source)
        .execute()
        .actionGet();
    }

    case act @ IrcAction(network, sender, login, hostname, message, target) => {
      val name = fixChanName(network, target)
      val kws = parseMessage(message)

      val obj = MongoDBObject(
        "type" -> action_type,
        "target" -> target,
        "sender" -> sender,
        "login" -> login,
        "hostname" -> hostname,
        "message" -> message,
        "keywords" -> kws,
        "date" -> act.date)

      val idx = MongoDBObject("sender" -> 1, "keywords" -> 1)
      mongo(name).ensureIndex(idx)

      log.info(obj.toString)

      mongo(name) += (obj)

      // index in elasticsearch
      val source = jsonBuilder()
        .startObject()
        .field("type", action_type)
        .field("target", target)
        .field("sender", sender)
        .field("login", login)
        .field("hostname", hostname)
        .field("message", message)
        .field("date", act.date)
        .startArray("keywords")
      kws.foreach(source.value(_))
      source.endArray.endObject

      val response = es.prepareIndex("irclogs", "message", obj.get("_id").toString)
        .setSource(source)
        .execute()
        .actionGet();
    }

    case priv@IrcPrivateMessage(network, sender, login, hostname, message) => {
      val obj = MongoDBObject("type" -> "privateMessage", "sender" -> sender, "login" -> login,
        "hostname" -> hostname, "message" -> message)
      //mongo("privateMessages") insert(obj)
    }

    case unknown => {}
  }
}

// vim: set ts=2 sw=2 sts=2 et:
