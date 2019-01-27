package com.company;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class LinksParser implements Callable<Map<String, Long>> {

    private String parseUrl;
    private Integer retries;

    LinksParser(String parseUrl, Integer retries) {
        this.parseUrl = parseUrl;
        this.retries = retries;
    }

    @Override
    public Map<String, Long> call() throws Exception {
        Map<String, Long> linksCountMap = new LinkedHashMap<>();

        Document page = null;

        for (int retryAttempts = 0; retryAttempts < retries; retryAttempts++) {
            try {
                page = Jsoup.connect(parseUrl).get();
                break;
            } catch (Exception e) {
                System.out.println("Exception during getting '" + parseUrl + "' url. Exception message: '" + e.getMessage() + "'");
            }
        }

        if (page == null) {
            System.out.println("Parser wasn't able to get data from '" + parseUrl + "'");
            return linksCountMap;
        }

        Elements allLinks = page.select("a");
        System.out.println("Successfully downloaded url '" + parseUrl + "': " + allLinks.size() + " links found on the page");
        for (Element link : allLinks) {
            String href = link.absUrl("href");

            if (href.length() == 0) {
                continue; // skip empty hrefs
            }

            if (href.contains("#")) {
                href = href.split("#")[0]; // remove anchors from url to reduce amount of work for parser (parse only unique urls)
            }

            long linkCounter = 1;
            if (linksCountMap.keySet().contains(href)) {
                linkCounter += linksCountMap.get(href);
            }
            linksCountMap.put(href, linkCounter);
        }
        return linksCountMap;
    }
}
