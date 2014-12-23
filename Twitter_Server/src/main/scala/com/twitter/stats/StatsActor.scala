package com.twitter.stats

import akka.actor.Actor
import info.monitorenter.gui.chart.Chart2D
import info.monitorenter.gui.chart.traces.Trace2DLtd
import java.awt.Color
import javax.swing.JFrame
import java.awt.event.WindowAdapter
import com.twitter.server.UpdateGraph

class StatsActor(title: String) extends Actor {
  // Create a chart:  
	    var chart = new Chart2D()
	    // Create an ITrace: 
	    // Note that dynamic charts need limited amount of values!!! 
	    var trace = new Trace2DLtd(200)
	    trace.setColor(Color.RED);	 
	    
	    // Add the trace to the chart. This has to be done before adding points (deadlock prevention): 
	    chart.addTrace(trace);
	    
	     // Make it visible:
	    // Create a frame. 
	    var frame = new JFrame(title);
	    // add the chart to the frame: 
	    frame.getContentPane().add(chart);
	    frame.setSize(400,300);
	    // Enable the termination button [cross on the upper right edge]: 
	    /*frame.addWindowListener(
	        new WindowAdapter(){
	          def windowClosing(WindowEvent e){
	              System.exit(0);
	          }
	        }
	      );*/
	    frame.setVisible(true); 
	 
	    
	def receive() = {
	  case UpdateGraph(x, y) => {
	    //println("(x, y): ("+x+", "+y+")")
		  trace.addPoint(x, y)
	  }
	  
	  case _ => {
	    
	  }
	}
}