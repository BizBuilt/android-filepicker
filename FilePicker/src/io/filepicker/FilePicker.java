/* PBM LLC Description of Fixed Issues 
 * All of fixed parts is marked PBM LLC 
 * 
 * 1. Code is optimized.
 * 2. Multiple downloading function was added.
 */

package io.filepicker;

import java.io.File;
import java.io.IOException;
//import java.net.CookieHandler;
import java.util.ArrayList;
import java.util.List;
//import java.util.Iterator;
//import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.filepicker.R;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
//import android.os.Environment;
import android.provider.MediaStore;
//import android.provider.MediaStore.MediaColumns;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ComponentName;
//import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
//import android.content.res.Resources;
//import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
//import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
//import android.view.animation.TranslateAnimation;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
//import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FilePicker extends Activity {
//PBM LLC Start
	public static final String LOAD_CONTENT = "LOAD_CONTENT";
//PBM LLC End	
	public static final String SAVE_CONTENT = "SAVE_CONTENT";
	public static final String PREFS_NAME = "filepicker";

	private ListView listview;
	private GridView gridview;
//PBM LLC Start	
	private ProgressDialog progressDialog;
	private boolean isLoadingContents = false;
	private Button btnUpload = null;
	private ArrayList<Inode> arrayCheckedInodes = new ArrayList<Inode>();
//PBM LLC End
	private AdapterView<? extends android.widget.Adapter> currentview = null;
	private String path = "/";
	private boolean saveas = false;
	private Intent filesToSave = null;
	private String mimetypes = "*/*";
	private final String TAG = "FilePicker";
	private Uri cameraImageUri = null; // for camera
	private String cameraImageMimeType = "image/jpeg";
	private String[] selectedServices = null;
	//private String extension = "";
	private String displayName = null;

	private static final int CAMERA_REQUEST = 1888;

	class ThumbnailLoaderDataHolder {
		public final String url;
		public final ImageView imgv;

		public ThumbnailLoaderDataHolder(ThumbnailView imgv, String url) {
			this.url = url;
			this.imgv = imgv;
		}
	}

	class FPInodeView extends LinearLayout {
		
		public CheckBox checkbox = null;
		private Inode mInode = null;
		
		public FPInodeView(Context context, Inode inode, boolean thumbnail) {
			super(FilePicker.this);
			setMinimumHeight(96);
			setOrientation(LinearLayout.HORIZONTAL);
			setGravity(Gravity.CENTER_VERTICAL);

			//PBM LLC Start
			mInode = inode;
			
			if(inode.getIsDir() == false && isLoadingContents == true){
				checkbox= new CheckBox(FilePicker.this);
				checkbox.setPadding(0, 0, 10, 16);
				
				checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener(){

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						
						int position = (Integer) buttonView.getTag();
						Log.d(TAG, "check button index = " + String.valueOf(position) + ", checked = " + String.valueOf(isChecked));
						
						if(isChecked == true){
							arrayCheckedInodes.add(mInode);
							
						}else{
							arrayCheckedInodes.remove(mInode);
						}
						
						Log.d(TAG, "array size = " + String.valueOf(arrayCheckedInodes.size()));
						Log.d(TAG, "Inode = " + String.valueOf(mInode));
						
						updateUI();
					}					
				});
				
				addView(checkbox);
			}			
