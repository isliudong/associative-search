package com.example.associativesearch;

import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.util.XmlUtil;
import com.alibaba.fastjson.JSON;
import com.example.associativesearch.model.Concept;
import com.example.associativesearch.model.DescriptorRecord;
import com.example.associativesearch.model.Term;
import com.example.associativesearch.service.EsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@SpringBootTest
public class test {

    @Autowired
    private EsService esService;

    @Test
    public void test1() {
        Document document = XmlUtil.readXML("D:\\IdeaProjects\\associative-search\\src\\main\\resources\\desc2021.xml");
        Element rootElement = XmlUtil.getRootElement(document);
        List<Element> descriptorRecords = XmlUtil.getElements(rootElement, "DescriptorRecord");
        ArrayList<DescriptorRecord> list = new ArrayList<>();

        for (Element descriptorRecord : descriptorRecords) {
            DescriptorRecord descriptorRecordModel = new DescriptorRecord();


            Element descriptorName = XmlUtil.getElement(descriptorRecord, "DescriptorName");
            Element name = XmlUtil.getElement(descriptorName, "String");
            String textContent = name.getFirstChild().getTextContent();
            descriptorRecordModel.setDescriptorName(textContent);


            Element conceptList = XmlUtil.getElement(descriptorRecord, "ConceptList");
            List<Element> concepts = XmlUtil.getElements(conceptList, "Concept");

            List<Concept> conceptListModel = new ArrayList<>();
            for (Element concept : concepts) {
                Concept conceptModel = new Concept();
                List<Term> terms = new ArrayList<>();
                Element conceptName = XmlUtil.getElement(concept, "ConceptName");
                String conceptName1 = XmlUtil.getElement(conceptName, "String").getFirstChild().getTextContent();
                conceptModel.setConceptName(conceptName1);
                conceptListModel.add(conceptModel);


                Element termList = XmlUtil.getElement(concept, "TermList");
                List<Element> termEs = XmlUtil.getElements(termList, "Term");
                for (Element term : termEs) {
                    Term termModel = new Term();
                    String termName = XmlUtil.getElement(term, "String").getFirstChild().getTextContent();
                    termModel.setString(termName);
                    terms.add(termModel);
                }
                conceptModel.setTermList(terms);

            }
            descriptorRecordModel.setConceptList(conceptListModel);
            list.add(descriptorRecordModel);
            //System.out.println(descriptorRecordModel);
        }
        //System.out.println(JSON.toJSONString(list));
        esService.addToEs(list, "test");
    }
}
