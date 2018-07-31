package com.ift.toolchain.controller;

import com.ift.toolchain.Service.MessageHubService;
import com.ift.toolchain.Service.TrafficeModelGenericService;
import com.ift.toolchain.Service.TrafficeModelService;
import com.ift.toolchain.dto.DataGridTM;
import com.ift.toolchain.dto.ObjectEvent;
import com.ift.toolchain.dto.TrafficeModelDto;
import com.ift.toolchain.model.MessageHub;
import com.ift.toolchain.model.TrafficModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

/**
 * API controller
 */

@RestController
@RequestMapping("/api")
public class DataAccessController {

    @Autowired
    MessageHubService messageHubService;


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
}
