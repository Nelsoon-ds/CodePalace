package com.codepalace.project.service;

import com.codepalace.project.model.Kata;
import com.codepalace.project.repository.KataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KataService
{
    @Autowired
    private KataRepository kataRepository;


    public List<Kata> getListOfKatas() {
        return kataRepository.findAll();
    }
}
