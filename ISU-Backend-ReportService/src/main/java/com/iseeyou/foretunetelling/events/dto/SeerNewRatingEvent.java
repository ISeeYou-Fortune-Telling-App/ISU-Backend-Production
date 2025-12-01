package com.iseeyou.foretunetelling.events.dto;

import com.iseeyou.foretunetelling.utils.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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