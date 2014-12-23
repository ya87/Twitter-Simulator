package com.twitter.common

case class ServerStats(totalNumUsers: Int, totalNumTweets: Int, avgNumTweets: Int) extends Serializable 