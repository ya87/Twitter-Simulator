package com.twitter.client

import java.io.BufferedReader
import java.io.FileReader
import scala.util.Random
import scala.io.Source

object RandomTweet {
	var fileName: String = "tweet_data//tweetfile.txt"
	var buffer: String = null
	var bufferSize: Int = 0
	var random = new Random()
	//val tweetSize = 140
	
	def getRandomTweet(tweetSize: Int): String = {
		if(buffer == null){
		  loadDataFromFile()
		}
		
		var index = random.nextInt(bufferSize-tweetSize)
		
		return buffer.substring(index, index+tweetSize)
	}
	
	def loadDataFromFile(){
	  try{
		  buffer = Source.fromFile(fileName).getLines().mkString
		  bufferSize = buffer.length()
	  }catch{
	    case ex: Exception => {
	      ex.printStackTrace()
	    }
	  }
	}
}