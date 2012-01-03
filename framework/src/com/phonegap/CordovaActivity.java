package com.phonegap;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Display;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import com.phonegap.LinearLayoutSoftKeyboardDetect;
import com.phonegap.api.PluginManager;

public class CordovaActivity extends Activity {

    CordovaView appView;
    private LinearLayoutSoftKeyboardDetect root;
    private int backgroundColor = Color.BLACK;
    PluginManager pluginManager;
    private boolean keepRunning;
    
    private static int ACTIVITY_STARTING = 0;
    private static int ACTIVITY_RUNNING = 1;
    private static int ACTIVITY_EXITING = 2;
    private int activityState = 0;
    
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

        root.addView(this.appView);
        setContentView(root);
        pluginManager = appView.getPluginManager();
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();

        // Don't process pause if shutting down, since onDestroy() will be called
        if (this.activityState == ACTIVITY_EXITING) {
            return;
        }

        if (this.appView == null) {
            return;
        }

        // Send pause event to JavaScript
        this.appView.loadUrl("javascript:try{PhoneGap.fireDocumentEvent('pause');}catch(e){};");

        // Forward to plugins
        this.pluginManager.onPause(this.keepRunning);

        // If app doesn't want to run in background
        if (!this.keepRunning) {
            // Pause JavaScript timers (including setInterval)
            this.appView.pauseTimers();
        }
    }
    
    @Override
    /**
     * Called when the activity receives a new intent
     **/
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        //Forward to plugins
        this.pluginManager.onNewIntent(intent);
    }

    protected void onResume()
    {
        super.onResume();
    }
    
}
