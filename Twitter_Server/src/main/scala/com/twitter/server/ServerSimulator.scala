package com.twitter.server

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import java.net.InetAddress
import akka.routing.SmallestMailboxPool

/**
 * Run as: sbt run
 */
object ServerSimulator {
  def main(args: Array[String]) {

    val serverSystemName: String = "ServerSystem"
    val config = ConfigFactory.load()

    var serverCustomConfig: String = null

    try {
      val hostAddress: String = InetAddress.getLocalHost().getHostAddress()
      serverCustomConfig = """
akka {
  actor {
    provider = "akka.remote.RemoteActorRefProvider"
  }
  remote {
    netty.tcp {
      hostname = """" + hostAddress + """"
      port = 2551
      
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

    val serverConfig = config.getConfig(serverSystemName)
    var system: ActorSystem = null
    if (null != serverCustomConfig) {
      val customConf = ConfigFactory.parseString(serverCustomConfig);
      system = ActorSystem(serverSystemName, ConfigFactory.load(customConf).withFallback(serverConfig))
    } else {
      system = ActorSystem(serverSystemName, serverConfig.withFallback(config))
    }

    var numServers: Int = (Runtime.getRuntime().availableProcessors() * serverConfig.getDouble("NumOfServersFactor")).toInt
    println("Number of servers: " + numServers)
    
    var serverController = system.actorOf(Props(classOf[ServerController], numServers), name = "server_controller")
    //println("serverController: " + serverController)
  }
}