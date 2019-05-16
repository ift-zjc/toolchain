package com.ift.toolchain.Service;

import com.ift.toolchain.model.MSAApplicationEvent;
import com.ift.toolchain.repository.MSAApplicationEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MSAApplicationEventService {

    @Autowired
    MSAApplicationEventRepository eventRepository;

    /**
     * Save event
     * @param applicationEvent
     * @return
     */
    public MSAApplicationEvent save(MSAApplicationEvent applicationEvent){
        return eventRepository.save(applicationEvent);
    }
}
