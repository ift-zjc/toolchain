package com.ift.toolchain.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;


@Entity
@Data
public class Tle {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

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
