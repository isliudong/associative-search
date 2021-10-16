package com.example.associativesearch.service;

import java.io.IOException;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.example.associativesearch.model.DescriptorRecord;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


/**
 *
 */
@Service
@AllArgsConstructor
@Slf4j
public class EsService {
    private final RestHighLevelClient restHighLevelClient;

    @Async
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
                log.info("ES数据请求失败");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
