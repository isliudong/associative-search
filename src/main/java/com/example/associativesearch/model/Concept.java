package com.example.associativesearch.model;

import java.util.List;

import lombok.Data;

@Data
public class Concept {
    private String conceptName;
    private List<Term> termList;
}
