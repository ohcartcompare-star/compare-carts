package com.cartcompare.personal;

import android.app.Activity;
import android.print.PrintManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int RETAILER_REQUEST = 711;
    private WebView webView;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new Bridge(), "Android");
        webView.loadUrl("file:///android_asset/index.html");
    }

    public class Bridge {
        @JavascriptInterface public void openRetailer(String store, String url, String itemId, String itemName) {
            runOnUiThread(() -> {
                Intent i = new Intent(MainActivity.this, RetailerActivity.class);
                i.putExtra("store", store); i.putExtra("url", url);
                i.putExtra("itemId", itemId); i.putExtra("itemName", itemName);
                startActivityForResult(i, RETAILER_REQUEST);
            });
        }
        @JavascriptInterface public void toast(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }
        @JavascriptInterface public void shareText(String subject, String content) {
            runOnUiThread(() -> {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_SUBJECT, subject);
                share.putExtra(Intent.EXTRA_TEXT, content);
                startActivity(Intent.createChooser(share, "Share CartCompare list"));
            });
        }
        @JavascriptInterface public void printPage() {
            runOnUiThread(() -> {
                PrintManager pm = (PrintManager)getSystemService(Context.PRINT_SERVICE);
                if (pm != null) pm.print("CartCompare Shopping List",
                    webView.createPrintDocumentAdapter("CartCompare Shopping List"), null);
            });
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RETAILER_REQUEST && resultCode == RESULT_OK && data != null) {
            String json = data.getStringExtra("quoteJson");
            if (json != null) {
                String safe = json.replace("\\","\\\\").replace("'","\\'").replace("\n","\\n");
                webView.evaluateJavascript("window.receiveRetailerQuote(JSON.parse('" + safe + "'));", null);
            }
        }
    }
}
