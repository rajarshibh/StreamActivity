package com.project.streamactivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ServerConnect extends Activity {
	
	int serverPort = 9864;
	static int serverResponseCode = 0;
	static CharSequence serverIP = "165.91.215.66"; 
	static byte[] serverResponsePkt;
	
	static private TextView serverIPTextField;
	private static TextView serverIPField;
	private Button connectButton;
	
	static DataOutputStream dos;
	static DataInputStream dis;
	static ByteArrayOutputStream baos;
	static ByteArrayInputStream bais;
	
	private static Thread connectThread;
	static DatagramSocket socket;
	static DatagramPacket packet;
	
	static Activity connectScreen;
	
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.server_ip);
		
		connectScreen = this;
		
		serverIPTextField = (TextView)findViewById(R.id.serverIPText);
		serverIPField = (TextView)findViewById(R.id.IPField);
		connectButton = (Button)findViewById(R.id.connectButton);
		
		serverIP = serverIPField.getText();
		
		connectButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				connectThread = new ConnectThread(getApplicationContext());
				connectThread.start();
			}
		});
	}
	
	private static class ConnectThread extends Thread {
		Context parentContext;
		public ConnectThread(Context context){
			this.parentContext = context;
		}
		public void run(){
			
			System.out.println("Server IP " + serverIP);
			try {
				socket = new DatagramSocket(9864);
			} catch (SocketException e) {
				e.printStackTrace();
			}
			
			connectScreen.runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					Intent i = new Intent(parentContext, StreamActivity.class);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					parentContext.startActivity(i);
				}
			});
			connectScreen.finish();
			
			baos = new ByteArrayOutputStream();
			dos = new DataOutputStream(baos);
			
			try {
				dos.writeInt(12345);
				dos.flush();
				dos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				packet = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length, 
						InetAddress.getByName(serverIP.toString()), 9864);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				socket.send(packet);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			serverResponsePkt = new byte[1500];
			packet = new DatagramPacket(serverResponsePkt, serverResponsePkt.length);
			try {
				socket.receive(packet);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			bais = new ByteArrayInputStream(serverResponsePkt);
			dis = new DataInputStream(bais);
			try {
				serverResponseCode =  dis.readInt();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (serverResponseCode == 12345)
				connectScreen.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						serverIPTextField.setText("Server IP Authenticated");
						serverIPField.setFocusable(false);
					}
				});
			return;
		}
	}

}
