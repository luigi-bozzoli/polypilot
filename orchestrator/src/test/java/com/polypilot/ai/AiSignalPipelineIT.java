package com.polypilot.ai;

import com.polypilot.auth.entity.Role;
import com.polypilot.auth.entity.User;
import com.polypilot.auth.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end coverage of the orchestrator side of the AI signal pipeline: a message lands on
 * {@code ai.signals} (published by ai-agent in production; published directly here to keep this
 * test self-contained) and {@link com.polypilot.ai.listener.AiSignalListener} must write both a
 * {@code sentiment_scores} and a {@code news_summaries} row, readable back through
 * {@code GET /market/{id}/sentiment/latest} and {@code /news/latest}.
 *
 * <p>Same image tags as docker-compose.yaml (Postgres 16, RabbitMQ 3.13-management), the same
 * default-profile application.yml (full SQL init against 001_schema.sql / 002_seed_data.sql,
 * {@code ddl-auto=validate}) so Hibernate's schema validation is exercised against a real
 * Postgres, not just the unit-test mocks the rest of the suite uses. Redis is deliberately not
 * containerized: every Redis-backed cache in this codebase (JsonRedisCache) is documented
 * best-effort and degrades to DB-only on a connection failure, so an unreachable Redis host is
 * exactly the condition it's designed to tolerate.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers(disabledWithoutDocker = true)
class AiSignalPipelineIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"));

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3.13-management-alpine"));

    /** Test-only values in the exact shape production requires — JwtService/EncryptionService
     * validate these at startup the same as they would against a real .env. */
    @DynamicPropertySource
    static void requiredSecrets(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () -> "TCjcvvtjTRHefQlyrDJOratZz+IDf7cGtois83a7iLg=");
        registry.add("ENCRYPTION_KEY", () -> "IXa/1Tq0clszOqx2xYhFGC+5ddpAuhPD+4BxAfj5t3k=");
        registry.add("DB_USER", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);
        registry.add("RABBITMQ_USER", () -> "guest");
        registry.add("RABBITMQ_PASSWORD", () -> "guest");
        // Cron jobs (Gamma/Binance/ai-agent HTTP calls) must not fire mid-test.
        registry.add("polypilot.scheduling.enabled", () -> "false");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ApplicationContext applicationContext;

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private String bearerToken() {
        Role role = new Role();
        role.setRoleName("USER");
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        return jwtService.generate(user);
    }

    private HttpEntity<Void> authorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken());
        return new HttpEntity<>(headers);
    }

    /** A series + market row good enough to satisfy every FK the pipeline touches. */
    private String seedMarket() {
        UUID seriesId = UUID.randomUUID();
        String marketId = "it-market-" + UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO series (id, polymarket_id, ticker, slug, title, recurrence, tracked) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                seriesId, "pm-series-1", "IT-TICKER-" + seriesId, "it-series-slug", "IT Series", "daily", false);
        jdbcTemplate.update(
                "INSERT INTO markets (id, polymarket_condition_id, question, series_id) VALUES (?, ?, ?, ?)",
                marketId, "0xcond-" + marketId, "Will BTC be up?", seriesId);
        return marketId;
    }

    private String signalJson(String marketId, String sentiment, double confidence) {
        return """
                {
                  "market_id": "%s",
                  "summary": "Bullish coverage across major outlets.",
                  "articles": [
                    {"title": "BTC rallies", "url": "https://news.example/1", "source": "news.example",
                     "published_at": "2025-01-01T00:00:00+00:00"}
                  ],
                  "sentiment": "%s",
                  "confidence": %s,
                  "reasoning": "Strong positive coverage",
                  "article_count": 1,
                  "model_used": "claude-haiku-4-5-20251001",
                  "raw_response": {"note": "fixture"},
                  "generated_at": "2025-01-01T00:00:00+00:00"
                }
                """.formatted(marketId, sentiment, confidence);
    }

    private void publish(String queue, String body) {
        Message message = new Message(body.getBytes(StandardCharsets.UTF_8), new MessageProperties());
        rabbitTemplate.send(queue, message);
    }

    @Test
    void contextLoadsAndSchemaValidatesAgainstRealPostgres() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void happyPath_signalMessageIsRecordedAndReadableThroughTheApi() {
        String marketId = seedMarket();

        publish("ai.signals", signalJson(marketId, "BULLISH", 0.75));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Integer sentimentRows = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sentiment_scores WHERE market_id = ?", Integer.class, marketId);
            Integer newsRows = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM news_summaries WHERE market_id = ?", Integer.class, marketId);
            assertThat(sentimentRows).isEqualTo(1);
            assertThat(newsRows).isEqualTo(1);
        });

        ResponseEntity<Map> sentimentResponse = restTemplate.exchange(
                baseUrl("/market/" + marketId + "/sentiment/latest"), HttpMethod.GET, authorized(), Map.class);
        assertThat(sentimentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sentimentResponse.getBody()).containsEntry("label", "BULLISH");
        assertThat(((Number) sentimentResponse.getBody().get("confidence")).doubleValue()).isEqualTo(0.75);

        ResponseEntity<Map> newsResponse = restTemplate.exchange(
                baseUrl("/market/" + marketId + "/news/latest"), HttpMethod.GET, authorized(), Map.class);
        assertThat(newsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(newsResponse.getBody()).containsEntry("summary", "Bullish coverage across major outlets.");
    }

    @Test
    void poisonMessage_malformedJsonIsDeadLetteredAndWritesNoRows() {
        String marketId = "it-market-poison-" + UUID.randomUUID();

        publish("ai.signals", "{ this is not valid json");

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Message dead = rabbitTemplate.receive("ai.signals.dlq", 200);
            assertThat(dead).as("message should have been rejected-without-requeue onto the DLQ").isNotNull();
        });

        Integer sentimentRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sentiment_scores WHERE market_id = ?", Integer.class, marketId);
        assertThat(sentimentRows).isZero();
    }

    @Test
    void securityRegression_apiAuthMeWithoutTokenIsRejected() {
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/api/auth/me"), HttpMethod.GET, HttpEntity.EMPTY, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
