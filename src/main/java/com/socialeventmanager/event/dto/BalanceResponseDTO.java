package com.socialeventmanager.event.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BalanceResponseDTO {
    private BigDecimal totalCost;
    private Integer participantCount;
    private BigDecimal costPerPerson;
    private List<BalanceParticipantDTO> balances;
    private List<SettlementDTO> settlements;
}
