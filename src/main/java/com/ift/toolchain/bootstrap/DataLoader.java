package com.ift.toolchain.bootstrap;

import com.ift.toolchain.Service.DataInitService;
import com.ift.toolchain.Service.OrbitService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DataLoader implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    DataInitService dataInitService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {

        // Read Orbit info

//         String orbitFileName = "C:\\Users\\zhijiang\\Documents\\Projects\\toolchain\\Crosslink Scenario Data\\GPS_TLE_31.txt";
        String orbitFileName = "/root/toolchain/Crosslink Scenario Data/GPS_TLE_31.txt";
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


        // Read configuration file from OMNet? Jsat track?
        JSONParser parser = new JSONParser();
        try{
//            Object obj = parser.parse(new FileReader("C:\\Users\\zhijiang\\Documents\\Projects\\toolchain\\Crosslink Scenario Data\\gpsCLscnwAppdefv4.json"));
            Object obj = parser.parse(new FileReader("/root/toolchain/Crosslink Scenario Data/gpsCLscnwAppdefv4.json"));
            JSONObject jsonObject = (JSONObject)obj;

            // Get satellite id.
            JSONArray satelliteArray = (JSONArray)((JSONObject)jsonObject.get("SatcomScnDef")).get("sateDef");

            // Loop array and update satellite init params
            satelliteArray.forEach(satellite -> {
                String satelliteName = ((JSONObject)satellite).get("satName").toString();
                String satelliteId = ((JSONObject)satellite).get("satID").toString();
               // Set param to maps
               Map params = new HashMap();
               JSONArray paramsArray = (JSONArray)((JSONObject)satellite).get("Config");
               paramsArray.forEach(param -> params.put(((JSONObject)param).get("paraName").toString(),
                       ((JSONObject)param).get("value").toString()));


               // Update satellite
                dataInitService.updateSatellite(satelliteName, satelliteId, params);
            });

            // Ground stations
            JSONArray groundStationArray = (JSONArray)((JSONObject)jsonObject.get("SatcomScnDef")).get("userDef");

            // Loop array and save to ground station table
            groundStationArray.forEach(groundStation -> {
                String gName = ((JSONObject)groundStation).get("name").toString();
                String gId = ((JSONObject)groundStation).get("ID").toString();
                // Set param to Maps
                Map params = new HashMap();
                JSONArray paramsArray = (JSONArray)((JSONObject)groundStation).get("Config");
                paramsArray.forEach(param -> params.put(((JSONObject)param).get("paraName").toString(),
                        ((JSONObject)param).get("value").toString()));

                dataInitService.initGroundStation(gName, gId, params);

            });
        }catch (Exception ex){
            ex.printStackTrace();
        }

    }
}
