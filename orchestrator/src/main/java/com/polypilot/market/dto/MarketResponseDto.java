package com.polypilot.market.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketResponseDto {

    private String id;
    private String question;

    private String conditionId;
    private String slug;

    private String twitterCardImage;
    private String resolutionSource;

    private Instant endDate;
    private Instant startDate;

    private String category;
    private String ammType;

    private String liquidity;
    private String volume;

    private String sponsorName;
    private String sponsorImage;

    private String xAxisValue;
    private String yAxisValue;

    private String denominationToken;
    private String fee;

    private String image;
    private String icon;

    private String lowerBound;
    private String upperBound;

    private String description;

    private String outcomes;
    private String outcomePrices;

    private Boolean active;
    private String marketType;
    private String formatType;

    private String lowerBoundDate;
    private String upperBoundDate;

    private Boolean closed;

    private String marketMakerAddress;

    private String closedTime;

    private Boolean wideFormat;
    private Boolean isNew;

    @JsonProperty("new")
    public void setNew(Boolean value) {
        this.isNew = value;
    }

    public Boolean getNew() {
        return isNew;
    }

    private String mailchimpTag;

    private Boolean featured;
    private Boolean archived;

    private String resolvedBy;

    private Boolean restricted;

    private Integer marketGroup;

    private String groupItemTitle;
    private String groupItemThreshold;

    private String questionID;

    private String umaEndDate;

    private Boolean enableOrderBook;

    private BigDecimal orderPriceMinTickSize;
    private BigDecimal orderMinSize;

    private String umaResolutionStatus;

    private Integer curationOrder;

    private BigDecimal volumeNum;
    private BigDecimal liquidityNum;

    private String endDateIso;
    private String startDateIso;
    private String umaEndDateIso;

    private Boolean hasReviewedDates;
    private Boolean readyForCron;
    private Boolean commentsEnabled;

    private BigDecimal volume24hr;
    private BigDecimal volume1wk;
    private BigDecimal volume1mo;
    private BigDecimal volume1yr;

    private String gameStartTime;

    private Integer secondsDelay;

    private String clobTokenIds;

    private String disqusThread;

    private String shortOutcomes;

    private String teamAID;
    private String teamBID;

    private String umaBond;
    private String umaReward;

    private Boolean fpmmLive;

    private BigDecimal liquidityAmm;
    private BigDecimal liquidityClob;

    private BigDecimal volume24hrAmm;
    private BigDecimal volume1wkAmm;
    private BigDecimal volume1moAmm;
    private BigDecimal volume1yrAmm;

    private BigDecimal volume24hrClob;
    private BigDecimal volume1wkClob;
    private BigDecimal volume1moClob;
    private BigDecimal volume1yrClob;

    private BigDecimal volumeAmm;
    private BigDecimal volumeClob;

    private BigDecimal makerBaseFee;
    private BigDecimal takerBaseFee;

    private BigDecimal customLiveness;

    private Boolean acceptingOrders;
    private Boolean notificationsEnabled;

    private BigDecimal score;


    private ImageOptimizedDto imageOptimized;

    private ImageOptimizedDto iconOptimized;


    private List<EventDto> events;

    private List<CategoryDto> categories;

    private List<TagDto> tags;


    private String creator;

    private Boolean ready;
    private Boolean funded;

    private String pastSlugs;

    private Instant readyTimestamp;
    private Instant fundedTimestamp;
    private Instant acceptingOrdersTimestamp;


    private BigDecimal competitive;

    private BigDecimal rewardsMinSize;
    private BigDecimal rewardsMaxSpread;

    private BigDecimal spread;


    private Boolean automaticallyResolved;

    private BigDecimal oneDayPriceChange;
    private BigDecimal oneHourPriceChange;
    private BigDecimal oneWeekPriceChange;
    private BigDecimal oneMonthPriceChange;
    private BigDecimal oneYearPriceChange;


    private BigDecimal lastTradePrice;
    private BigDecimal bestBid;
    private BigDecimal bestAsk;


    private Boolean automaticallyActive;

    private Boolean clearBookOnStart;


    private String chartColor;
    private String seriesColor;


    private Boolean showGmpSeries;
    private Boolean showGmpOutcome;


    private Boolean manualActivation;

    private Boolean negRiskOther;


    private String gameId;

    private String groupItemRange;

    private String sportsMarketType;


    private BigDecimal line;


    private String umaResolutionStatuses;


    private Boolean pendingDeployment;
    private Boolean deploying;


    private Instant deployingTimestamp;
    private Instant scheduledDeploymentTimestamp;


    private Boolean rfqEnabled;

    private Instant eventStartTime;


    private Boolean feesEnabled;


    private FeeScheduleDto feeSchedule;
}