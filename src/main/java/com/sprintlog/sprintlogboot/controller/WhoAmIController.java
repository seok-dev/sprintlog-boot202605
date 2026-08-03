package com.sprintlog.sprintlogboot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class WhoAmIController {


    @GetMapping("/whoami")
    public Map<String, String> whoami(){
        return Map.of("host", System.getenv().getOrDefault("HOSTNAME", "unknown"));
    }
}
