package com.shipping.wars.shipper.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import java.time.Instant;
import java.time.LocalDate;

@Builder
public record CreateRfqRequest(

        @NotBlank(message = "Name is required")
        String name,

        String description,

        @NotNull(message = "Bid start time is required")
        Instant bidStartTime,

        @NotNull(message = "Bid close time is required")
        Instant bidCloseTime,

        @NotNull(message = "Forced close time is required")
        Instant forcedCloseTime,

        @NotNull(message = "Pickup date is required")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate pickUpDate,

        CreateAuctionConfigRequest auctionConfig
) {
}

// Z at last indicates that the time is in UTC. For example, "2026-11-01T09:00:00Z" means 9 AM UTC on November 1, 2026.
// "bidStartTime": "2026-11-01T09:00:00Z"

//Java automatically converts this into the corresponding UTC instant.
// "bidStartTime": "2026-11-01T09:00:00+05:30"
