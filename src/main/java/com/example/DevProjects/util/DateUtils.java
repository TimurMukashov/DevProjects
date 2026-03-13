package com.example.devprojects.util;

import java.math.BigDecimal;

public class DateUtils {

    public static String getYearString(double years) {
        int yearsInt = (int) Math.floor(years);

        if (yearsInt % 10 == 1 && yearsInt % 100 != 11)
            return "год";
        else if (yearsInt % 10 >= 2 && yearsInt % 10 <= 4 && (yearsInt % 100 < 10 || yearsInt % 100 >= 20))
            return "года";
        else
            return "лет";
    }

    public static String formatYears(Number years) {
        if (years == null) return "0 лет";
        double yearsValue = years.doubleValue();
        int yearsInt = (int) Math.floor(yearsValue);

        boolean isInteger = Math.abs(yearsValue - yearsInt) < 0.0001;

        String formattedNumber;
        if (isInteger)
            formattedNumber = String.valueOf(yearsInt);
        else
            formattedNumber = String.format("%.1f", yearsValue).replace(",", ".");

        return formattedNumber + " " + getYearString(yearsValue);
    }
}