//PBM LLC End	
			ImageView icon = new ImageView(FilePicker.this);
			icon.setImageResource(inode.getImageResource());
			icon.setPadding(14, 18, 12, 16);
			addView(icon);

			TextView textView = new TextView(FilePicker.this);
			textView.setTextSize(18.0f);
			textView.setText(inode.getDisplayName());

			if (inode.isDisabled())
				textView.setTextColor(Color.GRAY);
			if (thumbnail) {
				textView.setTextColor(Color.WHITE);
				icon.setColorFilter(0xffffffff, Mode.XOR);
			}

			addView(textView);
		
		}

		void addNode(Inode inode){
			arrayCheckedInodes.add(inode);
		}

		@Override
		public void onWindowSystemUiVisibilityChanged(int visible) {
			System.out.println("Visibility: visible");
		}
	}

	// Handle the listview
	class InodeArrayAdapter<T> extends ArrayAdapter<T> {
		private boolean thumbnail = false;

		public InodeArrayAdapter(Context context, int textViewResourceId,
				T[] objects) {
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Inode inode = (Inode) getItem(position);

			if (inode.getThumb_exists() && thumbnail) {
				ThumbnailView icon = new ThumbnailView(FilePicker.this, inode);
				return icon;
			} else if (thumbnail) {
				return new NonThumbnailGridBlockView(FilePicker.this, inode);
			} else {
//PBM LLC Start
				FPInodeView inodeView = new FPInodeView(FilePicker.this, inode, thumbnail);
				
				if(inodeView.checkbox != null)
					inodeView.checkbox.setTag(Integer.valueOf(position));
				
				return inodeView;
//PBM LLC End				
			}
		}

		public void setThumbnail(boolean thumbnail) {
			this.thumbnail = true;
		}
	}

	// Load a new folder
	class FpapiTask extends AsyncTask<Long, Integer, Folder> {
		private AuthError authError = null;

		@Override
		protected Folder doInBackground(Long... l) {
			FilePickerAPI fpapi = FilePickerAPI.getInstance();
			try {
				if (path.equals("/")) {
					Inode[] root;
					if (selectedServices == null)
						root = fpapi.getProvidersForMimetype(mimetypes, saveas);
					else
						root = fpapi.getProvidersForServiceArray(selectedServices);
					return new Folder(root, "list", "");
				} else {
					return fpapi.getPath(path, mimetypes);
				}
			} catch (AuthError e) {
				e.printStackTrace();
				this.authError = e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Folder result) {
			if (this.authError != null) {
				// Display auth activity
				Intent intent = new Intent(FilePicker.this, AuthActivity.class);
				intent.putExtra("service", this.authError.getService());
				startActivityForResult(intent, FilePickerAPI.REQUEST_CODE_AUTH);
				overridePendingTransition(0, 0);
			} else if (result == null) {
				Toast.makeText(FilePicker.this, "An unexpected error occured. Are you connected to a network?", Toast.LENGTH_LONG).show();
				setResult(RESULT_CANCELED);
				finish();
			} else {
//PBM LLC Start				
//				ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar1);
//				progressBar.setVisibility(ProgressBar.INVISIBLE);
				hideProgress();
//PBM LLC End				
				
				InodeArrayAdapter<Inode> iarrayadapter = new InodeArrayAdapter<Inode>(FilePicker.this, 0, result.getInodes());
				
				if (!path.equals("/"))
					setTitle(result.getName());
				
				if (result.getView().equals("thumbnails")) {
					iarrayadapter.setThumbnail(true);
					gridview.setAdapter(iarrayadapter);
					Log.d(TAG, "currentview = gridview");
					currentview = gridview;
					gridview.setBackgroundColor(Color.BLACK);
					gridview.getRootView().setBackgroundColor(Color.BLACK);
				} else {
					listview.setAdapter(iarrayadapter);
					Log.d(TAG, "currentview = listview");
					currentview = listview;
				}
				
				currentview.setVisibility(View.VISIBLE);
				currentview.setOnItemClickListener(new OnItemClickListener() {
					
					@Override
					@SuppressLint("NewApi")
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						
						Log.d(TAG, "currentview item clicked");
						
						Inode inode = (Inode) (parent.getAdapter().getItem(position));
						
						if (inode.isDisabled()) {
							Toast.makeText(FilePicker.this, "App doesn't support this file type", Toast.LENGTH_SHORT).show();
							return;
						}
						
						if (inode.getIsDir()) {
							// is a subdirectory
							Log.d(TAG, "inode display name: " + inode.getDisplayName());
							if (inode.getDisplayName().equals("Gallery")) {
								
//									Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
////PBM LLC Start								
//									intent.setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
//									intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
//									startActivityForResult(intent,FilePickerAPI.REQUEST_CODE_GETFILE_LOCAL);
//PBM LLC End				    
									onShareClick(null);
							}
							else if (inode.getDisplayName().equals("Camera")) {
								Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
								// intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
								// android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
								try {
									FilePickerAPI fpapi = FilePickerAPI.getInstance();
									cameraImageUri = Uri.parse("file://"+ File.createTempFile("fpf", fpapi.getExtensionForMimeType(cameraImageMimeType)));
								} catch (IOException e) {
									e.printStackTrace();
								}
								
								intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
								startActivityForResult(intent, CAMERA_REQUEST);
							}
							else {
								Intent intent = new Intent(FilePicker.this, FilePicker.class);
								intent.putExtra("path", inode.getPath());
								intent.setType(mimetypes);
								intent.putExtra("display_name", inode.getDisplayName());
							
								if (saveas) {
									intent.setData(filesToSave.getData());
									
									//intent.setClipData(filesToSave.getClipData());
									if (filesToSave.getExtras() != null) {
										//Log.d(TAG, "*** ADDING EXTRAS ***");
										
										String[] localFilePaths = filesToSave.getExtras().getStringArray("localFilePaths");
										String[] fpUrls = filesToSave.getExtras().getStringArray("fpUrls");
										String[] fpMimeTypes = filesToSave.getExtras().getStringArray("fpMimeTypes");
										/*
										for (int j = 0; j < localFilePaths.length; j++) {
											Log.d(TAG, "localFilePaths[" + j + "]: " + localFilePaths[j]);
											Log.d(TAG, "fpUrls[" + j + "]: " + fpUrls[j]);
											Log.d(TAG, "fpMimeTypes[" + j + "]: " + fpMimeTypes[j]);
										}*/
										
										intent.putExtra("localFilePaths", localFilePaths);
										intent.putExtra("fpUrls", fpUrls);
										intent.putExtra("fpMimeTypes", fpMimeTypes);
									}

									intent.setAction(SAVE_CONTENT);
									
									/*
									if (extension.length() > 0) {
										intent.putExtra("extension", extension);
									}
									*/
									
									startActivityForResult(intent, FilePickerAPI.REQUEST_CODE_SAVEFILE);
								}
								else {
//PBM LLC Start
									intent.setAction(LOAD_CONTENT);
//PBM LLC End									
									startActivityForResult(intent, FilePickerAPI.REQUEST_CODE_GETFILE);
								}
							}
							
							overridePendingTransition(R.anim.right_slide_in,R.anim.right_slide_out);
						}
						else if (!saveas) {
//PBM LLC Start							
//							ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar1);
//							progressBar.setVisibility(ProgressBar.VISIBLE);
							showProgress("Downloading");
//PBM LLC End							
							int SDK_INT = android.os.Build.VERSION.SDK_INT;
							if (SDK_INT >= 11) {
								currentview.setAlpha((float) 0.3);
							}
							new PickFileTask().execute(inode.getPath());
						}
					}

				});
			}
		}
	}

	/*
	 * MWE - Method to make it so only the "Photos" and "Gallery" apps show on the Intent chooser dialog.
	 */
	public void onShareClick(View v) {

		Intent intent1 = createMultiImageSelectIntent();

	    PackageManager pm = getPackageManager();
	    Intent intent2 = createMultiImageSelectIntent();

	    List<ResolveInfo> resInfo = pm.queryIntentActivities(intent2, 0);
	    List<LabeledIntent> intentList = new ArrayList<LabeledIntent>();        
	    for (int i = 0; i < resInfo.size(); i++) {
	        // Extract the label, append it, and repackage it in a LabeledIntent
	        ResolveInfo ri = resInfo.get(i);
	        String packageName = ri.activityInfo.packageName;
	        //Log.d(TAG, "packageName: " + packageName);
	        if (packageName.contains("gallery")) { //"Gallery" app
	        	intent1.setPackage(packageName);
	        }
	        else if (packageName.contains("com.google.android.apps.plus")) { //"Photos" app
	        	
	            Intent intent = createMultiImageSelectIntent();
	            intent.setComponent(new ComponentName(packageName, ri.activityInfo.name));
	            //Use custom app label...
	            //intentList.add(new LabeledIntent(intent, packageName, ri.loadLabel(pm), ri.icon));
	            intentList.add(new LabeledIntent(intent, packageName, this.getString(R.string.mwe_photos_multi_select), ri.icon));
	        }
	    }

	    Intent openInChooser = Intent.createChooser(intent1, this.getString(R.string.mwe_choose_app));
	    
	    //Convert intentList to array
	    LabeledIntent[] extraIntents = intentList.toArray( new LabeledIntent[ intentList.size() ]);
	    
	    openInChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);
	    startActivityForResult(openInChooser, FilePickerAPI.REQUEST_CODE_GETFILE_LOCAL);
	}
	
	@SuppressLint("InlinedApi")
	private Intent createMultiImageSelectIntent() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
		intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		return intent;
	}
	
	// File selected
	
