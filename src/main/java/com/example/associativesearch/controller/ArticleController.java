package com.example.associativesearch.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.example.associativesearch.model.Article;
import com.example.associativesearch.model.Concept;
import com.example.associativesearch.model.DescriptorRecord;
import com.example.associativesearch.model.Key;
import com.example.associativesearch.model.KeyDTO;
import com.example.associativesearch.model.Links;
import com.example.associativesearch.model.Term;
import com.example.associativesearch.service.EsService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.collapse.CollapseBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
        return esService.search("keys", searchSourceBuilder, new TypeReference<DescriptorRecord>() {
                },
                null, pageRequest);
    }

    /**
     * 查询关系 网状关系
     *
     * @param keyword 关键词
     * @param page    页码
     * @param size    每页大小
     * @return DescriptorRecord
     */
    @GetMapping("/associate/search")
    public Page<KeyDTO> getAssociate(String keyword, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "3") int size) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.queryStringQuery(keyword));
        PageRequest pageRequest = PageRequest.of(page, size);
        searchSourceBuilder.size(pageRequest.getPageSize());
        searchSourceBuilder.from(pageRequest.getPageNumber());
        Page<DescriptorRecord> descriptorRecords = esService.search("descriptor-keys", searchSourceBuilder, new TypeReference<DescriptorRecord>() {
                },
                null, pageRequest);

        KeyDTO keyDTO = new KeyDTO();
        keyDTO.setDescriptorRecord(descriptorRecords.getContent());
        keyDTO.setLinks(getLinks(descriptorRecords.getContent()));
        return new PageImpl<>(Collections.singletonList(keyDTO),Pageable.ofSize(size),descriptorRecords.getTotalElements());

    }

    private List<Links> getLinks(List<DescriptorRecord> content) {
        ArrayList<Key> keyArrayList = new ArrayList<>();
        ArrayList<String> ids = new ArrayList<>();
        for (DescriptorRecord record : content) {
            keyArrayList.add(Key.builder().content(record.getDescriptorName()).id(record.getId()).build());
            ids.add(record.getId());
            for (Concept concept : record.getConceptList()) {
                keyArrayList.add(Key.builder().content(concept.getConceptName()).id(concept.getId()).build());
                ids.add(concept.getId());
                for (Term term : concept.getTermList()) {
                    keyArrayList.add(Key.builder().content(term.getString()).id(term.getId()).build());
                    ids.add(term.getId());
                }
            }
        }


        ArrayList<SearchSourceBuilder> builders = new ArrayList<>();

        for (Key string : keyArrayList) {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.termsQuery("id", ids));
            boolQueryBuilder.must(QueryBuilders.moreLikeThisQuery(new String[]{"content"},new String[]{string.getContent()},null).minTermFreq(0).minimumShouldMatch("50%"));
            sourceBuilder.query(boolQueryBuilder);
            sourceBuilder.size(10);
            builders.add(sourceBuilder);
        }


        List<Links> links = esService.busearch("keys", builders,keyArrayList);
        return links;
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
        searchSourceBuilder.collapse(new CollapseBuilder("title.keyword"));
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
