package com.socialeventmanager.event.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SettlementDTO {
    private String from;
    private String to;
    private BigDecimal amount;
}
