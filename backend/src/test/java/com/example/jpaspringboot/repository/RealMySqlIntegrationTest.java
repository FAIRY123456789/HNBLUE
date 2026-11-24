package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.entity.AgentEntitlement;
import com.example.jpaspringboot.entity.ProfessionalAccount;
import com.example.jpaspringboot.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opt-in validation against a real MySQL server. The default test suite remains
 * self-contained; RC validation enables this test with -Dhnblue.mysql.it=true
 * and supplies the isolated datasource through Spring system properties.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver",
        "spring.jpa.open-in-view=false",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.defer-datasource-initialization=false",
        "spring.sql.init.mode=never",
        "spring.data.redis.repositories.enabled=false",
        "management.health.redis.enabled=false"
})
@EnabledIfSystemProperty(named = "hnblue.mysql.it", matches = "true")
@Transactional
@Rollback
class RealMySqlIntegrationTest {
    private static final List<String> RC_TABLES = List.of(
            "professional_account",
            "agent_entitlement",
            "agent_quota_ledger",
            "agent_quota_reservation",
            "agent_task",
            "agent_dispatch_outbox"
    );

    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired ProfessionalAccountRepository professionals;
    @Autowired AgentEntitlementRepository entitlements;

    @Test
    void persistsPublicAndProfessionalAccountsAndCreatesAgentGovernanceTables() {
        String version = jdbc.queryForObject("select version()", String.class);
        String distribution = jdbc.queryForObject("select @@version_comment", String.class);
        assertTrue(version != null && version.startsWith("8.0."), version);
        assertTrue(distribution != null && !distribution.isBlank(), distribution);

        Integer tableCount = jdbc.queryForObject("""
                select count(*)
                  from information_schema.tables
                 where table_schema = database()
                   and table_name in ('professional_account', 'agent_entitlement',
                                      'agent_quota_ledger', 'agent_quota_reservation', 'agent_task',
                                      'agent_dispatch_outbox')
                """, Integer.class);
        assertEquals(RC_TABLES.size(), tableCount);

        Integer durableColumns = jdbc.queryForObject("""
                select count(*)
                  from information_schema.columns
                 where table_schema = database()
                   and table_name = 'agent_task'
                   and column_name in ('owner_username', 'owner_token_version',
                                       'password_change_required', 'input_message')
                """, Integer.class);
        assertEquals(4, durableColumns);

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User publicUser = users.saveAndFlush(new User(
                "rc3-public-" + suffix, "salt", "hash",
                "rc3-public-" + suffix + "@example.invalid", "2000-01-01"));
        assertTrue(users.findById(publicUser.getId()).isPresent());

        ProfessionalAccount professional = new ProfessionalAccount();
        professional.setUsername("rc3-professional-" + suffix);
        professional.setPasswordHash("{noop}local-test-only");
        professional.setEnabled(true);
        professional.setPasswordChangeRequired(true);
        professional.setCreatedBy("RC2_TEST");
        professional = professionals.saveAndFlush(professional);

        AgentEntitlement entitlement = new AgentEntitlement();
        entitlement.setProfessionalAccountId(professional.getId());
        entitlement.setMultiAgentEnabled(true);
        entitlement.setDailyRequestLimit(17);
        entitlements.saveAndFlush(entitlement);

        AgentEntitlement reloaded = entitlements.findByProfessionalAccountId(professional.getId()).orElseThrow();
        assertTrue(reloaded.isMultiAgentEnabled());
        assertEquals(17, reloaded.getDailyRequestLimit());
    }
}
