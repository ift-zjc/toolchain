package com.ift.toolchain.controller;

import com.ift.toolchain.Service.*;
import com.ift.toolchain.dto.*;
import com.ift.toolchain.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * API controller
 */

@RestController
@RequestMapping("/api")
public class DataAccessController {

    @Autowired
    MessageHubService messageHubService;
    @Autowired
    SatelliteService satelliteService;
    @Autowired
    GroundStationService groundStationService;


    @Autowired
    TrafficeModelGenericService trafficeModelGenericService;

    @PostMapping(value = "/event/trigger")
    public void saveEvent(@RequestBody ObjectEvent objectEvent){

        MessageHub messageHub = messageHubService.create(objectEvent);

    }


    /**
     * Get traffic model datagrid source
     * @return
     */
    @PostMapping(value = "/tmlist", produces = "application/json")
    @ResponseBody
    public String getTrafficeModelDataSource(){
        return trafficeModelGenericService.getTMList();
    }


    /**
     * Get Traffic Model by ID
     * @param key
     * @return
     */
    @PostMapping(value = "/tm/{key}", produces = "application/json")
    @ResponseBody
    public String getTrafficModelByKey(@PathVariable String key){
        Optional<TrafficModel> trafficModel =  trafficeModelGenericService.getByCode(key);

        // Json String
        String response = "{";
        if(trafficModel.isPresent()){
            TrafficModel model = trafficModel.get();
            response += "\"name\":\"" + model.getName() + "\",";
            response += "\"code\":\"" + model.getCode() + "\",";
            response += "\"desc\":\"" + model.getDescription() + "\",";

            // Get configuration
            List<TrafficModelConfig> trafficModelConfigs = model.getTrafficModelConfigs();
            for(TrafficModelConfig config : trafficModelConfigs){
                response += "\"" + config.getName() + "\": \"" + config.getValue() + "\",";
            }
        }

        response = response.substring(0, response.length()-1);

        response += "}";

        return response;
    }


    /**
     * Get object list
     * @return
     */
    @GetMapping(value = "/objectlist", produces = "application/json")
    public List<ObjectDto> getAllObjects(){
        List<ObjectDto> objectDtos = new ArrayList<>();
        // Get satellites
        List<Satellite> satellites = satelliteService.getAll();
        // Get ground stations
        List<GroundStation> groundStations = groundStationService.getAll();

        groundStations.forEach(groundStation -> {
            objectDtos.add(new ObjectDto(groundStation.getId(), groundStation.getName(), "Ground station"));
        });

        satellites.forEach(satellite -> {
            objectDtos.add(new ObjectDto(satellite.getId(), satellite.getName(), "Satellite"));
        });


        return objectDtos;
    }


    @PostMapping(value = "/object/{key}")
    @ResponseBody
    public ObjectDto getObjectById(@PathVariable String key){

        ObjectDto objectDto;

        // Try to find satellite
        Satellite satellite = satelliteService.findByName(key);
        if(satellite != null){
            objectDto = new ObjectDto(satellite.getId(), satellite.getName(), "Satellite");
        }else{
            GroundStation groundStation = groundStationService.findByName(key);
            objectDto = new ObjectDto(groundStation.getId(), groundStation.getName(), "Ground station");
        }

        return objectDto;
    }


    @Autowired
    StorageService storageService;
    @PostMapping("/file/upload")
    public void handleFileUpload(@RequestParam("files[]") MultipartFile[] files){
        // Save to storage
        Arrays.stream(files).forEach(file -> storageService.store(file));
    }


    @Autowired
    SimpMessagingTemplate webSocket;

    @PostMapping("/simulation/start")
    public void simulate() throws Exception{

        webSocket.convertAndSend("/topic/simulation/start", new SimulationMessage("Start reading TLE data ...", 1.0f));
        Thread.sleep(5000);
        webSocket.convertAndSend("/topic/simulation/start", new SimulationMessage("TLE data acquired ...", 30.0f));
        Thread.sleep(5000);
        webSocket.convertAndSend("/topic/simulation/start", new SimulationMessage("TLE data acquired ...", 100.0f));
    }
}
