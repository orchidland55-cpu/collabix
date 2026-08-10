package com.trio.backend.dto.ai;

import com.trio.backend.entity.HandoverEntry;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class HandoverAIGenerateRequest {

    @NotNull
    private UUID workspaceId;

    @NotNull
    private UUID departmentId;

    @NotNull
    private UUID projectId;

    private LocalDate date;

    private HandoverEntry.Shift shift;
}
