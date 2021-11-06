package com.example.associativesearch.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;


@Component
public class ObjectMapperHolder {

    private ObjectMapper objectMapper;

    public ObjectMapperHolder(ObjectMapper objectMapper) {
        this.objectMapper=objectMapper;
    }
    public ObjectMapper objectMapper() {
        return objectMapper;
    }
}
