package com.twitter.client

import akka.actor.ActorSelection

case object SimulateUser
case class SimulateFollowers(numtotalUsers: Int, numFollowers: Int)
case class SimulateTweeting(numTweets: Int, inNumSec: Int)
case object SimulateTweeting
case class SendTweet(numTweets: Int)
case object AskForUserTimeline
case object AskForHomeTimeline
case object AskForStats
case object ReportUserStatus
case object ReportFollowersSimulationStatus
case class CurrentUserStatus(status: Boolean)
case class CurrentFollowersSimulationStatus(status: Boolean)
case object SimulateUserStatsRequest
case object SimulateUserTimelineRequest
case object SimulateHomeTimelineRequest
case object SimulateReplyToTweet
case object ReplyToRandomTweet
case object SimulateReTweet
case object SendRandomReTweet

//Temporary Messages
case object ReportTweetStats
case object ReportNumTweets
case class UserNumTweets(num: Int)