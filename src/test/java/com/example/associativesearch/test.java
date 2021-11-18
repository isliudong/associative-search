package com.example.associativesearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.hutool.core.util.XmlUtil;
import com.example.associativesearch.model.Article;
import com.example.associativesearch.model.Concept;
import com.example.associativesearch.model.DescriptorRecord;
import com.example.associativesearch.model.Key;
import com.example.associativesearch.model.KeyDTO;
import com.example.associativesearch.model.Links;
import com.example.associativesearch.model.Term;
import com.example.associativesearch.service.EsService;
import com.example.associativesearch.service.SourceSearchService;
import com.example.associativesearch.util.Utils;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@SpringBootTest
@EnableAsync
public class test {

    @Autowired
    private EsService esService;
    @Autowired
    private Utils utils;

    @Test
    public void test0(){
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        PageRequest pageRequest = PageRequest.of(0, 3);
        sourceBuilder.size(pageRequest.getPageSize());
        sourceBuilder.from(pageRequest.getPageNumber());
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.termsQuery("id", Arrays.asList("6194ee28f5d6566eec3546f1","6194ee28f5d6566eec3546f2")));
        boolQueryBuilder.must(QueryBuilders.moreLikeThisQuery(new String[]{"content"},new String[]{"Virus, SARS-CoV-2"},null).minTermFreq(0));
        sourceBuilder.query(boolQueryBuilder);
        Page<Key> keys = esService.search("keys", sourceBuilder, new TypeReference<Key>() {
        }, null, pageRequest);

        Key key = keys.getContent().get(0);
        KeyDTO keyDTO = new KeyDTO();
        ArrayList<Links> links = new ArrayList<>();
        links.add(Links.builder().source(key.getId()).target("6194ee28f5d6566eec3546f1").build());
        keyDTO.setLinks(links);

