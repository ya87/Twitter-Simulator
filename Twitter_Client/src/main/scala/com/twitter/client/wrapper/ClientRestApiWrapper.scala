package com.twitter.client.wrapper

import akka.io.IO
import akka.pattern.ask
import spray.can.Http
import spray.http._
import spray.client.pipelining._
import akka.actor.Actor
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Future
import akka.util.Timeout
import scala.concurrent.duration._
import spray.json.AdditionalFormats
import spray.httpx.SprayJsonSupport._
import spray.httpx.SprayJsonSupport
import spray.json._
import DefaultJsonProtocol._
import com.twitter.common.MyJsonProtocol._
import spray.http.MediaTypes._
import com.twitter.common._

class CientWrapperActor(hostAddress: String) extends Actor  with SprayJsonSupport with AdditionalFormats{
  implicit val system = context.system
  import system.dispatcher

  implicit val timeout = Timeout(50 seconds)
  val host = "http://"+hostAddress+":8080"

  val pipeline: Future[SendReceive] =
    for (
      Http.HostConnectorInfo(connector, _) <- IO(Http) ? Http.HostConnectorSetup(hostAddress, port = 8080)
    ) yield sendReceive(connector)

  def receive = {

    case GetClientId => {
      val peer = sender

      val request = Post("/add/client")
      val response: Future[HttpResponse] = pipeline.flatMap(_(request))

      response onComplete {
        case Success(msg) =>
          peer ! (msg.entity.asString.parseJson).convertTo[SetClientId]
        case Failure(error) => println("error: " + error.getMessage())
      }
    }

    case RegisterUser(userId) => {
      val peer = sender

      val jsonAst = RegisterUser(userId).toJson
      val request = HttpRequest(method = spray.http.HttpMethods.POST, uri = host+"/add/user", entity = HttpEntity(`application/json`, jsonAst.prettyPrint))
      val response: Future[HttpResponse] = pipeline.flatMap(_(request))

      response onComplete {
        case Success(msg) =>
            peer ! (msg.entity.asString.parseJson).convertTo[UserRegistrationStatus]
        case Failure(error) => println("error: " + error.getMessage())
      }
    }
    
    case UpdateUserFollowersList(userId, followersList) => {
       val peer = sender

      val jsonAst = UpdateUserFollowersList(userId, followersList).toJson
      val request = HttpRequest(method = spray.http.HttpMethods.POST, uri = host+"/add/followers", entity = HttpEntity(`application/json`, jsonAst.prettyPrint))
      val response: Future[HttpResponse] = pipeline.flatMap(_(request))

      response onComplete {
        case Success(msg) =>
            peer ! (msg.entity.asString.parseJson).convertTo[UserFollowersListUpdateStatus]
        case Failure(error) => println("error: " + error.getMessage())
      }
    }
    
    case AddUserFollower(userId, followerId) => {
       val peer = sender

      val jsonAst = AddUserFollower(userId, followerId).toJson
      val request = HttpRequest(method = spray.http.HttpMethods.POST, uri = host+"/add/follower", entity = HttpEntity(`application/json`, jsonAst.prettyPrint))
      val response: Future[HttpResponse] = pipeline.flatMap(_(request))

      response onComplete {
        case Success(msg) =>
            peer ! (msg.entity.asString.parseJson).convertTo[UserFollowerAddStatus]
        case Failure(error) => println("error: " + error.getMessage())
      }
    }
    
    case PostTweet(userId, tweetText) => {
      val peer = sender

      val jsonAst = PostTweet(userId, tweetText).toJson
      val request = HttpRequest(method = spray.http.HttpMethods.POST, uri = host+"/add/tweet", entity = HttpEntity(`application/json`, jsonAst.prettyPrint))
      val response: Future[HttpResponse] = pipeline.flatMap(_(request))

      response onComplete {
        case Success(msg) =>
            peer ! (msg.entity.asString.parseJson).convertTo[TweetPostStatus]
        case Failure(error) => println("error: " + error.getMessage())
      }
    }
    
    case PostReply(userId, replyToTweetId, replyText) => {
      val peer = sender

      val jsonAst = PostReply(userId, replyToTweetId, replyText).toJson
      val request = HttpRequest(method = spray.http.HttpMethods.POST, uri = host+"/add/reply", entity = HttpEntity(`application/json`, jsonAst.prettyPrint))
      val response: Future[HttpResponse] = pipeline.flatMap(_(request))

      response onComplete {
        case Success(msg) =>
            peer ! (msg.entity.asString.parseJson).convertTo[ReplyPostStatus]
        case Failure(error) => println("error: " + error.getMessage())
      }
    }
    
    case ReTweet(userId, tweetId) => {
      val peer = sender

      val jsonAst = ReTweet(userId, tweetId).toJson
      val request = HttpRequest(method = spray.http.HttpMethods.POST, uri = host+"/add/retweet", entity = HttpEntity(`application/json`, jsonAst.prettyPrint))
      val response: Future[HttpResponse] = pipeline.flatMap(_(request))

      response onComplete {
        case Success(msg) =>
            peer ! (msg.entity.asString.parseJson).convertTo[RetweetPostStatus]
        case Failure(error) => println("error: " + error.getMessage())
      }
    }
    
    case RequestForUserStats(userId) => {
      val peer = sender

      val request = {Get("/user/stats?userId="+userId)}
      val response: Future[HttpResponse] = pipeline.flatMap(_(request))

      response onComplete {
        case Success(msg) =>
            peer ! (msg.entity.asString.parseJson).convertTo[ResponseForUserStats]
        case Failure(error) => println("error: " + error.getMessage())
      }
    }
    
    case RequestForUserTimeline(userId) => {
      val peer = sender

      val request = {Get("/user/usertimeline?userId="+userId)}
      val response: Future[HttpResponse] = pipeline.flatMap(_(request))

      response onComplete {
        case Success(msg) =>
            peer ! (msg.entity.asString.parseJson).convertTo[ResponseForUserTimeline]
        case Failure(error) => println("error: " + error.getMessage())
      }
    }
    
    case RequestForHomeTimeline(userId) => {
      val peer = sender

      val request = {Get("/user/hometimeline?userId="+userId)}
      val response: Future[HttpResponse] = pipeline.flatMap(_(request))

      response onComplete {
        case Success(msg) =>
            peer ! (msg.entity.asString.parseJson).convertTo[ResponseForHomeTimeline]
        case Failure(error) => println("error: " + error.getMessage())
      }
    }
    
    case RequestForMentions(userId) => {
      val peer = sender

      val request = {Get("/user/mentions?userId="+userId)}
      val response: Future[HttpResponse] = pipeline.flatMap(_(request))

      response onComplete {
        case Success(msg) =>
            peer ! (msg.entity.asString.parseJson).convertTo[ResponseForMentions]
        case Failure(error) => println("error: " + error.getMessage())
      }
    }
  }
}