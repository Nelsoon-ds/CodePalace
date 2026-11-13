package com.codepalace.project.model;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;




}
