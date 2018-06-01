package com.ift.toolchain.bootstrap;

import com.ift.toolchain.Service.DataInitService;
import com.ift.toolchain.Service.OrbitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DataLoader implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    DataInitService dataInitService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {

        // Read Orbit info

        String orbitFileName = "C:\\Users\\zhijiang\\Documents\\Projects\\toolchain\\Crosslink Scenario Data\\GPS_TLE_31.txt";

        List<String> orbitSatellites = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(Paths.get(orbitFileName))){

            orbitSatellites = br.lines().filter(line -> !(line == null || line.length() ==0)).collect(Collectors.toList());
        }catch (IOException e){
            e.printStackTrace();
        }

        orbitSatellites.forEach(orbitSatellite -> {
            String[] orbSat = orbitSatellite.split(" ");
            String satellite = orbSat[0];

            String[] orb = orbSat[1].split("-");

            String orbName = orb[0];
            int satelliteOrder = Integer.parseInt(orb[1]);

            dataInitService.initOrbitSatellite(orbName, satellite, satelliteOrder);

            System.out.println(satellite + " " + orbName+"-"+satelliteOrder);
        });
    }
}
