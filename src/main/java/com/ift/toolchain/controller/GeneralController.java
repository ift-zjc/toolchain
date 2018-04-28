package com.ift.toolchain.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import static com.ift.toolchain.util.CommonUtil.ecef2lla;

@Controller
public class GeneralController {

    @GetMapping(value = "/")
    public String index(){
        return "index";
    }
}
