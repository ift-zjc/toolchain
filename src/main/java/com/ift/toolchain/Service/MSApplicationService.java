package com.ift.toolchain.Service;

import com.ift.toolchain.model.MSAApplication;
import com.ift.toolchain.repository.MSApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MSApplicationService {

    @Autowired
    MSApplicationRepository msApplicationRepository;

    public void removeAll(){
        msApplicationRepository.deleteAll();
    }

    public List<MSAApplication> getApplications(){
        return msApplicationRepository.findAll();
    }

    public MSAApplication add(MSAApplication msaApplication){
        return msApplicationRepository.save(msaApplication);
    }
}
