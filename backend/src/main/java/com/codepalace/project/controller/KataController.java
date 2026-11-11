package com.codepalace.project.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KataController {

    @GetMapping("/api/categories")
    public void getCategories() {}

    @GetMapping("/api/katas")
    public void getKatas(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "tag", required = false) String tag,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "difficulty", required = false) String difficulty) {}
}
