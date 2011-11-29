package com.phonegap;

import org.json.JSONArray;
import org.json.JSONException;

import com.phonegap.api.IPlugin;
import com.phonegap.api.PhonegapActivity;
import com.phonegap.api.PluginManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.EditText;

public class GapClient extends WebChromeClient implements PhonegapActivity {
    private String TAG = "PhoneGapLog";
    private long MAX_QUOTA = 100 * 1024 * 1024;
    Activity mCtx;
    PluginManager pluginManager;
    CallbackServer callbackServer;
    WebView appView;

    public GapClient(WebView view, Context ctx)
    {
        mCtx = (Activity) ctx;
        callbackServer = new CallbackServer();
        appView = view;
        appView.setWebChromeClient(this);
        pluginManager = new PluginManager(appView, this);
    }
    
    /**
     * Tell the client to display a javascript alert dialog.
     * 
     * @param view
     * @param url
     * @param message
     * @param result
     */
    @Override
    public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
        AlertDialog.Builder dlg = new AlertDialog.Builder(this.mCtx);
        dlg.setMessage(message);
        dlg.setTitle("Alert");
        //Don't let alerts break the back button
        dlg.setCancelable(true);
        dlg.setPositiveButton(android.R.string.ok,
            new AlertDialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            });
        dlg.setOnCancelListener(
           new DialogInterface.OnCancelListener() {
               public void onCancel(DialogInterface dialog) {
                   result.confirm();
                   }
               });
        dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
            //DO NOTHING
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_BACK)
                {
                    result.confirm();
                    return false;
                }
                else
                    return true;
                }
            });
        dlg.create();
        dlg.show();
        return true;
    }

    /**
     * Tell the client to display a confirm dialog to the user.
     * 
     * @param view
     * @param url
     * @param message
     * @param result
     */
    @Override
    public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
        AlertDialog.Builder dlg = new AlertDialog.Builder(this.mCtx);
        dlg.setMessage(message);
        dlg.setTitle("Confirm");
        dlg.setCancelable(true);
        dlg.setPositiveButton(android.R.string.ok, 
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            });
        dlg.setNegativeButton(android.R.string.cancel, 
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    result.cancel();
                }
            });
        dlg.setOnCancelListener(
            new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    result.cancel();
                    }
                });
        dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
            //DO NOTHING
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_BACK)
                {
                    result.cancel();
                    return false;
                }
                else
                    return true;
                }
            });
        dlg.create();
        dlg.show();
        return true;
    }

    /**
     * Tell the client to display a prompt dialog to the user. 
     * If the client returns true, WebView will assume that the client will 
     * handle the prompt dialog and call the appropriate JsPromptResult method.
     * 
     * Since we are hacking prompts for our own purposes, we should not be using them for 
     * this purpose, perhaps we should hack console.log to do this instead!
     * 
     * @param view
     * @param url
     * @param message
     * @param defaultValue
     * @param result
     */
    @Override
    public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
        
        // Security check to make sure any requests are coming from the page initially
        // loaded in webview and not another loaded in an iframe.
        boolean reqOk = false;
//        if (url.startsWith("file://") || isUrlWhiteListed(url)) {
        if (url.startsWith("file://")) {
            reqOk = true;
        }
        
        // Calling PluginManager.exec() to call a native service using 
        // prompt(this.stringify(args), "gap:"+this.stringify([service, action, callbackId, true]));
        if (reqOk && defaultValue != null && defaultValue.length() > 3 && defaultValue.substring(0, 4).equals("gap:")) {
            JSONArray array;
            try {
                array = new JSONArray(defaultValue.substring(4));
                String service = array.getString(0);
                String action = array.getString(1);
                String callbackId = array.getString(2);
                boolean async = array.getBoolean(3);
                String r = pluginManager.exec(service, action, callbackId, message, async);
                result.confirm(r);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        // Polling for JavaScript messages 
        else if (reqOk && defaultValue != null && defaultValue.equals("gap_poll:")) {
            String r = callbackServer.getJavascript();
            result.confirm(r);
        }
        
        // Calling into CallbackServer
        else if (reqOk && defaultValue != null && defaultValue.equals("gap_callbackServer:")) {
            String r = "";
            if (message.equals("usePolling")) {
                r = ""+callbackServer.usePolling();
            }
            else if (message.equals("restartServer")) {
                callbackServer.restartServer();
            }
            else if (message.equals("getPort")) {
                r = Integer.toString(callbackServer.getPort());
            }
            else if (message.equals("getToken")) {
                r = callbackServer.getToken();
            }
            result.confirm(r);
        }
        
        // PhoneGap JS has initialized, so show webview
        // (This solves white flash seen when rendering HTML)
        else if (reqOk && defaultValue != null && defaultValue.equals("gap_init:")) {
            appView.setVisibility(View.VISIBLE);
            result.confirm("OK");
        }

        // Show dialog
        else {
            final JsPromptResult res = result;
            AlertDialog.Builder dlg = new AlertDialog.Builder(this.mCtx);
            dlg.setMessage(message);
            final EditText input = new EditText(this.mCtx);
            if (defaultValue != null) {
                input.setText(defaultValue);
            }
            dlg.setView(input);
            dlg.setCancelable(false);
            dlg.setPositiveButton(android.R.string.ok, 
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    String usertext = input.getText().toString();
                    res.confirm(usertext);
                }
            });
            dlg.setNegativeButton(android.R.string.cancel, 
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    res.cancel();
                }
            });
            dlg.create();
            dlg.show();
        }
        return true;
    }

    public void sendJavascript(String statement) {
        this.callbackServer.sendJavascript(statement);
    }

    public void startActivityForResult(IPlugin command, Intent intent,
            int requestCode) {
        // TODO Auto-generated method stub
        
    }

    public void setActivityResultCallback(IPlugin plugin) {
        // TODO Auto-generated method stub
        
    }

    public void loadUrl(String url) {
        appView.loadUrl(url);
    }

    public void postMessage(String id, Object data) {
        pluginManager.postMessage(id, data);
    }

    public Resources getResources() {
        return mCtx.getResources();
    }

    public String getPackageName() {
        return mCtx.getPackageName();
    }

    public Object getSystemService(String service) {
        // TODO Auto-generated method stub
        return mCtx.getSystemService(service);
    }
    
    public Context getContext()
    {
        return mCtx;
    }
    
    public Context getBaseContext() {
        return ((PhonegapActivity) mCtx).getBaseContext();
    }

    public Intent registerReceiver(BroadcastReceiver receiver,
            IntentFilter intentFilter) {
        return mCtx.registerReceiver(receiver, intentFilter);
    }

    public ContentResolver getContentResolver() {
        // TODO Auto-generated method stub
        return mCtx.getContentResolver();
    }

    public void unregisterReceiver(BroadcastReceiver receiver) {
        // TODO Auto-generated method stub
        mCtx.unregisterReceiver(receiver);
    }

    public Cursor managedQuery(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        // TODO Auto-generated method stub
        return mCtx.managedQuery(uri, projection, selection, selectionArgs, sortOrder);
    }

    public void runOnUiThread(Runnable runnable) {
        // TODO Auto-generated method stub
        mCtx.runOnUiThread(runnable);
    }

    public AssetManager getAssets() {
        // TODO Auto-generated method stub
        return mCtx.getAssets();
    }

    public Cursor managedQuery(Uri parse, String[] strings, Object object,
            Object object2, Object object3) {
        // TODO Auto-generated method stub
        return null;
    }
}
