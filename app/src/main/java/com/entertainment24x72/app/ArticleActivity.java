
package com.entertainment24x72.app;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public class ArticleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);

        int padding = (int) (16 *
                getResources().getDisplayMetrics().density);

        body.setPadding(padding, padding, padding, padding);

        scroll.addView(body);
        setContentView(scroll);

        TextView title = new TextView(this);

        title.setText(getIntent().getStringExtra("title"));
        title.setTextSize(24);
        title.setTextColor(0xFF171717);
        title.setTypeface(null, 1);

        body.addView(title);

        TextView date = new TextView(this);

        date.setText(getIntent().getStringExtra("date"));
        date.setTextSize(13);
        date.setTextColor(0xFF777777);
        date.setPadding(0, padding / 2, padding / 2, padding);

        body.addView(date);

        String html = getIntent().getStringExtra("content");

        org.jsoup.nodes.Document document =
                Jsoup.parse(html == null ? "" : html);

        for (Element element : document.body().children()) {

            Element img = element.selectFirst("img");

            if (img != null) {

                String imageUrl = img.attr("src");

                ImageView image = new ImageView(this);

                image.setAdjustViewBounds(true);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);

                body.addView(
                        image,
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                );

                Glide.with(this)
                        .load(imageUrl)
                        .into(image);

            } else {

                String text = element.text();

                if (!text.trim().isEmpty()) {

                    TextView paragraph = new TextView(this);

                    paragraph.setText(text);
                    paragraph.setTextSize(17);
                    paragraph.setTextColor(0xFF252525);
                    paragraph.setLineSpacing(4, 1f);
                    paragraph.setPadding(0, 0, 0, padding);

                    body.addView(paragraph);
                }
            }
        }
    }
}
