package com.ift.toolchain.dto;

import lombok.Data;

@Data
public class TLEDto {

    private String name;
    private String number;
    private String classification;
    private double inclination;
    private double ascensionAscending;
    private double eccentricity;
    private double perigeeArgument;
    private double meanAnomaly;
    private double meanMotion;

    private int epochYear;                  // last 2 digits
    private int year;
    private int month;
    private int day;
    private int hour;
    private int minute;
    private int second;
    private int millionsecond;
    private String julianDateStr;

    private double julianFraction;          // epoch julian day with fraction from current year Jan/1

}
