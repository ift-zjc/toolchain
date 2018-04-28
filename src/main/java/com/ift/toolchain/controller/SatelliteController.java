package com.ift.toolchain.controller;


import com.ift.toolchain.dto.SatelliteCollection;
import com.ift.toolchain.dto.SatelliteDto;
import com.ift.toolchain.util.CommonUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class SatelliteController {


    @GetMapping(value = "/api/satellite/init")
    public Map<String, List<SatelliteDto>> initSatelliteData(){

        SatelliteCollection satelliteCollection = new SatelliteCollection();
        String satellitePositionFolder = "/Users/lastcow/Projects/toolchain/Crosslink Scenario Data/Orbit Information";
        Map<String, List<SatelliteDto>> satelliteDtoMap = new HashMap<>();
        try {
            Files.list(Paths.get(satellitePositionFolder))
                    .filter(Files::isRegularFile)
                    .forEach(fileName -> {

                        String satelliteName = fileName.getFileName().toString().replaceAll(".txt", "");
                        try (BufferedReader br = Files.newBufferedReader(fileName)) {
                            List<SatelliteDto> satelliteDtos = br.lines().skip(7).limit(1440)
                                    .filter(
                                            line -> !(line == null ||line.trim().length() == 0))
                                    .map(line -> {
                                        String[] lineData = line.split(",");
                                        try {
                                            double[] cartesian3 = CommonUtil.ecef2lla(Double.parseDouble(lineData[1])*1000,
                                                    Double.parseDouble(lineData[2])*1000,
                                                    Double.parseDouble(lineData[3])*1000);
                                            return new SatelliteDto((long)Double.parseDouble(lineData[0]), cartesian3);
                                        }catch (NumberFormatException ex){

                                        }

                                        return null;

                                    }).collect(Collectors.toList());
                            satelliteDtoMap.put(satelliteName, satelliteDtos);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return satelliteDtoMap;
    }
}
