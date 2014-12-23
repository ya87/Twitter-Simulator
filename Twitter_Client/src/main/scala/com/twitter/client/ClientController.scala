package com.twitter.client

import akka.actor.Actor
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import com.twitter.common._
import akka.actor.ActorRef
import scala.util.Random
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import akka.actor.Cancellable
import akka.actor.ActorSelection
import akka.actor.Identify
import akka.actor.ActorIdentity
import akka.util.Timeout

class ClientController(serverIP: String, capacity: Double, isRest: Boolean) extends Actor {
  import context.dispatcher

  var id: Int = 0
  var numUsers: Int = (capacity * SimulationVariables.numTotalUsers / 100).toInt

  //var numUserActors: Int = (Runtime.getRuntime().availableProcessors() * SimulationVariables.numUserActorsFactor).toInt
  var numUserActors: Int = ((SimulationVariables.perUserActors * numUsers)/100).toInt
  println("Number of user actors: " + numUserActors)
  val numTweetsPerActor: Int = SimulationVariables.numTweetsPerActor
  val tweetsInSec: Int = SimulationVariables.tweetsInSec
  var users: Array[ActorRef] = new Array[ActorRef](numUserActors)

  var batchNum = 0
  var runningTotal = 0
  var batchSize = 1000
  var nextBatchAfter = 0.8 //send next batch after response received for this % of batch size
  var IsUserSimulationComplete = false
  var numActiveUsers: Int = 0

  var numUsersWithFollowers: Int = 0
  var followersSimulatedForNumUsers: Int = 0

  var router: ActorRef = null
  var routerSel: ActorSelection = null

  /*var tweetsStatsScheduler: Cancellable = null
  var numUsersReportedTweets = 0
  var totalTweets = 0*/

  if (isRest) {
    router = context.actorOf(Props(classOf[com.twitter.client.wrapper.CientWrapperActor], serverIP), name = "clientWrapper")
    println("Router: " + router)
    router ! GetClientId
  } else {
    implicit def timeout = Timeout(5 seconds)
    val routerPath = "akka.tcp://ServerSystem@" + serverIP + ":2551/user/server_controller/router"
    val future = context.actorSelection(routerPath).resolveOne.mapTo[ActorRef]
    future onSuccess {
      case ref =>
        router = ref
        println("Router: " + router)
        router ! GetClientId
    }
  }

  def receive() = {
    case ActorIdentity => {
      router = sender
      println("Router: " + router)
      router ! GetClientId
    }

    case SetClientId(cId) => {
      id = cId
      println("ClientId set to " + id)

      println("Simulating " + numUsers + " users")
      if (numUsers < batchSize) {
        batchSize = numUsers
      }
      runningTotal += batchSize
      simulateUsers((runningTotal - batchSize) + 1, batchSize)
    }

    case UserRegistrationStatus(status) => {
      if (status) {
        numActiveUsers += 1
        //println("Number of users registered: "+numActiveUsers)
        if (numActiveUsers >= numUsers) {
          simulateFollowers()
        } else if (!IsUserSimulationComplete && numActiveUsers < numUsers && numActiveUsers >= nextBatchAfter * runningTotal) {
          if (numUsers < runningTotal + batchSize) {
            batchSize = numUsers - runningTotal
            IsUserSimulationComplete = true
          }

          if (batchSize > 0) {
            runningTotal += batchSize
            simulateUsers((runningTotal - batchSize) + 1, batchSize)
          }
        }
      }
    }

    case UserFollowersListUpdateStatus(status) => {
      if (status) {
        followersSimulatedForNumUsers += 1
        if (followersSimulatedForNumUsers >= numUsersWithFollowers) {
          println("User follower simulation complete")
          simulateTweeting()

          //tweetsStatsScheduler = context.system.scheduler.schedule(Duration.Zero, Duration(10, TimeUnit.SECONDS), self, ReportTweetStats)

          if (SimulationVariables.userStatusRequestInSec > 0)
            simulateUserStatsRequest()

          if (SimulationVariables.userTimelineRequestInSec > 0)
            simulateUserTimelineRequest()

          if (SimulationVariables.homeTimelineRequestInSec > 0)
            simulateHomeTimelineRequest()

          if (SimulationVariables.replyToTweetInSec > 0)
            simulateReplyToTweet()

          if (SimulationVariables.reTweetInSec > 0)
            simulateReTweeting()
        }
      }
    }

    /*case ReportTweetStats => {
      totalTweets = 0
      numUsersReportedTweets = 0

      users.foreach { user =>
        user ! ReportNumTweets
      }
    }

    case UserNumTweets(num) => {
      numUsersReportedTweets += 1
      totalTweets += num

      if (numUsersReportedTweets >= numUsers) {
        println("Total tweets sent by client: " + totalTweets)
      }
    }*/

    case SimulateUserStatsRequest => {
      var rand = new Random()
      var num = rand.nextInt(numUserActors)
      users(num) ! AskForStats
    }

    case SimulateUserTimelineRequest => {
      var rand = new Random()
      var num = rand.nextInt(numUserActors)
      users(num) ! AskForUserTimeline
    }

    case SimulateHomeTimelineRequest => {
      var rand = new Random()
      var num = rand.nextInt(numUserActors)
      users(num) ! AskForHomeTimeline
    }

    case SimulateReplyToTweet => {
      var rand = new Random()
      var num = rand.nextInt(numUserActors)
      users(num) ! ReplyToRandomTweet
    }

    case SimulateReTweet => {
      var rand = new Random()
      var num = rand.nextInt(numUserActors)
      users(num) ! SendRandomReTweet
    }

    case _ => {
      println("Unexpected Message received by " + self)
    }
  }

