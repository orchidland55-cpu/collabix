package com.trio.backend.reporting.analytics.dto.admin;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class ActivityOverviewPointDto {
    LocalDate date;
    String label;
    long value;
}
