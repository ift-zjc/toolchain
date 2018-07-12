package com.ift.toolchain.controller;

import com.ift.toolchain.component.PdfGeneratorUtil;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.ift.toolchain.util.CommonUtil.ecef2lla;

@Controller
public class GeneralController {

    @Autowired
    PdfGeneratorUtil pdfGeneratorUtil;

    @GetMapping(value = "/")
    public String index(){
//        Map<String, String> data = new HashMap<String, String>();
//        data.put("name", "zhijiang chen");
//        try {
//            pdfGeneratorUtil.createPdf("email", data);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        return "index";
    }

    @GetMapping(value = "/simulate")
    public String simulate(){
        return "simulate";
    }


    @GetMapping(value = "/file")
    public @ResponseBody void getFile(HttpServletResponse response) throws IOException{
        try {
            InputStream in = new FileInputStream("C:\\Users\\zhijiang\\AppData\\Local\\Temp\\d6fd0888-93d1-4d3a-b3ed-9ab5bf6cebd38207167398551656025.pdf");
            response.setContentType("application/pdf");
            IOUtils.copy(in, response.getOutputStream());
            response.flushBuffer();
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
}
