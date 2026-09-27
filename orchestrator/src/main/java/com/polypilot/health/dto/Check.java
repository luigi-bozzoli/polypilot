package com.polypilot.health.dto;

import lombok.Value;

/**
 * One deploy / schema check.
 *
 * <ul>
 *   <li>{@code name} — machine-readable check name, shown as the card label</li>
 *   <li>{@code status} — {@code "pass"} or {@code "fail"}</li>
 *   <li>{@code detail} — single display-ready line of detail</li>
 * </ul>
 */
@Value
public class Check {
    String name;
    String status;
    String detail;
}
