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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class RetailerActivity extends Activity {
    private WebView webView;
    private String store, itemId, itemName, homeUrl;
    private EditText searchBox;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        store=getIntent().getStringExtra("store");
        itemId=getIntent().getStringExtra("itemId");
        itemName=getIntent().getStringExtra("itemName");
        homeUrl=getIntent().getStringExtra("url");

        if (store == null) store = "";
        if (itemId == null) itemId = "";
        if (itemName == null) itemName = "";
        if (homeUrl == null) homeUrl = "";

        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout bar=new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        Button back=new Button(this); back.setText("←");
        TextView title=new TextView(this); title.setText(store); title.setTextSize(16);
        Button save=new Button(this); save.setText("Save price");
        bar.addView(back,new LinearLayout.LayoutParams(60,ViewGroup.LayoutParams.WRAP_CONTENT));
        bar.addView(title,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        bar.addView(save);
        root.addView(bar);

        LinearLayout searchBar=new LinearLayout(this);
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        searchBar.setPadding(8,2,8,6);
        searchBox=new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setHint("Search this store");
        if (!itemId.isEmpty() && !itemName.equals("Price check")) searchBox.setText(itemName);
        Button searchButton=new Button(this); searchButton.setText("Search");
        searchBar.addView(searchBox,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        searchBar.addView(searchButton,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(searchBar);

        webView=new WebView(this);
        root.addView(webView,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        setContentView(root);

        WebSettings s=webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);

        CookieManager.getInstance().setAcceptCookie(true);
        if(android.os.Build.VERSION.SDK_INT>=21) CookieManager.getInstance().setAcceptThirdPartyCookies(webView,true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        // When opened from a shopping-list item, go straight to search results.
        if (!itemId.isEmpty() && !itemName.isEmpty() && !itemName.equals("Price check")) {
            webView.loadUrl(buildSearchUrl(store, itemName));
        } else {
            webView.loadUrl(homeUrl);
        }

        back.setOnClickListener(v->{ if(webView.canGoBack()) webView.goBack(); else finish(); });
        searchButton.setOnClickListener(v -> {
            String q=searchBox.getText().toString().trim();
            if(!q.isEmpty()) webView.loadUrl(buildSearchUrl(store,q));
        });
        searchBox.setOnEditorActionListener((v, actionId, event) -> {
            String q=searchBox.getText().toString().trim();
            if(!q.isEmpty()) webView.loadUrl(buildSearchUrl(store,q));
            return true;
        });
        save.setOnClickListener(v->showSaveDialog());
    }

    private String enc(String q) {
        try { return URLEncoder.encode(q, StandardCharsets.UTF_8.toString()); }
        catch(Exception e) { return q.replace(" ","+"); }
    }

    private String buildSearchUrl(String storeName, String query) {
        String q=enc(query);
        switch(storeName) {
            case "Kroger":
                return "https://www.kroger.com/search?query="+q;
            case "Walmart":
                return "https://www.walmart.com/search?q="+q;
            case "Target":
                return "https://www.target.com/s?searchTerm="+q;
            case "Meijer":
                return "https://www.meijer.com/shopping/search.html?text="+q;
            case "Aldi":
                return "https://www.aldi.us/results?q="+q;
            case "Giant Eagle":
                return "https://www.gianteagle.com/grocery/search?q="+q;
            case "Costco":
                return "https://www.costco.com/CatalogSearch?dept=All&keyword="+q;
            case "Sam’s Club":
                return "https://www.samsclub.com/s/"+q;
            case "BJ’s":
                // BJ's search URL changes more often, so use its site search page.
                return "https://www.bjs.com/search/?search="+q;
            default:
                return homeUrl;
        }
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
                    q.put("pageUrl",webView.getUrl()==null?"":webView.getUrl());
                    q.put("capturedAt",System.currentTimeMillis());
                    Intent out=new Intent(); out.putExtra("quoteJson",q.toString());
                    setResult(RESULT_OK,out); finish();
                }catch(Exception ex){
                    new AlertDialog.Builder(this).setMessage("Enter a valid numeric price.")
                        .setPositiveButton("OK",null).show();
                }
            }).show();
    }
}
