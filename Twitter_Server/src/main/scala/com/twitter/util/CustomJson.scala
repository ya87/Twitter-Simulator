package com.twitter.util

import com.owlike.genson._
import com.owlike.genson.ext.json4s._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.JsonAST._

object CustomGenson {
  val genson = new ScalaGenson(
    new GensonBuilder()
    .setSkipNull(true)
    .useIndentation(true)
      .withBundle(ScalaBundle(), Json4SBundle())
      .create())
}