package com.ift.toolchain.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import static com.ift.toolchain.util.CommonUtil.ecef2lla;

@Controller
public class GeneralController {

    @GetMapping(value = "/")
    public String index(){

        double[] data = ecef2lla(14788.5621*1000, -894.4494*1000, -21883.5687*1000);

        System.out.println(data[0]+"/"+data[1]+"/"+data[2]);

        return "index";
    }
}
