package com.ift.toolchain.Service;


import com.ift.toolchain.model.TimePoint;

public interface TimePointService {

    public TimePoint save(TimePoint timePoint);
    public TimePoint save(float offset);
}
