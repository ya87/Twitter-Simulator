package com.twitter.common

import scala.collection.mutable.Queue
import java.util.Date
import scala.collection.mutable.ArrayBuffer
import org.json4s.native.Serialization
import org.json4s.native.Serialization._
import org.json4s.ShortTypeHints

class User(uId: Int) extends Serializable {
  var id: Int = uId
  var active: Boolean = false
  //var screenName: String = "1"
  //var name: String = "1"
  //var gender: String = "1"
  //var age: Int = 1
  //var country: String = "1"
  //var continent: String = "1"
  //var description: String = "1"
  //var createdAt = new Date() //timestamp
  //var tweetCount: Int = 1 //The number of tweets (including retweets) issued by the user. 
  //var friends: List[String] = null
  var numTweets: Int = 0 //this will give the total number of tweets posted by this user, userTimeline.size will not give the correct answer as we are keeping only top 100 tweets
  var followers: ArrayBuffer[Int] = null //list of people following this user
  var following: ArrayBuffer[Int] = null //list of people this user is following
  var userTimeline: Queue[String] = null //list of tweet ids on user timeline
  var homeTimeline: Queue[String] = null //list of tweet ids on home timeline
  var userTimelineSize: Int = 0 //helps to check queue overflow
  var homeTimelineSize: Int = 0 //helps to check queue overflow
  //var notifications: Queue[String] = null //list of notifications
  var mentions: Queue[String] = null //list of tweet ids in which this user was mentioned using @userId
  var directMessages: Queue[String] = null //list of tweetIds which are directly sent to this user from his following list or sent by this user to users in his follower's list

  //var favoritesCount: Int = 1 //The number of tweets this user has favorited in the account lifetime
}

object User{
  private implicit val formats = Serialization.formats(ShortTypeHints(List(classOf[User])))

  def toJson(user: User): String = writePretty(user)
}