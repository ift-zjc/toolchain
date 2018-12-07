package com.ift.toolchain.Service;

import com.ift.toolchain.model.AppTrafficData;
import com.ift.toolchain.repository.AppTrafficDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AppTrafficDataService {

    @Autowired
    AppTrafficDataRepository appTrafficDataRepository;

    public AppTrafficData save(AppTrafficData appTrafficData){
        return appTrafficDataRepository.save(appTrafficData);
    }
}
