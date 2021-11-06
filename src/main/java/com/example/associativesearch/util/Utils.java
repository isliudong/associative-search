package com.example.associativesearch.util;

import java.io.IOException;
import java.util.ArrayList;

import com.example.associativesearch.model.Article;
import com.example.associativesearch.service.EsService;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class Utils {
    @Autowired
    EsService esService;

    @Async
    public void getArtlcles(String searchUrl, String key, Long page) {
        System.out.println("执行线程："+Thread.currentThread().getId());
        Connection connection = Jsoup.connect(searchUrl + key + "&page=" + page);
        ArrayList<Article> articles = new ArrayList<>();
        try {
            org.jsoup.nodes.Document document = connection.get();
            Elements resultElements = document.getElementsByClass("search-results-doc-container");
            if (CollectionUtils.isEmpty(resultElements)){
                return;
            }
            for (org.jsoup.nodes.Element resultElement : resultElements) {
                Elements urlElement = resultElement.getElementsByTag("a");
                Elements desElement = resultElement.getElementsByClass("search-results-doc-description");
                String href = urlElement.get(0).attr("href");
                String des = desElement.get(0).text();

                Article article = getInfo(href);
                if (article == null) {
                    continue;
                }
                article.setDesc(des);
                articles.add(article);
                System.out.println(article.getTitle());
                System.out.println("-------------------------------------------");
            }
            if (!CollectionUtils.isEmpty(articles)){
                esService.addSourceToEs(articles,"articles");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public Long getTotal(String searchUrl, String key) {
        Connection connection = Jsoup.connect(searchUrl + key);
        try {
            org.jsoup.nodes.Document document = connection.get();
            Elements lastPageElement = document.select("#ContentPane28 > ul > li.page.last-page > a");
            if (lastPageElement.isEmpty()){
                return 0L;
            }
            String total = lastPageElement.get(0).attr("data-metrics-link");
            return Long.parseLong(total);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0L;
    }

    private Article getInfo(String href) {
        Connection connection2;
        org.jsoup.nodes.Document infoDocu = null;
        try {
            connection2 = Jsoup.connect(href);
            infoDocu = connection2.get();
        } catch (Exception e) {
            System.out.println("跳过：" + href);
        }
        if (infoDocu == null) {
            return null;
        }
        Elements titleElement = infoDocu.select("#ContentPane30 > article > header > h1");
        Elements articleBodyElement = infoDocu.select("#ContentPane30 > article > div.article__body");
        String title = titleElement.text();
        String articleBody = articleBodyElement.text();
        if (StringUtils.isBlank(articleBody)) {
            return null;
        }
        return Article.builder().title(title).content(articleBody).build();
    }
}
