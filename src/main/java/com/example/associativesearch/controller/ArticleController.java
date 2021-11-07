package com.example.associativesearch.controller;

import com.example.associativesearch.model.Article;
import com.example.associativesearch.model.DescriptorRecord;
import com.example.associativesearch.service.EsService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public Page<DescriptorRecord> getAssociative(String keyword, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "3") int size) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.queryStringQuery(keyword));
        PageRequest pageRequest = PageRequest.of(page, size);
        searchSourceBuilder.size(pageRequest.getPageSize());
        searchSourceBuilder.from(pageRequest.getPageNumber());
        return esService.search("test", searchSourceBuilder, new TypeReference<DescriptorRecord>() {
                },
                null, pageRequest);
    }

    @GetMapping("/search")
    public Page<Article> getArticles(String keyword, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "3") int size) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.queryStringQuery(keyword));
        PageRequest pageRequest = PageRequest.of(page, size);
        searchSourceBuilder.size(pageRequest.getPageSize());
        searchSourceBuilder.from(pageRequest.getPageNumber());
        return esService.search("articles", searchSourceBuilder, new TypeReference<Article>() {
                },
                null, pageRequest);
    }


}
