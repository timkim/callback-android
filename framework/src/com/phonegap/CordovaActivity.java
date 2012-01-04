package com.phonegap;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import com.phonegap.LinearLayoutSoftKeyboardDetect;
import com.phonegap.api.IPlugin;
import com.phonegap.api.PluginManager;

public class CordovaActivity extends Activity {

    CordovaView appView;
    private LinearLayoutSoftKeyboardDetect root;
    private int backgroundColor = Color.BLACK;
    private IPlugin activityResultCallback;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        // This builds the view.  We could probably get away with NOT having a LinearLayout, but I like having a bucket!

        Display display = getWindowManager().getDefaultDisplay(); 
        int width = display.getWidth();
        int height = display.getHeight();
        
        root = new LinearLayoutSoftKeyboardDetect(this, width, height);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(this.backgroundColor);
        root.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 
                ViewGroup.LayoutParams.FILL_PARENT, 0.0F));
        
        appView = new CordovaView(this);

        root.addView(appView);
        setContentView(root);
    }

    //When the app is destroyed
    public void onDestroy()
    {
        super.onDestroy();
        appView.onDestroy();
    }
    
    public void loadUrl(String url)
    {
        appView.loadUrl(url);
    }
    
    @Override
    /**
     * Called when an activity you launched exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it. 
     * 
     * @param requestCode       The request code originally supplied to startActivityForResult(), 
     *                          allowing you to identify who this result came from.
     * @param resultCode        The integer result code returned by the child activity through its setResult().
     * @param data              An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
     protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
         super.onActivityResult(requestCode, resultCode, intent);
         appView.appCode.onPluginResult(requestCode, resultCode, intent);
     }

     public void setActivityResultCallback(IPlugin plugin) {
         this.activityResultCallback = plugin;
     }
     
     /**
      * Called when a key is pressed.
      * 
      * @param keyCode
      * @param event
      */
     @Override
     public boolean onKeyDown(int keyCode, KeyEvent event) {
         if (this.appView == null) {
             return super.onKeyDown(keyCode, event);
         }

         // If back key
         if (keyCode == KeyEvent.KEYCODE_BACK) {

             // If back key is bound, then send event to JavaScript
             if (this.appView.checkBackKey()) {
                 this.appView.loadUrl("javascript:PhoneGap.fireDocumentEvent('backbutton');");
                 return true;
             }

             // If not bound
             else {

                 // Go to previous page in webview if it is possible to go back
                 if (this.appView.backHistory()) {
                     return true;
                 }
                 // If not, then invoke behavior of super class
                 else {
                     //this.activityState = ACTIVITY_EXITING;
                     return super.onKeyDown(keyCode, event);
                 }
             }
         }

         // If menu key
         else if (keyCode == KeyEvent.KEYCODE_MENU) {
             this.appView.loadUrl("javascript:PhoneGap.fireDocumentEvent('menubutton');");
             return super.onKeyDown(keyCode, event);
         }

         // If search key
         else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
             this.appView.loadUrl("javascript:PhoneGap.fireDocumentEvent('searchbutton');");
             return true;
         }

         return false;
     }
     

     private void postMessage(String id, Object data) {
         // Forward to plugins
         this.appView.postMessage(id, data);
     }

     
     /* 
      * Hook in DroidGap for menu plugins
      * 
      */
     
     @Override
     public boolean onCreateOptionsMenu(Menu menu)
     {
         this.postMessage("onCreateOptionsMenu", menu);
         return super.onCreateOptionsMenu(menu);
     }
     
     
    @Override
     public boolean onPrepareOptionsMenu(Menu menu)
     {
         this.postMessage("onPrepareOptionsMenu", menu);
         return super.onPrepareOptionsMenu(menu);
     }
     
     @Override
     public boolean onOptionsItemSelected(MenuItem item)
     {
         this.postMessage("onOptionsItemSelected", item);
         return true;
     }
}
