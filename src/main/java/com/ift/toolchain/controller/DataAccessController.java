package com.ift.toolchain.controller;

import com.ift.toolchain.Service.MessageHubService;
import com.ift.toolchain.dto.ObjectEvent;
import com.ift.toolchain.model.MessageHub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * API controller
 */

@RestController
@RequestMapping("/api")
public class DataAccessController {

    @Autowired
    MessageHubService messageHubService;

    @PostMapping(value = "/event/trigger")
    public void saveEvent(@RequestBody ObjectEvent objectEvent){

        MessageHub messageHub = messageHubService.create(objectEvent);

    }
}
