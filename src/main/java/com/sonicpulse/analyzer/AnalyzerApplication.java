package com.sonicpulse.analyzer;

import com.sonicpulse.analyzer.dto.ArtistDto;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(SecurityConfig.class)
public class AnalyzerApplication {

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(AnalyzerApplication.class, args);
    }
}