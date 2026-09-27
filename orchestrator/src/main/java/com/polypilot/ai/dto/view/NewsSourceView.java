package com.polypilot.ai.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One article backing a {@link MarketNewsSummaryView}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsSourceView {
    private String title;
    private String url;
    private String publisher;
    private String publishedAt;
}
