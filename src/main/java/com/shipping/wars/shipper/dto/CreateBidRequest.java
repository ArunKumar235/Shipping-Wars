package com.shipping.wars.shipper.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateBidRequest(

        @NotNull(message = "Supplier ID is required")
        Long supplierId,

        @NotNull(message = "Carrier name is required")
        String carrierName,

        @Min(value = 0, message = "Price must be a positive number")
        BigDecimal freightCharge,

        @Min(value = 0, message = "Price must be a positive number")
        BigDecimal originCharge,

        @Min(value = 0, message = "Price must be a positive number")
        BigDecimal destinationCharge,

        @Min(value = 0, message = "Transit days must be a positive number")
        Integer transitDays,

        @NotNull(message = "Quote validity date is required")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate quoteValidity
) {
}
