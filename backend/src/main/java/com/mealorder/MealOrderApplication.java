package com.mealorder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.mealorder.**.mapper")
public class MealOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(MealOrderApplication.class, args);
    }
}
