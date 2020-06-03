package com.xuhaozhe.competition.verify.controller;

import com.xuhaozhe.competition.verify.service.LogicHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class TestController {

    private static class Request1 {
        int batchPos;
        Map<String, List<String>> map;

        public Request1(int batchPos, Map<String, List<String>> map) {
            this.batchPos = batchPos;
            this.map = map;
        }
    }

    private static class Request2 {
        int batchPos;
        Map<String, String> map;

        public Request2(int batchPos, Map<String, String> map) {
            this.batchPos = batchPos;
            this.map = map;
        }
    }

    @RequestMapping("/verifygetTraceMapFromRemote")
    public String verify1(@RequestBody Request1 request1) {
        LogicHandler.verify1(request1.map, request1.batchPos);
        return "suc";
    }

    @RequestMapping("/verifymd5")
    public String verify2(@RequestBody Request2 request2) {
        LogicHandler.verify2(request2.map, request2.batchPos);
        return "suc";
    }


}
