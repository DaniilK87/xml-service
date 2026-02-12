package com.xmlservice.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CurrencyData {

    private String code;
    private BigDecimal rate;
}
