package com.entertainment24x72.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.HorizontalScrollView;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String FEED = "https://entertainment24x72.blogspot.com/feeds/posts/default?alt=json&max-results=50";
    private final ArrayList<NewsItem> all = new ArrayList<>();
    private NewsAdapter adapter;
    private TextView status;
    private ProgressBar progress;
    private LinearLayout categoryContainer;
    private String selectedCategory = "প্রচ্ছদ";
    private String searchQuery = "";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView list = findViewById(R.id.newsList);
        EditText search = findViewById(R.id.searchBox);
        status = findViewById(R.id.statusText);
        progress = findViewById(R.id.progressBar);
        categoryContainer = findViewById(R.id.categoryContainer);
        adapter = new NewsAdapter(this, new ArrayList<>());
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        findViewById(R.id.refreshButton).setOnClickListener(v -> loadFeed());
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a) {}
            public void onTextChanged(CharSequence s,int st,int before,int count) {
                searchQuery = s.toString(); filter();
            }
            public void afterTextChanged(Editable e) {}
        });
        loadFeed();
    }

    private void loadFeed() {
        progress.setVisibility(View.VISIBLE);
        status.setText("সর্বশেষ সংবাদ আপডেট হচ্ছে...");
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(FEED).openConnection();
                connection.setConnectTimeout(15000); connection.setReadTimeout(20000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                StringBuilder result = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    String line; while ((line = reader.readLine()) != null) result.append(line);
                }
                JSONObject feed = new JSONObject(result.toString()).getJSONObject("feed");
                JSONArray entries = feed.optJSONArray("entry");
                ArrayList<NewsItem> parsed = new ArrayList<>();
                if (entries != null) for (int i=0; i<entries.length(); i++) {
                    JSONObject entry = entries.getJSONObject(i);
                    String title = entry.optJSONObject("title") == null ? "শিরোনাম নেই" : entry.getJSONObject("title").optString("$t");
                    JSONObject co = entry.optJSONObject("content");
                    String content = co == null ? "" : co.optString("$t", "");
                    String summary = Jsoup.parse(content).text();
                    if (summary.length()>190) summary=summary.substring(0,190)+"…";
                    String link="";
                    JSONArray links=entry.optJSONArray("link");
                    if (links!=null) for(int j=0;j<links.length();j++) {
                        JSONObject l=links.getJSONObject(j);
                        if ("alternate".equals(l.optString("rel"))) { link=l.optString("href"); break; }
                    }
                    String image="";
                    JSONObject thumb=entry.optJSONObject("media$thumbnail");
                    if(thumb!=null) image=thumb.optString("url","").replace("s72-c","s800");
                    if(image.isEmpty()) { Element img=Jsoup.parse(content).selectFirst("img"); if(img!=null) image=img.attr("src"); }
                    String date="";
                    JSONObject pub=entry.optJSONObject("published");
                    if(pub!=null) {
                        String raw=pub.optString("$t","");
                        try { Date d=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.US).parse(raw.substring(0,19));
                            date=new SimpleDateFormat("dd MMM yyyy",Locale.ENGLISH).format(d);
                        } catch(Exception ignored) { date=raw; }
                    }
                    String category="সর্বশেষ";
                    JSONArray cats=entry.optJSONArray("category");
                    if(cats!=null && cats.length()>0) category=cats.getJSONObject(0).optString("term","সর্বশেষ");
                    parsed.add(new NewsItem(title,summary,content,link,image,date,category));
                }
                handler.post(() -> {
                    all.clear(); all.addAll(parsed);
                    buildCategories(); filter();
                    progress.setVisibility(View.GONE);
                    status.setText(parsed.size()+"টি সংবাদ পাওয়া গেছে");
                });
            } catch(Exception e) {
                handler.post(() -> {
                    progress.setVisibility(View.GONE);
                    status.setText("সংবাদ লোড করা যায়নি। আবার চেষ্টা করো।");
                    Toast.makeText(this,"ইন্টারনেট সংযোগ পরীক্ষা করো",Toast.LENGTH_SHORT).show();
                });
            } finally { if(connection!=null) connection.disconnect(); }
        });
    }

    private void buildCategories() {
        categoryContainer.removeAllViews();
        addCategory("প্রচ্ছদ");
        addCategory("সর্বশেষ");
        Set<String> categories = new LinkedHashSet<>();
        for(NewsItem n:all) if(n.category!=null && !n.category.trim().isEmpty()) categories.add(n.category);
        for(String c:categories) if(!c.equals("সর্বশেষ") && !c.equals("প্রচ্ছদ")) addCategory(c);
        updateCategoryStyles();
    }
    private void addCategory(String name) {
        TextView chip=new TextView(this); chip.setText(name); chip.setTextSize(14);
        chip.setGravity(Gravity.CENTER); chip.setPadding(dp(15),dp(9),dp(15),dp(9));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2); lp.setMargins(dp(5),dp(4),dp(5),dp(4));
        categoryContainer.addView(chip,lp);
        chip.setOnClickListener(v->{ selectedCategory=name; updateCategoryStyles(); filter(); });
        chip.setTag(name);
    }
    private void updateCategoryStyles() {
        for(int i=0;i<categoryContainer.getChildCount();i++) {
            TextView t=(TextView)categoryContainer.getChildAt(i);
            boolean active=selectedCategory.equals(t.getTag());
            GradientDrawable bg=new GradientDrawable(); bg.setCornerRadius(dp(22));
            bg.setColor(active?Color.rgb(198,0,35):Color.WHITE);
            if(!active) bg.setStroke(dp(1),0xFFE4E4E4);
            t.setBackground(bg); t.setTextColor(active?Color.WHITE:0xFF333333);
            t.setTypeface(null,active?Typeface.BOLD:Typeface.NORMAL);
        }
    }
    private void filter() {
        ArrayList<NewsItem> filtered=new ArrayList<>();
        for(NewsItem n:all) {
            boolean categoryOk=selectedCategory.equals("প্রচ্ছদ") || selectedCategory.equals("সর্বশেষ") || selectedCategory.equals(n.category);
            String q=searchQuery.toLowerCase(Locale.ROOT);
            boolean searchOk=n.title.toLowerCase(Locale.ROOT).contains(q) || n.summary.toLowerCase(Locale.ROOT).contains(q);
            if(categoryOk && searchOk) filtered.add(n);
        }
        adapter.replaceItems(filtered);
        if(filtered.isEmpty() && !all.isEmpty()) status.setText("এই বিভাগে কোনো সংবাদ পাওয়া যায়নি");
        else status.setText(filtered.size()+"টি সংবাদ");
    }
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    @Override protected void onDestroy(){super.onDestroy(); executor.shutdown();}
}
