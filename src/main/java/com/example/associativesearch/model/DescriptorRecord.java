package com.example.associativesearch.model;

import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.XmlUtil;
import lombok.Data;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author liudong
 */
@Data
public class DescriptorRecord {
    private String id;
    private String descriptorName;
    private List<Concept> conceptList;

    public DescriptorRecord() {
        this.id = IdUtil.objectId();
    }

    public static List<DescriptorRecord> readXml(String path) {
        Document document = XmlUtil.readXML(path);
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
        }
        return list;
    }
}
