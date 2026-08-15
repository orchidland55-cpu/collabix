package com.trio.backend.dto.ai;

import com.trio.backend.enums.AIScopeType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class AnalyticsAIGenerateRequest {

    @NotNull
    private UUID workspaceId;

    private UUID departmentId;

    private UUID projectId;

    private UUID teamId;

    private AIScopeType scope;

    private LocalDate startDate;

    private LocalDate endDate;
}
