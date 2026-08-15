package com.trio.backend.reporting.analytics.dto.admin;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AdminProjectStatusResponse {
    long activeProjectCount;
    List<ProjectStatusSegmentDto> segments;
}
