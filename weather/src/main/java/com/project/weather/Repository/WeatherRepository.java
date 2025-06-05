package com.project.weather.Repository;

import com.project.weather.Models.Weather;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WeatherRepository extends JpaRepository<Weather, Long> {
    @Query("SELECT w FROM Weather w WHERE w.location = :location AND w.recordedAt >= :fromDate ORDER BY w.recordedAt ASC")
    List<Weather> findRecentWeatherData(
            @Param("location") String location,  // User's input location
            @Param("fromDate") LocalDateTime fromDate // Starting date to fetch records from
    );


}