//PBM LLC Start	
	class PickMultipleFileTask extends AsyncTask<Inode, String, Intent> { 
		
		
		
		@SuppressLint("NewApi")
		@Override
		protected Intent doInBackground(Inode... inodes) {
			
			//Log.d(TAG, "PickMultipleFileTask.doInBackground(" + inodes + ")");

			/*
			 * MWE - Prepare return intent / clip
			 */
			Intent resultIntent = new Intent();
			resultIntent.setData(null);
			ClipData clipData = null;
			
			int numInodes = inodes.length;
			int numErrors = 0;
			
			String[] localFilePaths = new String[numInodes];
			String[] fpUrls = new String[numInodes];
			String[] fpMimeTypes = new String[numInodes];
			
			for (int i = 0; i < numInodes; i++) {
				
				if (i == 0 && numInodes == 1) {
					publishProgress("");
				}
				else {
					publishProgress( new StringBuilder().append(i + 1).append(" ").append(getResources().getString(R.string.mwe_of)).append(" ").append(numInodes).toString());
				}
				
				Inode inode = inodes[i];
				FPFile fpFile = null;
				Uri localFilePath = null;
				String fpUrl = null;
				String fpMimeType = null;
				try {
					
					/*
					 * MWE - remember cookies so subsequent web service calls don't get "Internal server error"
					 */
					getCookiesFromBrowser();
			
					//Log.d(TAG, "inode.getPath(): " + inode.getPath());
					fpFile = FilePickerAPI.getInstance().getLocalFileForPath(inode.getPath(),FilePicker.this);
					//Log.d(TAG, "fpFile: " + fpFile);
					localFilePath = Uri.parse(fpFile.getLocalPath());
					//Log.d(TAG, "localPath: " + localPath);
					fpUrl = fpFile.getFPUrl();
					//Log.d(TAG, "fpUrl: " + fpUrl);
					fpMimeType = getType(fpFile);
					
					//Log.d(TAG, "mimeType: " + fpMimeType);
				}
				catch (AuthError e) {
					e.printStackTrace();
					numErrors++;
				}
				finally {
					
					/*
					 * MWE - Store URLs and mime types in array (to be added as extras)
					 */
					localFilePaths[i] = localFilePath.toString();
					fpUrls[i] = fpUrl;
					fpMimeTypes[i] = fpMimeType;
					
				}

			}
			
			resultIntent.putExtra("localFilePaths", localFilePaths);
			resultIntent.putExtra("fpUrls", fpUrls);
			resultIntent.putExtra("fpMimeTypes", fpMimeTypes);
			
			resultIntent.putExtra("numErrors", numErrors);
			
			return resultIntent;
			
		}
		
		protected void onProgressUpdate(String... progress) {
			if (progress[0] != null && progress[0].length() > 0) {
				showProgress( new StringBuilder(getResources().getString(R.string.mwe_downloading)).append(" ").append(progress[0]).toString().trim());
			}
			else {
				showProgress(getResources().getString(R.string.mwe_downloading));
			}
	    }
		
		@SuppressLint("NewApi")
		@Override
		protected void onPostExecute(Intent resultIntent) {
			//Log.d(TAG, "PickMultipleFileTask.onPostExecute(" + resultIntent + ")");

			if (resultIntent.getExtras().getInt("numErrors") == 0) {
				setResult(RESULT_OK, resultIntent);
			}
			else {
				setResult(RESULT_CANCELED, resultIntent);
			}
			//DataCache.getInstance().clearCache();
			finish();
			
		}
	}	
