package com.github.robert2411.m3uplaylistfilter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class M3uPlaylistFilterApplication {

    public static void main(String[] args) {
        SpringApplication.run(M3uPlaylistFilterApplication.class, args);
    }

}
