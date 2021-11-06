package com.example.associativesearch.controller;

import java.util.List;

import com.example.associativesearch.model.Article;
import com.example.associativesearch.model.DescriptorRecord;
import com.example.associativesearch.service.EsService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author liudong
 */

@RestController
@RequestMapping("/articles")
public class ArticleController {

    @Autowired
    EsService esService;

    @GetMapping("/associative/search")
    public List<DescriptorRecord> getAssociative(String keyword){
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.queryStringQuery(keyword));
        searchSourceBuilder.size(3);
        return esService.search("test", searchSourceBuilder, new TypeReference<DescriptorRecord>() {
                },
                null);
    }

    @GetMapping("/search")
    public List<Article> getArticles(String keyword){
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.queryStringQuery(keyword));
        searchSourceBuilder.size(3);
        return esService.search("articles", searchSourceBuilder, new TypeReference<Article>() {
                },
                null);
    }



}
