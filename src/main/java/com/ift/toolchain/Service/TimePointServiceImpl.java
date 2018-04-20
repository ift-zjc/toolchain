package com.ift.toolchain.Service;

import com.ift.toolchain.model.TimePoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TimePointServiceImpl implements TimePointService {

    @Autowired
    TimePointService timePointService;

    @Override
    public TimePoint save(TimePoint timePoint) {
        return timePointService.save(timePoint);
    }

    @Override
    public TimePoint save(float offset) {

        TimePoint timePoint = new TimePoint();
        timePoint.setOffset(offset);

        return save(timePoint);
    }
}
