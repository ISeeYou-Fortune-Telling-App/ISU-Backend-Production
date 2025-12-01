package com.iseeyou.foretunetelling.events.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class UserChangeEvent {
    private String eventId;
    private String userId;
    private String role;
    private String fullName;
    private String avatarUrl;
}