//PBM LLC End			
	class PickFileTask extends AsyncTask<String, Integer, FPFile> {
		
//PBM LLC Start		
//		private String fpurl;
//PBM LLC End
		@Override
		protected FPFile doInBackground(String... arg0) {
			if (arg0.length != 1) {
				FilePickerAPI.debug("ERROR");
				return null;
			}
			String path = arg0[0];
			try {
				return FilePickerAPI.getInstance().getLocalFileForPath(path,FilePicker.this);
			} catch (AuthError e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(FPFile result) {
			Intent resultIntent = new Intent();
			Uri localPath = Uri.parse("file://" + result.getLocalPath());
			resultIntent.setData(localPath);
			resultIntent.putExtra("fpUrl", result.getFPUrl());
			
			String fpMimeType = getType(result);
			resultIntent.putExtra("fpMimeType", fpMimeType);
			//Log.d(TAG, "7. mimeType: " + mimeType);
			
			resultIntent.putExtra("fpfile", result);
			setResult(RESULT_OK, resultIntent);
			finish();
		}
	}

	class UploadLocalFileTask extends AsyncTask<Intent, String, Intent> {

		@SuppressLint("NewApi")
		@Override
		protected Intent doInBackground(Intent... intents) {
			//Log.d(TAG, "UploadLocalFileTask.doInBackground(" + intents + ")");

			/*
			 * MWE - Prepare return intent / clip
			 */
			Intent resultIntent = new Intent();
			resultIntent.setData(null);
			ClipData clipData = null;
			
			int numUris = intents.length;
			//Log.d(TAG, "8. numUris: " + numUris);
			int numErrors = 0;
			
			String[] localFilePaths = new String[numUris];
			String[] fpUrls = new String[numUris];
			String[] fpMimeTypes = new String[numUris];
			
			for (int i = 0; i < numUris; i++) {
				
				if (i == 0 && numUris == 1) {
					publishProgress("");
				}
				else {
					publishProgress((i + 1) + " of " + numUris);
				}
				
				FilePickerAPI fpapi = FilePickerAPI.getInstance();
				Uri localFilePath = intents[i].getData(); //todo
				//Log.d(TAG, "8. localUri: " + localUri);
				String fpMimeType = getType(intents[i]);
				//Log.d(TAG, "8. mimeType: " + mimeType);
				
				String fpUrl = null;
				try {
					FPFile fpFile = fpapi.uploadFileToTemp(localFilePath, FilePicker.this, fpMimeType);
					fpUrl = fpFile.getFPUrl();
				}
				catch (IOException e) {
					e.printStackTrace();
					numErrors++;
				}
				finally {
					
					/*
					 * MWE - Store URLs and mime types in array (to be added as extras)
					 */
					localFilePaths[i] = localFilePath.toString();
					fpUrls[i] = fpUrl;
					fpMimeTypes[i] = fpMimeType;
					
				}
			}
			
			resultIntent.putExtra("localFilePaths", localFilePaths);
			resultIntent.putExtra("fpUrls", fpUrls);
			resultIntent.putExtra("fpMimeTypes", fpMimeTypes);
			
			resultIntent.putExtra("numErrors", numErrors);
			
			return resultIntent;
		}

		protected void onProgressUpdate(String... progress) {
			showProgress(("Uploading " + progress[0]).trim());
	    }
		
		@Override
		protected void onPostExecute(Intent resultIntent) {
			//Log.d(TAG, "UploadLocalFileTask.onPostExecute(" + resultIntent + ")");

			if (resultIntent.getExtras().getInt("numErrors") == 0) {
				setResult(RESULT_OK, resultIntent);
			}
			else {
				setResult(RESULT_CANCELED, resultIntent);
			}
			
			DataCache.getInstance().clearCache();
			finish();
		}
	}

	@SuppressLint("NewApi")
	protected void getCookiesFromBrowser() {
		String fpcookie = CookieManager.getInstance().getCookie(FilePickerAPI.FPHOSTNAME);
		Pattern regex = Pattern.compile("session=\"(.*)\"");
		Matcher match = regex.matcher(fpcookie);
		
		if (!match.matches())
			return;
		
		String cookieData = match.group(1);
		// HttpCookie cookie = new HttpCookie(
		// "session",
		// "kI9Uzii1UpDIJGzpkKylOYUbwL8=?_expires=STEzNDM2NDAzMTEKLg==&_id=UydoXHhjMlx4YjNceGZje1x4YWJceDgxXHhjOHRceGUyXHhkY1x4ZmI1XHhhZUZcbicKcDEKLg==&_permanent=STAxCi4=&arg_cache=UycnCi4=&auth_dropbox=KGxwMQpTJ3E4M3MweXljcjJseGd0cycKcDIKYVMnNWxzOGRuZm5tbXR5cnQ0JwpwMwphLg==&dropbox_request_token=Y2NvcHlfcmVnCl9yZWNvbnN0cnVjdG9yCnAxCihjb2F1dGgub2F1dGgKT0F1dGhUb2tlbgpwMgpjX19idWlsdGluX18Kb2JqZWN0CnAzCk50UnA0CihkcDUKUydzZWNyZXQnCnA2ClMncWJpanU4aXc4OXprcHd4JwpwNwpzUydrZXknCnA4ClMnMXd2dzA1emhtZDIwd2I5JwpwOQpzYi4=");
		FilePickerAPI.getInstance().setSessionCookie(cookieData);

		// save persistently
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("sessionCookie", cookieData);
		editor.commit();
	}

	protected void unauth(final Service service) {
		if (service.getServiceId().length() == 0)
			return; //local
		new AlertDialog.Builder(this)
		.setTitle("Logout")
		.setMessage("Log out of " + service.getDisplayName() + "?")
		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				FilePickerAPI.debug("Starting unauth");
				new UnAuthTask().execute(service);

			}
		})
		.setNegativeButton("Cancel", null)
		.show();
	}

	class UnAuthTask extends AsyncTask<Service, Integer, Void> {
		@Override
		protected Void doInBackground(Service... arg0) {
			if (arg0.length != 1) {
				FilePickerAPI.debug("ERROR with unauth task arguments");
				return null;
			}
			Service service = arg0[0];
			FilePickerAPI.getInstance().unauth(service);
			return null;
		}
	}

	class SaveFileTask extends AsyncTask<String, String, Integer> {

		@SuppressLint("NewApi")
		@Override
		protected Integer doInBackground(String... arg0) {
			//Log.d(TAG, "SaveFileTask.doInBackground(" + arg0 + ")...");
			
			FilePickerAPI fpapi = FilePickerAPI.getInstance();
			
			int numErrors = 0;
			
			String[] localFilePaths = filesToSave.getExtras().getStringArray("localFilePaths");
			String[] fpUrls = filesToSave.getExtras().getStringArray("fpUrls");
			String[] fpMimeTypes = filesToSave.getExtras().getStringArray("fpMimeTypes");
			int numFiles = fpUrls.length;
			for (int i = 0; i < numFiles; i++) {
				try {
					if (i == 0 && numFiles == 1) {
						publishProgress("");
					}
					else {
						publishProgress(new StringBuilder().append(i + 1).append(" ").append(getResources().getString(R.string.mwe_of)).append(" ").append(numFiles).toString()); 
					}
					Uri uri = Uri.parse(localFilePaths[i]);
					String mimeType = fpMimeTypes[i];
					//Log.d(TAG, "1. mimeType: " + mimeType);
					String filename = fpapi.getFilenameFromUri(uri);
					//Log.d(TAG, "1. filename: " + filename);
					String fullPath = path + filename + fpapi.getExtensionForMimeType(mimeType);
					//Log.d(TAG, "1. fullPath: " + fullPath);
					FilePickerAPI.getInstance().saveFileAs(fullPath, uri, FilePicker.this, mimeType);
				}
				catch (IOException e) {
					e.printStackTrace();
					numErrors++;
				}
			}
			
			/*
			if (filesToSave != null && filesToSave.getClipData() != null && filesToSave.getClipData().getItemCount() > 0) {
				
				// MWE - Multiple files. Get URIs from clip.
				
				int numFiles = filesToSave.getClipData().getItemCount();
				for (int i = 0; i < numFiles; i++) {
					try {
						if (i == 0 && numFiles == 1) {
							publishProgress("");
						}
						else {
							publishProgress(new StringBuilder().append(i + 1).append(" ").append(getResources().getString(R.string.mwe_of)).append(" ").append(numFiles).toString()); 
						}
						Uri uri = filesToSave.getClipData().getItemAt(i).getIntent().getData();
						String mimeType = filesToSave.getClipData().getItemAt(i).getIntent().getExtras().getString("fpMimeType");
						//Log.d(TAG, "1. mimeType: " + mimeType);
						String filename = fpapi.getFilenameFromUri(uri);
						//Log.d(TAG, "1. filename: " + filename);
						String fullPath = path + filename + fpapi.getExtensionForMimeType(mimeType);
						//Log.d(TAG, "1. fullPath: " + fullPath);
						FilePickerAPI.getInstance().saveFileAs(fullPath, uri, FilePicker.this, mimeType);
					}
					catch (IOException e) {
						e.printStackTrace();
						numErrors++;
					}
				}
			}
			else {

				// MWE - Single file. Get URI from data.
				 
				try {
					Uri uri = filesToSave.getData();
					String mimeType = filesToSave.getExtras().getString("fpMimeType");
					//Log.d(TAG, "2. mimeType: " + mimeType);
					if (mimeType == null) mimeType = getType(uri);
					//Log.d(TAG, "2. mimeType: " + mimeType);
					String filename = fpapi.getFilenameFromUri(uri);
					//Log.d(TAG, "2. filename: " + filename);
					String fullPath = path + filename + fpapi.getExtensionForMimeType(mimeType); // (e.g., "/DropBox/myfile.png")
					//Log.d(TAG, "2. fullPath: " + fullPath);
					FilePickerAPI.getInstance().saveFileAs(fullPath, uri, FilePicker.this, mimeType);
				} catch (IOException e) {
					e.printStackTrace();
					numErrors++;
				}
			}*/
			
			return numErrors;
		}
		
		protected void onProgressUpdate(String... progress) {
			showProgress(new StringBuilder(getResources().getString(R.string.mwe_saving)).append(" ").append(progress[0]).toString().trim());
	    }

		@Override
		protected void onPostExecute(Integer numErrors) {
			Toast.makeText(FilePicker.this, getResources().getString(R.string.mwe_saved_succesfully),
					Toast.LENGTH_SHORT).show();
			Intent resultIntent = new Intent();
			// resultIntent.setData(Uri.parse("file://" + result));
			// resultIntent.putExtra("filepath", result);
			if (numErrors == 0) {
				setResult(RESULT_OK, resultIntent);
			}
			else {
				setResult(RESULT_CANCELED, resultIntent);
			}
			finish();
		}

	}

	@SuppressLint("NewApi")
	public void save() {
		//Log.d(TAG, "save()");
		if (currentview == null) {
			Toast.makeText(this, this.getString(R.string.mwe_wait), Toast.LENGTH_SHORT).show();
			return;
		}
		//EditText editText = (EditText) findViewById(R.id.editText1);

//PBM LLC Start		
//		ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar1);
//		progressBar.setVisibility(ProgressBar.VISIBLE);
//		Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show();
		showProgress(this.getString(R.string.mwe_saving));
//PBM LLC End		
		int SDK_INT = android.os.Build.VERSION.SDK_INT;
		if (SDK_INT >= 11) {
			currentview.setAlpha((float) 0.3);
		}
		
		//Log.d(TAG, "SaveFileTask.execute()...");
		//new SaveFileTask().execute(path + "/" + filename);
		new SaveFileTask().execute();

	}
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Log.d(TAG, "FilePicker.onCreate()");

		if (!FilePickerAPI.isKeySet()) {
			Toast.makeText(this, this.getString(R.string.mwe_api_key_not_set),Toast.LENGTH_LONG).show();
			setResult(RESULT_CANCELED);
			finish();
		}

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String sessionCookie = settings.getString("sessionCookie", "");
		FilePickerAPI.getInstance().setSessionCookie(sessionCookie);
		
		//java.net.CookieHandler.setDefault(new java.net.CookieManager());

		Intent myIntent = getIntent();
		//Log.d(TAG, "myIntent: " + myIntent);
		
		if (myIntent != null && myIntent.getExtras() != null) {
			if (myIntent.getExtras().containsKey("path")) {
				path = myIntent.getExtras().getString("path");
				//Log.d(TAG, "path: " + path);
			}
			
			if (myIntent.getExtras().containsKey("services")) {
				selectedServices = myIntent.getExtras().getStringArray("services");
				//Log.d(TAG, "selectedServices: " + selectedServices);
			}
			/*
			if (myIntent.getExtras().containsKey("extension")) {
				extension = myIntent.getExtras().getString("extension");
				Log.d(TAG, "$$$$ extension: " + extension);
			}
			*/
			
			if (myIntent.getExtras().containsKey("display_name")) {
				displayName = myIntent.getExtras().getString("display_name");
				//Log.d(TAG, "displayName: " + displayName);
			}
		}
		
		if (path.equals("/")) {
			setTitle(this.getString(R.string.mwe_choose_image_source));
		} else {
			String[] splitPath = path.split("/");
			if (displayName != null) {
				setTitle(displayName);
			} else {
				setTitle(splitPath[splitPath.length - 1]);
			}
		}

		CookieSyncManager.createInstance(this); // webview

