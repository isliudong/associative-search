package com.example.associativesearch.model;

import cn.hutool.core.util.IdUtil;
import lombok.Data;

@Data
public class Term {
    private String id;
    private String string;

    public Term() {
        this.id = IdUtil.objectId();
    }
}
