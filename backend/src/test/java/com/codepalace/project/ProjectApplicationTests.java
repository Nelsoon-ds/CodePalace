package com.codepalace.project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the whole application against a real PostgreSQL database.
 *
 * <p>This is the test that proves the schema and the entities agree: Flyway applies the
 * migrations and Hibernate validates the mapping against the result, so a column that exists in
 * Java but not in the database fails the build instead of failing at runtime.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class ProjectApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.jwt.secret", () -> "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=");
    }

    @Test
    void contextLoads() {
    }

    @Test
    void everyMigrationIsApplied() {
        Integer applied = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success", Integer.class);

        assertThat(applied).isEqualTo(3);
    }

    @Test
    void theDraftColumnExists() {
        Integer columns = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns " +
                        "where table_name = 'sessions' and column_name = 'draft_code'", Integer.class);

        assertThat(columns).isEqualTo(1);
    }

    @Test
    void theEntityMappingMatchesTheSchema() {
        // Hibernate validated the mapping while the context started; this proves the tables the
        // entities expect are really there.
        Integer sessions = jdbcTemplate.queryForObject("select count(*) from sessions", Integer.class);
        Integer workspaces = jdbcTemplate.queryForObject("select count(*) from workspaces", Integer.class);
        Integer katas = jdbcTemplate.queryForObject("select count(*) from katas", Integer.class);

        assertThat(sessions).isNotNull();
        assertThat(workspaces).isNotNull();
        assertThat(katas).isNotNull();
    }
}
