package com.twitter.common

case class Tweet(id: String, userId: Int, text: String, tweetType: String, inReplyToTweetId: String) extends Serializable {
  
  def this(userId: Int, text: String){
    this("1", userId, text, "T", null)
  }
}