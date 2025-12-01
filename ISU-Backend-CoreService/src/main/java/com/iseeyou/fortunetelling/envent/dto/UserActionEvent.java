package com.iseeyou.fortunetelling.envent.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class UserActionEvent {
    private String eventId;
    private String userId;
    private String role;
    private String action;
    private BigDecimal amount;
}
