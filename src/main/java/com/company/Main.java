package com.company;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {

    private static final int N_THREADS = 15;
    private static final int RETRIES = 3;
    private static final String STARTING_URL = "https://en.wikipedia.org/wiki/Europe";
    private static final String RESULTS_FILENAME = "results.log";

    public static void main(String[] args) {

        final Map<String, Long> mainLinksCountMap = new LinkedHashMap<>();

        ExecutorService executorService = Executors.newFixedThreadPool(N_THREADS);
        Future<Map<String, Long>> futureStartPage = executorService.submit(new LinksParser(STARTING_URL, RETRIES));
        try {
           mainLinksCountMap.putAll(futureStartPage.get());
           System.out.println("Starting url has " + mainLinksCountMap.size() + " unique links to parse");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        List<Future<Map<String, Long>>> futures = new ArrayList<>();

        for (String followUpLink: mainLinksCountMap.keySet()) {
            futures.add(executorService.submit(new LinksParser(followUpLink, RETRIES)));
        }

        long totalPagesToParse = futures.size();

        while (futures.size() > 0) {
            for (Iterator<Future<Map<String, Long>>> iterator = futures.iterator(); iterator.hasNext();) {
                Future<Map<String, Long>> futureFollowedPage = iterator.next();
                if (futureFollowedPage.isDone()) {
                    try {
                        mergeHashMaps(mainLinksCountMap, futureFollowedPage.get());
                        iterator.remove();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Progress: " + (totalPagesToParse - futures.size()) + " parsed pages out of " + totalPagesToParse + " total");
        }
        executorService.shutdown();

        System.out.println("Saving " + mainLinksCountMap.size() + " urls to " + RESULTS_FILENAME);
        try {
            Files.write(Paths.get(RESULTS_FILENAME), () -> mainLinksCountMap.entrySet().stream()
                    .<CharSequence>map(e -> e.getKey() + " [count:" + e.getValue() + "]")
                    .iterator());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Done saving data");
    }

    private static void mergeHashMaps(Map<String, Long> map1, Map<String, Long> map2) {
        for (String key: map2.keySet()) {
            long counter = 1;
            if (map1.containsKey(key)) {
                counter = map1.get(key) + map2.get(key);
            }
            map1.put(key, counter);
        }
    }
}
