package com.project.weather.Service;

import com.project.weather.Repository.WeatherRepository;
import org.assertj.core.api.ObjectEnumerableAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class weatherServiceTests {

    @Autowired
    WeatherRepository weatherRepository;

    @Test
    public void repoExistingDataTest(){
       assertNotNull(weatherRepository.findRecentWeatherData("almora", LocalDateTime.now()));
    }
    @Test
    public void repoNoDataTest(){
        assertIterableEquals(List.of(), weatherRepository.findRecentWeatherData("almora", LocalDateTime.now()));
    }



}
