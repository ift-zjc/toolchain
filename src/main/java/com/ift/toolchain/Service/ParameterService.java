package com.ift.toolchain.Service;

import com.ift.toolchain.model.Parameter;
import com.ift.toolchain.model.Satellite;
import org.springframework.stereotype.Service;

public interface ParameterService {

    public Parameter save(String name, String value, Satellite satellite);
    public Parameter save(Parameter parameter);
}
