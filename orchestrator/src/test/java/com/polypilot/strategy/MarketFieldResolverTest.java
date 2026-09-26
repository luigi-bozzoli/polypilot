package com.polypilot.strategy;

import com.polypilot.entity.SentimentScore;
import com.polypilot.enums.SentimentType;
import com.polypilot.market.entity.Market;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.repository.SentimentScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketFieldResolverTest {

    private static final String MARKET_ID = "0xabc";

    @Mock
    private MarketRepository marketRepository;

    @Mock
    private SentimentScoreRepository sentimentScoreRepository;

    private MarketFieldResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new MarketFieldResolver(marketRepository, sentimentScoreRepository);
    }

    @Test
    void marketFoundAndSentimentFound_copiesEveryFieldIntoTheSnapshot() {
        Market market = Market.builder()
                .id(MARKET_ID)
                .upPrice(new BigDecimal("0.6"))
                .downPrice(new BigDecimal("0.4"))
                .volume24h(new BigDecimal("100000"))
                .liquidity(new BigDecimal("10000"))
                .build();
        SentimentScore sentiment = SentimentScore.builder()
                .sentiment(SentimentType.BULLISH)
                .confidence(new BigDecimal("0.85"))
                .build();
        when(marketRepository.findById(MARKET_ID)).thenReturn(Optional.of(market));
        when(sentimentScoreRepository.findFirstByMarketIdOrderByScoredAtDesc(MARKET_ID))
                .thenReturn(Optional.of(sentiment));

        MarketFieldSnapshot snapshot = resolver.resolve(MARKET_ID);

        assertThat(snapshot.getUpPrice()).isEqualByComparingTo("0.6");
        assertThat(snapshot.getDownPrice()).isEqualByComparingTo("0.4");
        assertThat(snapshot.getVolume24h()).isEqualByComparingTo("100000");
        assertThat(snapshot.getLiquidity()).isEqualByComparingTo("10000");
        assertThat(snapshot.getSentiment()).isEqualTo(SentimentType.BULLISH);
        assertThat(snapshot.getSentimentConfidence()).isEqualByComparingTo("0.85");
    }

    @Test
    void marketFoundButNoSentiment_snapshotHasNullSentimentFields() {
        Market market = Market.builder()
                .id(MARKET_ID)
                .upPrice(new BigDecimal("0.6"))
                .build();
        when(marketRepository.findById(MARKET_ID)).thenReturn(Optional.of(market));
        when(sentimentScoreRepository.findFirstByMarketIdOrderByScoredAtDesc(MARKET_ID))
                .thenReturn(Optional.empty());

        MarketFieldSnapshot snapshot = resolver.resolve(MARKET_ID);

        assertThat(snapshot.getUpPrice()).isEqualByComparingTo("0.6");
        assertThat(snapshot.getSentiment()).isNull();
        assertThat(snapshot.getSentimentConfidence()).isNull();
    }

    @Test
    void marketNotFound_throws404() {
        when(marketRepository.findById(MARKET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(MARKET_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
    }

    @Test
    void usesTheLatestSentimentQuery_notAHistoricalWindow() {
        Market market = Market.builder().id(MARKET_ID).build();
        when(marketRepository.findById(MARKET_ID)).thenReturn(Optional.of(market));
        when(sentimentScoreRepository.findFirstByMarketIdOrderByScoredAtDesc(MARKET_ID))
                .thenReturn(Optional.empty());

        resolver.resolve(MARKET_ID);

        verify(sentimentScoreRepository).findFirstByMarketIdOrderByScoredAtDesc(MARKET_ID);
    }
}
