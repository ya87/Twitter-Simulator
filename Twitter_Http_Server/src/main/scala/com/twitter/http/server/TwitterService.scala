package com.twitter.http.server

import akka.actor.Actor
import spray.routing.HttpServiceActor
import spray.routing.Route
import spray.routing.HttpService
import scala.concurrent.duration._
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import spray.http.MediaTypes
import akka.dispatch.OnSuccess
import spray.http.HttpResponse
import spray.json._
import spray.httpx.SprayJsonSupport._
import DefaultJsonProtocol._
import com.twitter.common.MyJsonProtocol._
import com.twitter.common._
import spray.can.Http
import spray.can.server.Stats
import spray.http.HttpEntity

class TwitterService(serverIP: String) extends Actor with HttpService
{
  def actorRefFactory = context 
  def receive = runRoute(route)

  implicit def executionContext = actorRefFactory.dispatcher
  implicit val timeout = Timeout(50 seconds)
  
  val routerPath = "akka.tcp://ServerSystem@" + serverIP + ":2551/user/server_controller/router"
  lazy val router = actorRefFactory.actorSelection(routerPath)
  println("Router: " + router)

  //TODO - handle scenario when user does not exist
  lazy val route = post {
    respondWithMediaType(MediaTypes.`application/json`) {
      path("add" / "client") {
        complete {
          (router ? GetClientId).mapTo[SetClientId].map(res => res.toJson.prettyPrint)
        }
      } ~
        path("add" / "user") {
          entity(as[RegisterUser]) { req =>
            complete {
              (router ? req).mapTo[UserRegistrationStatus].map(res => res.toJson.prettyPrint)
            }
          }
        } ~
        path("add" / "followers") {
          entity(as[UpdateUserFollowersList]) { req =>
            complete {
              (router ? req).mapTo[UserFollowersListUpdateStatus].map(res => res.toJson.prettyPrint)
            }
          }
        } ~
        path("add" / "follower") {
          entity(as[AddUserFollower]) { req =>
            complete {
              (router ? req).mapTo[UserFollowerAddStatus].map(res => res.toJson.prettyPrint)
            }
          }
        } ~
        path("add" / "tweet") {
          entity(as[PostTweet]) { req =>
            complete {
              (router ? req).mapTo[TweetPostStatus].map(res => res.toJson.prettyPrint)
            }
          }
        } ~
        path("add" / "reply") {
          entity(as[PostReply]) { req =>
            complete {
              (router ? req).mapTo[ReplyPostStatus].map(res => res.toJson.prettyPrint)
            }
          }
        } ~
        path("add" / "retweet") {
          entity(as[ReTweet]) { req =>
            complete {
              (router ? req).mapTo[RetweetPostStatus].map(res => res.toJson.prettyPrint)
            }
          }
        }
    }
  } ~
    get {
      respondWithMediaType(MediaTypes.`application/json`) {
        parameters('userId.as[Int]) { userId =>
          path("user" / "usertimeline") {
            complete {
              (router ? RequestForUserTimeline(userId)).mapTo[ResponseForUserTimeline].map(timeline => timeline.toJson.prettyPrint)
            }
          } ~ path("user" / "stats") {
            complete {
              (router ? RequestForUserStats(userId)).mapTo[ResponseForUserStats].map(stats => stats.toJson.prettyPrint)
            }
          } ~
            path("user" / "hometimeline") {
              complete {
                (router ? RequestForHomeTimeline(userId)).mapTo[ResponseForHomeTimeline].map(timeline => timeline.toJson.prettyPrint)
              }
            }~
            path("user" / "mentions") {
              complete {
                (router ? RequestForMentions(userId)).mapTo[ResponseForMentions].map(timeline => timeline.toJson.prettyPrint)
              }
            }
        }~
        path("server" / "stats"){
          complete {
              (router ? RequestForServerStats).mapTo[ResponseForServerStats].map(stats => stats.toJson.prettyPrint)
            }
        }
      }~
      path("http-server" / "stats"){
        complete{
          (self ? Http.GetStats).mapTo[Stats].map {
            case x: Stats => statsPresentation(x)
          }
        }
      }
    }
  
  def statsPresentation(s: Stats) = HttpResponse(
    entity = HttpEntity(MediaTypes.`text/html`,
      <html>
        <body>
          <h1>HttpServer Stats</h1>
          <table>
            <tr><td>uptime:</td><td>{s.uptime.formatted("hh:mm:ss")}</td></tr>
            <tr><td>totalRequests:</td><td>{s.totalRequests}</td></tr>
            <tr><td>openRequests:</td><td>{s.openRequests}</td></tr>
            <tr><td>maxOpenRequests:</td><td>{s.maxOpenRequests}</td></tr>
            <tr><td>totalConnections:</td><td>{s.totalConnections}</td></tr>
            <tr><td>openConnections:</td><td>{s.openConnections}</td></tr>
            <tr><td>maxOpenConnections:</td><td>{s.maxOpenConnections}</td></tr>
            <tr><td>requestTimeouts:</td><td>{s.requestTimeouts}</td></tr>
          </table>
        </body>
      </html>.toString()
    )
  )
}