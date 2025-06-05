package com.project.weather.Controllers;

import com.project.weather.Models.Forecast;
import com.project.weather.Models.Weather;
import com.project.weather.Service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/curr")
public class WeatherController {

    @Autowired
    WeatherService weatherService;

    /*@GetMapping
    public List<Weather> take(){
        return weatherService.getData();
    }*/

    /*@GetMapping
    public ResponseEntity<Weather> getAndPush(@RequestBody Weather weather){
        return weatherService.pushData(weather);
    }*/


   @PostMapping
    public ResponseEntity<Weather> push(@RequestBody Weather weather){
        return weatherService.pushData(weather);
    }

    @GetMapping("/loc")
    public List<Weather> getWeatherDataFor(@RequestParam String location,@RequestParam int days){
        return weatherService.getWeatherHistory(location,days);
    }

    @GetMapping("/getPreviousData")
    public List<Double> getPastTemperatures(@RequestParam String location){
       return weatherService.pastTemperatures(location);
    }

    @GetMapping("/forecast")
    public List<Forecast> getForecast(@RequestParam String location){
        return weatherService.getThreeDayForecast(location);
    }

}
