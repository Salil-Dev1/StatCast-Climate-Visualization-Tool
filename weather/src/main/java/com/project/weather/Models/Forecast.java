package com.project.weather.Models;

import java.time.LocalDate;

public class Forecast {

    private String location;
    private LocalDate predictionDate;
    private double predictedTemperature;
    private double predictedHumidity;
    private double predictedHighTemp;
    private double predictedLowTemp;
    private double predictedPrecipitationChance;
    private double predictedWinds;
    private String weatherCondition;

    public double getPredictedHumidity() {
        return predictedHumidity;
    }

    public void setPredictedHumidity(double predictedHumidity) {
        this.predictedHumidity = predictedHumidity;
    }

    public double getPredictedHighTemp() {
        return predictedHighTemp;
    }

    public void setPredictedHighTemp(double predictedHighTemp) {
        this.predictedHighTemp = predictedHighTemp;
    }

    public double getpredictedLowTemp() {
        return predictedLowTemp;
    }

    public void setPredictedLowTemp(double predictedLowTemp) {
        this.predictedLowTemp = predictedLowTemp;
    }

    public double getPredictedPrecipitationChance() {
        return predictedPrecipitationChance;
    }

    public void setPredictedPrecipitationChance(double predictedPrecipitationChance) {
        this.predictedPrecipitationChance = predictedPrecipitationChance;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }

    public void setWeatherCondition(String weatherCondition) {
        this.weatherCondition = weatherCondition;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getPredictionDate() {
        return predictionDate;
    }

    public void setPredictionDate(LocalDate predictionDate) {
        this.predictionDate = predictionDate;
    }

    public Double getPredictedTemperature() {
        return predictedTemperature;
    }

    public void setPredictedTemperature(Double predictedTemperature) {
        this.predictedTemperature = predictedTemperature;
    }

    public Double getPredictedWinds() {
        return predictedWinds;
    }

    public void setPredictedWinds(Double predictedWinds) {
        this.predictedWinds = predictedWinds;
    }


}
