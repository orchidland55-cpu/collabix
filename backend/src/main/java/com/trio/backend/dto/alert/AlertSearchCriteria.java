package com.trio.backend.dto.alert;

import com.trio.backend.entity.Alert;
import lombok.Getter;
import lombok.Setter;

/**
 * Search/filter criteria for the alerts list endpoint.
 */
@Getter
@Setter
public class AlertSearchCriteria {

    private Alert.AlertStatus status;

    private Alert.AlertType type;

    private Alert.Severity severity;
}
