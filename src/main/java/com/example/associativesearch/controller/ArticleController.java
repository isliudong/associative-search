package com.example.associativesearch.controller;

import com.example.associativesearch.model.Article;
import com.example.associativesearch.model.DescriptorRecord;
import com.example.associativesearch.service.EsService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    RestHighLevelClient restHighLevelClient;
    @Autowired
    EsService esService;

    /**
     * 查询关系
     *
     * @param keyword 关键词
     * @param page    页码
     * @param size    每页大小
     * @return DescriptorRecord
     */
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

    /**
     * 查询文章
     *
     * @param keyword 关键词
     * @param page    页码
     * @param size    大小
     * @return Page<Article>
     */
    @GetMapping("/search")
    public Page<Article> getArticles(String keyword, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "3") int size) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.queryStringQuery(keyword));
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("content");
        highlightBuilder.field("title");
        highlightBuilder.field("desc");
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");
        //多高亮
        highlightBuilder.requireFieldMatch(true);
        PageRequest pageRequest = PageRequest.of(page, size);
        searchSourceBuilder.size(pageRequest.getPageSize());
        searchSourceBuilder.from(pageRequest.getPageNumber());
        searchSourceBuilder.highlighter(highlightBuilder);
        return esService.search("articles", searchSourceBuilder, new TypeReference<Article>() {
                },
                null, pageRequest);
    }

    /**
     * 文章详情
     *
     * @param id 文章id
     * @return Article
     */
    @GetMapping("/{id}")
    public Article getArticles(@PathVariable String id) {

        return esService.searchDetail("articles", new TypeReference<Article>() {
        }, id);
    }


}
