package com.project.streamactivity;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;


import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import ch.epfl.arni.ncutils.FiniteField;
import ch.epfl.arni.ncutils.f256.F256;
import ch.epfl.arni.ncutils.f256.F256CodedPacket;
import ch.epfl.arni.ncutils.f256.F256PacketDecoder;

public class StreamActivity extends Activity {

	static String TAG = "StreamActivity";
	
	int part = 0;
	int bound = 15;
	static int dgPort = 9865;
	int noOfBlocks = 10;
	File destFile;
	WifiManager wifi;
	InetAddress addr;
	FiniteField finField;
	MulticastLock mcLock;
	static DatagramSocket socket;
	FileOutputStream outStream;
	String extStorageDirectory;
	String name = "tempOutFile";
	String inpFname = "coded";
	static DatagramPacket dgPkt = null;
	static String multicastIP = "192.168.0.255";
	static Activity stream_screen;
	static TextView deficitText;
	static TextView frameNumberText;
	static TextView ewmaText;
	static TextView headerLabel;
	static Thread send;
	static int serverFrame = -1;
	static int p2pFrame = -1;
	static F256CodedPacket[][] recvPackets = new F256CodedPacket[1500][15];
	static F256PacketDecoder[] pktDecoderRecord = new F256PacketDecoder[1500];
	static int[] recvPacketsCount = new int[1500];
	
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stream);
		stream_screen = this;
		headerLabel = (TextView) findViewById(R.id.textView1);
		deficitText = (TextView) findViewById(R.id.deficitValue);
		frameNumberText = (TextView) findViewById(R.id.frameNumberValue);
		ewmaText = (TextView) findViewById(R.id.ewmaValue);
		
		
		finField = F256.getF256();
		//extStorageDirectory = Environment.getExternalStorageDirectory().toString();
		wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE );
		
        if(wifi != null)
        {
            mcLock = wifi.createMulticastLock("mylock");
            mcLock.acquire();
            Log.i(TAG,"Wifi Manager is not null. Acquired multicast lock.");
        }
        
        send = new sendThread();
		send.start();
        
	}
	
	private static class sendThread extends Thread {
    	public void run(){
    		//Looper.prepare();
    		try 
            {
    			socket = new DatagramSocket(dgPort);
    			socket.setBroadcast(true);
    			Thread rThread = new Thread(new ReceiverThread(stream_screen));
    			rThread.start();
    			Thread p2pThread = new Thread(new P2PThread());
    			p2pThread.start();
    			return;
    			
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			socket.close();
    			Log.e(TAG, "Error in Datagram socket!");
    			e.printStackTrace();
    		}
    		
    		//Looper.loop();
    	}
	}
	
	
}
