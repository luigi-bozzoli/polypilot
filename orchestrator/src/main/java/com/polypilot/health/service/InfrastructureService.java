package com.polypilot.health.service;

import com.polypilot.health.dto.InfraComponent;
import com.polypilot.health.dto.InfrastructureResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Heavier infra probes for the health dashboard — Postgres, Redis, RabbitMQ on
 * the {@code polypilot-net} bridge network. Each probe degrades on its own:
 * one component being unreachable never fails the others. Contract:
 * {@code contracts/health-infrastructure.md}.
 */
@Slf4j
@Service
@AllArgsConstructor
public class InfrastructureService {

    private static final String SIGNALS_QUEUE = "ai.signals";

    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;
    private final AmqpAdmin amqpAdmin;
    private final ConnectionFactory rabbitConnectionFactory;

    public InfrastructureResponse getInfrastructure() {
        return new InfrastructureResponse(List.of(postgres(), redis(), rabbitmq()));
    }

    private InfraComponent postgres() {
        try {
            Integer conns = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_stat_activity WHERE datname = current_database()",
                    Integer.class);
            String size = jdbcTemplate.queryForObject(
                    "SELECT pg_size_pretty(pg_database_size(current_database()))",
                    String.class);
            String detail = "%d conns · %s".formatted(conns == null ? 0 : conns, size);
            return new InfraComponent("Postgres 16", "ready", null, 5432, detail);
        } catch (Exception ex) {
            log.warn("Postgres infra probe failed", ex);
            return new InfraComponent("Postgres 16", "unreachable", null, 5432, message(ex));
        }
    }

    private InfraComponent redis() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            Long keys = connection.serverCommands().dbSize();
            String detail = "%d keys · challenge TTL".formatted(keys == null ? 0 : keys);
            return new InfraComponent("Redis 7", "ready", null, 6379, detail);
        } catch (Exception ex) {
            log.warn("Redis infra probe failed", ex);
            return new InfraComponent("Redis 7", "unreachable", null, 6379, message(ex));
        }
    }

    private InfraComponent rabbitmq() {
        try (Connection connection = rabbitConnectionFactory.createConnection()) {
            if (!connection.isOpen()) {
                return new InfraComponent("RabbitMQ 3", "unreachable", null, 5672, "connection closed");
            }
            QueueInformation info = amqpAdmin.getQueueInfo(SIGNALS_QUEUE);
            String detail = info == null
                    ? SIGNALS_QUEUE + " · not declared"
                    : "%s · %d pending".formatted(SIGNALS_QUEUE, info.getMessageCount());
            return new InfraComponent("RabbitMQ 3", "ready", "connected", 5672, detail);
        } catch (Exception ex) {
            log.warn("RabbitMQ infra probe failed", ex);
            return new InfraComponent("RabbitMQ 3", "unreachable", null, 5672, message(ex));
        }
    }

    private static String message(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }
}
