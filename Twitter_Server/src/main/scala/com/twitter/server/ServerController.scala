package com.twitter.server

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Actor
import com.twitter.common._
import akka.actor.ActorRef
import akka.routing.SmallestMailboxPool
import scala.collection.mutable.HashMap
import com.twitter.datastore._
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import akka.actor.Cancellable
import com.twitter.stats.StatsActor
import java.util.Calendar
import java.text.SimpleDateFormat
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._
import akka.actor.ActorKilledException
import akka.routing.RoundRobinPool
import scala.collection.concurrent.TrieMap

class ServerController(numServers: Int) extends Actor {
  var numClientsJoined = 0;
  var dataStores: TrieMap[Int, DataStore] = new TrieMap[Int, DataStore]()

  val maxUserTimelineTweets = context.system.settings.config.getInt("MaxUserTimelineTweets")
  val maxHomeTimelineTweets = context.system.settings.config.getInt("MaxHomeTimelineTweets")
  val statsInSec = context.system.settings.config.getInt("StatsInSec")
  val persistent: Boolean = context.system.settings.config.getBoolean("Persistent")
  val routingStrategy: String = context.system.settings.config.getString("RoutingStrategy")
  val plotGraph: Boolean = context.system.settings.config.getBoolean("PlotGraph")

  var router: ActorRef = null
  if (routingStrategy.equalsIgnoreCase("SmallestMailbox")) {
    router = context.actorOf(SmallestMailboxPool(numServers).props(Props(classOf[ServerActor], dataStores, persistent)), "router")
  } else {
    router = context.actorOf(RoundRobinPool(numServers).props(Props(classOf[ServerActor], dataStores, persistent)), "router")
  }
  println("Router: " + router)

  var statsActor: ActorRef = null
  import context.dispatcher
  val statsScheduler: Cancellable = context.system.scheduler.schedule(Duration(statsInSec, TimeUnit.SECONDS), Duration(statsInSec, TimeUnit.SECONDS), self, ReportStats)

  var startTimeMillis = System.currentTimeMillis()
  //var lastTweetCount = 0
  var firstTweetReceived = false

  def receive() = {
    /*case SendStats => {
      var x = System.currentTimeMillis() - startTimeMillis
      var currTweetCount = getCurrTweetCount()
      //println("currentTweetCount: "+currTweetCount)
      statsActor ! UpdateGraph(x, currTweetCount - lastTweetCount)
      lastTweetCount = currTweetCount
    }*/

    case FirstTweetReceived => {
      if (!firstTweetReceived) {
        firstTweetReceived = true
        startTimeMillis = System.currentTimeMillis()
        if (plotGraph) {
          statsActor = context.actorOf(Props(classOf[StatsActor], "Average Tweets/Sec"), name = "stats_actor")
        }
      }
    }

    case ReportStats => {
      var currTimeMillis = System.currentTimeMillis()
      var x = (currTimeMillis - startTimeMillis) / 1000
      if (x > 0) {
        var currTweetCount = getCurrTweetCount()
        var currUserCount = getCurrUserCount()
        var avgTweetCount = (currTweetCount / x).toInt
        println("(CurrTimeSec, Total Users, Total Tweets, TweetsPerSec): (" + currTimeMillis / 1000 + ", " + currUserCount + ", " + currTweetCount + ", " + avgTweetCount + ")")
        if(statsActor != null)
        	statsActor ! UpdateGraph(x, avgTweetCount)
        //lastTweetCount = currTweetCount
      }
    }

    case SendStats(peer) => {
      var currTimeMillis = System.currentTimeMillis()
      var x = (currTimeMillis - startTimeMillis) / 1000
      var stats: ServerStats = null
      var currTweetCount = getCurrTweetCount()
      var currUserCount = getCurrUserCount()
      if (x > 0) {
        stats = ServerStats(currUserCount, currTweetCount, (currTweetCount / x).toInt)
      } else {
        stats = ServerStats(currUserCount, currTweetCount, 0)
      }

      peer ! ResponseForServerStats(stats)
    }

    case SendClientId(peer) => {
      numClientsJoined += 1
      println("Client" + numClientsJoined + " joined !!")

      var ds = new DataStore(numClientsJoined, persistent)
      ds.setMaxUserTimelineTweets(maxUserTimelineTweets)
      ds.setMaxHomeTimelineTweets(maxHomeTimelineTweets)
      dataStores += ((numClientsJoined, ds))

      println("datastore created for client" + numClientsJoined)

      peer ! SetClientId(numClientsJoined)
    }

    case _ => {
      println("Unexpected message received by server controller !!!")
    }
  }

  def getCurrUserCount(): Int = {
    var currUserCount = 0
    dataStores.values.foreach(ds => currUserCount += ds.getNumUsers())

    return currUserCount
  }

  def getCurrTweetCount(): Int = {
    var currTweetCount = 0
    dataStores.values.foreach(ds => currTweetCount += ds.getNumTweets())

    return currTweetCount
  }
}