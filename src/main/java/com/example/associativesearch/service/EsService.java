package com.example.associativesearch.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.fastjson.JSON;
import com.example.associativesearch.model.DescriptorRecord;
import com.example.associativesearch.model.Key;
import com.example.associativesearch.model.Links;
import com.example.associativesearch.util.ObjectMapperHolder;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


/**
 *
 */
@Service
@AllArgsConstructor
@Slf4j
public class EsService {
    private final RestHighLevelClient restHighLevelClient;
    private final ObjectMapperHolder objectMapperHolder;

    //@Async
    public void addToEs(List<DescriptorRecord> descriptorRecords, String indexName) {
        //数据放入es
        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.timeout("2m");
        for (DescriptorRecord question : descriptorRecords) {
            bulkRequest.add(new IndexRequest(indexName).source(JSON.toJSONString(question), XContentType.JSON));
        }
        //执行请求
        BulkResponse bulk;
        try {
            bulk = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (BooleanUtils.isNotTrue(bulk.hasFailures())) {
                log.info("ES数据请求完成");
            } else {
                log.info("ES数据请求失败" + bulk.buildFailureMessage());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public <T> void addSourceToEs(List<T> list, String index) {

        //数据放入es
        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.timeout("2m");
        for (T source : list) {
            bulkRequest.add(new IndexRequest(index).source(JSON.toJSONString(source), XContentType.JSON));
        }
        //执行请求
        BulkResponse bulk;
        try {
            bulk = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (BooleanUtils.isNotTrue(bulk.hasFailures())) {
                log.info("ES数据请求完成");
            } else {
                log.info("ES数据请求失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Long getTotal(String index) {
        CountRequest countRequest = new CountRequest(index);
        CountResponse count = null;
        try {
            count = restHighLevelClient.count(countRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (count == null) {
            return 0L;
        }
        return count.getCount();
    }

    public <T> Page<T> search(String index, SearchSourceBuilder builder,
                              TypeReference<T> reference, BiConsumer<SearchHit, T> consumer, Pageable pageable) {
        Objects.requireNonNull(builder);
        // build
        SearchRequest request = new SearchRequest(index);
        request.source(builder);
        // execute
        SearchResponse response = handleExecute(() -> restHighLevelClient.search(request, RequestOptions.DEFAULT));
        // judge
        required20X(response.status(), "error.es.search");
        SearchHits hits = response.getHits();
        return parse(hits, reference, consumer, pageable);
    }

    public  List<Links> busearch(String index, List<SearchSourceBuilder> builders, ArrayList<Key> keyArrayList) {
        Objects.requireNonNull(builders);
        MultiSearchRequest multiSearchRequest = new MultiSearchRequest();

        // build
        for (SearchSourceBuilder builder : builders) {
            SearchRequest request = new SearchRequest(index);
            request.source(builder);
            multiSearchRequest.add(request);
        }
        ArrayList<Links> links = new ArrayList<>();


        // execute
        MultiSearchResponse allresponse = handleExecute(() -> restHighLevelClient.msearch(multiSearchRequest, RequestOptions.DEFAULT));
        assert allresponse != null;
        for (int i = 0; i < allresponse.getResponses().length; i++) {
            // judge
            SearchResponse response = allresponse.getResponses()[i].getResponse();
            required20X(response.status(), "error.es.search");
            SearchHits hits = response.getHits();
            List<Key> list = parse2(hits, null);
            for (Key t : list) {
                Links build = Links.builder().source(keyArrayList.get(i).getId()).target(t.getId()).build();
                links.add(build);
            }
        }
        List<Links> list = links.stream().distinct().collect(Collectors.toList());
        return list;
    }

    public <T> T searchDetail(String index,
                              TypeReference<T> reference, String id) {
        GetRequest articles = new GetRequest(index);
        articles.id(id);
        try {
            GetResponse documentFields = restHighLevelClient.get(articles, RequestOptions.DEFAULT);
            Map<String, Object> source = documentFields.getSource();
            source.put("id", documentFields.getId());
            return objectMapperHolder.objectMapper().readValue(JSON.toJSONString(source), reference);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 实际调用的IO异常统一处理
     *
     * @param query client query
     * @param <T>   返回类型
     * @return 处理结果
     */
    private <T> T handleExecute(HandlerSupplier<T, IOException> query) {
        Objects.requireNonNull(query);
        try {
            return query.execute();
        } catch (IOException e) {
            log.error("es request execute error: [{}]", e.getMessage());

        }
        return null;
    }

    @FunctionalInterface
    public interface HandlerSupplier<T, E extends Exception> {
        T execute() throws E;
    }

    private <T> Page<T> parse(SearchHits hits, TypeReference<T> reference, BiConsumer<SearchHit, T> consumer, Pageable pageable) {
        if (hits.getTotalHits().value <= 0) {
            return new PageImpl<>(Collections.emptyList());
        }
        List<T> results = new ArrayList<>((int) hits.getTotalHits().value);
        // 反序列化
        hits.forEach(hit -> {
            try {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                hit.getHighlightFields().forEach((k, v) -> {
                    String hight = Arrays.stream(v.getFragments()).map(Text::string).collect(Collectors.joining(" "));
                    sourceAsMap.put(k, hight);
                    sourceAsMap.put("id", hit.getId());
                });
                //InputStream hitIn = hit.getSourceRef().streamInput();
                T result = objectMapperHolder.objectMapper().readValue(JSON.toJSONString(sourceAsMap), reference);
                if (Objects.nonNull(consumer)) {
                    consumer.accept(hit, result);
                }
                results.add(result);
            } catch (IOException e) {
                log.error("error to read hit or deserialization to java object, hit:[{}], case: {}",
                        hit, e.getMessage());
            }
        });
        return new PageImpl<>(results, pageable, hits.getTotalHits().value);
    }

    private  List<Key> parse2(SearchHits hits, BiConsumer<SearchHit, Key> consumer) {
        if (hits.getTotalHits().value <= 0) {
            return Collections.emptyList();
        }
        List<Key> results = new ArrayList<>((int) hits.getTotalHits().value);
        // 反序列化
        hits.forEach(hit -> {
            try {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                hit.getHighlightFields().forEach((k, v) -> {
                    String hight = Arrays.stream(v.getFragments()).map(Text::string).collect(Collectors.joining(" "));
                    sourceAsMap.put(k, hight);
                    sourceAsMap.put("id", hit.getId());
                });
                //InputStream hitIn = hit.getSourceRef().streamInput();
                Key result = objectMapperHolder.objectMapper().readValue(JSON.toJSONString(sourceAsMap), new TypeReference<Key>() {
                });
                if (Objects.nonNull(consumer)) {
                    consumer.accept(hit, result);
                }
                results.add(result);
            } catch (IOException e) {
                log.error("error to read hit or deserialization to java object, hit:[{}], case: {}",
                        hit, e.getMessage());
            }
        });
        return results;
    }

    /**
     * 结果必须为20X
     *
     * @param status 响应状态
     */
    private void required20X(RestStatus status, String errorMessage) {
        Objects.requireNonNull(status);
        int statusValue = status.getStatus();
        if (statusValue >= 200 && statusValue < 300) {
            return;
        }
        log.error(errorMessage);
    }
}
