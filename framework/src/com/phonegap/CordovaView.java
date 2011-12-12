package com.phonegap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParserException;

import com.phonegap.api.LOG;

import android.app.Activity;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebSettings.LayoutAlgorithm;


public class CordovaView extends WebView {

    private static final String TAG = null;
    GapClient appCode;
    CordovaClient viewClient;
    Activity app;
    private boolean classicRender;
    private ArrayList<Pattern> whiteList = new ArrayList<Pattern>();
    private HashMap<String, Boolean> whiteListCache = new HashMap<String,Boolean>();

    
    public CordovaView(Context context)
    {
        super(context);
        app = (Activity) context;
        init();
    }
    
    public CordovaView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        app = (Activity) context;
        init();
    }
    
    public CordovaView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        app = (Activity) context;
        init();
    }
    
    public CordovaView(Context context, AttributeSet attrs, int defStyle,
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
        viewClient = new CordovaClient(app, this);
        
    }

    
    private void loadConfiguration() {
        int id = app.getResources().getIdentifier("phonegap", "xml", app.getPackageName());
        if (id == 0) {
            LOG.i("PhoneGapLog", "phonegap.xml missing. Ignoring...");
            return;
        }
        XmlResourceParser xml = getResources().getXml(id);
        int eventType = -1;
        while (eventType != XmlResourceParser.END_DOCUMENT) {
            if (eventType == XmlResourceParser.START_TAG) {
                String strNode = xml.getName();
                if (strNode.equals("access")) {
                    String origin = xml.getAttributeValue(null, "origin");
                    String subdomains = xml.getAttributeValue(null, "subdomains");
                    if (origin != null) {
                        this.addWhiteListEntry(origin, (subdomains != null) && (subdomains.compareToIgnoreCase("true") == 0));
                    }
                }
                else if (strNode.equals("log")) {
                    String level = xml.getAttributeValue(null, "level");
                    LOG.i("PhoneGapLog", "Found log level %s", level);
                    if (level != null) {
                        LOG.setLogLevel(level);
                    }
                }
                else if(strNode.equals("render")) {
                    String enabled = xml.getAttributeValue(null, "enabled");
                    if(enabled != null)
                    {
                        this.classicRender = enabled.equals("true");
                    }
                    
                }
            }
            try {
                eventType = xml.next();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Add entry to approved list of URLs (whitelist)
     * 
     * @param origin        URL regular expression to allow
     * @param subdomains    T=include all subdomains under origin
     */
    public void addWhiteListEntry(String origin, boolean subdomains) {
      try {
        // Unlimited access to network resources
        if(origin.compareTo("*") == 0) {
            LOG.d(TAG, "Unlimited access to network resources");
            whiteList.add(Pattern.compile("*"));
        } else { // specific access
          // check if subdomains should be included
          // TODO: we should not add more domains if * has already been added
          if (subdomains) {
              // XXX making it stupid friendly for people who forget to include protocol/SSL
              if(origin.startsWith("http")) {
                whiteList.add(Pattern.compile(origin.replaceFirst("https{0,1}://", "^https{0,1}://.*")));
              } else {
                whiteList.add(Pattern.compile("^https{0,1}://.*"+origin));
              }
              LOG.d(TAG, "Origin to allow with subdomains: %s", origin);
          } else {
              // XXX making it stupid friendly for people who forget to include protocol/SSL
              if(origin.startsWith("http")) {
                whiteList.add(Pattern.compile(origin.replaceFirst("https{0,1}://", "^https{0,1}://")));
              } else {
                whiteList.add(Pattern.compile("^https{0,1}://"+origin));
              }
              LOG.d(TAG, "Origin to allow: %s", origin);
          }    
        }
      } catch(Exception e) {
        LOG.d(TAG, "Failed to add origin %s", origin);
      }
    }
    

    /**
     * Determine if URL is in approved list of URLs to load.
     * 
     * @param url
     * @return
     */
    private boolean isUrlWhiteListed(String url) {

        // Check to see if we have matched url previously
        if (whiteListCache.get(url) != null) {
            return true;
        }

        // Look for match in white list
        Iterator<Pattern> pit = whiteList.iterator();
        while (pit.hasNext()) {
            Pattern p = pit.next();
            Matcher m = p.matcher(url);

            // If match found, then cache it to speed up subsequent comparisons
            if (m.find()) {
                whiteListCache.put(url, true);
                return true;
            }
        }
        return false;
    }
}
