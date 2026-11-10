
package com.entertainment24x72.app;

public class NewsItem {
    public String title, summary, content, url, image, date, category;

    public NewsItem(String title, String summary, String content,
                    String url, String image, String date, String category) {
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.url = url;
        this.image = image;
        this.date = date;
        this.category = category;
    }
}
