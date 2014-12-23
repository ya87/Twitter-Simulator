package com.twitter.client

import com.typesafe.config.Config
import java.util.StringTokenizer
import scala.collection.mutable.MutableList

object SimulationVariables {
  var perUserActors: Double = 1.0
  var numTweetsPerActor: Int = 0
  var tweetsInSec: Int = 0
  var maxTweetSize: Int = 0
  var numTotalUsers: Int = 0
  var numAvgTweetsPerSec: Int = 0
  var numFollowersPerUser: Int = 0
  var perUsersWithNoFollowers: Int = 0
  var numPerFollowersPerUser: MutableList[Tuple3[Int, Int, Double]] = null
  var numAvgTweetsPerUser: MutableList[Tuple4[Int, Int, Int, Double]] = null
  var userStatusRequestInSec: Int = 0
  var userTimelineRequestInSec: Int = 0
  var homeTimelineRequestInSec: Int = 0
  var replyToTweetInSec: Int = 0
  var reTweetInSec: Int = 0

  //load these variable from properties file
  def loadSimulationVariables(config: Config) {
    perUserActors = config.getDouble("PerUserActors")
    numTweetsPerActor = config.getInt("NumTweetsPerActor")
    tweetsInSec = config.getInt("TweetsInSec")
    maxTweetSize = config.getInt("MaxTweetSize")
    numTotalUsers = config.getInt("NumUsers")
    numAvgTweetsPerSec = 10 //5000

    var tempList = config.getString("NumPerFollowersPerUser")
    //(numFollowersLowerLimit, numFollowersUpperLimit, %users)
    numPerFollowersPerUser = getTuple3List(tempList)   
    //println("numPerFollowersPerUser: "+numPerFollowersPerUser)
    
    tempList = config.getString("NumAvgTweetsPerUser")
    //(numTweetsLowerLimit, numTweetsUpperLimit, numSec, %users)
    numAvgTweetsPerUser = getTuple4List(tempList)
    //println("numAvgTweetsPerUser: "+numAvgTweetsPerUser)
    
    userStatusRequestInSec = config.getInt("UserStatusRequestInSec")
    userTimelineRequestInSec = config.getInt("UserTimelineRequestInSec")
    homeTimelineRequestInSec = config.getInt("HomeTimelineRequestInSec")
    replyToTweetInSec = config.getInt("ReplyToTweetInSec")
    reTweetInSec = config.getInt("ReTweetInSec")
  }
  
  def getTuple4List(str: String): MutableList[Tuple4[Int, Int, Int, Double]] = {
	  var st1 = new StringTokenizer(str, "|")
	  var list = new MutableList[Tuple4[Int, Int, Int, Double]]
	  
	  while(st1.hasMoreTokens()){
	    var st2 = new StringTokenizer(st1.nextToken(), ",")
	    list += Tuple4(st2.nextToken().toInt, st2.nextToken().toInt, st2.nextToken().toInt, st2.nextToken().toDouble)
	  }
	  return list
  }
  
  def getTuple3List(str: String): MutableList[Tuple3[Int, Int, Double]] = {
	  var st1 = new StringTokenizer(str, "|")
	  var list = new MutableList[Tuple3[Int, Int, Double]]
	  
	  while(st1.hasMoreTokens()){
	    var st2 = new StringTokenizer(st1.nextToken(), ",")
	    list += Tuple3(st2.nextToken().toInt, st2.nextToken().toInt, st2.nextToken().toDouble)
	  }
	  return list
  }
}