//PBM LLC Start		
		//Log.d(TAG, "myIntent.getAction(): " + myIntent.getAction());
		if (myIntent.getAction() != null) {
			
			if(myIntent.getAction().equals(SAVE_CONTENT)){
				
				//Log.d(TAG, "myIntent.getData(): " + myIntent.getData());
				//Log.d(TAG, "myIntent.getClipData(): " + myIntent.getClipData());

				//if (myIntent.getData() == null && (myIntent.getClipData() == null || myIntent.getClipData().getItemCount() == 0)) {
				if (myIntent.getData() == null && (myIntent.getExtras() == null || myIntent.getExtras().getStringArray("fpUrls") == null || myIntent.getExtras().getStringArray("fpUrls").length == 0)) {
					Log.e(TAG, "No 'fpUrls' data found in intent");
					setResult(RESULT_CANCELED);
					finish();
				}
				else {
					saveas = true;
				}

			}
			else if(myIntent.getAction().equals(LOAD_CONTENT)){

				isLoadingContents = true;
			}
		}
//PBM LLC End
		//Log.d(TAG, "3. mimeType: " + getType(myIntent));
		if (getType(myIntent) != null) {
			mimetypes = getType(myIntent);
		}

		//Log.d(TAG, "saveas: " + saveas);
		if (saveas) {
			filesToSave = myIntent;
			//Log.d(TAG, "filesToSave: " + filesToSave);
			
			/*
			 * MWE - Save files here. (I.e., skip the filename input stage.)
			 */
			
			setContentView(R.layout.activity_file_picker_saveas);
			Button saveButton = (Button) findViewById(R.id.buttonSave);
			/*
			Log.d(TAG, "$$$$ extension: " + extension);
			if (extension.length() > 0) {
				TextView textView = (TextView) findViewById(R.id.textView1);
				textView.setText(extension);
			}
			*/
			//Log.d(TAG, "path: " + path);
			if (path.equals("/") || path.equals("/Facebook/")) {
				saveButton.setEnabled(false);
			}
			else {
				
				saveButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						//Log.d(TAG, "save...");
						FilePicker.this.save();
					}
				});
				
			}
		}
		else {
			setContentView(R.layout.activity_file_picker);
			
			/*
			 * MWE - btnUpload code moved here as it only applies to R.layout.activity_file_picker.
			 * (This fixes another NullPointerException.)  
			 */
			btnUpload = (Button)findViewById(R.id.buttonUpload);
			btnUpload.setVisibility(View.INVISIBLE);
			btnUpload.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					//Log.d(TAG, "upload button click");
					
					/*
					 * MWE - Convert ArrayList<Inode> to Inode[]
					 */
					Inode[] checkedInodesArray = new Inode[arrayCheckedInodes.size()];
					checkedInodesArray = arrayCheckedInodes.toArray(checkedInodesArray);
					
					new PickMultipleFileTask().execute(checkedInodesArray);
				}
			});
		}
		
		listview = (ListView) findViewById(R.id.listView1);
		listview.setVisibility(View.INVISIBLE);
