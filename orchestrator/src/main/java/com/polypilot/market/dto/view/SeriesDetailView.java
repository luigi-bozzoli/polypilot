package com.polypilot.market.dto.view;

import com.polypilot.series.enums.SeriesType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * The series detail page: series metadata plus every market that belongs to it,
 * newest first.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeriesDetailView {

    private UUID id;
    private String polymarketId;
    private String ticker;
    private String slug;
    private String title;
    private String recurrence;
    private SeriesType seriesType;

    private List<MarketView> markets;
}
