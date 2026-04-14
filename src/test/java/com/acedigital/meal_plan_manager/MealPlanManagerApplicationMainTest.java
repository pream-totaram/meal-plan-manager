package com.acedigital.meal_plan_manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

/**
 * Coverage for {@link MealPlanManagerApplication#main(String[])}. We can't
 * actually let it boot a second context, so we stub
 * {@link SpringApplication#run(Class, String...)} statically and just
 * assert that main() (a) flips the JVM default timezone and (b) hands off
 * to {@code SpringApplication.run}.
 */
public class MealPlanManagerApplicationMainTest {

  @Test
  void main_setsTimezoneAndDelegatesToSpringApplication() {
    TimeZone original = TimeZone.getDefault();
    try (MockedStatic<SpringApplication> mocked = Mockito.mockStatic(SpringApplication.class)) {
      mocked.when(() -> SpringApplication.run(eq(MealPlanManagerApplication.class), any(String[].class)))
          .thenReturn(null);

      MealPlanManagerApplication.main(new String[] { "--server.port=0" });

      assertEquals("America/New_York", TimeZone.getDefault().getID());
      mocked.verify(() -> SpringApplication.run(
          eq(MealPlanManagerApplication.class), any(String[].class)));
    } finally {
      TimeZone.setDefault(original);
    }
  }
}
