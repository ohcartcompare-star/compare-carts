package com.cartcompare.personal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;

public class RetailerActivity extends Activity {
    private WebView webView;
    private String store, itemId, itemName;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        store=getIntent().getStringExtra("store");
        itemId=getIntent().getStringExtra("itemId");
        itemName=getIntent().getStringExtra("itemName");
        String url=getIntent().getStringExtra("url");

        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout bar=new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        Button back=new Button(this); back.setText("←");
        TextView title=new TextView(this); title.setText(store+" · "+itemName); title.setTextSize(16);
        Button save=new Button(this); save.setText("Save price");
        bar.addView(back,new LinearLayout.LayoutParams(60,ViewGroup.LayoutParams.WRAP_CONTENT));
        bar.addView(title,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        bar.addView(save);
        root.addView(bar);

        webView=new WebView(this);
        root.addView(webView,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        setContentView(root);

        WebSettings s=webView.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        if(android.os.Build.VERSION.SDK_INT>=21) CookieManager.getInstance().setAcceptThirdPartyCookies(webView,true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(url);

        back.setOnClickListener(v->{ if(webView.canGoBack()) webView.goBack(); else finish(); });
        save.setOnClickListener(v->showSaveDialog());
    }

    private EditText field(String hint){
        EditText e=new EditText(this); e.setHint(hint); return e;
    }

    private void showSaveDialog(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        EditText brand=field("Brand");
        EditText product=field("Exact product");
        EditText size=field("Package size");
        EditText price=field("Price");
        price.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText note=field("Sale/member note (optional)");
        box.addView(brand); box.addView(product); box.addView(size); box.addView(price); box.addView(note);

        new AlertDialog.Builder(this)
            .setTitle("Save "+store+" price")
            .setMessage("Enter exactly what you see. CartCompare never asks for your retailer password.")
            .setView(box)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Save",(d,w)->{
                try{
                    JSONObject q=new JSONObject();
                    q.put("itemId",itemId); q.put("itemName",itemName); q.put("store",store);
                    q.put("brand",brand.getText().toString().trim());
                    q.put("product",product.getText().toString().trim());
                    q.put("size",size.getText().toString().trim());
                    q.put("price",Double.parseDouble(price.getText().toString().trim()));
                    q.put("note",note.getText().toString().trim());
                    Intent out=new Intent(); out.putExtra("quoteJson",q.toString());
                    setResult(RESULT_OK,out); finish();
                }catch(Exception ex){
                    new AlertDialog.Builder(this).setMessage("Enter a valid numeric price.")
                        .setPositiveButton("OK",null).show();
                }
            }).show();
    }
}
