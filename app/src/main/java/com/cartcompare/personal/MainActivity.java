package com.cartcompare.personal;

import android.app.Activity;
import android.print.PrintManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

public class MainActivity extends Activity {
    private static final int RETAILER_REQUEST = 711;
    private static final int EXPORT_CSV_REQUEST = 712;
    private WebView webView;
    private String pendingCsv = "";

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

        @JavascriptInterface public void scanBarcode() {
            runOnUiThread(() -> {
                GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8)
                    .enableAutoZoom()
                    .build();

                GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(MainActivity.this, options);
                scanner.startScan()
                    .addOnSuccessListener(barcode -> {
                        String raw = barcode.getRawValue();
                        if (raw != null && !raw.trim().isEmpty()) {
                            webView.evaluateJavascript("window.receiveBarcode(" + JSONObject.quote(raw.trim()) + ");", null);
                        } else {
                            Toast.makeText(MainActivity.this, "No barcode value found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnCanceledListener(() -> {})
                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this,
                        "Barcode scanner could not start. Try again in a moment.", Toast.LENGTH_LONG).show());
            });
        }

        @JavascriptInterface public void exportCsv(String filename, String content) {
            runOnUiThread(() -> {
                pendingCsv = content == null ? "" : content;
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("text/csv");
                i.putExtra(Intent.EXTRA_TITLE, (filename == null || filename.trim().isEmpty())
                    ? "CartCompare-Shopping-List.csv" : filename);
                startActivityForResult(i, EXPORT_CSV_REQUEST);
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
            return;
        }

        if (requestCode == EXPORT_CSV_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out != null) {
                        out.write(pendingCsv.getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        Toast.makeText(this, "CSV exported", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Could not export CSV", Toast.LENGTH_LONG).show();
                }
            }
            pendingCsv = "";
        }
    }
}
