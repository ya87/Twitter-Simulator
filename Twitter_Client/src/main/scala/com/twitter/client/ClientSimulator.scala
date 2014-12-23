package com.twitter.client

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import java.net.InetAddress

/**
 * Run as: sbt "run <server-ip> <capacity>"
 */
object ClientSimulator {
  def main(args: Array[String]) {

    val clientSystemName: String = "ClientSystem"

    val config = ConfigFactory.load()
    var clientCustomConfig: String = null
    
    try {
      val hostAddress: String = InetAddress.getLocalHost().getHostAddress()
      clientCustomConfig = """
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
  #scheduler {
   # tick-duration = 10ms
    #ticks-per-wheel = 2048
  #}
  #extensions = ["kamon.metric.Metrics"]
}
"""
    } catch {
      case ex: Exception => {
        clientCustomConfig = null
      }
    }

    val clientConfig = config.getConfig(clientSystemName)
    var system: ActorSystem = null
    if (null != clientCustomConfig) {
      val customConf = ConfigFactory.parseString(clientCustomConfig);
      system = ActorSystem(clientSystemName, ConfigFactory.load(customConf).withFallback(clientConfig))
    } else {
      system = ActorSystem(clientSystemName, clientConfig.withFallback(config))
    }

    //Loading Simulation Data from config file
    SimulationVariables.loadSimulationVariables(clientConfig)
    
    val serverIP: String = args(0) //IP of HTTP Server
    val capacity: Double = args(1).toDouble
    var isRest: Boolean = false 
    if(args.length > 2){ isRest = true}
    var client = system.actorOf(Props(classOf[ClientController], serverIP, capacity, isRest), name = "client")
  }
}