//PBM LLC Start
		listview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		
//PBM LLC End				
		gridview = (GridView) findViewById(R.id.gridView1);
		gridview.setVisibility(View.INVISIBLE);
//PBM LLC Start
		progressDialog = new ProgressDialog(this);
		progressDialog.setTitle(this.getString(R.string.mwe_wait));
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(true);
		
		if(isLoadingContents == true){
			showProgress("Uploading contents");
//			btnUpload.setVisibility(View.VISIBLE);
		}
		
		if(arrayCheckedInodes.size() > 0)
			arrayCheckedInodes.removeAll(arrayCheckedInodes);
		
//PBM LLC End
		new FpapiTask().execute(5L);
	}
	
	private String getType(Intent intent) {
		String mimeType = intent.getType();
		if (mimeType == null) {
			FilePickerAPI fpapi = FilePickerAPI.getInstance();
			mimeType = fpapi.getMimeTypeForContentUri(intent.getData(), this);
		}
		return mimeType;
	}
	
	private String getType(FPFile fpFile) {
		String mimeType = fpFile.getType();
		if (mimeType == null) {
			FilePickerAPI fpapi = FilePickerAPI.getInstance();
			mimeType = fpapi.getMimeTypeForContentUri(Uri.parse(fpFile.getLocalPath()), this);
		}
		return mimeType;
	}
	
	private String getType(Uri uri) {
		FilePickerAPI fpapi = FilePickerAPI.getInstance();
		return fpapi.getMimeTypeForContentUri(uri, this);
	
	}
	
	@Override
	public void onPause() {
		super.onPause();
		/*
		 * MWE - prevent leaked window
		 */
		dismissProgress();
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(R.anim.right_slide_out_back,R.anim.right_slide_in_back);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		switch (requestCode) {
		case FilePickerAPI.REQUEST_CODE_AUTH:
			if (resultCode == RESULT_OK) {
				getCookiesFromBrowser();
				new FpapiTask().execute(6L);
			} else {
				setResult(RESULT_CANCELED);
				finish();
			}
			break;
		case FilePickerAPI.REQUEST_CODE_GETFILE:
			//Log.d(TAG, "onActivityResult - intent: " + intent);
			setResult(resultCode, intent);
			DataCache.getInstance().clearCache();
			finish();
			//MWE - break here...
			break;
		case FilePickerAPI.REQUEST_CODE_SAVEFILE:
			if (resultCode == RESULT_OK) {
				setResult(RESULT_OK, intent);
				DataCache.getInstance().clearCache();
				finish();
			}
			break;
		case FilePickerAPI.REQUEST_CODE_GETFILE_LOCAL:
			if (resultCode == RESULT_OK) {
				
				//Log.d(TAG, "getFilesFromGallery...");
				/*
				 * MWE - Pass intent object rather than intent.getData() as the latter will be null as we have specified EXTRA_ALLOW_MULTIPLE.
				 */
				//new UploadLocalFileTask().execute(data.getData());
				getFilesFromGallery(intent);
				// enableLoading()
//PBM LLC Start				
//				setProgressVisible();
				//showProgress("Uploading");
//PBM LLC End				
			}
			break;
		case CAMERA_REQUEST:
			if (resultCode == RESULT_OK) {
				Intent cameraImageIntent = new Intent();
				cameraImageIntent.setData(cameraImageUri);
				new UploadLocalFileTask().execute(cameraImageIntent);
//PBM LLC Start				
//				setProgressVisible();
				showProgress("Uploading...");
//PBM LLC End				
				// enableLoading
			}
			break;
		}

	}
