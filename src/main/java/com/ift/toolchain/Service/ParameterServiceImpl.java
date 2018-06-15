package com.ift.toolchain.Service;

import com.ift.toolchain.model.Parameter;
import com.ift.toolchain.model.Satellite;
import com.ift.toolchain.repository.ParameterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ParameterServiceImpl implements ParameterService {

    @Autowired
    ParameterRepository parameterRepository;

    @Override
    public Parameter save(String name, String value, Satellite satellite) {
        Parameter parameter = new Parameter();
        parameter.setName(name);
        parameter.setValue(value);
        parameter.setSatellite(satellite);
        return parameterRepository.save(parameter);
    }

    @Override
    public Parameter save(Parameter parameter) {
        return parameterRepository.save(parameter);
    }
}
