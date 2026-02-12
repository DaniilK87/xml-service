package com.xmlservice.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OfferData {
    private String vendorCode;
    private String name;
    private String categoryId;
    private BigDecimal price;
    private String currencyCode;
}
