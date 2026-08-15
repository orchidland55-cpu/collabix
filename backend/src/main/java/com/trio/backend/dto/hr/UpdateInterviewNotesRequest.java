package com.trio.backend.dto.hr;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateInterviewNotesRequest {

    @Size(max = 10000)
    private String notes;
}
