package com.gymplatform.dto;



import java.math.BigDecimal;

import java.util.List;



public record ProductResponse(

        Long id,

        String name,

        String codePrefix,

        String description,

        String imageUrl,

        List<ProductCategoryResponse> categories,

        int stockUnits,

        int unitsPerPackage,

        int fullPackagesAvailable,

        String packageLabel,

        String unitLabel,

        BigDecimal packagePrice,

        BigDecimal unitPrice,

        boolean sellByPackage,

        boolean sellByUnit,

        boolean outOfStock,

        boolean applyIva,

        BigDecimal ivaPercent,

        List<PriceAddonResponse> priceAddons,

        BigDecimal packagePriceWithAddons,

        BigDecimal unitPriceWithAddons

) {}

