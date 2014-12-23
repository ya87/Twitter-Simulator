package com.twitter.datastore

import com.twitter.common.User
import com.twitter.common.Tweet
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Queue
import com.twitter.common.UserStats
import scala.collection.concurrent.TrieMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentLinkedQueue

class DataStore(id: Int, persistent: Boolean) {
  //key is userId, value is user object
  var users: TrieMap[Int, User] = new TrieMap[Int, User]()
  //key is tweetId, value is tweet object
  var tweets: TrieMap[String, Tweet] = new TrieMap[String, Tweet]()

  var maxUserTimelineTweets: Int = 0
  var maxHomeTimelineTweets: Int = 0

  val currUserCount = new AtomicInteger
  val currTweetCount = new AtomicInteger

  def setMaxUserTimelineTweets(num: Int) {
    maxUserTimelineTweets = num
  }

  def setMaxHomeTimelineTweets(num: Int) {
    maxHomeTimelineTweets = num
  }

  def addUserToDataStore(userId: Int): Boolean = {
    currUserCount.incrementAndGet()

    var user = new User(userId)
    /*user.homeTimeline = new Queue[String]
    user.userTimeline = new Queue[String]
    user.mentions = new Queue[String]*/
    user.homeTimeline = new ConcurrentLinkedQueue[String]
    user.userTimeline = new ConcurrentLinkedQueue[String]
    user.mentions = new ConcurrentLinkedQueue[String]
    user.followers = List()
    user.following = List()

    users += ((userId, user))
    //println("user" + userId + " added to datastore")

    return true
  }

  def addUserFollower(userId: Int, followerId: Int): Boolean = {
    var user = users.getOrElse(userId, null)
    if (user != null) {
      user.followers = followerId :: user.followers

      //adding userId to following list of follower
      var follower = users.getOrElse(followerId, null)
      if (follower != null) {
        follower.following = userId :: follower.following
      }
      return true
    } else {
      return false
    }
  }

  def updateUserFollowersListInDataStore(userId: Int, followers: List[Int]): Boolean = {
    var user = users.getOrElse(userId, null)
    if (user != null) {
      user.followers = followers
      //println("user" + user.id + " has " + followers.size + " followers")

      //updating following list based on followers list
      var iterator = user.followers.iterator
      while (iterator.hasNext) {
        var follower = users.getOrElse(iterator.next, null)
        if (follower != null) {
          var following = follower.following
          following = userId :: following
        }
      }
      return true
    } else {
      return false
    }
  }

  def addTweetToDataStore(userId: Int, tweetText: String, tweetType: String, inReplyToTweetId: String): Boolean = {
    var tweetId: String = null
    var user = users.getOrElse(userId, null)

    if (user != null) {
      tweetId = (userId + "" + currTweetCount.incrementAndGet())
      var tweet = new Tweet(tweetId, userId, tweetText, tweetType, inReplyToTweetId)
      tweets += ((tweetId, tweet))

      tweetType match {
        case "T" => {
          if (!persistent) {
            if (user.userTimelineSize >= maxUserTimelineTweets) {
              //var oldTweetId = user.userTimeline.dequeue()
              var oldTweetId = user.userTimeline.poll()
              tweets.remove(oldTweetId)
              user.userTimelineSize -= 1
            }
            if (user.homeTimelineSize >= maxHomeTimelineTweets) {
              //var oldTweetId = user.homeTimeline.dequeue()
              var oldTweetId = user.homeTimeline.poll()
              tweets.remove(oldTweetId)
              user.homeTimelineSize -= 1
            }
          }

          //add tweet to sender's profile page and home timeline
          //user.userTimeline.enqueue(tweetId)
          user.userTimeline.add(tweetId)
          user.userTimelineSize += 1
          //user.homeTimeline.enqueue(tweetId)
          user.homeTimeline.add(tweetId)
          user.homeTimelineSize += 1

          user.numTweets += 1

          //add tweet to home timeline of user's followers
          var followers = user.followers
          if (followers != null && followers.size > 0) {
            followers.foreach(followerId =>
              addTweetToHomeTimelineOfUser(followerId, tweetId))
          }
        }

        case "R" => {
          if (!persistent) {
            if (user.userTimelineSize >= maxUserTimelineTweets) {
              //var oldTweetId = user.userTimeline.dequeue()
              var oldTweetId = user.userTimeline.poll()
              tweets.remove(oldTweetId)
              user.userTimelineSize -= 1
            }
          }

          //add reply to sender's profile page
          //user.userTimeline.enqueue(tweetId)
          user.userTimeline.add(tweetId)
          user.userTimelineSize += 1

          user.numTweets += 1

          sendReplyToRecepient(userId, tweetId, inReplyToTweetId)
        }
      }
      return true
    } else {
      println("user" + userId + " does not exist on server")
      return false
    }
  }

