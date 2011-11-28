package com.phonegap;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebSettings.LayoutAlgorithm;


public class PhoneGapView extends WebView {

    GapClient appCode;
    
    public PhoneGapView(Context context)
    {
        super(context);
        init();
    }
    
    public PhoneGapView(Context context, AttributeSet attrs, int defStyle,
            boolean privateBrowsing) {
        super(context, attrs, defStyle, privateBrowsing);
        init();
    }
    
    @SuppressWarnings("deprecation")
    public void init()
    {
      //14 is Ice Cream Sandwich!
        if(android.os.Build.VERSION.SDK_INT < 14)
        {
            //This hack fixes legacy PhoneGap apps
            //We should be using real pixels, not pretend pixels
            final float scale = getResources().getDisplayMetrics().density;
            int initialScale = (int) (scale * 100);
            setInitialScale(initialScale);
        }
        else
        {
            setInitialScale(0);
        }
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
    }

}
