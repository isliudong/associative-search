package com.example.associativesearch.service;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;


public class SourceSearchService {
    public static void main(String[] args) {
        //设置webdriver路径
        String path = SourceSearchService.class.getClassLoader().getResource("chromedriver.exe").getPath();
        System.setProperty("webdriver.chrome.driver", path);

        //创建webDriver
        WebDriver webDriver=new ChromeDriver();
        webDriver.get("https://www.webmd.com/search/search_results/default.aspx?query="+"key");


        //获取页面元素并操作

        WebElement chosenElementad = webDriver.findElement(By.xpath("//*[@id=\"ContentPane28\"]"));


        //chosenElementad.click();



        //操作业务

        clickOption(webDriver, "工作经验", "不限");
        clickOption(webDriver, "学历要求", "本科");
        clickOption(webDriver, "融资阶段", "不限");
        clickOption(webDriver, "公司规模", "不限");
        clickOption(webDriver, "行业领域", "移动互联网");
        getJobByPage(webDriver);


    }

    private static void getJobByPage(WebDriver webDriver) {
        //解析页面元素

        List<WebElement> jobElements = webDriver.findElements(By.className("con_list_item"));
        for (WebElement jobElement : jobElements) {
            WebElement moneyElement = jobElement.findElement(By.className("position")).findElement(By.className("money"));
            String companyName = jobElement.findElement(By.className("company_name")).getText();
            System.out.println(companyName+" : "+moneyElement.getText());
        }

        WebElement next_pageBtn = webDriver.findElement(By.className("pager_next"));
        if (!next_pageBtn.getAttribute("class").contains("pager_next_disabled")){
            next_pageBtn.click();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            getJobByPage(webDriver);
        }
    }

    private static void clickOption(WebDriver webDriver, String choseTitle, String optinTitle) {
        WebElement chosenElement = webDriver.findElement(By.xpath("//li[@class='multi-chosen']//span[contains(text(),'" + choseTitle + "')]"));
        WebElement optionElement = chosenElement.findElement(By.xpath("../a[contains(text(),'" + optinTitle + "')]"));
        optionElement.click();
    }
}
