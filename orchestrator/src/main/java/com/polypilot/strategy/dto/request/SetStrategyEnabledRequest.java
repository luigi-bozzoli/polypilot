package com.polypilot.strategy.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body of {@code PUT /strategies/{id}/enabled} — flips a strategy's schedulable state. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetStrategyEnabledRequest {

    @NotNull
    private Boolean enabled;
}
