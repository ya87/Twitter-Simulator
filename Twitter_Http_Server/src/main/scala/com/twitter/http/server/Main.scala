package com.twitter.http.server

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import spray.can.Http
import akka.io.IO
import com.typesafe.config.ConfigFactory
import java.net.InetAddress

object Main extends App{
	val systemName: String = "HttpServerSystem"
    val config = ConfigFactory.load()

    var serverCustomConfig: String = null
    var hostAddress: String = null

    try {
      hostAddress = InetAddress.getLocalHost().getHostAddress()
      serverCustomConfig = """
akka {
  actor {
    provider = "akka.remote.RemoteActorRefProvider"
  }
  remote {
    netty.tcp {
      hostname = """" + hostAddress + """"
      port = 0
      
      send-buffer-size = 8192000b
      receive-buffer-size = 8192000b
      maximum-frame-size = 4096000b
    }
  }
  #extensions = ["kamon.metric.Metrics"]
}
"""
    } catch {
      case ex: Exception => {
        serverCustomConfig = null
      }
    }

    val serverConfig = config.getConfig(systemName)
    var system: ActorSystem = null
    if (null != serverCustomConfig) {
      val customConf = ConfigFactory.parseString(serverCustomConfig);
      system = ActorSystem(systemName, ConfigFactory.load(customConf).withFallback(serverConfig))
    } else {
      system = ActorSystem(systemName, serverConfig.withFallback(config))
    }

    val serverIP: String = args(0)
	val mainActor = system.actorOf(Props(classOf[MainActor], hostAddress, serverIP), name="mainActor")
	mainActor ! "start"
}

class MainActor(host: String, serverIP: String) extends Actor {
  def receive = {
    case "start" => {
      implicit val system = context.system
      
      val handler = context.actorOf(Props(classOf[TwitterService], serverIP), name="twitterService")
      IO(Http) ! Http.Bind(handler, interface=host, port=8080)
    }
  }
}