        System.out.print(keys.getContent());
        System.out.print(keyDTO);
    }

    @Test
    public void test1() {
        Document document = XmlUtil.readXML("D:\\IdeaProjects\\associative-search\\src\\main\\resources\\covid-19.xml");
        Element rootElement = XmlUtil.getRootElement(document);
        List<Element> descriptorRecords = XmlUtil.getElements(rootElement, "DescriptorRecord");
        ArrayList<DescriptorRecord> descriptorList = new ArrayList<>();
        ArrayList<Key> keyList = new ArrayList<>();

        for (Element descriptorRecord : descriptorRecords) {
            DescriptorRecord descriptorRecordModel = new DescriptorRecord();

            Element descriptorName = XmlUtil.getElement(descriptorRecord, "DescriptorName");
            Element name = XmlUtil.getElement(descriptorName, "String");
            String textContent = name.getFirstChild().getTextContent();
            descriptorRecordModel.setDescriptorName(textContent);
            keyList.add(Key.builder().id(descriptorRecordModel.getId()).content(descriptorRecordModel.getDescriptorName()).build());


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
                keyList.add(Key.builder().id(conceptModel.getId()).content(conceptModel.getConceptName()).build());


                Element termList = XmlUtil.getElement(concept, "TermList");
                List<Element> termEs = XmlUtil.getElements(termList, "Term");
                for (Element term : termEs) {
                    Term termModel = new Term();
                    String termName = XmlUtil.getElement(term, "String").getFirstChild().getTextContent();
                    termModel.setString(termName);
                    keyList.add(Key.builder().id(termModel.getId()).content(termModel.getString()).build());
                    terms.add(termModel);
                }
                conceptModel.setTermList(terms);

            }
            descriptorRecordModel.setConceptList(conceptListModel);
            descriptorList.add(descriptorRecordModel);
            //System.out.println(descriptorRecordModel);
        }
        //System.out.println(JSON.toJSONString(list));
        esService.addToEs(descriptorList, "descriptor-keys");
        esService.addSourceToEs(keyList, "keys");
    }

    @Test
    public void test2() {
        //设置webdriver路径
        String path = SourceSearchService.class.getClassLoader().getResource("chromedriver.exe").getPath();
        System.setProperty("webdriver.chrome.driver", path);

        //创建webDriver
        ChromeOptions option = new ChromeOptions();
        option.setBinary("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
        WebDriver webDriver = new ChromeDriver(option);
        WebDriver webDriver2 = new ChromeDriver(option);
        webDriver.get("https://www.webmd.com/search/search_results/default.aspx?query=" + "key");


        //获取页面元素并操作

        List<WebElement> resultsElements = webDriver.findElements(By.className("search-results-doc-container"));
        for (WebElement resultsElement : resultsElements) {
            WebElement titleElement = resultsElement.findElement(By.tagName("a"));
            WebElement descriptionElement = resultsElement.findElement(By.className("search-results-doc-description"));

            String title = titleElement.getText();
            String href = titleElement.getAttribute("href");
            String description = descriptionElement.getText();
            //查详情
            String info = getInfo(webDriver2, href);
            System.out.println(title);
            System.out.println(description);
            System.out.println(info);

        }

    }

    private String getInfo(WebDriver webDriver2, String href) {
        WebElement articleElement = null;
        try {
            webDriver2.get(href);
            WebElement title = webDriver2.findElement(By.xpath("//*[@id=\"ContentPane30\"]/article/header/h1"));
            articleElement = webDriver2.findElement(By.xpath("//*[@id=\"ContentPane30\"]/article/div[2]"));
        } catch (Exception e) {
            System.out.println("解析异常： " + href);
        }
        if (articleElement == null) {
            return null;
        }
        return articleElement.getText();
    }

    @Test
    public void test3() {
        String searchUrl = "https://www.webmd.com/search/search_results/default.aspx?query=";
        String key = "key";
        Long total = utils.getTotal(searchUrl, key);
        for (int i = 2; i <= total; i++) {
            utils.getArtlcles(searchUrl, key, (long) i);
        }
        System.out.println(total);

    }

    @Async
    public Long getArtlcles(String searchUrl, String key, Long page) {
        System.out.println(Thread.currentThread().getId());
        Connection connection = Jsoup.connect(searchUrl + key + "&page=" + page);
        try {
            org.jsoup.nodes.Document document = connection.get();
            Elements ResultElements = document.getElementsByClass("search-results-doc-container");
            for (org.jsoup.nodes.Element resultElement : ResultElements) {
                Elements urlElement = resultElement.getElementsByTag("a");
                Elements desElement = resultElement.getElementsByClass("search-results-doc-description");
                String href = urlElement.get(0).attr("href");
                String des = desElement.get(0).text();

                Article article = getInfo(href);
                if (article == null) {
                    continue;
                }
                article.setDesc(des);
                System.out.println(article);
                System.out.println("-------------------------------------------");
            }
            Elements lastPageElement = document.select("#ContentPane28 > ul > li.page.last-page > a");
            String total = lastPageElement.get(0).attr("data-metrics-link");
            return Long.parseLong(total);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0L;
    }
    public Long getTotal(String searchUrl, String key) {
        Connection connection = Jsoup.connect(searchUrl + key);
        try {
            org.jsoup.nodes.Document document = connection.get();
            Elements lastPageElement = document.select("#ContentPane28 > ul > li.page.last-page > a");
            String total = lastPageElement.get(0).attr("data-metrics-link");
            return Long.parseLong(total);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0L;
    }

    private Article getInfo(String href) {
        Connection connection2;
        org.jsoup.nodes.Document infoDocu = null;
        try {
            connection2 = Jsoup.connect(href);
            infoDocu = connection2.get();
        } catch (Exception e) {
            System.out.println("跳过：" + href);
        }
        if (infoDocu == null) {
            return null;
        }
        Elements titleElement = infoDocu.select("#ContentPane30 > article > header > h1");
        Elements articleBodyElement = infoDocu.select("#ContentPane30 > article > div.article__body");
        String title = titleElement.text();
        String articleBody = articleBodyElement.text();
        if (StringUtils.isBlank(articleBody)) {
            return null;
        }
        return Article.builder().title(title).content(articleBody).build();
    }
}
