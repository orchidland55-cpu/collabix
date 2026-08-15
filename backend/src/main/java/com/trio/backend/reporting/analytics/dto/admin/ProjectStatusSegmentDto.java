package com.trio.backend.reporting.analytics.dto.admin;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProjectStatusSegmentDto {
    String status;
    String label;
    long count;
    double percentage;
}
