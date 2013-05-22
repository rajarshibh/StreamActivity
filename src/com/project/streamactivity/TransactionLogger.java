package com.project.streamactivity;

import java.io.BufferedWriter;
import java.io.IOException;

public class TransactionLogger {
	
	//While recording transactions in a file, the following structure is followed
	//<Flag>	<Current Frame Number>	<Packet Frame Number>	<Received From IP>
	//Flag=0 => Packet Sent
	//Flag=1 => Packet Received
	//The last two fields are set to 0 when a packet is sent 
	
	public void recordReceived (BufferedWriter transactionPrintWriter, int currentFrameNumber, 
			int packetFrameNumber,  String address){
		System.out.println("Recording Transaction");
		
		try {
			transactionPrintWriter.append( "1" +  "\t");
			transactionPrintWriter.append( currentFrameNumber +  "\t");
			transactionPrintWriter.append( packetFrameNumber +  "\t");
			transactionPrintWriter.append( address);
			transactionPrintWriter.newLine();
			transactionPrintWriter.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void recordSent (BufferedWriter transactionPrintWriter, int frameNumber){
		System.out.println("Recording Transaction");
		try {
			transactionPrintWriter.append("0" + "\t");
			transactionPrintWriter.append(frameNumber + "\t");
			transactionPrintWriter.append("0" + "\t");
			transactionPrintWriter.append("0" + "\t");
			transactionPrintWriter.newLine();
			transactionPrintWriter.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
