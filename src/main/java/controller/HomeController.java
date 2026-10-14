package com.syfe.finance.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
            "name", "Personal Finance Manager API",
            "status", "UP",
            "message", "API is running successfully",
            "health", "/health"
        );
    }
}
