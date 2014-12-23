package com.twitter.client

import akka.actor.Actor
import com.twitter.common._
import scala.util.Random
import akka.actor.ActorSelection
import scala.collection.mutable.LinkedList
import scala.collection.mutable.ArrayBuffer
import akka.actor.Cancellable
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import akka.actor.ActorRef

class UserActor(clientId: Int, totalUsers: Int, router: ActorRef) extends Actor {
  import context.dispatcher

  var tweetScheduler: Cancellable = null
  val maxTweetSize: Int = SimulationVariables.maxTweetSize

  var numTweetsToSend: Int = 0
  var readyToResendTweets = true
  var numTweetsSent: Int = 0
  var isSimulatingReply = false
  var isSimulatingReTweet = false

  def receive() = {
    //TODO - sometimes client stops tweeting after tweeting for few sec, check what's causing the problem
    case SimulateTweeting(numTweets, inNumSec) => {
      if (numTweets > 0) {
        import context.dispatcher

        numTweetsToSend = numTweets
        var gapBetweenTweets = inNumSec //* 1000 //(inNumSec * 1000 / numTweets).toInt
        var delay = new Random().nextInt(60)
        //it'll send numTweets in one iteration
        tweetScheduler = context.system.scheduler.schedule(Duration(delay, TimeUnit.SECONDS), Duration(gapBetweenTweets, TimeUnit.SECONDS), self, SendTweet(numTweets))
      }
    }

    case SendTweet(numTweets) => {
      if (readyToResendTweets) {
        readyToResendTweets = false
        numTweetsSent = 0
        for (i <- 1 to numTweets) {
          var rand = new Random()
          var userId = (clientId + "" + (rand.nextInt(totalUsers) + 1)).toInt
          var tweetText = RandomTweet.getRandomTweet(rand.nextInt(maxTweetSize) + 1)

          //println("user"+userId+" sending tweet: "+tweetText)
          router ! PostTweet(userId, tweetText)
        }
      }
    }

    case TweetPostStatus(status) => {
      if (status) {
        numTweetsSent += 1
        if (numTweetsSent >= numTweetsToSend) {
          readyToResendTweets = true
        }
      }
    }

    /*
    case ReportNumTweets => {
      sender ! UserNumTweets(numTweetsSent)
    }*/

    case ReplyToRandomTweet => {
      if (!isSimulatingReply) {
        isSimulatingReply = true
        var rand = new Random()
        var userId = (clientId + "" + (rand.nextInt(totalUsers) + 1)).toInt
        router ! RequestForHomeTimeline(userId)
      }
    }

    case ReplyPostStatus(status) => {
      
    }
    
    //TODO - uncomment code and make changes for String to Int change
    case SendRandomReTweet => {
      if (!isSimulatingReTweet) {
        isSimulatingReTweet = true
        var rand = new Random()
        var userId = (clientId + "" + (rand.nextInt(totalUsers) + 1)).toInt
        router ! RequestForHomeTimeline(userId)
      }
    }

    case RetweetPostStatus(status) => {
      
    }
    
    case AskForUserTimeline => {
      var rand = new Random()
      var userId = (clientId + "" + (rand.nextInt(totalUsers) + 1)).toInt
      router ! RequestForUserTimeline(userId)
    }

    case AskForHomeTimeline => {
      var rand = new Random()
      var userId = (clientId + "" + (rand.nextInt(totalUsers) + 1)).toInt
      router ! RequestForHomeTimeline(userId)
    }

    case ResponseForUserTimeline(userId, userTimeline) => {
      //println("User Timeline of user" + userId + ": " + userTimeline) 
    }

    case ResponseForHomeTimeline(userId, homeTimeline) => {
      //println("Home Timeline of user"+userId+": "+homeTimeline)

      if (homeTimeline.size > 0) {
        if (isSimulatingReply) {
          replyToRandomTweet(userId, homeTimeline)
        }
        if (isSimulatingReTweet) {
          sendRandomReTweet(userId, homeTimeline)
        }
      }
    }

    case AskForStats => {
      var rand = new Random()
      var userId = (clientId + "" + (rand.nextInt(totalUsers) + 1)).toInt
      router ! RequestForUserStats(userId)
    }

    case ResponseForUserStats(userId: Int, stats: UserStats) => {
      println("Stats for user" + userId)
      println("No. of Tweets: " + stats.numTweets)
      println("No. of Followers: " + stats.numFollowers)
      println("No. of Following: " + stats.numFollowing)
    }

    case _ => {
      println("Unexpected Message received by " + self)
    }
  }

  def replyToRandomTweet(userId: Int, homeTimeline: List[Tweet]) {
    var rand = new Random()
    //pick random tweet from home timeline and reply to it
    var randNum = rand.nextInt(homeTimeline.size)
    var randTweet = homeTimeline(randNum)
    var randTweetUserId = randTweet.userId

    var replyText = "@" + randTweetUserId + " " + RandomTweet.getRandomTweet(rand.nextInt(maxTweetSize) - (randTweetUserId.toString()).length() - 1)
    println("user"+userId+" replying to tweet"+randTweet.id+": "+replyText)

    router ! PostReply(userId, randTweet.id, replyText)
    isSimulatingReply = false
  }

  def sendRandomReTweet(userId: Int, homeTimeline: List[Tweet]) {
    var rand = new Random()
    //pick random tweet from home timeline and retweet it
    var randNum = rand.nextInt(homeTimeline.size)
    var randTweet = homeTimeline(randNum)

    if (randTweet.userId != userId){
      println("user"+userId+" retweeting tweet"+randTweet.id)
      router ! ReTweet(userId, randTweet.id)
    }

    isSimulatingReTweet = false
  }
}