// PBM LLC Start
	/*
	 * MWE - This method is called after the user has selected the image(s) from the Gallery / Photo app
	 */
	@SuppressLint("NewApi")
	public void getFilesFromGallery(Intent intent) {
		//Log.d(TAG, "getFilesFromGallery(" + intent + ")");
		
		//FilePickerAPI fpapi = FilePickerAPI.getInstance();
		
		/*
		 * MWE - Create URIs array
		 */
		//Uri[] uris;
		ClipData clipData = intent.getClipData();
		
		if (clipData == null) { // MWE - Single image selected

			//Log.d(TAG, "Single local image selected");
            
			new UploadLocalFileTask().execute(intent);
		}
		else { //clipData != null. Multiple images selected.

			//Log.d(TAG, "Multiple local images selected");
			
			/*
			 * MWE - Convert the clip data into an array of intents
			 */
			
			int numItems = clipData.getItemCount();
			Intent[] intents = new Intent[numItems];
			Uri tempUri;
			for (int i = 0; i < numItems; i++) {
				intents[i] = new Intent();
				tempUri = clipData.getItemAt(i).getUri();
				intents[i].setData(tempUri);
			}
			
			new UploadLocalFileTask().execute(intents);
			
		}
	}
	
//	private void processSingleLocalImage(Uri uri) {
//		Log.d(TAG, "***** processSingleLocalImage(" + uri + ")");
//		
//		/*
//		 * MWE - This is the customer's original code block
//		 */
//		String[] projection = {MediaColumns.DATA};
//	    Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
//	    //Log.d(TAG, "cursor: " + cursor);
//	    if(cursor != null) {
//	        //HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
//	        //THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
//	        cursor.moveToFirst();
//	        int columnIndex = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
//	        
//	        String filePath = cursor.getString(columnIndex);
//	        //Log.d(TAG, "filePath: " + filePath);
//	        Uri fileUri = Uri.fromFile(new File(filePath));
//	    	new UploadLocalFileTask().execute(fileUri);
//	        cursor.close();
//	    }
//	    else{
//	    	new UploadLocalFileTask().execute(uri);
//	    }
//	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		
		super.onRestoreInstanceState(savedInstanceState);

		if(savedInstanceState.containsKey("cameraImageUri")){
			cameraImageUri = Uri.parse(savedInstanceState.getString("cameraImageUri"));
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		
		super.onSaveInstanceState(outState);

		if(cameraImageUri != null){
			outState.putString("cameraImageUri", cameraImageUri.toString());
		}
	}	
	
	/*
	private File createImageTempFile(){
		File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.jpg");
		try {
			file.createNewFile();
		} catch (IOException e) {
			Log.d(TAG, "createImageTempFile exception = " + e.getMessage());
		}
		
		return file;
	}
	*/
	
	private void hideProgress() {
		progressDialog.hide();
	}
	
	/*
	 * MWE - method (for consistency) to dismiss progress dialog
	 */
	private void dismissProgress() {
		progressDialog.dismiss();
	}

	private void showProgress(String msg) {
		
		progressDialog.setMessage(msg);
		progressDialog.show();
	}
	/*
	private void setProgressMessage(String msg){
		progressDialog.setMessage(msg);
	}
	*/
	
	private void updateUI(){
		
		//TranslateAnimation mAnimation = new TranslateAnimation(0, 0, 0, 599);
		if(arrayCheckedInodes.size()==0){
			btnUpload.setVisibility(View.INVISIBLE);

		}else{
			int files = arrayCheckedInodes.size();
			btnUpload.setText(new StringBuilder(this.getString(R.string.mwe_upload)).append(" ").append(String.valueOf(files)).append(" ").append(this.getString(R.string.mwe_files)).toString());
			btnUpload.setVisibility(View.VISIBLE);
		}		
	}
// PBM LLC End
}
