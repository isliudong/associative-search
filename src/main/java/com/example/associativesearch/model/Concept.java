package com.example.associativesearch.model;

import java.util.List;

import cn.hutool.core.util.IdUtil;
import lombok.Data;

@Data
public class Concept {
    private String id;
    private String conceptName;
    private List<Term> termList;

    public Concept() {
        this.id = IdUtil.objectId();
    }
}
