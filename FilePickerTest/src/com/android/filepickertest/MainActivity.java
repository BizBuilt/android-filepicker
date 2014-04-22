package com.android.filepickertest;

//import android.net.Uri;
//import java.util.Iterator;
//import java.util.Set;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.annotation.SuppressLint;
import android.app.Activity;
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
		//else if (photoIntent != null && photoIntent.getClipData() != null && photoIntent.getClipData().getItemCount() > 0) {
		else if (photoIntent != null && photoIntent.getExtras() != null && photoIntent.getExtras().getStringArray("fpUrls") != null && photoIntent.getExtras().getStringArray("fpUrls").length > 0) {
			/*
			 * MWE - Multiple images selected. Store multiple data in clip.
			 */
			Intent intent = new Intent(this, FilePicker.class);
			intent.setAction(FilePicker.SAVE_CONTENT);
			//intent.putExtra("extension", ".png");
			intent.setData(photoIntent.getData()); //should be null
			//intent.setClipData(photoIntent.getClipData());
			intent.putExtra("localFilePaths", photoIntent.getExtras().getStringArray("localFilePaths"));
			intent.putExtra("fpUrls", photoIntent.getExtras().getStringArray("fpUrls"));
			intent.putExtra("fpMimeTypes", photoIntent.getExtras().getStringArray("fpMimeTypes"));
			//Log.d(LOGTAG, "Multiple - startActivityForResult...");
			startActivityForResult(intent, FilePickerAPI.REQUEST_CODE_SAVEFILE);
		}
		else {
			Toast.makeText(this, this.getString(R.string.mwe_open_images_first), Toast.LENGTH_SHORT).show();
		}
	}
	
	void openFilePicker(){
		Intent intent = new Intent(this, FilePicker.class);
		startActivityForResult(intent, FilePickerAPI.REQUEST_CODE_GETFILE); //Loads the "Choose image source..." screen
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
	        //ClipData clipData = intent.getClipData();
	        //if (intent.getExtras().getString("allSelectedLocalPaths") != null) {
	        //if (clipData == null) {
	        Uri localFilePath = intent.getData();
	        if (localFilePath != null) {
	        	/*
	        	 * MWE - Single image
	        	 */
	        	
	        	//Log.d(LOGTAG, "File path:" + intent.getData());
		        //Log.d(LOGTAG, "Ink file URL: " +  intent.getExtras().getString("fpUrl"));
		        
		        mTxtLocalPath.setText("Local file path:\n" + localFilePath);
		        mTxtGlobalPath.setText("Ink file URL:\n" +  intent.getExtras().getString("fpUrl"));
	        	
	        }
	        else { //localFilePath == null
	        	/*
	        	 * MWE - Multiple images
	        	 */
	        	
	        	/*
	        	 * Create StringBuilders to show URI info on screen
	        	 */
	        	StringBuilder sbLocal = new StringBuilder();
	        	StringBuilder sbRemote = new StringBuilder();
	        	//int numImages = clipData.getItemCount();
	        	String[] fpUrls = intent.getExtras().getStringArray("fpUrls");
	        	String[] localFilePaths = intent.getExtras().getStringArray("localFilePaths");
	        	int numImages = fpUrls.length;
	        	for (int i = 0; i < numImages; i++) {
	        		sbLocal.append(i + 1).append(". ").append(localFilePaths[i]).append("\n");
	        		sbRemote.append(i + 1).append(". ").append(fpUrls[i]).append("\n");
	        	}
	        	mTxtLocalPath.setText("Local file paths:\n" + sbLocal);
		        mTxtGlobalPath.setText("Ink file URLs:\n" + sbRemote);
	        }
	        
	    }
	}


}
