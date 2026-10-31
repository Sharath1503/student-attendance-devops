package com.attendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StudentAttendanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentAttendanceApplication.class, args);
    }
}
