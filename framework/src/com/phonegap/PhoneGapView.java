package com.phonegap;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebSettings.LayoutAlgorithm;


public class PhoneGapView extends WebView {

    GapClient appCode;
    PhoneGapClient viewClient;
    Activity app;
    
    public PhoneGapView(Context context)
    {
        super(context);
        app = (Activity) context;
        init();
    }
    
    public PhoneGapView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        app = (Activity) context;
        init();
    }
    
    public PhoneGapView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        app = (Activity) context;
        init();
    }
    
    public PhoneGapView(Context context, AttributeSet attrs, int defStyle,
            boolean privateBrowsing) {
        super(context, attrs, defStyle, privateBrowsing);
        init();
    }
    
    public void onDestroy()
    {
        appCode.onDestroy();
    }
    
    @SuppressWarnings("deprecation")
    public void init()
    {
        setInitialScale(0);
        setVerticalScrollBarEnabled(false);
        requestFocusFromTouch();
        // Enable JavaScript
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
        
        //Set the nav dump for HTC
        settings.setNavDump(true);

        // Enable database
        settings.setDatabaseEnabled(true);
        String databasePath = getContext().getDir("database", Context.MODE_PRIVATE).getPath(); 
        settings.setDatabasePath(databasePath);

        // Enable DOM storage
        WebViewReflect.setDomStorage(settings);
        
        // Enable built-in geolocation
        WebViewReflect.setGeolocationEnabled(settings, true);
        
        //Initalize the other parts of the application
        appCode = new GapClient(this, this.getContext());
        viewClient = new PhoneGapClient(app, this);
        
    }

}
