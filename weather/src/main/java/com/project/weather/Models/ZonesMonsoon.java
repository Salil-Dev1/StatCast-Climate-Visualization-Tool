package com.project.weather.Models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ZonesMonsoon {
    private String name;       // Matches JSON "name"
    private double latMin;     // Matches JSON "latMin"
    private double latMax;     // Matches JSON "latMax"
    private double lonMin;     // Matches JSON "lonMin"
    private double lonMax;     // Matches JSON "lonMax"
    private int startMonth;    // Matches JSON "startMonth"
    private int endMonth;      // Matches JSON "endMonth"
    private String type;       // Matches JSON "type"

    // Getters and setters:

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public double getLatMin() {
        return latMin;
    }
    public void setLatMin(double latMin) {
        this.latMin = latMin;
    }

    public double getLatMax() {
        return latMax;
    }
    public void setLatMax(double latMax) {
        this.latMax = latMax;
    }

    public double getLonMin() {
        return lonMin;
    }
    public void setLonMin(double lonMin) {
        this.lonMin = lonMin;
    }

    public double getLonMax() {
        return lonMax;
    }
    public void setLonMax(double lonMax) {
        this.lonMax = lonMax;
    }

    public int getStartMonth() {
        return startMonth;
    }
    public void setStartMonth(int startMonth) {
        this.startMonth = startMonth;
    }

    public int getEndMonth() {
        return endMonth;
    }
    public void setEndMonth(int endMonth) {
        this.endMonth = endMonth;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
}
