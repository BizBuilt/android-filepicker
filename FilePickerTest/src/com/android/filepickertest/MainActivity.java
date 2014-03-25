package com.android.filepickertest;

//import android.net.Uri;
//import java.util.Iterator;
//import java.util.Set;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import io.filepicker.FilePicker;
import io.filepicker.FilePickerAPI;

public class MainActivity extends Activity {

	private static final String LOGTAG = "MainActivity";
	private static final String FILE_PICKER_API_KEY = "AeYZP5oqhSX2TuMe8B99Oz";
	private Button mBtnOpen = null;
	private Button saveButton = null;
	private TextView mTxtLocalPath = null;
	private TextView mTxtGlobalPath = null;
	
	private Intent photoIntent = null;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		FilePickerAPI.setKey(FILE_PICKER_API_KEY);
		
		mBtnOpen = (Button)findViewById(R.id.buttonOpen);
		mBtnOpen.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				openFilePicker();
				
			}
		});
		
		saveButton = (Button)findViewById(R.id.buttonSave);
		saveButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {				
				saveFile();				
			}
		}); 
		
		mTxtLocalPath = (TextView)findViewById(R.id.textView1);
		mTxtGlobalPath = (TextView)findViewById(R.id.textView2);
	}

	@SuppressLint("NewApi")
	void saveFile(){
		//Log.d(LOGTAG, "saveFile()");
		
		if (photoIntent != null && photoIntent.getData() != null) {
			/*
			 * MWE - Single image selected. Store data as normal.
			 */
			Intent intent = new Intent(this, FilePicker.class);
			intent.setAction(FilePicker.SAVE_CONTENT);
			//intent.putExtra("extension", ".png");
			intent.setData(photoIntent.getData());
			//Log.d(LOGTAG, "Single - startActivityForResult...");
			startActivityForResult(intent, FilePickerAPI.REQUEST_CODE_SAVEFILE);
		}
		else if (photoIntent != null && photoIntent.getClipData() != null && photoIntent.getClipData().getItemCount() > 0) {
			/*
			 * MWE - Multiple images selected. Store multiple data in clip.
			 */
			Intent intent = new Intent(this, FilePicker.class);
			intent.setAction(FilePicker.SAVE_CONTENT);
			//intent.putExtra("extension", ".png");
			intent.setData(photoIntent.getData()); //should be null
			intent.setClipData(photoIntent.getClipData());
			//Log.d(LOGTAG, "Multiple - startActivityForResult...");
			startActivityForResult(intent, FilePickerAPI.REQUEST_CODE_SAVEFILE);
		}
		else {
			Toast.makeText(this, this.getString(R.string.mwe_open_images_first), Toast.LENGTH_SHORT).show();
		}
	}
	
	void openFilePicker(){
		Intent intent = new Intent(this, FilePicker.class);
		startActivityForResult(intent, FilePickerAPI.REQUEST_CODE_GETFILE);
	}
	
	@SuppressLint("NewApi")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
	    if (requestCode == FilePickerAPI.REQUEST_CODE_GETFILE) {
	    	
	        if (resultCode != RESULT_OK) {
	            return;
	        }
	        
	        photoIntent = intent;
	        
	        /*
	         * MWE - quick show all selected paths / URLs
	         */
	        ClipData clipData = intent.getClipData();
	        //if (intent.getExtras().getString("allSelectedLocalPaths") != null) {
	        if (clipData == null) {
	        	/*
	        	 * MWE - Single image
	        	 */
	        	
	        	//Log.d(LOGTAG, "File path:" + intent.getData());
		        //Log.d(LOGTAG, "Ink file URL: " +  intent.getExtras().getString("fpurl"));
		        
		        mTxtLocalPath.setText("File path:\n" + intent.getData());
		        mTxtGlobalPath.setText("Ink file URL:\n" +  intent.getExtras().getString("fpurl"));
	        	
	        }
	        else { //clipData != null
	        	/*
	        	 * MWE - Multiple images
	        	 */
	        	
	        	/*
	        	 * Create StringBuilders to show URI info on screen
	        	 */
	        	StringBuilder sbLocal = new StringBuilder();
	        	StringBuilder sbRemote = new StringBuilder();
	        	int numImages = clipData.getItemCount();
	        	for (int i = 0; i < numImages; i++) {
	        		sbLocal.append(i).append(". ").append(clipData.getItemAt(i).getIntent().getData()).append("\n");
	        		sbRemote.append(i).append(". ").append(clipData.getItemAt(i).getIntent().getExtras().getString("fpurl")).append("\n");
	        	}
	        	mTxtLocalPath.setText("File paths:\n" + sbLocal);
		        mTxtGlobalPath.setText("Ink file URLs:\n" + sbRemote);
	        }
	        
	    }
	}


}
