package com.example.associativesearch.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 */
@Configuration
public class ElasticsearchConfig {

    @Bean
    public RestHighLevelClient restHighLevelClient(Config config) {
        return new RestHighLevelClient(RestClient.builder(
                new HttpHost(config.getHost(), 9200, "http")
        ));
    }
}

