package com.ift.toolchain.controller;


import com.ift.toolchain.Service.SatelliteService;
import com.ift.toolchain.dto.SatelliteCollection;
import com.ift.toolchain.dto.SatelliteDto;
import com.ift.toolchain.model.Orbit;
import com.ift.toolchain.model.Satellite;
import com.ift.toolchain.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class SatelliteController {

    @Autowired
    SatelliteService satelliteService;




    @GetMapping(value = "/api/satellite/init")
    public List<SatelliteCollection> initSatelliteData(){

        List<SatelliteCollection> satelliteCollections = new ArrayList<>();
        String satellitePositionFolder = "/root/data/Orbits";
        try {
            Files.list(Paths.get(satellitePositionFolder))
                    .filter(Files::isRegularFile)
                    .forEach(fileName -> {

                        String satelliteName = fileName.getFileName().toString().replaceAll(".txt", "");

                        // Get orb name for this satellite
                        Satellite satellite = satelliteService.findByName(satelliteName);
                        List<Satellite> satellites = satelliteService.getSatelliesOnSameOrb(satelliteName);
                        List<Satellite> satelliteSorted = satellites.stream().sorted(Comparator.comparing(Satellite::getOrderOnOrbit)).collect(Collectors.toList());


                        try (BufferedReader br = Files.newBufferedReader(fileName)) {
                            List<SatelliteDto> satelliteDtos = br.lines().skip(7).limit(1440)
                                    .filter(
                                            line -> !(line == null ||line.trim().length() == 0))
                                    .map(line -> {
                                        String[] lineData = line.split(",");
                                        try {
                                            double[] cartesian3 = {Double.parseDouble(lineData[1])*1000, Double.parseDouble(lineData[2])*1000, Double.parseDouble(lineData[3])*1000};
                                            SatelliteDto satelliteDto = new SatelliteDto();
                                            satelliteDto.setTime((long)Double.parseDouble(lineData[0]));
                                            satelliteDto.setCartesian3(cartesian3);



                                            return satelliteDto;
                                        }catch (Exception ex){
//                                            System.out.println(ex.getMessage());
                                        }

                                        return null;

                                    }).collect(Collectors.toList());

                            SatelliteCollection satelliteCollection = new SatelliteCollection();

                            // Find selected satellite index.
                            int satelliteIndex = 0;
                            for(int index = 0; index<satelliteSorted.size(); index++){
                                if(satellite.getId().equalsIgnoreCase(satelliteSorted.get(index).getId())){
                                    satelliteIndex = index;
                                    break;
                                }
                            }

                            if(satelliteIndex >0){
                                if(satelliteIndex<satelliteSorted.size()-1){
                                    satelliteCollection.setLeftSatelliteName(satelliteSorted.get(satelliteIndex-1).getName());
                                    satelliteCollection.setRightSatelliteName(satelliteSorted.get(satelliteIndex+1).getName());
                                }else{
                                    satelliteCollection.setLeftSatelliteName(satelliteSorted.get(satelliteIndex-1).getName());
                                    satelliteCollection.setRightSatelliteName(satelliteSorted.get(0).getName());
                                }
                            }else{
                                satelliteCollection.setLeftSatelliteName(satelliteSorted.get(satelliteSorted.size()-1).getName());
                                satelliteCollection.setRightSatelliteName(satelliteSorted.get(satelliteIndex+1).getName());
                            }

                            satelliteCollection.setOrbName(satellite.getOrbit().getName());
                            satelliteCollection.setName(satelliteName);
                            satelliteCollection.setSatellites(satelliteDtos);

                            satelliteCollections.add(satelliteCollection);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return satelliteCollections;
    }
}
