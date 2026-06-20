package com.polypilot;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    // Flyway is configured manually rather than via Spring Boot autoconfiguration
    // due to Spring Boot 4.1.0 autoconfiguration changes. See application.yml.
    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .validateOnMigrate(true)
                .load();

        flyway.migrate();
        return flyway;
    }
}


