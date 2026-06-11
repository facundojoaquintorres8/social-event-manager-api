package com.socialeventmanager.event.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BalanceParticipantDTO {
    private String name;
    private BigDecimal paid;
    private BigDecimal shouldPay;
    private BigDecimal balance;
}