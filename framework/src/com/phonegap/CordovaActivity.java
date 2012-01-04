package com.phonegap;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
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

}
