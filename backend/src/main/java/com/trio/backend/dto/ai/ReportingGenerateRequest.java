package com.trio.backend.dto.ai;

import com.trio.backend.entity.ExecutiveReport;
import com.trio.backend.enums.AIScopeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class ReportingGenerateRequest {

    @NotNull
    private UUID workspaceId;

    private UUID departmentId;

    private UUID projectId;

    private UUID teamId;

    private AIScopeType scope;

    @NotBlank
    private String title;

    @NotNull
    private ExecutiveReport.ReportType reportType;

    private LocalDate periodStart;

    private LocalDate periodEnd;
}
