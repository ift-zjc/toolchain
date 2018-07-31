package com.ift.toolchain.Service;

import com.ift.toolchain.dto.DataGridTM;
import com.ift.toolchain.model.TrafficModel;
import com.ift.toolchain.dto.TrafficeModelDto;
import com.ift.toolchain.repository.TrafficeModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service(value = "tmGenericService")
public class TrafficeModelGenericService {

    @Autowired
    TrafficeModelRepository trafficeModelRepository;


    /**
     * For datagrid
     * @return
     */
    public DataGridTM getTMList(){
        List<TrafficModel> trafficModels = trafficeModelRepository.findAll();

        List<TrafficeModelDto> trafficeModelDtos = trafficModels.stream().map(trafficeModel -> {
            TrafficeModelDto trafficeModelDto = new TrafficeModelDto();

            trafficeModelDto.setTmId(trafficeModel.getId());
            trafficeModelDto.setTmCode(trafficeModel.getCode());
            trafficeModelDto.setTmName(trafficeModel.getName());
            trafficeModelDto.setTmDesc(trafficeModel.getDescription());

            return trafficeModelDto;
        }).collect(Collectors.toList());


        DataGridTM dataGridTM = new DataGridTM();
        dataGridTM.setItems(trafficeModelDtos);
        dataGridTM.setTotalCount(trafficeModelDtos.size());

        return dataGridTM;
    }

    /**
     * Get traffic Model by ID
     * @param id
     * @return
     */
    public TrafficModel getById(String id){
        return trafficeModelRepository.getOne(id);
    }

    public TrafficModel getByCode(String code){
        return trafficeModelRepository.getByCode(code);
    }
}
