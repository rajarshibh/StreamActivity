package com.project.streamactivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;

import ch.epfl.arni.ncutils.UncodedPacket;
import ch.epfl.arni.ncutils.f256.F256CodedPacket;
import ch.epfl.arni.ncutils.f256.F256PacketDecoder;
import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class P2PThread implements Runnable{
	
	int noOfBlocks = 10;
	int payloadLen = 1450;
	int codedPktLen = 1460;
	int backoffTime_1 = 5;
	int backoffTime_2 = 15;
	int sleepTimer = 0;
	int track = 0;
	int version = 0;
	int staticIdentifier = 12345;
	int identifier = 0;
	float deficit = 0;
	float maxDeficit = 0;
	int numberOfPktsRececeived;
	byte[] codedData = null;
	byte[] pktData = new byte[1468];
	String TAG = "P2P Thread";
	String multicastIP = "192.168.0.255";
	Random rand = new Random();
	ByteArrayOutputStream byteOutStream = null;
	DataOutputStream dataOutStream = null;
	ByteArrayInputStream byteInpStream = null;
	DataInputStream dataInpStream = null;
	F256CodedPacket[] recvPktsFromServer;
	F256CodedPacket recvPkt;
	F256PacketDecoder pktDecoder = new F256PacketDecoder(noOfBlocks, payloadLen);
	List<UncodedPacket> uncodedPktVectorCurrentFrame = null;
	DatagramSocket socket;
	DatagramPacket dgPkt = null, pilotPkt = null;
	int pktsToSend = 0;
	int totalPktsSent = 0;
	boolean stop = false;
	
	public Handler mHandler;
	
	/*public P2PThread(F256CodedPacket[] recvPktsFromServerPreviousFrame,
			int numberOfPktsRececeivedFromServerPrevFrame, 
			F256PacketDecoder pktDecoder, int track, float deficit, float maxDeficit) {
		this.numberOfPktsRececeived = numberOfPktsRececeivedFromServerPrevFrame;
		this.recvPktsFromServer = recvPktsFromServerPreviousFrame;
		this.pktDecoder = pktDecoder;
		this.deficit = deficit;
		this.maxDeficit = maxDeficit;
		this.track = track;
	}*/
	
	public P2PThread(){};
	
	public void setStop(boolean value){
		this.stop = true;
	}

	@SuppressLint("HandlerLeak")
	@Override
	public void run() {
		
		System.out.println("Starting P2P stream for frame " + track);
		socket = StreamActivity.socket;
		Looper.prepare();
		
		//ReceiverThread.this.fileHandler

		/*mHandler = new Handler() {
		        public void handleMessage(Message msg) {
		            // Act on the message
		        	Thread.currentThread().interrupt();
		        }
		};*/
		while(StreamActivity.p2pFrame<0){
			Log.e(TAG, "Waiting!");
		}
        
        while(StreamActivity.p2pFrame >=0 ){
        	if ((StreamActivity.serverFrame - StreamActivity.p2pFrame)>=2)
        	{
        		StreamActivity.p2pFrame = StreamActivity.serverFrame - 1;
        		track = StreamActivity.p2pFrame;
        		numberOfPktsRececeived = StreamActivity.recvPacketsCount[track];
        		if (numberOfPktsRececeived==0)
            		continue;
        		recvPktsFromServer = new F256CodedPacket[numberOfPktsRececeived];
        		System.arraycopy(StreamActivity.recvPackets[track], 0, 
        				recvPktsFromServer, 0, numberOfPktsRececeived);
        		pktDecoder = StreamActivity.pktDecoderRecord[track];
        	}
        	
        	if (totalPktsSent<5 && totalPktsSent<numberOfPktsRececeived){

    			
    			byte[] header = new byte[8];
    			byte[] pktData = new byte[1468];
    			byteOutStream = new ByteArrayOutputStream();
    			dataOutStream = new DataOutputStream(byteOutStream);
    			
    			// Phase 1 devices backoff for 1 to a maximum
    			// backoffTime_1 = 5ms
    			if (pktDecoder.getSubspaceSize() < noOfBlocks / 2
    					&& pktsToSend < 5) {
    				try {
    					Thread.sleep(rand.nextInt(backoffTime_1));
    					//transactionLog.recordReceived(transactionsPrintWriter,
    					//		track, version, "Phase 1");
    				} catch (InterruptedException e) {
    					// TODO Auto-generated catch block
    					Log.e(TAG, "Error in Thread Sleep!");
    					e.printStackTrace();
    				}
    				pktsToSend++;
    				totalPktsSent++;
    			}
    			// Phase 2 devices backoff for 1 to a maximum
    			// backoffTime_1 = 15ms
    			else if (pktsToSend < 5) {
    				try {
    					Thread.sleep(rand.nextInt(backoffTime_2
    							- backoffTime_1 + 1)
    							+ backoffTime_1);
    					//transactionLog.recordReceived(transactionsPrintWriter,
    					//		track, version, "Phase 2");
    				} catch (InterruptedException e) {
    					// TODO Auto-generated catch block
    					Log.e(TAG, "Error in Thread Sleep!");
    					e.printStackTrace();
    				}
    				pktsToSend++;
    				totalPktsSent++;
    			}
    			// Phase 3..backoff according to deficits and keep sending
    			else if (pktDecoder.getSubspaceSize() > noOfBlocks / 2) {
    				System.out.println(" Values " + deficit + " " + maxDeficit);
    				if (maxDeficit != 0 && deficit != 0)
    					sleepTimer = rand.nextInt((int) ((deficit / maxDeficit) * (backoffTime_2
    									- backoffTime_1 + 1)))
    							+ backoffTime_1;
    				else
    					sleepTimer = rand.nextInt(backoffTime_2
    							- backoffTime_1 + 1)
    							+ backoffTime_1;
    				//transactionLog.recordReceived(transactionsPrintWriter,
    						//track, version, "Phase 3");
    				try {
    					Thread.sleep(sleepTimer);
    				} catch (InterruptedException e) {
    					// TODO Auto-generated catch block
    					Log.e(TAG, "Error in Thread Sleep!");
    					e.printStackTrace();
    				}
    				totalPktsSent++;
    			}

    			try {
    				dataOutStream.writeInt(staticIdentifier);
    				dataOutStream.writeInt(track);
    				//System.out.println("Writing deficit " + deficit);
    				dataOutStream.writeFloat(deficit);
    				dataOutStream.close();
    				header = byteOutStream.toByteArray();
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				Log.e(TAG, "Error writing to output stream!");
    				e.printStackTrace();
    			}
    			codedData = new byte[1460];
    			System.arraycopy(header, 0, pktData, 0, 8);
    			
    			codedData = recvPktsFromServer[totalPktsSent].toByteArray();
    			System.arraycopy(codedData, 0, pktData, 8, codedPktLen);

    			try {
    				dgPkt = new DatagramPacket(pktData, pktData.length,
    						InetAddress.getByName(multicastIP), 9865);
    			} catch (UnknownHostException e) {
    				// TODO Auto-generated catch block
    				Log.e(TAG, "Error creating Datagram packet!");
    				e.printStackTrace();
    			}
    			try {
    				Log.e(TAG, "Sending pkt with track " + track);
    				StreamActivity.socket.send(dgPkt);
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				Log.e(TAG, "Error sending datagram packet!");
    				e.printStackTrace();
    			}

    		
            }
        	
        	//Receiving section
        	pktData = new byte[1460];
			byte[] recvData = new byte[1468];
			try {
				pilotPkt = new DatagramPacket(recvData, recvData.length,
						InetAddress.getByName(multicastIP), 9865);
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				Log.e(TAG, "Error creating datagram packet!");
				e1.printStackTrace();
			}
			try {
				StreamActivity.socket.receive(pilotPkt);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(TAG, "Error receiving datagram packet!");
				e.printStackTrace();
			}

			byteInpStream = new ByteArrayInputStream(pilotPkt.getData());
			dataInpStream = new DataInputStream(byteInpStream);
			try {
				identifier = dataInpStream.readInt();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(TAG, "Error reading from input stream!");
				e.printStackTrace();
			}
			if (identifier != 12345) break;
			try {
				version = dataInpStream.readInt();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(TAG, "Error reading from input stream!");
				e.printStackTrace();
			}
			Log.e(TAG, "Receiving pkt with version " + version + " from "
					+ pilotPkt.getAddress().getHostAddress());
			recvPkt = new F256CodedPacket(noOfBlocks, pktData, 0,
					codedPktLen);
			
			if (version == track && pktDecoder.getSubspaceSize() < 10){
				//Vector<UncodedPacket> packets = pktDecoder
				//.addPacket(recvPkt);
				uncodedPktVectorCurrentFrame = pktDecoder
						.addPacket(recvPkt);
				Log.e(TAG, "Adding to current fame : Rank - "
						+ pktDecoder.getSubspaceSize());
				//WUP(packets, payloadLen, outStream);
			}
        	
        }
        
        //Looper.loop();
        System.out.println("Ending P2P stream for frame " + track);
        return;
		
	}

}
