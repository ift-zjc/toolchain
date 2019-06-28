package com.ift.toolchain.bootstrap;

import com.ift.toolchain.Service.*;
import com.ift.toolchain.model.GroundStation;
import com.ift.toolchain.model.TrafficModel;
import com.ift.toolchain.model.TrafficModelConfig;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Autowired
    TrafficeModelGenericService trafficeModelGenericService;

    @Autowired
    TrafficModelConfigService trafficModelConfigService;
    @Autowired
    GroundStationService groundStationService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {

//        // Read Orbit info
//
//         String orbitFileName = "C:\\Users\\zhijiang\\Documents\\Projects\\toolchain\\Crosslink Scenario Data\\GPS_TLE_31.txt";
////        String orbitFileName = "/home/cloud_ift/toolchain/Crosslink Scenario Data/GPS_TLE_31.txt";
//        List<String> orbitSatellites = new ArrayList<>();
//
//        try (BufferedReader br = Files.newBufferedReader(Paths.get(orbitFileName))){
//
//            orbitSatellites = br.lines().filter(line -> !(line == null || line.length() ==0)).collect(Collectors.toList());
//        }catch (IOException e){
//            e.printStackTrace();
//        }
//
//        orbitSatellites.forEach(orbitSatellite -> {
//            String[] orbSat = orbitSatellite.split(" ");
//            String satellite = orbSat[0];
//
//            String[] orb = orbSat[1].split("-");
//
//            String orbName = orb[0];
//            int satelliteOrder = Integer.parseInt(orb[1]);
//
//            dataInitService.initOrbitSatellite(orbName, satellite, satelliteOrder);
//
//            System.out.println(satellite + " " + orbName+"-"+satelliteOrder);
//        });
//
//
//        // Read configuration file from OMNet? Jsat track?
//        JSONParser parser = new JSONParser();
//        try{
//            Object obj = parser.parse(new FileReader("C:\\Users\\zhijiang\\Documents\\Projects\\toolchain\\Crosslink Scenario Data\\gpsCLscnwAppdefv4.json"));
////            Object obj = parser.parse(new FileReader("/home/cloud_ift/toolchain/Crosslink Scenario Data/gpsCLscnwAppdefv4.json"));
//            JSONObject jsonObject = (JSONObject)obj;
//
//            // Get satellite id.
//            JSONArray satelliteArray = (JSONArray)((JSONObject)jsonObject.get("SatcomScnDef")).get("sateDef");
//
//            // Loop array and update satellite init params
//            satelliteArray.forEach(satellite -> {
//                String satelliteName = ((JSONObject)satellite).get("satName").toString();
//                String satelliteId = ((JSONObject)satellite).get("satID").toString();
//               // Set param to maps
//               Map params = new HashMap();
//               JSONArray paramsArray = (JSONArray)((JSONObject)satellite).get("Config");
//               paramsArray.forEach(param -> params.put(((JSONObject)param).get("paraName").toString(),
//                       ((JSONObject)param).get("value").toString()));
//
//
//               // Update satellite
//                dataInitService.updateSatellite(satelliteName, satelliteId, params);
//            });
//
//            // Ground stations
//            JSONArray groundStationArray = (JSONArray)((JSONObject)jsonObject.get("SatcomScnDef")).get("userDef");
//
//            // Loop array and save to ground station table
//            groundStationArray.forEach(groundStation -> {
//                String gName = ((JSONObject)groundStation).get("name").toString();
//                String gId = ((JSONObject)groundStation).get("ID").toString();
//                // Set param to Maps
//                Map params = new HashMap();
//                JSONArray paramsArray = (JSONArray)((JSONObject)groundStation).get("Config");
//                paramsArray.forEach(param -> params.put(((JSONObject)param).get("paraName").toString(),
//                        ((JSONObject)param).get("value").toString()));
//
//                dataInitService.initGroundStation(gName, gId, params);
//
//            });
//        }catch (Exception ex){
//            ex.printStackTrace();
//        }


        // Traffice model
        TrafficModel trafficModel = new TrafficModel();
        trafficModel.setCode("TM1");
        trafficModel.setName("One time data transmission");
        trafficModel.setDescription("Transmit a given amount of data at a given time");
        trafficModel  = trafficeModelGenericService.save(trafficModel);

        TrafficModelConfig trafficModelConfig = new TrafficModelConfig();
        trafficModelConfig.setName("time");
        trafficModelConfig.setValue("10");
        trafficModelConfig.setTrafficModel(trafficModel);
        trafficModelConfigService.save(trafficModelConfig);
        trafficModelConfig = new TrafficModelConfig();
        trafficModelConfig.setName("datavolume");
        trafficModelConfig.setValue("4000");
        trafficModelConfig.setTrafficModel(trafficModel);
        trafficModelConfigService.save(trafficModelConfig);

        // Static periodical data transmission
        trafficModel = new TrafficModel();
        trafficModel.setCode("TM2");
        trafficModel.setName("Static periodical data transmission");
        trafficModel.setDescription("Periodically transmit a fixed amount of data with fixed time interval");
        trafficModel  = trafficeModelGenericService.save(trafficModel);
        // Configuration
        trafficModelConfig = new TrafficModelConfig();
        trafficModelConfig.setName("timeinterval");
        trafficModelConfig.setValue("10");
        trafficModelConfig.setTrafficModel(trafficModel);
        trafficModelConfigService.save(trafficModelConfig);
        trafficModelConfig = new TrafficModelConfig();
        trafficModelConfig.setName("datavolume");
        trafficModelConfig.setValue("100");
        trafficModelConfig.setTrafficModel(trafficModel);
        trafficModelConfigService.save(trafficModelConfig);

        // Regular random data transmission
        trafficModel = new TrafficModel();
        trafficModel.setCode("TM3");
        trafficModel.setName("Regular random data transmission");
        trafficModel.setDescription("Multiple data transmission with random data volume and interval.");
        trafficModel  = trafficeModelGenericService.save(trafficModel);
        // Configuration
        trafficModelConfig = new TrafficModelConfig();
        trafficModelConfig.setName("timeinterval");
        trafficModelConfig.setValue("10");
        trafficModelConfig.setTrafficModel(trafficModel);
        trafficModelConfigService.save(trafficModelConfig);
        trafficModelConfig = new TrafficModelConfig();
        trafficModelConfig.setName("timeintervaldelta");
        trafficModelConfig.setValue("3");
        trafficModelConfig.setTrafficModel(trafficModel);
        trafficModelConfigService.save(trafficModelConfig);
        trafficModelConfig = new TrafficModelConfig();
        trafficModelConfig.setName("datavolume");
        trafficModelConfig.setValue("100");
        trafficModelConfig.setTrafficModel(trafficModel);
        trafficModelConfigService.save(trafficModelConfig);
        trafficModelConfig = new TrafficModelConfig();
        trafficModelConfig.setName("datavolumedelta");
        trafficModelConfig.setValue("30");
        trafficModelConfig.setTrafficModel(trafficModel);
        trafficModelConfigService.save(trafficModelConfig);

        // Small data short interval transmission
        trafficModel = new TrafficModel();
        trafficModel.setCode("TM4");
        trafficModel.setName("Small data short interval transmission");
        trafficModel.setDescription("Continuous transmission with small data volume in each transmission and short interval between transmissions. Both data volume and time interval are random.");
        trafficModel  = trafficeModelGenericService.save(trafficModel);
        // Configuration
        trafficModelConfig = new TrafficModelConfig();
        trafficModelConfig.setName("timeinterval");
        trafficModelConfig.setValue("10");
        trafficModelConfig.setTrafficModel(trafficModel);
        trafficModelConfigService.save(trafficModelConfig);
        trafficModelConfig = new TrafficModelConfig();
        trafficModelConfig.setName("datavolume");
        trafficModelConfig.setValue("100");
        trafficModelConfig.setTrafficModel(trafficModel);
        trafficModelConfigService.save(trafficModelConfig);

        // Small data regular interval transmission
        trafficModel = new TrafficModel();
        trafficModel.setCode("TM5");
        trafficModel.setName("Small data regular interval transmission");
        trafficModel.setDescription("Multiple data transmissions with small data volume in each transmission. Both data volume and time interval are random.");
        trafficModel  = trafficeModelGenericService.save(trafficModel);
        // Configuration
        trafficModelConfig = new TrafficModelConfig();
        trafficModelConfig.setName("timeinterval");
        trafficModelConfig.setValue("10");
        trafficModelConfig.setTrafficModel(trafficModel);
        trafficModelConfigService.save(trafficModelConfig);
        trafficModelConfig = new TrafficModelConfig();
        trafficModelConfig.setName("timeintervaldelta");
        trafficModelConfig.setValue("3");
        trafficModelConfig.setTrafficModel(trafficModel);
        trafficModelConfigService.save(trafficModelConfig);
        trafficModelConfig = new TrafficModelConfig();
        trafficModelConfig.setName("datavolume");
        trafficModelConfig.setValue("100");
        trafficModelConfig.setTrafficModel(trafficModel);
        trafficModelConfigService.save(trafficModelConfig);


        // Add ground station here.
        GroundStation groundStation = new GroundStation();
        groundStation.setName("Hawaii");
        groundStation.setStationId("Hawaii");
        groundStation.setX(-5.4631*1000000);
        groundStation.setY(-2.4802*1000000);
        groundStation.setZ(2.1570*1000000);
        groundStation.setGsId(3);
        groundStationService.save(groundStation);

        groundStation = new GroundStation();
        groundStation.setName("Cape Canaveral");
        groundStation.setStationId("Cape Canaveral");
        groundStation.setX(0.9189*1000000);
        groundStation.setY(-5.5343*1000000);
        groundStation.setZ(3.0242*1000000);
        groundStation.setGsId(2);
        groundStationService.save(groundStation);

        groundStation = new GroundStation();
        groundStation.setName("Ascension");
        groundStation.setStationId("Ascension");
        groundStation.setX(6.1200*1000000);
        groundStation.setY(-1.5663*1000000);
        groundStation.setZ(-0.8759*1000000);
        groundStation.setGsId(4);
        groundStationService.save(groundStation);

        groundStation = new GroundStation();
        groundStation.setName("Diego Garcia");
        groundStation.setStationId("Diego Garcia");
        groundStation.setX(1.9105*1000000);
        groundStation.setY(6.0311*1000000);
        groundStation.setZ(-0.8072*1000000);
        groundStation.setGsId(5);
        groundStationService.save(groundStation);

        groundStation = new GroundStation();
        groundStation.setName("Kwajalein");
        groundStation.setStationId("Kwajalein");
        groundStation.setX(-6.1610*1000000);
        groundStation.setY(1.3396*1000000);
        groundStation.setZ(0.9602*1000000);
        groundStation.setGsId(6);
        groundStationService.save(groundStation);

        groundStation = new GroundStation();
        groundStation.setName("Schriever AFB");
        groundStation.setStationId("Schriever AFB");
        groundStation.setX(-1.2482*1000000);
        groundStation.setY(-4.8176*1000000);
        groundStation.setZ(3.9758*1000000);
        groundStation.setGsId(1);
        groundStationService.save(groundStation);


    }
}
