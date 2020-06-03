package com.xuhaozhe.competition.verify.service;

import com.xuhaozhe.competition.verify.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LogicHandler implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogicHandler.class.getName());

    private static final Map<String, List<String>> TRACE_MAP = new HashMap<>();

    private static final Set<String> BAD_TRACE_IDS = new HashSet<>();

    private static final Map<String, Set<String>> BAD_TRACE_ID_SPANS = new HashMap<>();

    private static final Map<String, String> CHECK_SUM_MAP = new HashMap<>();

    private static final String PATH = "http://localhost:80/trace1.data";

    public static void start() {
        new Thread(new LogicHandler()).start();
    }

    public static void verify1(Map<String, List<String>> map, int batchPos) {
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String badTraceId = entry.getKey();
            List<String> spansUnderBadTraceId = entry.getValue();
            Set<String> knownSpans = BAD_TRACE_ID_SPANS.get(badTraceId);
            boolean spanOK = true;
            //LOGGER.info("batchPos: " + batchPos + " traceId: " + badTraceId + " foundSpanSize: " + spansUnderBadTraceId.size() + " knownSpanSize: " + knownSpans.size());
            for (String span : spansUnderBadTraceId) {
                if (!knownSpans.contains(span)) {
                    spanOK = false;
                    break;
                }
            }
            if (spanOK && (spansUnderBadTraceId.size() == knownSpans.size())) {
                //LOGGER.info("batchPos: " + batchPos + " traceId: " + badTraceId + " all good");
            } else {
                LOGGER.error("batchPos: " + batchPos + " traceId: " + badTraceId + " not right");
            }
        }
    }

    public static void verify2(Map<String, String> map, int batchPos) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String traceId = entry.getKey();
            String computedMd5Hash = entry.getValue();
            String knownMd5Hash = CHECK_SUM_MAP.get(traceId);
            if (computedMd5Hash.equalsIgnoreCase(knownMd5Hash)) {
                //LOGGER.info("md5 batchPos: " + batchPos + " traceId: " + traceId + " all good, computedHash: " + computedMd5Hash + " knownHash: " + knownMd5Hash);
            } else {
                LOGGER.error("md5 batchPos: " + batchPos + " traceId: " + traceId + "not right");
            }
        }
    }

    @Override
    public void run() {
        try {
            java.net.URL url = new URL(PATH);
            LOGGER.info("data path: " + PATH);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
            InputStream input = httpConnection.getInputStream();
            BufferedReader bf = new BufferedReader(new InputStreamReader(input));
            String line;
            while ((line = bf.readLine()) != null) {
                String[] cols = line.split("\\|");
                if (cols != null && cols.length > 1) {
                    String traceId = cols[0];
                    List<String> spanList = TRACE_MAP.get(traceId);
                    if (spanList == null) {
                        spanList = new ArrayList<>();
                        TRACE_MAP.put(traceId, spanList);
                    }
                    spanList.add(line);
                    if (cols.length > 8) {
                        String tags = cols[8];
                        if (tags != null) {
                            if (tags.contains("error=1")) {
                                BAD_TRACE_IDS.add(traceId);
                            } else if (tags.contains("http.status_code=") && tags.indexOf("http.status_code=200") < 0) {
                                BAD_TRACE_IDS.add(traceId);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error happened.", e);
        }
        LOGGER.info("Finish stage1.");

        for (String badTraceId : BAD_TRACE_IDS) {
            List<String> spansList = TRACE_MAP.get(badTraceId);
            Set<String> spanSet = new HashSet<>();
            spanSet.addAll(spansList);
            BAD_TRACE_ID_SPANS.put(badTraceId, spanSet);
        }
        LOGGER.info("Finish stage2.");

        for (Map.Entry<String, Set<String>> entry : BAD_TRACE_ID_SPANS.entrySet()) {
            String badTraceId = entry.getKey();
            Set<String> spanSet = entry.getValue();
            String spans = spanSet.stream().sorted(
                    Comparator.comparing(LogicHandler::getStartTime)).collect(Collectors.joining("\n"));
            spans = spans + "\n";
            CHECK_SUM_MAP.put(badTraceId, Utils.MD5(spans));
        }
        LOGGER.info("Finish stage3.");
    }

    public static long getStartTime(String span) {
        if (span != null) {
            String[] cols = span.split("\\|");
            if (cols.length > 8) {
                return Utils.toLong(cols[1], -1);
            }
        }
        return -1;
    }
}
