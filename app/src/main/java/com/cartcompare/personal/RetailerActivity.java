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
import org.json.JSONTokener;
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
        save.setOnClickListener(v->extractProductThenShowDialog());
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

    private void extractProductThenShowDialog() {
        String js =
            "(function(){try{" +
            "var out={brand:'',product:'',size:'',price:'',note:'',error:''};" +
            "function clean(v){return (v==null?'':String(v)).replace(/\\s+/g,' ').trim();}" +
            "function meta(sel){var e=document.querySelector(sel);return e?clean(e.getAttribute('content')||e.content||e.innerText||''):'';}" +
            "function text(sel){var e=document.querySelector(sel);return e?clean(e.innerText||e.textContent||''):'';}" +
            "function brandVal(v){if(!v)return '';if(typeof v==='string')return clean(v);if(v.name)return clean(v.name);return '';}" +
            "function priceVal(v){if(v==null)return '';var m=clean(v).match(/(?:\\$\\s*)?(\\d{1,5}(?:,\\d{3})*(?:\\.\\d{2}))/);return m?m[1].replace(/,/g,''):'';}" +
            "function sizeVal(v){var m=clean(v).match(/(\\d+(?:\\.\\d+)?\\s*(?:fl\\s*oz|oz|lb|lbs|pounds?|ct|count|pk|pack|gal|gallon|qt|pt|ml|kg|g|l)\\b(?:\\s*[x×]\\s*\\d+)?)/i);return m?clean(m[1]):'';}" +
            "function absorb(p){if(!p||typeof p!=='object')return;" +
              "if(!out.product)out.product=clean(p.name||p.title||'');" +
              "if(!out.brand)out.brand=brandVal(p.brand||p.manufacturer);" +
              "if(!out.size)out.size=clean(p.size||p.weight||'');" +
              "var o=p.offers;if(Array.isArray(o))o=o[0];" +
              "if(o&&typeof o==='object'&&!out.price)out.price=priceVal(o.price||o.lowPrice||o.highPrice);" +
              "if(!out.price)out.price=priceVal(p.price);" +
            "}" +
            "function walk(v,depth){if(!v||depth>8)return;if(Array.isArray(v)){v.forEach(function(x){walk(x,depth+1);});return;}if(typeof v!=='object')return;" +
              "var t=v['@type'];if(t==='Product'||(Array.isArray(t)&&t.indexOf('Product')>=0))absorb(v);" +
              "Object.keys(v).forEach(function(k){var x=v[k];if(x&&typeof x==='object')walk(x,depth+1);});" +
            "}" +
            "document.querySelectorAll('script[type=\"application/ld+json\"]').forEach(function(s){try{walk(JSON.parse(s.textContent),0);}catch(e){}});" +
            "var next=document.querySelector('script#__NEXT_DATA__');if(next){try{walk(JSON.parse(next.textContent),0);}catch(e){}}" +
            "if(!out.product)out.product=meta('meta[property=\"og:title\"]')||meta('meta[name=\"twitter:title\"]')||text('h1')||clean(document.title);" +
            "if(!out.brand)out.brand=meta('meta[property=\"product:brand\"]')||meta('meta[name=\"brand\"]')||text('[itemprop=\"brand\"]')||text('[data-testid*=\"brand\"]')||text('[data-automation-id*=\"brand\"]');" +
            "if(!out.price)out.price=priceVal(meta('meta[itemprop=\"price\"]')||meta('meta[property=\"product:price:amount\"]')||text('[itemprop=\"price\"]')||text('[data-automation-id=\"product-price\"]')||text('[data-testid*=\"price\"]')||text('[class*=\"price\"]'));" +
            "var desc=meta('meta[name=\"description\"]')||meta('meta[property=\"og:description\"]');" +
            "if(!out.size)out.size=sizeVal(out.product+' '+desc);" +
            "if(!out.price){var body=clean(document.body?document.body.innerText:'').slice(0,12000);var pm=body.match(/\\$\\s*(\\d{1,5}(?:,\\d{3})*(?:\\.\\d{2}))/);if(pm)out.price=pm[1].replace(/,/g,'');}" +
            "if(!out.size){var body2=clean(document.body?document.body.innerText:'').slice(0,12000);out.size=sizeVal(body2);}" +
            "return JSON.stringify(out);" +
            "}catch(e){return JSON.stringify({brand:'',product:'',size:'',price:'',note:'',error:String(e&&e.message?e.message:e)});}})()";

        webView.evaluateJavascript(js, value -> {
            JSONObject auto=new JSONObject();
            try {
                Object decoded=new JSONTokener(value).nextValue();
                String raw=decoded instanceof String ? (String)decoded : value;
                if(raw!=null && !raw.equals("null")) auto=new JSONObject(raw);
            } catch(Exception ignored) {}
            showSaveDialog(auto);
        });
    }

    private void showSaveDialog(JSONObject auto){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        EditText brand=field("Brand");
        EditText product=field("Exact product");
        EditText size=field("Package size");
        EditText price=field("Price");
        price.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText note=field("Sale/member note (optional)");

        brand.setText(auto.optString("brand",""));
        product.setText(auto.optString("product",""));
        size.setText(auto.optString("size",""));
        price.setText(auto.optString("price",""));

        box.addView(brand); box.addView(product); box.addView(size); box.addView(price); box.addView(note);

        new AlertDialog.Builder(this)
            .setTitle("Save "+store+" price")
            .setMessage((auto.optString("product","").isEmpty() && auto.optString("price","").isEmpty())
                ? "I could not read product details from this page. Open the exact product page (not just search results), then tap Save price again."
                : "I filled in what I could read from this product page. Review it before saving, especially sale/member prices and package size.")
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
                    new AlertDialog.Builder(this).setMessage("Please check the extracted price and enter a valid numeric price.")
                        .setPositiveButton("OK",null).show();
                }
            }).show();
    }
}
