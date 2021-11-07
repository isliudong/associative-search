package com.example.associativesearch.service;

import java.util.ArrayList;
import java.util.List;

import com.example.associativesearch.config.Config;
import com.example.associativesearch.model.Concept;
import com.example.associativesearch.model.DescriptorRecord;
import com.example.associativesearch.model.Term;
import com.example.associativesearch.util.Utils;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Pageable;


//@Component
public class Task implements ApplicationRunner {
    @Autowired
    Utils utils;
    @Autowired
    Config config;
    @Autowired
    EsService esService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String index = "test";
        Long totalKey = esService.getTotal(index);
        System.out.println("关键词量:"+totalKey);
        ArrayList<String> keys = new ArrayList<>();
        for (long i = config.getKeyIndex(); i < totalKey; i = i + 1000) {
            System.out.println("获取关键词："+i);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.matchAllQuery());
            searchSourceBuilder.size(1000);
            searchSourceBuilder.from((int) i);
            List<DescriptorRecord> articles = esService.search(index, searchSourceBuilder, new TypeReference<DescriptorRecord>() {
                    },
                    null, Pageable.ofSize(3)).getContent();

            for (DescriptorRecord article : articles) {
                keys.add(article.getDescriptorName());
                for (Concept concept : article.getConceptList()) {
                    keys.add(concept.getConceptName());
                    for (Term term : concept.getTermList()) {
                        keys.add(term.getString());
                    }
                }
            }

        }


        //遍历关键字爬取网站
        String searchUrl = "https://www.webmd.com/search/search_results/default.aspx?query=";
        for (int i=0;i<keys.size();i++) {
            String key = keys.get(i);
            if (StringUtils.isBlank(key)){
                continue;
            }
            Long total = utils.getTotal(searchUrl, key);
            System.out.println("key序号："+i);
            System.out.println(key+" 相关总数：" + total);
            for (int j = 1; j <= total; j++) {
                utils.getArtlcles(searchUrl, key, (long) j);
            }

        }


    }
}
