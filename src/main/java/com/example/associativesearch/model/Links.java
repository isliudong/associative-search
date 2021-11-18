package com.example.associativesearch.model;

import java.util.Objects;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Links {
    private String source;
    private String target;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Links links = (Links) o;
        return Objects.equals(source, links.source) && Objects.equals(target, links.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target);
    }
}
