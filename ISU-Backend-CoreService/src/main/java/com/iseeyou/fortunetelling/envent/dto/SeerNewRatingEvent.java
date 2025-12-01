package com.iseeyou.fortunetelling.envent.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class SeerNewRatingEvent {
    private String eventId;
    private String seerId;
    private Integer totalRates;
    private Double avgRating;
    private String seerTier;
}