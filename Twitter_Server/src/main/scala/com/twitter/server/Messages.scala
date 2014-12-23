package com.twitter.server

import akka.actor.ActorRef


case object ReportStats
case class SendStats(peer: ActorRef)
case class UpdateGraph(x: Double, y: Double)
case object FirstTweetReceived