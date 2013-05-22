package com.project.streamactivity;

import java.io.BufferedWriter;
import java.io.IOException;

public class DeficitLogger {
	
	public void recordDeficit (BufferedWriter deficitPrintWriter, int frameNumber, float currentDeficit, int rank){
		
		System.out.println("Writing deficit values frame:" + frameNumber + " deficit:" + currentDeficit);
		try {
			deficitPrintWriter.append(frameNumber + "\t" + currentDeficit + "\t" + rank);
			deficitPrintWriter.newLine();
			deficitPrintWriter.flush();
			//deficitPrintWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}

