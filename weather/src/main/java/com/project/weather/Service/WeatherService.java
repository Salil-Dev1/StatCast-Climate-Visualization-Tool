package com.project.weather.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.project.weather.Models.Forecast;
import com.project.weather.Models.Weather;
import com.project.weather.Models.ZonesMonsoon;
import com.project.weather.Repository.WeatherRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.sql.Array;
import java.sql.Time;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRulesProvider;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class WeatherService {


    @Autowired
    WeatherRepository weatherRepository;

    public List<Weather> getData(){
        return weatherRepository.findAll(); // Return the list of Weather data
    }
    public List<Weather> getWeatherHistory(String location, int days){
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        //save the date in fromDate which is x days before the present date.
        return weatherRepository.findRecentWeatherData(location,fromDate);
    }


    private final List<ZonesMonsoon> boxes = loadBoxesSafely();

    private List<ZonesMonsoon> loadBoxesSafely() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = new ClassPathResource("Monsoon_Zones.json").getInputStream();
            List<ZonesMonsoon> loaded = mapper.readValue(is,
                    new TypeReference<List<ZonesMonsoon>>() {});
            return (loaded == null) ? Collections.emptyList() : loaded;
        } catch (Exception e) {
            System.err.println("Failed to load monsoon_zones.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }





    public double calculateWeightedMovingAverage(List<Double> values) {
        int size = values.size();
        double weightedSum = 0;
        double totalWeight = 0;
        for (int i = 0; i < size; i++) {
            double weight = i + 1; // More weight to recent data
            weightedSum += values.get(i) * weight;
            totalWeight += weight;
        }
        return weightedSum / totalWeight;
    }


    public double exponentialSmoothing(List<Double> values, double alpha) {
        double smoothed = values.get(0);

        for (int i = 1; i < values.size(); i++) {
            smoothed = alpha * values.get(i) + (1 - alpha) * smoothed;
        }

        return smoothed;
    }

    public double linearRegression(List<Double> values) {
        int n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;
        return intercept + slope * (n + 1); // Predict next day's temp
    }

    public double forecastTemperature(List<Double> weatherElement) {
        double wma = calculateWeightedMovingAverage(weatherElement);
        double es = exponentialSmoothing(weatherElement, 0.5); // Alpha = 0.5 for smoothing
        double lr = linearRegression(weatherElement);
        return (wma + es + lr) / 3; // Blend all three methods
    }

    private final List<ZonesMonsoon> zones = loadZonesSafely();

    private List<ZonesMonsoon> loadZonesSafely() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = new ClassPathResource("Monsoon_Zones.json").getInputStream();
            List<ZonesMonsoon> loaded = mapper.readValue(is,
                    new TypeReference<List<ZonesMonsoon>>() {});
            return (loaded == null) ? Collections.emptyList() : loaded;
        } catch (Exception e) {
            // Log a warning and return an empty list if JSON fails
            System.err.println("Failed to load monsoon_zones.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // (b) Helper to parse strings like "late June" or "June 10" into a LocalDate in the given year.
    private LocalDate parseZoneDate(String raw, int year) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        // Remove citations
        String normalized = raw.toLowerCase().replaceAll("\\[.*?\\]", "").trim();
        if (normalized.contains("early")) normalized = normalized.replace("early", "05");
        if (normalized.contains("mid"))   normalized = normalized.replace("mid",   "15");
        if (normalized.contains("late"))  normalized = normalized.replace("late",  "25");

        String[] parts = normalized.split("\\s+");
        String monthToken;
        int dayOfMonth;

        if (parts.length == 2) {
            // e.g. ["10", "june"]
            String first = parts[0].replaceAll("[^0-9]", "");
            String second = parts[1];
            if (first.matches("\\d+")) {
                dayOfMonth = Integer.parseInt(first);
                monthToken = second;
            } else {
                return null;
            }
        } else if (parts.length == 1) {
            // e.g. ["june"] → default to day 15
            monthToken = parts[0];
            dayOfMonth = 15;
        } else {
            return null; // too many tokens
        }

        int monthNum = getMonthNumber(monthToken);
        if (monthNum < 1 || monthNum > 12) {
            return null;
        }

        try {
            return LocalDate.of(year, monthNum, dayOfMonth);
        } catch (DateTimeException ex) {
            return null;
        }
    }

    private int getMonthNumber(String month) {
        switch (month.toLowerCase()) {
            case "january":   return 1;
            case "february":  return 2;
            case "march":     return 3;
            case "april":     return 4;
            case "may":       return 5;
            case "june":      return 6;
            case "july":      return 7;
            case "august":    return 8;
            case "september": return 9;
            case "october":   return 10;
            case "november":  return 11;
            case "december":  return 12;
            default:          return 0;
        }
    }

    // Utility to clamp a value between two bounds
    private double clamp(double min, double max, double value) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }


    /*public double prototypeCalculateRainProbability(
            List<Double> humidities,
            List<Double> pressures,
            List<Double> cloudPercents,
            List<Double> windSpeeds,
            double latitude,
            double longitude) {

        // 1) If no data, fallback to 10% chance.

        // 2) Compute averages of the weather parameters:
        double avgHumidity = humidities.stream()
                .mapToDouble(Double::doubleValue) // Convert Double objects to primitive double
                .average()
                .orElse(0);
        double avgPressure   = pressures.stream()
                .mapToDouble(Double::doubleValue) // Convert Double objects to primitive double
                .average()
                .orElse(0);
        double avgWindSpeed  = windSpeeds.stream()
                .mapToDouble(Double::doubleValue) // Convert Double objects to primitive double
                .average()
                .orElse(0);
        double avgCloudCover = cloudPercents.stream()
                .mapToDouble(Double::doubleValue) // Convert Double objects to primitive double
                .average()
                .orElse(0);

        // 3) Build “meteorological confidence” C (0.0–1.0) from these four factors:
        //    a) humidityFactor = avgHumidity / 100.0
        double humidityFactor = clamp(0.0, 1.0, avgHumidity / 100.0);

        //    b) pressureFactor: lower-than-standard pressures → higher factor
        //       We assume “1013 hPa” is “neutral.” Each 1 hPa below 1013 adds 1/20 to factor,
        //       capped at 1.0. Pressures above 1013 give negative diff → clamp to 0.
        double pressureDiff   = 1013.0 - avgPressure;         // e.g. if avgPressure=1003 → diff=10
        double pressureFactor = clamp(0.0, 1.0, pressureDiff / 20.0);

        //    c) cloudFactor = avgCloudCover / 100.0
        double cloudFactor = clamp(0.0, 1.0, avgCloudCover / 100.0);

        //    d) windFactor: low wind helps clouds form (0–5 km/h → 0.7), moderate (5–15 → 0.5),
        //                   high (>15 → 0.2)
        double windFactor;
        if (avgWindSpeed <= 5.0) {
            windFactor = 0.7;
        } else if (avgWindSpeed <= 15.0) {
            windFactor = 0.5;
        } else {
            windFactor = 0.2;
        }

        //    e) Confidence = (humidityFactor + pressureFactor + cloudFactor + windFactor) / 4
        double confidence = (humidityFactor + pressureFactor + cloudFactor + windFactor) / 4.0;

        // 4) Compute “area coverage” A from cloud cover alone (as a simple proxy):
        //    If avgCloudCover = 80, assume 80% of the area is covered by rain‐producing clouds.
        double areaCoverage = clamp(0.0, 1.0, avgCloudCover / 100.0);

        // 5) Base PoP (0–100) = confidence × areaCoverage × 100
        double basePoP = confidence * areaCoverage * 100.0;
        basePoP = clamp(0.0, 100.0, basePoP); //value won't exceed 100 or go below 0.

        // 6) Now check if (latitude, longitude) is inside any monsoon zone that is “active today.”
        int currentMonth = LocalDate.now().getMonthValue();
        boolean inActiveMonsoonZone = false;

        for (ZonesMonsoon zone : zones) {
            // Check bounding box
            if (latitude >= zone.getLatMin() &&
                    latitude <= zone.getLatMax() &&
                    longitude >= zone.getLonMin() &&
                    longitude <= zone.getLonMax()) {

                int start = zone.getStartMonth();  // e.g. 4
                int end   = zone.getEndMonth();    // e.g. 10

                boolean seasonActive;
                if (start <= end) {
                    // Normal case (e.g. April (4) to October (10))
                    seasonActive = (currentMonth >= start && currentMonth <= end);
                } else {
                    // Cross‐year case (e.g. November (11) to March (3))
                    seasonActive = (currentMonth >= start || currentMonth <= end);
                }

                if (seasonActive) {
                    inActiveMonsoonZone = true;
                    break;
                }
            }
        }

        if (inActiveMonsoonZone) {
            basePoP += 15.0;  // monsoon‐zone boost
        }

        double finalPoP = clamp(10.0, 95.0, basePoP);
        return finalPoP;
    }
*/
    public double calculateRainProbability(List<Double> humidities, List<Double> pressures, List<Double> cloudPercents,
    List<Double> windSpeeds) {



        double avgHumidity = humidities.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgPressure = pressures.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgWindSpeed = windSpeeds.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgCloudCover = cloudPercents.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        double humidityFactor = Math.max(0.0, Math.min(1,avgHumidity/100));
        double pressureFactor = Math.max(0.0, Math.min(1,(1013 - avgPressure)/50));
        double cloudFactor = Math.max(0.0, Math.min(1,avgCloudCover/100));

        double AreaCoverage = (cloudFactor + humidityFactor + pressureFactor)/3;

        LocalDate today = LocalDate.now();

        double rainChance = 0;
        double confidence = 1;

        if (avgHumidity > 80 && avgPressure < 1010 && avgCloudCover > 50) {
            confidence += 0.2;
        }

        if(avgHumidity > 50) {
            confidence +=0.3;
        }

        else if (avgPressure < 995) {
            confidence += 0.3; // reinforce rain chance slightly
        }

        else if (avgHumidity > 70 && avgCloudCover > 50 ) {
            confidence += 0.1;
        }

        else if (avgWindSpeed > 10 && avgWindSpeed <= 15) {
            // No adjustment, so maybe explicitly comment that it’s neutral.
        }


        else if (avgWindSpeed > 15 && confidence > 0.5) {
             confidence -= 0.1;
            // reduce mildly
        }

         else if (avgWindSpeed > 5 && avgWindSpeed <= 10) {
            confidence += 0.2;
        }
         else if(avgWindSpeed < 5){
             confidence += 0.1;
        }

         double pOP = AreaCoverage * confidence;

        return Math.round(pOP*100);
    }

    private double fetchLatitude(String location){
        String fetch = convertToApi(location);
        JSONObject jsonObject = new JSONObject(fetch);
        double lat = jsonObject.getJSONObject("coord").getDouble("lat");
        return lat;
    }
    private double fetchLongitude(String location){
        String fetch = convertToApi(location);
        JSONObject jsonObject = new JSONObject(fetch);
        double lon = jsonObject.getJSONObject("coord").getDouble("lon");
        return lon;
    }



    public List<Forecast> ifNotEnoughData(String location,double lat,double lon){

            String forecastApi = convertForeCastApi(location);
            JSONObject jsonObject = new JSONObject(forecastApi);

            JSONArray list = jsonObject.getJSONArray("list");

            List<Weather> weatherDataList = new ArrayList<>();
            String lastDate = "";
            double highTemp = Double.MIN_VALUE, lowTemp = Double.MAX_VALUE;
            double totalTemp = 0, totalHumidity = 0, totalCloudPercent = 0;
            double totalWindSpeed = 0, totalPressure = 0;
            int count = 0;

            String weatherCondition = "";
            List<Double> temperatures = new ArrayList<>();// temp data for 3 days
            List<Double> highTemps = new ArrayList<>(); //highTemps list including
            List<Double> lowTemps = new ArrayList<>();// similar
            List<Double> humidities = new ArrayList<>();
            List<Double> pressures = new ArrayList<>();
            List<Double> cloudPercents = new ArrayList<>();
            List<Double> windSpeeds = new ArrayList<>();
            List<String> weatherConditions = new ArrayList<>();

            List<Forecast> forecastList = new ArrayList<>();
            for (int i = 0; i < list.length(); i++) { // Loop through all timestamps
                JSONObject entry = list.getJSONObject(i);
                JSONObject main = entry.getJSONObject("main");
                String currentDate = entry.getString("dt_txt").split(" ")[0];
                double temp = main.getDouble("temp")-273.15;
                double pressure = main.getDouble("pressure");
                double humidity = main.getDouble("humidity");
                double cloudPercent = entry.getJSONObject("clouds").getDouble("all");
                double windSpeed = entry.getJSONObject("wind").getDouble("speed");
                weatherCondition = entry.getJSONArray("weather").getJSONObject(0).getString("description"); // Fix this

                if (!currentDate.equals(lastDate) && !lastDate.isEmpty()) {
                    // Save previous day's aggregated data
                    Weather weather = new Weather();
                    weather.setLocation(location);
                    weather.setRecordedAt(LocalDate.parse(lastDate).atStartOfDay()); // Corrected
                    weather.setTemp(totalTemp / count);
                    weather.setHighTemp(highTemp);
                    weather.setLowTemp(lowTemp);
                    weather.setHumidity(totalHumidity / count);
                    weather.setCloudPercentage(totalCloudPercent / count);
                    weather.setPressure(totalPressure / count);
                    weather.setWinds(totalWindSpeed / count);
                    weather.setWeatherCondition(weatherCondition);
                    weather.setPrecipitationChance((double)Math.round(calculateRainProbability(humidities,pressures,cloudPercents,windSpeeds)));
                    weatherDataList.add(weather);

                    temperatures.add(totalTemp / count);
                    highTemps.add(highTemp);
                    lowTemps.add(lowTemp);
                    humidities.add(totalHumidity / count);
                    pressures.add(totalPressure / count);
                    cloudPercents.add(totalCloudPercent / count);
                    windSpeeds.add(totalWindSpeed / count);
                    weatherConditions.add(weatherCondition);

                    // Reset for new day
                    highTemp = Double.MIN_VALUE;
                    lowTemp = Double.MAX_VALUE;
                    totalTemp = totalHumidity = totalCloudPercent = totalWindSpeed = totalPressure = 0;
                    count = 0;
                    weatherCondition = "";
                }

                // Aggregate data
                highTemp = Math.max(highTemp, temp);
                lowTemp = Math.min(lowTemp, temp);
                totalTemp += temp;
                totalHumidity += humidity;
                totalCloudPercent += cloudPercent;
                totalPressure += pressure;
                totalWindSpeed += windSpeed;
                count++;
                lastDate = currentDate;
            }

            // Store last day's data
            if (!lastDate.isEmpty()) {
                Weather weather = new Weather();
                weather.setLocation(location);

                weather.setRecordedAt(LocalDate.parse(lastDate).atStartOfDay()); // Corrected parsing
                weather.setTemp(totalTemp / count);
                weather.setHighTemp(highTemp);
                weather.setLowTemp(lowTemp);
                weather.setHumidity(totalHumidity / count);
                weather.setCloudPercentage(totalCloudPercent / count);
                weather.setPressure(totalPressure / count);
                weather.setWinds(totalWindSpeed / count);
                weather.setWeatherCondition(weatherCondition);
                weather.setPrecipitationChance((double)Math.round(calculateRainProbability(humidities,pressures,cloudPercents,windSpeeds)));

                temperatures.add(totalTemp / count);
                highTemps.add(highTemp);
                lowTemps.add(lowTemp);
                humidities.add(totalHumidity / count);
                pressures.add(totalPressure / count);
                cloudPercents.add(totalCloudPercent / count);
                windSpeeds.add(totalWindSpeed / count);
                weatherConditions.add(weatherCondition);

                weatherDataList.add(weather);
            }

            weatherRepository.saveAll(weatherDataList);

            List<Double> predictedHumidities = new ArrayList<>();
            List<Double> predictedClouds = new ArrayList<>();
            List<Double> predictedWindspeedslist = new ArrayList<>();
            List<Double> predictedPressures = new ArrayList<>();
            //foreCast Logic Starts here
            for (int i = 1; i <= 3; i++) {
                double predictedHighTemps = forecastTemperature(highTemps.subList(0, Math.min(highTemps.size(), i + 1)));
                double predictedTemperature = forecastTemperature(temperatures.subList(0, Math.min(temperatures.size(), i + 1)));
                double predictedLowTemps = forecastTemperature(lowTemps.subList(0, Math.min(lowTemps.size(), i + 1)));
                double predictedHumidity = forecastTemperature(humidities.subList(0, Math.min(humidities.size(), i + 1)));
                double predictedCloudPercent = forecastTemperature(cloudPercents.subList(0, Math.min(cloudPercents.size(), i + 1)));
                double predictedWindSpeeds = forecastTemperature(windSpeeds.subList(0, Math.min(windSpeeds.size(), i + 1)));
                double predictedPressure = forecastTemperature(pressures.subList(0,Math.min(pressures.size(), i+1)));

                predictedHumidities.add(predictedHumidity);
                predictedClouds.add(predictedCloudPercent);
                predictedPressures.add(predictedPressure);
                predictedWindspeedslist.add(predictedWindSpeeds);

                double predictedPrecipitation = calculateRainProbability(predictedHumidities,predictedPressures,predictedClouds,predictedWindspeedslist);
                String predictedWeatherCondition;
                if (predictedPrecipitation >= 80 && predictedCloudPercent > 50) predictedWeatherCondition = "Rainy";
                else if (predictedCloudPercent > 50) predictedWeatherCondition = "Cloudy";
                else predictedWeatherCondition = "Sunny";

                Forecast forecast = new Forecast();
                forecast.setLocation(location);
                forecast.setPredictedTemperature((double) Math.round(predictedTemperature));
                forecast.setPredictedWinds((double) Math.round(predictedWindSpeeds));
                forecast.setPredictionDate(LocalDate.now().plusDays(i));
                forecast.setPredictedHighTemp((double) Math.round(predictedHighTemps));
                forecast.setPredictedLowTemp((double) Math.round(predictedLowTemps));
                forecast.setPredictedHumidity(Math.round(predictedHumidity));
                forecast.setPredictedPrecipitationChance((double) Math.round(predictedPrecipitation));
                forecast.setWeatherCondition(predictedWeatherCondition);
                forecastList.add(forecast);
            }
            return forecastList;

    }


    public List<Forecast> getThreeDayForecast(String location) {
        List<Forecast> forecastList = new ArrayList<>();

            LocalDateTime fromDate = LocalDateTime.now().minusDays(3);
            List<Weather> recentData = weatherRepository.findRecentWeatherData(location, fromDate);

            double lat = fetchLatitude(location);
            double lon = fetchLongitude(location);

            if (recentData.size() < 3) { //Not enough data to predict, fetch new data and process it
                return ifNotEnoughData(location,lat,lon);
            }

            //if Data exists, read existing values
            //create a list of all the Entities for 3 days.
            List<Double> temperatures = recentData.stream().map(Weather::getTemp).toList(); // temp data for 3 days
            List<Double> highTemps = recentData.stream().map(Weather::getHighTemp).toList(); //highTemps list including
            List<Double> lowTemps = recentData.stream().map(Weather::getLowTemp).toList(); // similar
            List<Double> humidities = recentData.stream().map(Weather::getHumidity).toList();
            List<Double> pressures = recentData.stream().map(Weather::getPressure).toList();
            List<Double> cloudPercent = recentData.stream().map(Weather::getCloudPercentage).toList();
            List<Double> windSpeeds = recentData.stream().map(Weather::getWinds).toList();
             // create a List of Forecast POJO type

            System.out.println("Already existing temperatures before the predicted loop "+ temperatures);

            List<Double> predictedPressures = new ArrayList<>();
            List<Double> predictedHumidities = new ArrayList<>();
            List<Double> predictedClouds = new ArrayList<>();
            List<Double> predictedWindspeedslist = new ArrayList<>();

            for (int i = 1; i <= 3; i++) {
                double predictedHighTemps = forecastTemperature(highTemps.subList(0, Math.min(highTemps.size(), i + 1)));
                double predictedTemperature = forecastTemperature(temperatures.subList(0, Math.min(temperatures.size(), i + 1)));
                double predictedLowTemps = forecastTemperature(lowTemps.subList(0, Math.min(lowTemps.size(), i + 1)));
                double predictedHumidity = forecastTemperature(humidities.subList(0, Math.min(humidities.size(), i + 1)));
                double predictedCloudPercent = forecastTemperature(cloudPercent.subList(0, Math.min(cloudPercent.size(), i + 1)));
                double predictedWindSpeeds = forecastTemperature(windSpeeds.subList(0, Math.min(windSpeeds.size(), i + 1)));
                double predictedPressure = forecastTemperature(pressures.subList(0,Math.min(pressures.size(),i+1)));

                predictedHumidities.add(predictedHumidity);
                predictedClouds.add(predictedCloudPercent);
                predictedPressures.add(predictedPressure);
                predictedWindspeedslist.add(predictedWindSpeeds);

                double predictedPrecipitation = calculateRainProbability(predictedHumidities, predictedPressures, predictedClouds,predictedWindspeedslist);
                String predictedWeatherCondition;
                System.out.println("predicted High Temperature" + predictedHighTemps);
                System.out.println("predicted Low Temperature " + predictedLowTemps);
                System.out.println("predicted Temperatures "+ predictedTemperature);
            /* These lines are working like,
            forecastTemperature is a method that calculate moving averages from a list of elements
            .subList is a new list which contains elements from (given index to given index)
            Math.min(highTemps.size(),i+1)  i.e. minimum of highTemps.size() i.e. 5 and i+1= 2
            (since i = 1 in first iteration)
            So, Math.min(5,2) = 2;
            So in every iteration it includes elements from 0 to i+1, thereby increasing the list elements in each
            iteration, and save the moving average of all elements in it.

            */
                if (predictedPrecipitation >= 80 && predictedCloudPercent > 50) predictedWeatherCondition = "Rainy";
                else if (predictedCloudPercent > 50) predictedWeatherCondition = "Cloudy";
                else predictedWeatherCondition = "Sunny";

                Forecast forecast = new Forecast();
                forecast.setLocation(location);
                forecast.setPredictedTemperature((double) Math.round(predictedTemperature));
                forecast.setPredictedWinds((double) Math.round(predictedWindSpeeds));
                forecast.setPredictionDate(LocalDate.now().plusDays(i));
                forecast.setPredictedHighTemp((double) Math.round(predictedHighTemps));
                forecast.setPredictedLowTemp((double) Math.round(predictedLowTemps));
                forecast.setPredictedHumidity((double) Math.round(predictedHumidity));
                forecast.setPredictedPrecipitationChance((double) Math.round(predictedPrecipitation));
                forecast.setWeatherCondition(predictedWeatherCondition);
                forecastList.add(forecast);
            }
            Weather weather = new Weather();
            weather.setLocation(location);
            pushData(weather);
            return forecastList;
        }




    public void fetchHighLow(List<Double> dailyHighTemps, List<Double> dailyLowTemps, String location){

            //for Fetching High and Low Temperatures
            String forecastApi = convertForeCastApi(location);
            JSONObject jsonObject = new JSONObject(forecastApi);
            JSONArray forecastList = jsonObject.getJSONArray("list");
            double highTemp = Double.MIN_VALUE;
            double lowTemp = Double.MAX_VALUE;
            String lastDate = "";

            for (int i = 0; i < forecastList.length(); i++) {
                JSONObject forecast = forecastList.getJSONObject(i);
                double temp = forecast.getJSONObject("main").getDouble("temp") - 273.15; // Convert Kelvin to Celsius
                String currentDate = forecast.getString("dt_txt").split(" ")[0]; // Extract date

                if (!currentDate.equals(lastDate) && !lastDate.isEmpty()) {
                    // Save previous day's high/low
                    System.out.println("High Temp: " + highTemp + ", Low Temp: " + lowTemp + " for " + lastDate);
                    highTemp = Double.MIN_VALUE;
                    lowTemp = Double.MAX_VALUE;
                }
                highTemp = Math.max(highTemp, temp);
                lowTemp = Math.min(lowTemp, temp);
                lastDate = currentDate;
                dailyLowTemps.add(lowTemp);
                dailyHighTemps.add(highTemp);
            }
            dailyLowTemps.add(lowTemp);
            dailyHighTemps.add(highTemp);

    }


    public ResponseEntity<Weather> pushData(Weather weather){

            List<Weather> existingRecord = weatherRepository.findRecentWeatherData(weather.getLocation(), LocalDateTime.now());
            if (!existingRecord.isEmpty()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(existingRecord.get(0)); // Prevent duplicate entry
            }


            String record = convertToApi(weather.getLocation());
            List<Double> dailyHighTemps = new ArrayList<>();
            List<Double> dailyLowTemps = new ArrayList<>();
            double highTemp = 0;
            double lowTemp = 0;
            fetchHighLow(dailyHighTemps,dailyLowTemps,weather.getLocation());
            highTemp = forecastTemperature(dailyHighTemps);
            lowTemp = forecastTemperature(dailyLowTemps);
            JSONObject jsonObject = new JSONObject(record);

            List<Double> humidities = new ArrayList<>();
            List<Double> cloudPercents = new ArrayList<>();
            List<Double> pressures = new ArrayList<>();
            List<Double> windSpeeds = new ArrayList<>();

            double lon = jsonObject.getJSONObject("coord").getDouble("lon");
            double lat = jsonObject.getJSONObject("coord").getDouble("lat");
            double temperature = jsonObject.getJSONObject("main").getDouble("temp") - 273.15;
            double windSpeed = jsonObject.getJSONObject("wind").getDouble("speed");
            double humidity = jsonObject.getJSONObject("main").getDouble("humidity");
            double pressure = jsonObject.getJSONObject("main").getDouble("pressure");
            double cloudPercentage = jsonObject.getJSONObject("clouds").getDouble("all");
            JSONArray weatherArray = jsonObject.getJSONArray("weather");
            String description = weatherArray.getJSONObject(0).getString("description");

            List<Weather> records = new ArrayList<>();

            humidities.add(humidity); //add only today's entry to this list to pass it to calculate rain probability
            pressures.add(pressure);//add single entry to pass it to calculate rain probability
            windSpeeds.add(windSpeed);
            cloudPercents.add(cloudPercentage);

            //double rainProbability = calculateRainProbability(records,lat,lon);
            weather.setTemp(Math.round(temperature));
            weather.setHumidity(humidity);
            weather.setPressure(pressure);
            weather.setWeatherCondition(description);
            weather.setCloudPercentage(cloudPercentage);
            weather.setWinds(Math.round(windSpeed));
            weather.setPrecipitationChance(calculateRainProbability(humidities,pressures,cloudPercents,windSpeeds));
            weather.setHighTemp(Math.round(highTemp));
            weather.setLowTemp(Math.round(lowTemp));
            return ResponseEntity.ok(weatherRepository.save(weather));
        }


    public String convertToApi(String location){

            String apiId = "e9ef9764db93deb810444e4b7fd55ad0";
            String CurrentApiUrl = "https://api.openweathermap.org/data/2.5/weather?q="+location+"&appid="+apiId;
            RestTemplate restTemplate = new RestTemplate();
            return restTemplate.getForObject(CurrentApiUrl, String.class);
    }

    public String convertForeCastApi(String location){

            String apiKey = "e9ef9764db93deb810444e4b7fd55ad0";
            String forecastApi = "https://api.openweathermap.org/data/2.5/forecast?q="+location+"&appid="+apiKey;
            RestTemplate restTemplate = new RestTemplate();
            return restTemplate.getForObject(forecastApi,String.class);
    }

    public List<Double> pastTemperatures(String location){

            List<Double> pastTemperatures = new ArrayList<>();
            String link = convertForeCastApi(location);
            JSONObject jsonObject = new JSONObject(link);
            JSONArray list = jsonObject.getJSONArray("list");
            String lastDate = "";
            double totalTemp = 0;
            double count = 0;
            for(int i = 0;i<list.length();i++){
                JSONObject entry = list.getJSONObject(i);
                JSONObject main = entry.getJSONObject("main");
                String currentDate = entry.getString("dt_txt").split(" ")[0];
                double temp = main.getDouble("temp");


                if(!currentDate.equals(lastDate) && (!lastDate.isEmpty())){
                    pastTemperatures.add((double)Math.round(totalTemp/count));
                    count = 0;
                    totalTemp = 0;
                }
                lastDate = currentDate;
                totalTemp += temp-273.15;
                count++;

            }
            if(!lastDate.isEmpty()){
                pastTemperatures.add((double)Math.round(totalTemp/count));
            }
            return pastTemperatures;

    }


}
