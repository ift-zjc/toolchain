package com.ift.toolchain.controller;

import com.ift.toolchain.Service.*;
import com.ift.toolchain.dto.DataGridTM;
import com.ift.toolchain.dto.ObjectDto;
import com.ift.toolchain.dto.ObjectEvent;
import com.ift.toolchain.dto.TrafficeModelDto;
import com.ift.toolchain.model.GroundStation;
import com.ift.toolchain.model.MessageHub;
import com.ift.toolchain.model.Satellite;
import com.ift.toolchain.model.TrafficModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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
    public DataGridTM getTrafficeModelDataSource(){
        return trafficeModelGenericService.getTMList();
    }


    /**
     * Get Traffic Model by ID
     * @param key
     * @return
     */
    @PostMapping(value = "/tm/{key}")
    @ResponseBody
    public TrafficeModelDto getTrafficModelByKey(@PathVariable String key){
        TrafficModel trafficModel =  trafficeModelGenericService.getByCode(key);
        TrafficeModelDto trafficeModelDto = new TrafficeModelDto();
        trafficeModelDto.setTmId(trafficModel.getId());
        trafficeModelDto.setTmName(trafficModel.getName());
        trafficeModelDto.setTmCode(trafficModel.getCode());
        trafficeModelDto.setTmDesc(trafficModel.getDescription());

        return trafficeModelDto;
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
}
