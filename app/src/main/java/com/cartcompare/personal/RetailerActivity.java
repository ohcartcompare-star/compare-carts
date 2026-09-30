package com.cartcompare.personal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.autofill.AutofillManager;
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
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class RetailerActivity extends Activity {
    private WebView webView;
    private String store = "";
    private String homeUrl = "";
    private EditText searchBox;
    private TextView title;
    private Button searchButton;

    private JSONArray queue = new JSONArray();
    private JSONArray savedQuotes = new JSONArray();
    private int currentIndex = 0;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);

        String incomingStore = getIntent().getStringExtra("store");
        String incomingUrl = getIntent().getStringExtra("url");
        store = incomingStore == null ? "" : incomingStore;
        homeUrl = incomingUrl == null ? "" : incomingUrl;

        loadQueueFromIntent();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(6, 4, 6, 4);

        Button done = new Button(this);
        done.setText("Done");

        title = new TextView(this);
        title.setTextSize(15);
        title.setPadding(8, 0, 8, 0);

        Button skip = new Button(this);
        skip.setText("Skip");

        Button save = new Button(this);
        save.setText("Save");

        bar.addView(done, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        bar.addView(title, new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1
        ));
        bar.addView(skip, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        bar.addView(save, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        root.addView(bar);

        LinearLayout searchBar = new LinearLayout(this);
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        searchBar.setPadding(8, 2, 8, 6);

        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setHint("Search this store");

        searchButton = new Button(this);
        searchButton.setText("Search");

        searchBar.addView(searchBox, new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1
        ));
        searchBar.addView(searchButton, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        root.addView(searchBar);

        // Autofill is controlled by Android's selected password manager.
        // This help link never reads retailer credentials or login form values.
        TextView loginHelp = new TextView(this);
        loginHelp.setText("🔑 Saved-password autofill help");
        loginHelp.setTextColor(Color.rgb(22, 78, 112));
        loginHelp.setTextSize(12);
        loginHelp.setPadding(16, 5, 12, 9);
        loginHelp.setOnClickListener(v -> showAutofillHelp());
        root.addView(loginHelp);

        webView = new WebView(this);
        root.addView(webView, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1
        ));
        setContentView(root);

        // Allow the WebView's actual website login fields to participate in
        // Android Autofill. Do NOT attempt to inspect or store passwords.
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
        }

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        CookieManager.getInstance().setAcceptCookie(true);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Preserve retailer sessions where the site allows it.
                CookieManager.getInstance().flush();
            }
        });

        done.setOnClickListener(v -> finishSession());
        skip.setOnClickListener(v -> skipCurrentItem());
        save.setOnClickListener(v -> extractProductThenShowDialog());

        searchButton.setOnClickListener(v -> searchCurrentText());

        searchBox.setOnEditorActionListener((v, actionId, event) -> {
            searchCurrentText();
            return true;
        });

        loadCurrentItem(false);
    }

    private void showAutofillHelp() {
        String status = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillManager manager = (AutofillManager) getSystemService(Context.AUTOFILL_SERVICE);
            status = (manager != null && manager.isEnabled())
                ? "Your phone has an autofill service enabled.\\n\\n"
                : "No enabled autofill service was detected.\\n\\n";
        }
        new AlertDialog.Builder(this)
            .setTitle("Use your saved retailer passwords")
            .setMessage(status.replace("\\\\n", "\\n") +
                "On the retailer's sign-in page, tap its username or password box. " +
                "Choose your phone's password manager if Android offers it. " +
                "For Google passwords, set Google as your preferred autofill service in " +
                "Samsung Settings > General management > Passwords, passkeys and autofill.\\n\\n" +
                "Some retailers or password managers do not support sign-in in this " +
                "in-app browser. CartCompare never reads or saves your passwords.")
            .setPositiveButton("OK", null)
            .show();
    }

    @Override protected void onPause() {
        CookieManager.getInstance().flush();
        super.onPause();
    }

    private void loadQueueFromIntent() {
        try {
            String queueJson = getIntent().getStringExtra("queueJson");
            if (queueJson != null && !queueJson.trim().isEmpty()) {
                queue = new JSONArray(queueJson);
            }
        } catch (Exception ignored) {
            queue = new JSONArray();
        }

        if (queue.length() == 0) {
            try {
                String itemId = getIntent().getStringExtra("itemId");
                String itemName = getIntent().getStringExtra("itemName");

                JSONObject item = new JSONObject();
                item.put("itemId", itemId == null ? "" : itemId);
                item.put("itemName", itemName == null ? "Price check" : itemName);
                item.put("searchTerm", itemName == null ? "" : itemName);
                queue.put(item);
            } catch (Exception ignored) {}
        }
    }

    private JSONObject currentItem() {
        return queue.optJSONObject(currentIndex);
    }

    private String currentItemId() {
        JSONObject item = currentItem();
        return item == null ? "" : item.optString("itemId", "");
    }

    private String currentItemName() {
        JSONObject item = currentItem();
        return item == null ? "Price check" : item.optString("itemName", "Price check");
    }

    private String currentSearchTerm() {
        JSONObject item = currentItem();
        if (item == null) return "";
        String term = item.optString("searchTerm", "");
        if (term.trim().isEmpty()) term = item.optString("itemName", "");
        return term;
    }

    private void loadCurrentItem(boolean announce) {
        if (currentIndex >= queue.length()) {
            finishSession();
            return;
        }

        String name = currentItemName();
        String term = currentSearchTerm();

        title.setText(store + " • " + (currentIndex + 1) + "/" + queue.length());
        searchBox.setText(term);

        if (announce) {
            Toast.makeText(this, "Next: " + name, Toast.LENGTH_SHORT).show();
        }

        if (!term.trim().isEmpty()) {
            webView.stopLoading();
            webView.loadUrl(buildSearchUrl(store, term));
        } else if (!homeUrl.isEmpty()) {
            webView.loadUrl(homeUrl);
        }
    }

    private void searchCurrentText() {
        String q = searchBox.getText().toString().trim();
        if (q.isEmpty()) return;

        searchButton.setText("Loading…");
        webView.loadUrl(buildSearchUrl(store, q));
        webView.postDelayed(() -> searchButton.setText("Search"), 1500);
    }

    private void skipCurrentItem() {
        String skipped = currentItemName();
        Toast.makeText(this, "Skipped: " + skipped, Toast.LENGTH_SHORT).show();
        currentIndex++;
        loadCurrentItem(true);
    }

    private void advanceAfterSave() {
        currentIndex++;
        loadCurrentItem(true);
    }

    private void finishSession() {
        Intent result = new Intent();
        result.putExtra("quotesJson", savedQuotes.toString());
        setResult(RESULT_OK, result);
        finish();
    }

    private String enc(String q) {
        try {
            return URLEncoder.encode(q, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return q.replace(" ", "+");
        }
    }

    private String buildSearchUrl(String storeName, String query) {
        String q = enc(query);

        switch (storeName) {
            case "Kroger":
                return "https://www.kroger.com/search?query=" + q;
            case "Walmart":
                return "https://www.walmart.com/search?q=" + q;
            case "Target":
                return "https://www.target.com/s?searchTerm=" + q;
            case "Meijer":
                return "https://www.meijer.com/shopping/search.html?text=" + q;
            case "Aldi":
                return "https://shop.aldi.us/store/aldi/s?k=" + q;
            case "Giant Eagle":
                return "https://www.gianteagle.com/grocery/search?q=" + q;
            case "Costco":
                return "https://sameday.costco.com/store/costco/s?k=" + q;
            case "Sam’s Club":
                return "https://www.samsclub.com/c/kp/" + q;
            case "BJ’s":
                return "https://www.bjs.com/search/?search=" + q;
            default:
                return homeUrl;
        }
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        return e;
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
            JSONObject auto = new JSONObject();

            try {
                Object decoded = new JSONTokener(value).nextValue();
                String raw = decoded instanceof String ? (String)decoded : value;
                if (raw != null && !raw.equals("null")) {
                    auto = new JSONObject(raw);
                }
            } catch (Exception ignored) {}

            showSaveDialog(auto);
        });
    }

    private void showSaveDialog(JSONObject auto) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        EditText brand = field("Brand");
        EditText product = field("Exact product");
        EditText size = field("Package size");
        EditText price = field("Price");
        price.setInputType(
            InputType.TYPE_CLASS_NUMBER |
            InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        EditText note = field("Sale/member note (optional)");

        brand.setText(auto.optString("brand", ""));
        product.setText(auto.optString("product", ""));
        size.setText(auto.optString("size", ""));
        price.setText(auto.optString("price", ""));

        box.addView(brand);
        box.addView(product);
        box.addView(size);
        box.addView(price);
        box.addView(note);

        String message =
            (auto.optString("product", "").isEmpty() &&
             auto.optString("price", "").isEmpty())
            ? "I could not read product details from this page. Open the exact product page, or enter the missing details manually."
            : "Review the details I found. When you save, CartCompare will automatically search the next item in this same store.";

        new AlertDialog.Builder(this)
            .setTitle("Save " + store + " price")
            .setMessage(message)
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save & Next", (d, w) -> {
                try {
                    JSONObject q = new JSONObject();
                    q.put("itemId", currentItemId());
                    q.put("itemName", currentItemName());
                    q.put("store", store);
                    q.put("brand", brand.getText().toString().trim());
                    q.put("product", product.getText().toString().trim());
                    q.put("size", size.getText().toString().trim());
                    q.put(
                        "price",
                        Double.parseDouble(price.getText().toString().trim())
                    );
                    q.put("note", note.getText().toString().trim());
                    q.put(
                        "pageUrl",
                        webView.getUrl() == null ? "" : webView.getUrl()
                    );
                    q.put("capturedAt", System.currentTimeMillis());

                    savedQuotes.put(q);
                    Toast.makeText(
                        this,
                        "Saved: " + currentItemName(),
                        Toast.LENGTH_SHORT
                    ).show();

                    advanceAfterSave();
                } catch (Exception ex) {
                    new AlertDialog.Builder(this)
                        .setMessage("Please check the price and enter a valid numeric price.")
                        .setPositiveButton("OK", null)
                        .show();
                }
            })
            .show();
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            finishSession();
        }
    }
}
