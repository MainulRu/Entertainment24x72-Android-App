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
import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.Holder> {
    private final Context context;
    private final List<NewsItem> items = new ArrayList<>();
    public NewsAdapter(Context context, List<NewsItem> initial) {
        this.context=context; if(initial!=null) items.addAll(initial);
    }
    public void replaceItems(List<NewsItem> data) {
        items.clear(); items.addAll(data); notifyDataSetChanged();
    }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type) {
        return new Holder(LayoutInflater.from(context).inflate(R.layout.item_news,parent,false));
    }
    @Override public void onBindViewHolder(@NonNull Holder h,int position) {
        NewsItem n=items.get(position);
        h.title.setText(n.title); h.summary.setText(n.summary);
        h.date.setText(n.date); h.category.setText(n.category);
        Glide.with(context).load(n.image).placeholder(R.drawable.news_placeholder).centerCrop().into(h.image);
        h.itemView.setOnClickListener(v->{
            Intent i=new Intent(context,ArticleActivity.class);
            i.putExtra("title",n.title); i.putExtra("content",n.content);
            i.putExtra("url",n.url); i.putExtra("date",n.date); i.putExtra("image",n.image);
            context.startActivity(i);
        });
    }
    @Override public int getItemCount(){return items.size();}
    static class Holder extends RecyclerView.ViewHolder {
        ImageView image; TextView title,summary,date,category;
        Holder(View v){super(v); image=v.findViewById(R.id.newsImage);
            title=v.findViewById(R.id.newsTitle); summary=v.findViewById(R.id.newsSummary);
            date=v.findViewById(R.id.newsDate); category=v.findViewById(R.id.newsCategory);}
    }
}
