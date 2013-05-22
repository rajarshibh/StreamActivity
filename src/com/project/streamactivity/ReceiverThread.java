package com.project.streamactivity;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import ch.epfl.arni.ncutils.FiniteField;
import ch.epfl.arni.ncutils.UncodedPacket;
import ch.epfl.arni.ncutils.f256.F256;
import ch.epfl.arni.ncutils.f256.F256CodedPacket;
import ch.epfl.arni.ncutils.f256.F256PacketDecoder;

public class ReceiverThread implements Runnable {
	int part = 0;
	int track = 1;
	int version = -1;
	int frameCount = 0;
	int retVal = -1;
	int fileIdx = 1;
	int pktsToSend = 0;
	int totalPktsSent = 0;
	int backoffTime_1 = 5;
	int backoffTime_2 = 15;
	int codedPktLen = 1460;
	int rangeOfCount = 0;
	int randomCount = 0;
	int maxRecv = 8;
	int minRecv = 5;
	int noOfBlocks = 10;
	int bound = 30;
	int payloadLen = 1450;
	int dgPort = 9864;
	int sleepTimer = 0;
	int identifier;
	int staticIdentifier = 12345;
	int currentFrameNumber = -1;
	int numberOfPktsRececeivedFromServerCurrentFrame = 0;
	int numberOfPktsRececeivedFromServerPrevFrame = 0;
	int firstRunFlag = -1;
	float deficit = 0;
	float maxDeficit = 0;
	float pktDeficit = 0;
	double EWMA = 0;
	int versionOfMaxDeficit = 0;
	private static final float QOS = (float) 0.95;
	File srcFile = null;
	File destFile = null;
	File deficitLogFile = null;
	File transactionLogFile = null;
	BufferedWriter deficitPrintWriter = null;
	BufferedWriter transactionsPrintWriter = null;
	FiniteField finField = null;
	String extStorageDirectory;
	String inpFname = "coded";
	String TAG = "Receiver Thread";
	String name = "tempOutFile";
	String multicastIP = "192.168.0.255";
	String deficitStringValue;
	String trackStringValue;
	String ewmaStringValue;
	Random rand = new Random();
	MediaPlayer player = null;
	FileOutputStream outStream = null;
	byte[] header = new byte[8];
	byte[] pktData = new byte[1468];
	byte[] codedData = null;
	byte[] recvData;
	F256PacketDecoder pktDecoder = null;
	F256PacketDecoder tmpPktDecoder = null;
	F256CodedPacket recvPkt = null;
	F256CodedPacket[] recvPktsFromServerCurrentFrame = new F256CodedPacket[10];
	F256CodedPacket[] recvPktsFromServerPreviousFrame = new F256CodedPacket[10];
	F256CodedPacket[] codedPkt = new F256CodedPacket[30];
	FileInputStream inputStream = null;
	ByteArrayInputStream byteInpStream = null;
	DataInputStream dataInpStream = null;
	ByteArrayOutputStream byteOutStream = null;
	DataOutputStream dataOutStream = null;
	DatagramPacket dgPkt = null, pilotPkt = null, serverPkt = null;
	List<UncodedPacket> uncodedPktVector = null;
	List<UncodedPacket> uncodedPktVectorPrevFrame = null;
	List<UncodedPacket> uncodedPktVectorCurrentFrame = null;
	List<UncodedPacket> uncodedPktVectorFullRank = null;
	DeficitLogger deficitLog = new DeficitLogger();
	TransactionLogger transactionLog = new TransactionLogger();
	ExponentialMovingAverage expMovingAverage;
	static Activity stream_screen;
	static P2PThread p2pStreamer = null;
	Message msg = null;
	
	public ReceiverThread(Activity mActivity) {
		this.stream_screen = mActivity;
	}

