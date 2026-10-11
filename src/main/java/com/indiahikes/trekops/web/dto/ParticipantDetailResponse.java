package com.indiahikes.trekops.web.dto;

import java.util.List;

/** A participant with their check-ins in time order. */
public record ParticipantDetailResponse(
    ParticipantResponse participant, List<CheckInResponse> checkIns) {}
