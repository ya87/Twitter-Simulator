package com.twitter.common

import spray.json._
import DefaultJsonProtocol._
import akka.actor.ActorRef

case class GetClientId()
case class SendClientId(peer: ActorRef)
case class SetClientId(id: Int)
case class RegisterUser(userId: Int)
case class UserRegistrationStatus(status: Boolean)
case class UpdateUserFollowersList(userId: Int, followers: List[Int])
case class UserFollowersListUpdateStatus(status: Boolean)
case class AddUserFollower(userId: Int, followerId: Int)
case class UserFollowerAddStatus(status: Boolean)
case class PostTweet(userId: Int, tweet: String)
case class TweetPostStatus(status: Boolean)
case class PostReply(userId: Int, replyToTweetId: String, reply: String)
case class ReplyPostStatus(status: Boolean)
case class ReTweet(userId: Int, tweetId: String)
case class RetweetPostStatus(status: Boolean)

case class RequestForUserTimeline(userId: Int)
case class RequestForHomeTimeline(userId: Int)
case class ResponseForUserTimeline(userId: Int, tweets: List[Tweet])
case class ResponseForHomeTimeline(userId: Int, tweets: List[Tweet])
case class RequestForUserStats(userId: Int)
case class ResponseForUserStats(userId: Int, stats: UserStats)
case class RequestForMentions(userId: Int)
case class ResponseForMentions(userId: Int, tweets: List[Tweet])
case class RequestForServerStats()
case class ResponseForServerStats(stats: ServerStats)

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val getClientIdFormat = jsonFormat0(GetClientId)
  implicit val setClientIdFormat = jsonFormat1(SetClientId)
  implicit val registerUserFormat = jsonFormat1(RegisterUser)
  implicit val userRegistrationStatusFormat = jsonFormat1(UserRegistrationStatus)
  implicit val updateUserFollowersListFormat = jsonFormat2(UpdateUserFollowersList)
  implicit val userFollowersListUpdateStatusFormat = jsonFormat1(UserFollowersListUpdateStatus)
  implicit val addUserFollowerFormat = jsonFormat2(AddUserFollower)
  implicit val UserFollowerAddStatusFormat = jsonFormat1(UserFollowerAddStatus)
  implicit val postTweetFormat = jsonFormat2(PostTweet)
  implicit val tweetPostStatusFormat = jsonFormat1(TweetPostStatus)
  implicit val postReplyFormat = jsonFormat3(PostReply)
  implicit val replyPostStatusFormat = jsonFormat1(ReplyPostStatus)
  implicit val reTweetFormat = jsonFormat2(ReTweet)
  implicit val reTweetPostStatus = jsonFormat1(RetweetPostStatus)
  
  implicit val tweetFormat = jsonFormat5(Tweet)
  implicit val userStatsFormat = jsonFormat3(UserStats)
  implicit val serverStatsFormat = jsonFormat3(ServerStats)
  
  implicit val responseForHomeTimelineFormat = jsonFormat2(ResponseForHomeTimeline)
  implicit val responseForUserTimelineFormat = jsonFormat2(ResponseForUserTimeline)
  implicit val reponseForUserStatsFormat = jsonFormat2(ResponseForUserStats)
  implicit val reponseForMentionsFormat = jsonFormat2(ResponseForMentions)
  implicit val reponseForServerStatsFormat = jsonFormat1(ResponseForServerStats)
}