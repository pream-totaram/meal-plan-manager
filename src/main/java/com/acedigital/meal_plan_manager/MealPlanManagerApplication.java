package com.acedigital.meal_plan_manager;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MealPlanManagerApplication {

  public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
    SpringApplication.run(MealPlanManagerApplication.class, args);
  }

}
