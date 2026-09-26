package com.polypilot.strategy.json;

import com.polypilot.strategy.BooleanNode;
import com.polypilot.strategy.CompareNode;
import com.polypilot.strategy.MarketFieldCompareNode;
import com.polypilot.strategy.StrategyNode;
import com.polypilot.strategy.UnaryBooleanNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;

/**
 * Registers {@link StrategyNodeSerializer} / {@link StrategyNodeDeserializer} on the
 * application's {@code ObjectMapper}. Spring Boot's Jackson 3 auto-configuration collects every
 * {@link JacksonModule} bean in the context and adds it to the {@code JsonMapper.Builder}, so no
 * further wiring is needed — this module applies to {@code Strategy.ruleTree} (de)serialization
 * wherever it happens (JPA converter, {@code POST /strategies} request/response bodies, …).
 *
 * <p>Registered for {@link StrategyNode} itself (so a bare tree root serializes/deserializes
 * correctly) and each concrete subtype (so nested {@code left}/{@code right}/{@code child} fields
 * declared as {@code StrategyNode} resolve to the same logic without depending on Jackson's
 * base-type serializer lookup for polymorphic dispatch).
 */
@Configuration
public class StrategyNodeJacksonModule {

    @Bean
    public JacksonModule strategyNodeModule() {
        StrategyNodeSerializer serializer = new StrategyNodeSerializer();
        StrategyNodeDeserializer deserializer = new StrategyNodeDeserializer();

        SimpleModule module = new SimpleModule("StrategyNodeModule");
        module.addSerializer(StrategyNode.class, serializer);
        module.addSerializer(BooleanNode.class, serializer);
        module.addSerializer(UnaryBooleanNode.class, serializer);
        module.addSerializer(CompareNode.class, serializer);
        module.addSerializer(MarketFieldCompareNode.class, serializer);
        module.addDeserializer(StrategyNode.class, deserializer);
        return module;
    }
}