  def simulateUsers(start: Int, num: Int) {
    batchNum += 1
    println("Simulating batch" + batchNum + " of users")

    for (i <- start until start + num) {
      var userId = (id + "" + i).toInt

      router ! RegisterUser(userId)
    }
  }

  /**
   * Simulates followers of all users based on simulation criteria (defined in config file)
   */
  def simulateFollowers() {
    println("Simulating followers for users")

    var totalF: Double = 0
    var maxF: Int = 0

    var rand = new Random()
    var list = SimulationVariables.numPerFollowersPerUser
    var start = 0
    for (i <- 0 to list.size - 1) {
      var numU = start + ((list(i)._3 * numUsers) / 100).toInt
      var numFollowersEachUserLL = (capacity * list(i)._1 / 5).toInt
      var numFollowersEachUserUL = (capacity * list(i)._2 / 5).toInt

      if (numFollowersEachUserUL - numFollowersEachUserLL > 0) {
        for (j <- start until numU) {
          var numFollowersEachUser = numFollowersEachUserLL + rand.nextInt(numFollowersEachUserUL - numFollowersEachUserLL)

          if (numFollowersEachUser > 0) {
            if (numFollowersEachUser > maxF)
              maxF = numFollowersEachUser

            totalF += numFollowersEachUser

            numUsersWithFollowers += 1

            simulateFollowersForUser((id + "" + (start + 1)).toInt, numFollowersEachUser)
          }
        }
        start = numU
      }
    }

    println("Number of users with followers: " + numUsersWithFollowers)
    println("Average number of followers per user: " + (totalF / numUsers).toDouble)
    println("Max. number of followers of a user: " + maxF)
  }

  /**
   * Randomly selects numFollowers to become followers of user userId
   */
  def simulateFollowersForUser(userId: Int, numFollowers: Int) {
    var rand = new Random()
    var list: List[Int] = List()
    for (k <- 1 to numFollowers) {
      var uId = (id + "" + rand.nextInt(numUsers + 1)).toInt
      list = uId :: list
    }

    router ! UpdateUserFollowersList(userId, list)

  }

  /**
   * Simulates tweeting based on simulation criteria (defined in config file)
   */
  def simulateTweeting() {
    println("Simulating tweeting")

    for (i <- 1 to numUserActors) {
      users(i - 1) = context.actorOf(Props(classOf[UserActor], id, numUsers, router), name = "userActor" + id + "" + i)
      users(i - 1) ! SimulateTweeting(numTweetsPerActor, tweetsInSec)
    }
  }

  /**
   * Setting up scheduler to request stats for a random user
   */
  def simulateUserStatsRequest() {
    var statsRequestScheduler = context.system.scheduler.schedule(Duration(SimulationVariables.userStatusRequestInSec, TimeUnit.SECONDS), Duration(SimulationVariables.userStatusRequestInSec, TimeUnit.SECONDS), self, SimulateUserStatsRequest)
    println("scheduler setup for user stats request " + statsRequestScheduler)
  }

  /**
   * Setting up scheduler to request user timeline for a random user
   */
  def simulateUserTimelineRequest() {
    var userTimelineRequestScheduler = context.system.scheduler.schedule(Duration(SimulationVariables.userTimelineRequestInSec, TimeUnit.SECONDS), Duration(SimulationVariables.userTimelineRequestInSec, TimeUnit.SECONDS), self, SimulateUserTimelineRequest)
    println("scheduler setup for user timeline request " + userTimelineRequestScheduler)
  }

  /**
   * Setting up scheduler to request home timeline for a random user
   */
  def simulateHomeTimelineRequest() {
    var homeTimelineRequestScheduler = context.system.scheduler.schedule(Duration(SimulationVariables.homeTimelineRequestInSec, TimeUnit.SECONDS), Duration(SimulationVariables.homeTimelineRequestInSec, TimeUnit.SECONDS), self, SimulateHomeTimelineRequest)
    println("scheduler setup for home timeline request " + homeTimelineRequestScheduler)
  }

  /**
   * Setting up scheduler to reply of a random tweet from a random user
   */
  def simulateReplyToTweet() {
    var replyToTweetScheduler = context.system.scheduler.schedule(Duration(SimulationVariables.replyToTweetInSec, TimeUnit.SECONDS), Duration(SimulationVariables.replyToTweetInSec, TimeUnit.SECONDS), self, SimulateReplyToTweet)
    println("scheduler setup for reply to tweet " + replyToTweetScheduler)
  }

  /**
   * Setting up scheduler to retweet a random tweet from a random user
   */
  def simulateReTweeting() {
    var reTweetScheduler = context.system.scheduler.schedule(Duration(SimulationVariables.reTweetInSec, TimeUnit.SECONDS), Duration(SimulationVariables.reTweetInSec, TimeUnit.SECONDS), self, SimulateReTweet)
    println("scheduler setup for re-tweet " + reTweetScheduler)
  }
}