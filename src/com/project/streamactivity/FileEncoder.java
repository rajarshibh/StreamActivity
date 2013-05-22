package com.project.streamactivity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.app.Activity;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import ch.epfl.arni.ncutils.FiniteField;
import ch.epfl.arni.ncutils.UncodedPacket;
import ch.epfl.arni.ncutils.f256.F256;
import ch.epfl.arni.ncutils.f256.F256CodedPacket;

public class FileEncoder extends Activity {
	
	int part = 0;
	int retVal = 0;
	int bound = 30;
	int frameNumber = 0;
	int noOfBlocks = 10;
	int payloadLen = 1450;
	int initialiseButtonFlag = 1;
	String TAG = "File Encoder";
	Bundle bundle;
	File srcFile;
	File destFile;
	FiniteField finField;
	String extStorageDirectory;
	BufferedInputStream inpStream;
	FileOutputStream outStream;
	F256CodedPacket[] codedPkt;
	private Button initialiseButton;
	private Button skipButton;
	private TextView mainText;
	byte[] temp = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_file_encoder);
		
		initialiseButton = (Button) findViewById(R.id.initialise);
		skipButton = (Button) findViewById(R.id.skipButton);
		mainText = (TextView) findViewById(R.id.textView1);
		
		initialiseButton.setOnClickListener(new View.OnClickListener() 
        {
            public void onClick(View v) 
            {
            	if(initialiseButtonFlag==1){
            		mainText.setText("Initialising...");
            		initialiseButton.setEnabled(false);
            		initialiseButtonFlag = 0;
            	}
            	
        		finField = F256.getF256();
        		extStorageDirectory = Environment.getExternalStorageDirectory().toString();
        		srcFile = new File(extStorageDirectory + "/DCIM/100MEDIA","madari.mp3");	
        		try {
        			inpStream = new BufferedInputStream(new FileInputStream(srcFile));
        		} catch (FileNotFoundException e) {
        			e.printStackTrace();
        		}
        		 		
        		try {
					while (inpStream.available() > 0){
						
						codedPkt = new F256CodedPacket[bound];
						UncodedPacket[] inputPackets = new UncodedPacket[noOfBlocks];
						
						for(int itr = 0; itr < noOfBlocks; itr++) 
						{
							try {
							    temp = new byte[payloadLen];
								retVal = inpStream.read(temp, 0, payloadLen);
							} catch (IOException e) {
								e.printStackTrace();
							}
						    inputPackets[itr] = new UncodedPacket(itr, temp);
						}
						        			
						F256CodedPacket[] codewords = new F256CodedPacket[noOfBlocks];
     	
					    for (int itr = 0; itr < noOfBlocks; itr++) 
					    {
					        codewords[itr] = new F256CodedPacket(inputPackets[itr], noOfBlocks);
					    }
					    long seed = System.nanoTime();
					    Random rand = new Random(seed);
					   
						for(int itr = 0; itr < bound; itr++) 
						{
							codedPkt[itr] = new F256CodedPacket(noOfBlocks, payloadLen);
						    for(int i = 0 ; i < noOfBlocks ; i++) 
						    {
						        int x = rand.nextInt(finField.getCardinality());                
						        F256CodedPacket tempPkt = codewords[i].scalarMultiply(x);
						        codedPkt[itr] = codedPkt[itr].add(tempPkt);
						    }					    					  
						}

						if(frameNumber%40 == 0){
							Toast toast = Toast.makeText(getApplicationContext(), "Created " + frameNumber + " files" , Toast.LENGTH_SHORT);
							toast.show();
						}
						frameNumber++;
						destFile = new File(extStorageDirectory + "/DCIM/100MEDIA","coded" + frameNumber);
						try {
							outStream = /*new BufferedOutputStream*/(new FileOutputStream(destFile));
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						Log.e(TAG, "Opened file to write: " + destFile);
						
						try {
							for (int i=0; i<bound; i++){
								outStream.write(codedPkt[i].toByteArray(), 0, codedPkt[i].getLengthInBytes());
								outStream.flush();
							}
							outStream.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		Toast toast = Toast.makeText(getApplicationContext(), "File initialisation complete", Toast.LENGTH_LONG);
    			toast.show();
    			Intent i = new Intent(getApplicationContext(), StreamActivity.class);
    			startActivity(i);
    			finish();
            	
            }
        });
		
		skipButton.setOnClickListener(new View.OnClickListener() 
        {
            public void onClick(View v) 
            {
            	Intent i = new Intent(getApplicationContext(), StreamActivity.class);
    			startActivity(i);
    			finish();
            }
        });
		
	}
}
