package com.example.associativesearch.model;

import java.util.List;

import lombok.Data;

@Data
public class KeyDTO {
    private List<DescriptorRecord> descriptorRecord;
    private List<Links> links;
}
