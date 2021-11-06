package com.example.associativesearch.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "as")
public class Config {
    private String host = "elasticsearch";
    private Long keyIndex = 0L;
}