	public void run() {

		Looper.prepare();

		finField = F256.getF256();
		extStorageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
		destFile = new File("sdcard" + "/DCIM/100MEDIA", name + part
				+ ".mp3");

		try {
			outStream = new FileOutputStream(destFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			deficitLogFile = new File(extStorageDirectory + "/DCIM/"
					+ "Deficit Log.txt");
			deficitLogFile.createNewFile();
			deficitPrintWriter = new BufferedWriter(new FileWriter(
					deficitLogFile));
			transactionLogFile = new File(extStorageDirectory + "/DCIM/"
					+ "Transactions Log.txt");
			transactionLogFile.createNewFile();
			transactionsPrintWriter = new BufferedWriter(new FileWriter(
					transactionLogFile));
			//System.out.println("Opened files to write log");

		} catch (IOException e2) {
			e2.printStackTrace();
		}

		//Initialising the alpha value for EWMA calculation
		expMovingAverage = new ExponentialMovingAverage(0.1);

		//do {
		if (StreamActivity.socket.isClosed()) {
			//exit
		}



		pktDecoder = new F256PacketDecoder(noOfBlocks, payloadLen);
		
		pktsToSend = 0;
		totalPktsSent = 0;
		while (true) {

			// A new file is created for every 50 packets/5 frames
			if (frameCount == 50) {
				// Creates a new file so that a new decoded block can be
				// written here.
				part++;
				frameCount = 0;
				try {
					outStream.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				destFile = new File(extStorageDirectory + "/DCIM/100MEDIA",
						name + part + ".mp3");
				try {
					outStream = new FileOutputStream(destFile);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//System.out.println("Calling media player");
				fileHandler.sendMessage(fileHandler.obtainMessage());
			}

			//!!!!!!!!!!!!!!!!!!!Receiving from the server!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			pktData = new byte[1460];
			recvData = new byte[1468];
			try {
				serverPkt = new DatagramPacket(recvData, recvData.length,
						InetAddress.getByName(ServerConnect.serverIP.toString()), 9864);
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				Log.e(TAG, "Error creating datagram packet!");
				e1.printStackTrace();
			}
			try {
				ServerConnect.socket.receive(serverPkt);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			byteInpStream = new ByteArrayInputStream(serverPkt.getData());
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
					+ serverPkt.getAddress().getHostAddress());
			if (currentFrameNumber==-1){
				currentFrameNumber = version;
				StreamActivity.p2pFrame = 0;
			}
			if(currentFrameNumber<version){
				currentFrameNumber = version;
				StreamActivity.serverFrame = version;
				recvPktsFromServerPreviousFrame = new F256CodedPacket[10];
				System.arraycopy(recvPktsFromServerCurrentFrame, 0, StreamActivity.recvPackets[version], 
						0, numberOfPktsRececeivedFromServerCurrentFrame);
				StreamActivity.recvPacketsCount[version] = numberOfPktsRececeivedFromServerCurrentFrame;
				StreamActivity.pktDecoderRecord[version] = pktDecoder;
				recvPktsFromServerPreviousFrame = recvPktsFromServerCurrentFrame;
				recvPktsFromServerCurrentFrame = new F256CodedPacket[10];
				numberOfPktsRececeivedFromServerPrevFrame = numberOfPktsRececeivedFromServerCurrentFrame;
				numberOfPktsRececeivedFromServerCurrentFrame = 0;
				//System.out.println("Rank of last received frame " + pktDecoder.getSubspaceSize());
				WUP(uncodedPktVectorCurrentFrame, payloadLen, outStream);
				
				frameCount++;
				System.out.println("Previous Frame: " + (currentFrameNumber-1) + " number received: " + numberOfPktsRececeivedFromServerPrevFrame);
				/*new thread for P2P
				if(p2pStreamer != null) 
					msg = Message.obtain();
					msg.obj = "kill"; // Some Arbitrary object
					p2pStreamer.mHandler.sendMessage(msg);
					
				p2pStreamer = new P2PThread(recvPktsFromServerPreviousFrame, numberOfPktsRececeivedFromServerPrevFrame, 
					0, 0);
				
				if (firstRunFlag != -1){
					p2pStreamer.setStop(true);
					firstRunFlag = 0;
				}
				//p2pStreamer = new P2PThread(recvPktsFromServerPreviousFrame, numberOfPktsRececeivedFromServerPrevFrame, 
				//		pktDecoder, currentFrameNumber-1, deficit, maxDeficit);
				//p2pStreamer.run();
				*/
				pktDecoder = new F256PacketDecoder(noOfBlocks, payloadLen);
			}
			System.arraycopy(serverPkt.getData(), 8, pktData, 0, codedPktLen);
			recvPkt = new F256CodedPacket(noOfBlocks, pktData, 0,
					codedPktLen);
			if(numberOfPktsRececeivedFromServerCurrentFrame<10)
				recvPktsFromServerCurrentFrame[numberOfPktsRececeivedFromServerCurrentFrame++]=recvPkt;
			if(recvPkt!=null)
				uncodedPktVectorCurrentFrame = pktDecoder
				.addPacket(recvPkt);
			//WUP(uncodedPktVectorCurrentFrame, payloadLen, outStream);
			//System.out.println("Rank of received frame " + pktDecoder.getSubspaceSize());
			//!!!!!!!!!!!!!!!!!!!Receiving from the server ends!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!


		}

		StreamActivity.stream_screen.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(stream_screen, "Streaming done...", Toast.LENGTH_LONG).show();
				StreamActivity.headerLabel.setText("Transferred");
			}
		});
		try {
			deficitPrintWriter.close();
			transactionsPrintWriter.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Looper.loop();

	}

	// Save the decoded packets
	private void WUP(Iterable<UncodedPacket> packets, int payloadLen,
			FileOutputStream outStream) {
		if(packets != null)
			for (UncodedPacket p : packets) {
				try {
					byte[] buffer = p.getPayload();
					outStream.write(buffer, 0, payloadLen);
				} catch (IOException e) {
					Log.e(TAG, "Error writing to File Output Stream!");
					e.printStackTrace();
				}
			}
	}

	/*
	@SuppressLint("HandlerLeak")
	static Handler myHandle = new Handler() {
		public void handleMessage(Message msg) {
			// update ProgressBar
		}
	};*/

	@SuppressLint("HandlerLeak")
	Handler fileHandler = new Handler(Looper.getMainLooper()) {
		/*private final class FileHandler extends Handler{

		public FileHandler(Looper looper)
	    {
	        super(looper);
	    }*/

		public void handleMessage(Message msg) {
			// Media Player operations
			player = new MediaPlayer();

			int fileIndex = part - 1;
			try {
				player.setDataSource(extStorageDirectory + "/DCIM/100MEDIA/"
						+ name + fileIndex + ".mp3");
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				player.prepare();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			player.start();
		}
	};
}