package com.twitter.server

import akka.actor.Actor
import scala.collection.mutable.HashMap
import akka.actor.ActorRef
import akka.actor.Props
import com.twitter.datastore.DataStore
import com.twitter.common._
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._
import akka.actor.ActorKilledException
import scala.collection.concurrent.TrieMap
import com.twitter.util.CustomGenson.genson._

class ServerActor(dataStores: TrieMap[Int, DataStore], persistent: Boolean) extends Actor {
  var firstTweetReceived = false
  lazy val serverController = context.actorSelection("//ServerSystem/user/server_controller")

  def receive() = {

    case GetClientId => {
      serverController ! SendClientId(sender)
    }
    
    case RegisterUser(userId) => {
      var clientId = (userId.toString.substring(0, 1)).toInt
      var status = getDataStore(clientId).addUserToDataStore(userId)
      sender ! UserRegistrationStatus(status)
    }

    case PostTweet(userId, tweetText) => {
      var clientId = (userId.toString.substring(0, 1)).toInt
      if (!firstTweetReceived) {
        firstTweetReceived = true
        serverController ! FirstTweetReceived
      }

      //println("tweet received from user" + userId)
      var status = getDataStore(clientId).addTweetToDataStore(userId, tweetText, "T", "")

      sender ! TweetPostStatus(status)
    }

    case AddUserFollower(userId, followerId) => {
      var clientId = (userId.toString.substring(0, 1)).toInt
      var status = getDataStore(clientId).addUserFollower(userId, followerId)
      sender ! UserFollowerAddStatus(status)
    }

    case UpdateUserFollowersList(userId, followers) => {
      var clientId = (userId.toString.substring(0, 1)).toInt
      //println("Updating followers list for user"+userId)
      var status = getDataStore(clientId).updateUserFollowersListInDataStore(userId, followers)
      sender ! UserFollowersListUpdateStatus(status)
    }

    case PostReply(userId, replyToTweetId, replyText) => {
      var clientId = (userId.toString.substring(0, 1)).toInt
      var status = getDataStore(clientId).addTweetToDataStore(userId, replyText, "R", replyToTweetId)
      sender ! ReplyPostStatus(status)
    }

    case ReTweet(userId, tweetId) => {
      var clientId = (userId.toString.substring(0, 1)).toInt
      //println("user" + userId + " retweeted tweet" + tweetId)
      var status = getDataStore(clientId).processReTweet(userId, tweetId)
      sender ! RetweetPostStatus(status)
    }

    case RequestForUserTimeline(userId) => {
      var clientId = (userId.toString.substring(0, 1)).toInt
      sender ! ResponseForUserTimeline(userId, getDataStore(clientId).getUserTimeline(userId))
    }

    case RequestForHomeTimeline(userId) => {
      var clientId = (userId.toString.substring(0, 1)).toInt
      sender ! ResponseForHomeTimeline(userId, getDataStore(clientId).getHomeTimeline(userId))
    }

    case RequestForUserStats(userId) => {
      var clientId = (userId.toString.substring(0, 1)).toInt
      sender ! ResponseForUserStats(userId, getDataStore(clientId).getUserStats(userId))
    }

    case RequestForMentions(userId) => {
      var clientId = (userId.toString.substring(0, 1)).toInt
      sender ! ResponseForMentions(userId, getDataStore(clientId).getMentions(userId))
    }

    case RequestForServerStats => {
      serverController ! SendStats(sender)
    }
    case _ => {
      println("Unexpected Message received by " + self)
    }
  }

  def getDataStore(clientId: Int): DataStore = {
    return dataStores.getOrElseUpdate(clientId, new DataStore(clientId, persistent))
  }
}