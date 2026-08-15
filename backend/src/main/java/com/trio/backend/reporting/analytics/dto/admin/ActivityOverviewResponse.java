package com.trio.backend.reporting.analytics.dto.admin;

import com.trio.backend.enums.AdminAnalyticsPeriod;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ActivityOverviewResponse {
    AdminAnalyticsPeriod period;
    List<ActivityOverviewPointDto> points;
    long total;
}
