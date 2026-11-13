package com.codepalace.project.controller;

import com.codepalace.project.model.Kata;
import com.codepalace.project.service.KataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class KataController {

    @Autowired
    KataService kataService;

    @GetMapping("/api/categories")
    public void getCategories() {}

    @GetMapping("/api/katas")
    public List<Kata> getKatas(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "tag", required = false) String tag,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "difficulty", required = false) String difficulty) {
        return kataService.getListOfKatas();
    }
}
