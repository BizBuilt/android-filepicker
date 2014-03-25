
/*
 * PBM LLC Description of Fixed Issues 
 * All of fixed parts is marked PBM LLC
 * 
 * 1. The problem of cookie sync is fixed. there is no cookie sync module at original code. cookie sync module is added newly. 
 * 2. The issues of UI is fixed. there was 
 * 3. The wrong url to authorize is fixed.
 * 4. Code is optimized.
 */

package io.filepicker;

import java.net.MalformedURLException;
import java.net.URL;

import io.filepicker.R;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.support.v4.app.NavUtils;

public class AuthActivity extends Activity {
	private static final String TAG = "FilePicker";

	private String service;
//PBM LLC Start
//	private ProgressBar progressBar;
	private ProgressDialog progressDialog;
//PBM LLC End
	
	private WebView webView;
	
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_file_picker_auth);
		
		webView = (WebView) findViewById(R.id.webView1);
//PBM LLC Start		
//		progressBar = (ProgressBar) findViewById(R.id.progressBar1);
//PBM LLC End
		
		progressDialog = new ProgressDialog(this);
		//progressDialog.setTitle("Wait...");
		progressDialog.setMessage("Loading...");
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(true);

		Intent intent = getIntent();
		
		if (intent.getExtras().containsKey("service")) {
		
			service = intent.getExtras().getString("service");
			setTitle("Please Authenticate");
			webView.getSettings().setJavaScriptEnabled(true);
			webView.setWebViewClient(new WebViewClient() {

				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					
					Log.d(TAG, "shouldOverrideUrlLoading url = " + url);

/*PBM LLC					
					if (url.startsWith(FilePickerAPI.FPBASEURL + "api/client") && url.contains("authCallback/open")) {

						setResult(RESULT_OK);
						AuthActivity.this.finish();
						overridePendingTransition(R.anim.right_slide_out_back, R.anim.right_slide_in_back);
						return true;
					}
*/										
// PBM LLC Start
					//progressBar.setVisibility(ProgressBar.VISIBLE);
					progressDialog.show();

					try {
						URL urlObject = new URL(url);
						
						Log.d(TAG, "url host = " + urlObject.getHost());
						Log.d(TAG, "url path = " + urlObject.getPath());
						
						if(urlObject.getPath().contains("/dialog/open") == true){

							setResult(RESULT_OK);
							finish();
							overridePendingTransition(R.anim.right_slide_out_back,R.anim.right_slide_in_back);					
						}
						
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
// PBM LLC End					
					return false;
				}
				
				public void onPageFinished(WebView view, String url) {
					Log.d(TAG, "onPageFinished url = " + url);

// PBM LLC Start					
					CookieSyncManager.getInstance().sync();
					//progressBar.setVisibility(ProgressBar.INVISIBLE);
					progressDialog.hide();
// PBM LLC End
				}
			});

// PBM LLC Start		
			String url = FilePickerAPI.FPBASEURL + "api/client/" + service + "/auth/open?m=*/*&key=" + FilePickerAPI.FPAPIKEY + "&id=0&modal=false";
// PBM LLC End			
			Log.d(TAG, "load url = " + url);
			webView.loadUrl(url);
			
			/*
			 * MWE - Overcome scrolling problem for Box authentication 
			 */
			webView.setWebChromeClient(new WebChromeClient() {
				public void onProgressChanged(WebView view, int progress) {
					if (progress == 100) {
						view.scrollTo(0, 0);
					}
				}
			 });

			
			
		} else {
			setResult(RESULT_CANCELED);
			finish();
		}
	}

// PBM LLC Start
	@Override
	protected void onPause() {
		super.onPause();
		
		CookieSyncManager.getInstance().stopSync();
	}

	@Override
	protected void onResume() {

		super.onResume();
		
		CookieSyncManager.getInstance().startSync();
	}

//PBM LLC End	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(R.anim.right_slide_out_back, R.anim.right_slide_in_back);
	}

}
