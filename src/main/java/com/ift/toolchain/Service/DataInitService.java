package com.ift.toolchain.Service;

import java.util.Map;

public interface DataInitService {

    public void initOrbitSatellite(String orbName, String satelliteName, int satelliteOrder);
    public void updateSatellite(String satelliteName, String satelliteId, Map params);
    public void initGroundStation(String name, String gId, Map params);
}