  def addTweetToHomeTimelineOfUser(userId: Int, tweetId: String): Boolean = {
    var user = users.getOrElse(userId, null)
    if (user != null) {
      if (!persistent) {
        if (user.homeTimelineSize >= maxHomeTimelineTweets) {
          //var oldTweetId = user.homeTimeline.dequeue()
          var oldTweetId = user.homeTimeline.poll()
          tweets.remove(oldTweetId)
          user.homeTimelineSize -= 1
        }
      }
      //user.homeTimeline.enqueue(tweetId)
      user.homeTimeline.add(tweetId)
      user.homeTimelineSize += 1
      return true
    } else {
      return false
    }
  }

  def sendReplyToRecepient(userId: Int, replyTweetId: String, inReplyToTweetId: String) {
    var inReplyToTweet = tweets.getOrElse(inReplyToTweetId, null)
    if (inReplyToTweet != null) {
      var inReplyToUserId = inReplyToTweet.userId
      var inReplyToUser = users.getOrElse(inReplyToUserId, null)
      if (inReplyToUser != null) {
        //add reply to receiver's mentions list
        //inReplyToUser.mentions.enqueue(replyTweetId)
        inReplyToUser.mentions.add(replyTweetId)
        var user = users.getOrElse(userId, null)
        if (user != null) {
          var userFollowers = user.followers
          if (userFollowers.contains(inReplyToUserId)) {
            //if receiver is following the sender then add reply to receiver's home timeline as well
            if (!persistent) {
              if (inReplyToUser.homeTimelineSize >= maxHomeTimelineTweets) {
                //var oldTweetId = inReplyToUser.homeTimeline.dequeue()
                var oldTweetId = inReplyToUser.homeTimeline.poll()
                tweets.remove(oldTweetId)
                inReplyToUser.homeTimelineSize -= 1
              }
            }
            //inReplyToUser.homeTimeline.enqueue(replyTweetId)
            inReplyToUser.homeTimeline.add(replyTweetId)
            inReplyToUser.homeTimelineSize += 1
          }

          //people following both the sender and receiver of reply
          var commonFollowers = userFollowers.intersect(inReplyToUser.followers)
          commonFollowers.foreach(userId =>
            addTweetToHomeTimelineOfUser(userId, replyTweetId))
        }
      }
    }
  }

  def processReTweet(userId: Int, tweetId: String): Boolean = {
    var user = users.getOrElse(userId, null)
    if (user != null) {
      if (!persistent) {
        if (user.userTimelineSize >= maxUserTimelineTweets) {
          //var oldTweetId = user.userTimeline.dequeue()
          var oldTweetId = user.userTimeline.poll()
          tweets.remove(oldTweetId)
          user.userTimelineSize -= 1
        }
      }

      //user.userTimeline.enqueue(tweetId)
      user.userTimeline.add(tweetId)
      user.userTimelineSize += 1

      var followers = user.followers
      //add tweet to home timeline of followers
      followers.foreach(followerId =>
        addTweetToHomeTimelineOfUser(followerId, tweetId))

      return true
    }

    return false
  }

  def getUserStats(userId: Int): UserStats = {
    var user = users.getOrElse(userId, null)
    if (user != null) {
      return UserStats(user.numTweets, user.followers.size, user.following.size)
    }
    return null
  }

  def getUserTimeline(userId: Int): List[Tweet] = {
    var list: List[Tweet] = List()

    var user = users.getOrElse(userId, null)
    var userTimeline = user.userTimeline

    var iterator = userTimeline.iterator
    var count = 0

    while (count < maxUserTimelineTweets && iterator.hasNext) {
      var tweet = tweets.getOrElse(iterator.next, null)
      if (tweet != null) {
        list = tweet :: list
        count += 1
      }
    }
    
    return list
  }

  def getHomeTimeline(userId: Int): List[Tweet] = {
    var list: List[Tweet] = List()

    var user = users.getOrElse(userId, null)
    if (user != null) {
      var homeTimeline = user.homeTimeline

      var iterator = homeTimeline.iterator
      var count = 0

      while (count < maxHomeTimelineTweets && iterator.hasNext) {
        var tweet = tweets.getOrElse(iterator.next, null)
        if (tweet != null) {
          list = tweet :: list
          count += 1
        }
      }
    }
    return list
  }

  def getMentions(userId: Int): List[Tweet] = {
    var list: List[Tweet] = List()
    var user = users.getOrElse(userId, null)

    if (user != null) {
      var iterator = user.mentions.iterator
      while (iterator.hasNext) {
        var tweet = tweets.getOrElse(iterator.next, null)
        list = tweet :: list
      }
    }

    return list
  }

  def getNumUsers(): Int = {
    //return users.size
    return currUserCount.get
  }

  def getNumTweets(): Int = {
    //return tweets.size
    return currTweetCount.get
  }
}