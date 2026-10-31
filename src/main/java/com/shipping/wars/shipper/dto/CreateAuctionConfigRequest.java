package com.shipping.wars.shipper.dto;

import com.shipping.wars.shipper.entity.TriggerType;
import jakarta.validation.constraints.Min;
import lombok.Builder;

@Builder
public record CreateAuctionConfigRequest(
        @Min(value = 0, message = "Trigger window cannot be negative")
        Integer triggerWindowInMinutes,

        @Min(value = 0, message = "Extension minutes cannot be negative")
        Integer extensionMinutes,

        TriggerType triggerType
) {
}
