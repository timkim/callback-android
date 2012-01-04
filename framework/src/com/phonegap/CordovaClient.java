package com.phonegap;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.util.Log;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.phonegap.api.LOG;

/**
 * The webview client receives notifications about appView
 */
public class CordovaClient extends WebViewClient {

    Activity ctx;
    CordovaView appView;
    String TAG = "PhoneGapClient";

    /**
     * Constructor.
     * 
     * @param ctx
     */
    public CordovaClient(Activity ctx, CordovaView appCode) {
        this.ctx = ctx;
        this.appView = appCode;
        appCode.setWebViewClient(this);
    }
    
    /**
     * Give the host application a chance to take over the control when a new url 
     * is about to be loaded in the current WebView.
     * 
     * @param view          The WebView that is initiating the callback.
     * @param url           The url to be loaded.
     * @return              true to override, false for default behavior
     */
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        
        // First give any plugins the chance to handle the url themselves
        if (appView.appCode.pluginManager.onOverrideUrlLoading(url)) {
        }
        
        // If dialing phone (tel:5551212)
        else if (url.startsWith(WebView.SCHEME_TEL)) {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse(url));
                ctx.startActivity(intent);
            } catch (android.content.ActivityNotFoundException e) {
                Log.e(TAG, "Error dialing "+url+": "+ e.toString());
            }
        }

        // If displaying map (geo:0,0?q=address)
        else if (url.startsWith("geo:")) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                ctx.startActivity(intent);
            } catch (android.content.ActivityNotFoundException e) {
                Log.e(TAG, "Error showing map "+url+": "+ e.toString());
            }
        }

        // If sending email (mailto:abc@corp.com)
        else if (url.startsWith(WebView.SCHEME_MAILTO)) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                ctx.startActivity(intent);
            } catch (android.content.ActivityNotFoundException e) {
                LOG.e(TAG, "Error sending email "+url+": "+ e.toString());
            }
        }

        // If sms:5551212?body=This is the message
        else if (url.startsWith("sms:")) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);

                // Get address
                String address = null;
                int parmIndex = url.indexOf('?');
                if (parmIndex == -1) {
                    address = url.substring(4);
                }
                else {
                    address = url.substring(4, parmIndex);

                    // If body, then set sms body
                    Uri uri = Uri.parse(url);
                    String query = uri.getQuery();
                    if (query != null) {
                        if (query.startsWith("body=")) {
                            intent.putExtra("sms_body", query.substring(5));
                        }
                    }
                }
                intent.setData(Uri.parse("sms:"+address));
                intent.putExtra("address", address);
                intent.setType("vnd.android-dir/mms-sms");
                ctx.startActivity(intent);
            } catch (android.content.ActivityNotFoundException e) {
                Log.e(TAG, "Error sending sms "+url+":"+ e.toString());
            }
        }

        // All else
        else {

            // If our app or file:, then load into a new phonegap webview container by starting a new instance of our activity.
            // Our app continues to run.  When BACK is pressed, our app is redisplayed.
            if (url.startsWith("file://")) {
                appView.appCode.loadUrl(url);
            }

            // If not our application, let default viewer handle
            else {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    ctx.startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) {
                    LOG.e(TAG, "Error loading url "+url, e);
                }
            }
        }
        return true;
    }
    
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {

        // Clear history so history.back() doesn't do anything.  
        // So we can reinit() native side CallbackServer & PluginManager.
        view.clearHistory(); 
    }
    
    /**
     * Notify the host application that a page has finished loading.
     * 
     * @param view          The webview initiating the callback.
     * @param url           The url of the page.
     */
    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        // Clear timeout flag
        //this.ctx.loadUrlTimeout++;

        // Try firing the onNativeReady event in JS. If it fails because the JS is
        // not loaded yet then just set a flag so that the onNativeReady can be fired
        // from the JS side when the JS gets to that code.
        if (!url.equals("about:blank")) {
            appView.loadUrl("javascript:try{ PhoneGap.onNativeReady.fire();}catch(e){_nativeReady = true;}");
        }

        // Make app visible after 2 sec in case there was a JS error and PhoneGap JS never initialized correctly
        if (appView.getVisibility() == View.INVISIBLE) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(2000);
                        ctx.runOnUiThread(new Runnable() {
                            public void run() {
                                appView.setVisibility(View.VISIBLE);
                            }
                        });
                    } catch (InterruptedException e) {
                    }
                }
            });
            t.start();
        }


        // Shutdown if blank loaded
        if (url.equals("about:blank")) {
            if (appView.appCode.callbackServer != null) {
                appView.appCode.callbackServer.destroy();
            }
            //((Object) ctx).endActivity();
            ctx.finish();
        }
    }
    
    /**
     * Report an error to the host application. These errors are unrecoverable (i.e. the main resource is unavailable). 
     * The errorCode parameter corresponds to one of the ERROR_* constants.
     *
     * @param view          The WebView that is initiating the callback.
     * @param errorCode     The error code corresponding to an ERROR_* value.
     * @param description   A String describing the error.
     * @param failingUrl    The url that failed to load. 
     */
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        LOG.d(TAG, "DroidGap: GapViewClient.onReceivedError: Error code=%s Description=%s URL=%s", errorCode, description, failingUrl);

        // Clear timeout flag
        //this.ctx.loadUrlTimeout++;

        // Stop "app loading" spinner if showing
        //this.ctx.spinnerStop();

        // Handle error
        //((Object) ctx).onReceivedError(errorCode, description, failingUrl);
    }
    
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        
        final String packageName = this.ctx.getPackageName();
        final PackageManager pm = this.ctx.getPackageManager();
        ApplicationInfo appInfo;
        try {
            appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                // debug = true
                handler.proceed();
                return;
            } else {
                // debug = false
                super.onReceivedSslError(view, handler, error);
            }
        } catch (NameNotFoundException e) {
            // When it doubt, lock it out!
            super.onReceivedSslError(view, handler, error);
        }
    }
}