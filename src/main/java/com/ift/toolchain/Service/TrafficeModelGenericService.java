package com.ift.toolchain.Service;

import com.ift.toolchain.dto.DataGridTM;
import com.ift.toolchain.dto.TrafficModelConfigDto;
import com.ift.toolchain.model.TrafficModel;
import com.ift.toolchain.dto.TrafficeModelDto;
import com.ift.toolchain.model.TrafficModelConfig;
import com.ift.toolchain.repository.TrafficeModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service(value = "tmGenericService")
public class TrafficeModelGenericService {

    @Autowired
    TrafficeModelRepository trafficeModelRepository;


    /**
     * For datagrid
     * @return
     */
    public String getTMList(){
        List<TrafficModel> trafficModels = trafficeModelRepository.findAll();

        String responseArray = "[";

        for (TrafficModel model: trafficModels) {

            // Json String
            String response = "{";

            response += "\"name\":\"" + model.getName()+"\",";
            response += "\"code\":\"" + model.getCode()+"\",";
            response += "\"desc\":\"" + model.getDescription() + "\",";

            // Get configuration
            List<TrafficModelConfig> trafficModelConfigs = model.getTrafficModelConfigs();
            for(TrafficModelConfig config : trafficModelConfigs){
                response += "\"" + config.getName() + "\": \"" + config.getValue() + "\",";
            }

            response = response.substring(0, response.length()-1);

            response += "},";

            responseArray += response;

        }

        responseArray = responseArray.substring(0, responseArray.length() - 1);
        responseArray += "]";


//        List<TrafficeModelDto> trafficeModelDtos = trafficModels.stream().map(trafficeModel -> {
//            TrafficeModelDto trafficeModelDto = new TrafficeModelDto();
//
//            trafficeModelDto.setTmId(trafficeModel.getId());
//            trafficeModelDto.setTmCode(trafficeModel.getCode());
//            trafficeModelDto.setTmName(trafficeModel.getName());
//            trafficeModelDto.setTmDesc(trafficeModel.getDescription());
//
//
//            return trafficeModelDto;
//        }).collect(Collectors.toList());
//
//
//        DataGridTM dataGridTM = new DataGridTM();
//        dataGridTM.setItems(trafficeModelDtos);
//        dataGridTM.setTotalCount(trafficeModelDtos.size());

        return responseArray;
    }

    /**
     * Get traffic Model by ID
     * @param id
     * @return
     */
    public TrafficModel getById(String id){
        return trafficeModelRepository.getOne(id);
    }

    public Optional<TrafficModel> getByCode(String code){
        return trafficeModelRepository.getByCode(code);
    }

    public TrafficModel save(TrafficModel trafficModel){
        return trafficeModelRepository.save(trafficModel);
    }
}
