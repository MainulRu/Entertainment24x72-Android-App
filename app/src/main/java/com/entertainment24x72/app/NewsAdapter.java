
package com.entertainment24x72.app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.Holder> {

    private final Context context;
    private final List<NewsItem> items;

    public NewsAdapter(Context context, List<NewsItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_news, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        NewsItem news = items.get(position);

        holder.title.setText(news.title);
        holder.summary.setText(news.summary);
        holder.date.setText(news.date);
        holder.category.setText(news.category);

        if (news.image != null && !news.image.isEmpty()) {
            Glide.with(context)
                    .load(news.image)
                    .placeholder(R.drawable.news_placeholder)
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.news_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ArticleActivity.class);
            intent.putExtra("title", news.title);
            intent.putExtra("content", news.content);
            intent.putExtra("url", news.url);
            intent.putExtra("date", news.date);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {

        ImageView image;
        TextView title, summary, date, category;

        Holder(View view) {
            super(view);

            image = view.findViewById(R.id.newsImage);
            title = view.findViewById(R.id.newsTitle);
            summary = view.findViewById(R.id.newsSummary);
            date = view.findViewById(R.id.newsDate);
            category = view.findViewById(R.id.newsCategory);
        }
    }
}
