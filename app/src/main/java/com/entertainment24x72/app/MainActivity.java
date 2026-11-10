
package com.entertainment24x72.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String FEED =
            "https://entertainment24x72.blogspot.com/feeds/posts/default?alt=json&max-results=50";

    private final ArrayList<NewsItem> all = new ArrayList<>();
    private NewsAdapter adapter;
    private TextView status;
    private ProgressBar progress;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView list = findViewById(R.id.newsList);
        EditText search = findViewById(R.id.searchBox);

        status = findViewById(R.id.statusText);
        progress = findViewById(R.id.progressBar);

        adapter = new NewsAdapter(this, all);

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadFeed();
    }

    private void loadFeed() {

        progress.setVisibility(View.VISIBLE);
        status.setText("সংবাদ লোড হচ্ছে...");

        executor.execute(() -> {

            try {
                HttpURLConnection connection =
                        (HttpURLConnection) new URL(FEED).openConnection();

                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                StringBuilder result = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                connection.getInputStream(), "UTF-8"))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                } finally {
                    connection.disconnect();
                }

                JSONObject feed = new JSONObject(result.toString())
                        .getJSONObject("feed");

                JSONArray entries = feed.optJSONArray("entry");

                ArrayList<NewsItem> parsed = new ArrayList<>();

                if (entries != null) {

                    for (int i = 0; i < entries.length(); i++) {

                        JSONObject entry = entries.getJSONObject(i);

                        String title = entry.getJSONObject("title")
                                .optString("$t", "শিরোনাম নেই");

                        JSONObject contentObject =
                                entry.optJSONObject("content");

                        String content = contentObject != null
                                ? contentObject.optString("$t", "")
                                : "";

                        String summary = Jsoup.parse(content).text();

                        if (summary.length() > 180) {
                            summary = summary.substring(0, 180) + "...";
                        }

                        String link = "";
                        JSONArray links = entry.optJSONArray("link");

                        if (links != null) {
                            for (int j = 0; j < links.length(); j++) {
                                JSONObject item = links.getJSONObject(j);

                                if ("alternate".equals(
                                        item.optString("rel"))) {
                                    link = item.optString("href");
                                    break;
                                }
                            }
                        }

                        String image = "";

                        JSONObject thumbnail =
                                entry.optJSONObject("media$thumbnail");

                        if (thumbnail != null) {
                            image = thumbnail.optString("url", "")
                                    .replace("s72-c", "s640");
                        }

                        if (image.isEmpty()) {
                            Document document = Jsoup.parse(content);
                            Element img = document.selectFirst("img");

                            if (img != null) {
                                image = img.attr("src");
                            }
                        }

                        String date = "";

                        JSONObject published =
                                entry.optJSONObject("published");

                        if (published != null) {
                            date = published.optString("$t", "");

                            try {
                                Date parsedDate = new SimpleDateFormat(
                                        "yyyy-MM-dd'T'HH:mm:ss",
                                        Locale.US
                                ).parse(date.substring(0, 19));

                                date = new SimpleDateFormat(
                                        "dd MMM yyyy",
                                        Locale.getDefault()
                                ).format(parsedDate);

                            } catch (Exception ignored) {}
                        }

                        String category = "সর্বশেষ";

                        JSONArray categories =
                                entry.optJSONArray("category");

                        if (categories != null &&
                                categories.length() > 0) {
                            category = categories.getJSONObject(0)
                                    .optString("term", "সর্বশেষ");
                        }

                        parsed.add(new NewsItem(
                                title,
                                summary,
                                content,
                                link,
                                image,
                                date,
                                category
                        ));
                    }
                }

                handler.post(() -> {

                    all.clear();
                    all.addAll(parsed);

                    adapter.notifyDataSetChanged();

                    progress.setVisibility(View.GONE);

                    status.setText(
                            parsed.size() + "টি সংবাদ পাওয়া গেছে"
                    );
                });

            } catch (Exception error) {

                handler.post(() -> {
                    progress.setVisibility(View.GONE);
                    status.setText(
                            "সংবাদ লোড করা যায়নি। ইন্টারনেট পরীক্ষা করো।"
                    );
                });
            }
        });
    }

    private void filter(String query) {

        ArrayList<NewsItem> filtered = new ArrayList<>();

        for (NewsItem news : all) {

            if (news.title.toLowerCase().contains(
                    query.toLowerCase()) ||
                    news.summary.toLowerCase().contains(
                            query.toLowerCase())) {

                filtered.add(news);
            }
        }

        adapter = new NewsAdapter(this, filtered);

        RecyclerView list = findViewById(R.id.newsList);
        list.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
