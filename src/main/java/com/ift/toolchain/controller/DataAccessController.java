package com.ift.toolchain.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API controller
 */

@RestController
@RequestMapping("/api")
public class DataAccessController {


    @PostMapping(name = "/event/trigger")
    public void saveEvent(){

    }
}
