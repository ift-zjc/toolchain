package com.ift.toolchain.controller;


import com.ift.toolchain.Service.*;
import com.ift.toolchain.dto.*;
import com.ift.toolchain.model.Satellite;
import com.ift.toolchain.model.TrafficModel;
import com.ift.toolchain.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@RestController
public class SimulateController {

    private final int gap = 60;
    private final double maxDistance = 51591*1000;       // in meter
    private final double maxAngularVelocity = 0.000087;
    private final double lightSpeed = 299792458;        // in meter

    @Autowired
    MessageHubService messageHubService;
    @Autowired
    StorageService storageService;
    @Autowired
    TrafficeModelGenericService trafficModelGenericService;


    @PostMapping(value = "/simulate", consumes = "application/json")
    public SimulateData simulate(@RequestBody Map<String, Object> payload){


        List<String> dataStr = new ArrayList<>();
        int startOffset = (int) Float.parseFloat(payload.get("offsetStart").toString());
        int endOffset = (int) Float.parseFloat(payload.get("offsetEnd").toString());
        int delta = (int) Float.parseFloat(payload.get("delta").toString()) * 60;   // Minute
        delta = delta == 0 ? 60 : delta;
        List<HashMap<String, Object>> applicationDtos = (List<HashMap<String, Object>>) payload.get("appData");

        List<SimulateResultDto> simulateResultDtos = new ArrayList<>();

        for(int i = startOffset; i <= endOffset;  i += delta){

            int currentTimeInSecond = i;
            // Loop satellites
            CommonUtil.satellites.stream().forEach(satelliteCollection -> {

                try {
                    List<String> pairedSatellites = new ArrayList<>();
                    // Get current satellite position
                    SatellitePosition currentSelectedSatellitePosition = getSatellitePosition(currentTimeInSecond, satelliteCollection);
                    // Get last 1 second satellite position
                    SatellitePosition currentSelectedSatellitePosition1SecondBefore = getSatellitePosition(currentTimeInSecond - 1, satelliteCollection);
                    // pair satellite
                    SatelliteCollection pairSatellite = null;

                    // Find pair satellite
                    do {
                        pairSatellite = CommonUtil.satellites.stream()
                                .filter(satelliteCollection1 ->
                                        // Not selected satellite
                                        !satelliteCollection1.getName().equalsIgnoreCase(satelliteCollection.getName())
                                                // Not been selected before
                                                && !pairedSatellites.contains(satelliteCollection1.getName())
                                                // After selected satellite (avoid duplicated calculation)
                                                && satelliteCollection1.getSort() > satelliteCollection.getSort())
                                .findAny().orElse(null);

                        if (pairSatellite == null) continue; // Quit loop

                        // Add paired satellite name to list
                        pairedSatellites.add(pairSatellite.getName());

                        // Get paired satellite's position
                        SatellitePosition currentPairedSatellitePosition = getSatellitePosition(currentTimeInSecond, pairSatellite);
                        // Get last 1 second paired satellite's position
                        SatellitePosition currentPairedSatellitePosition1SecondBefore = getSatellitePosition(currentTimeInSecond - 1, pairSatellite);

                        // Calculate distance between pair
                        double distance = getDistanceBetweenPair(currentPairedSatellitePosition, currentSelectedSatellitePosition);

                        // Calculate the angel velocity
                        double angleVelocity = getAngleVelocity(currentSelectedSatellitePosition, currentSelectedSatellitePosition1SecondBefore,
                                currentPairedSatellitePosition, currentPairedSatellitePosition1SecondBefore);

                        boolean connected = distance < maxDistance && angleVelocity < maxAngularVelocity;

                        double delay = connected ? distance / lightSpeed : -1;

                        // Write to database.

//                    System.out.println(satelliteCollection.getName() + " - " + pairSatellite.getName() + " : " + distance + " / " + angleVelocity + " : " + connected);

                        // Only updated the connectivity now
                        /**
                         * MsgType: category of the message for link, for application, or for node, etc.
                         * MsgType = 1,   link parameter update
                         * MsgType= 2,   application behavior update
                         * MsgTyp2= 3,   satellite node parameter update
                         * SubMsgType: sub message category under each MsgType
                         * MsgType=1
                         * 	SubMsgType=1, link delay update
                         * 	SubMsgType=2, link through update
                         * 	SubMsgtype=3, link connectivity update
                         * MsgType=2
                         * 	SubMsgType=1, application data transmission message
                         *
                         * MsgSentTime:  millisecond after simulation start the message is generated
                         * MsgEffTime: millisecond after simulation start the message should be effective
                         */

                        simulateResultDtos.add(new SimulateResultDto(satelliteCollection.getName(),
                                pairSatellite.getName(),
                                connected,
                                currentTimeInSecond * 1000,
                                1,
                                3,
                                delay == -1 ? 0f : (float) delay,
                                angleVelocity,
                                delay == -1 ? 0f : (float)(Math.random()*1025),
                                satelliteCollection.getName() + "-" + pairSatellite.getName())
                                );
//                    messageHubService.create(satelliteCollection.getName(), pairSatellite.getName(), currentTimeInSecond*1000, 1, 3, connected ? 1f: 0f, (float) delay);

                        dataStr.add(satelliteCollection.getName() + "|" +
                                pairSatellite.getName() + "|" +
                                connected + "|" +
                                currentTimeInSecond * 1000 + "|" +
                                "1|3|" + (delay == -1 ? "0f" : "" + (float) delay) + "|" +
                                "" + angleVelocity + "|" +
                                satelliteCollection.getName() + "-" + pairSatellite.getName()
                        );
                    } while (pairSatellite != null);
                }catch(Exception ex){
                    ex.printStackTrace();
                }

            });
        }

        // Save to file
        storageService.store(dataStr);


        List<ApplicationTraffic> applicationTraffics = new ArrayList<>();
        /**
         * Simulate application traffic model
         */






        SimulateData simulateData = new SimulateData();
        simulateData.setSimulateResultDtos(simulateResultDtos);
        simulateData.setApplicationTraffic(applicationTraffics);

        return simulateData;
    }


    /**
     * Get satellite position in any time by satellite name
     * @param offset
     * @param selectedSatellite
     * @return
     */
    private SatellitePosition getSatellitePosition(int offset, SatelliteCollection selectedSatellite){

        if(offset < 0 ) return null;
        SatellitePosition satellitePosition = new SatellitePosition();

        int offsetMod = Math.floorMod(offset, gap);

        List<SatelliteDto> satelliteDtos = selectedSatellite.getSatellites();

        // Get two points for satellite based on offset
        SatelliteDto leftSatelliteDto = satelliteDtos.stream().filter(satelliteDto -> satelliteDto.getTime() <= offset)
                .collect(Collectors.toList())
                .stream()
                .reduce((first, last) -> last).orElse(null);

        SatelliteDto rightSatelliteDto = satelliteDtos.stream().filter(satelliteDto -> satelliteDto.getTime() >= offset)
                .findFirst().orElse(null);

        double x = leftSatelliteDto.getCartesian3()[0] + (rightSatelliteDto.getCartesian3()[0]-leftSatelliteDto.getCartesian3()[0])*offsetMod/gap;
        double y = leftSatelliteDto.getCartesian3()[1] + (rightSatelliteDto.getCartesian3()[1]-leftSatelliteDto.getCartesian3()[1])*offsetMod/gap;
        double z = leftSatelliteDto.getCartesian3()[2] + (rightSatelliteDto.getCartesian3()[2]-leftSatelliteDto.getCartesian3()[2])*offsetMod/gap;

        satellitePosition.setX(x);
        satellitePosition.setY(y);
        satellitePosition.setZ(z);

        return satellitePosition;
    }


    /**
     * Get distance between two satellites.
     * @param satelliteSourcePosition
     * @param satelliteDestPosition
     * @return
     */
    private double getDistanceBetweenPair(SatellitePosition satelliteSourcePosition, SatellitePosition satelliteDestPosition){

        return Math.sqrt(
                Math.pow(satelliteSourcePosition.getX() - satelliteDestPosition.getX(), 2)
                + Math.pow(satelliteSourcePosition.getY() - satelliteDestPosition.getY(), 2)
                + Math.pow(satelliteSourcePosition.getZ() - satelliteDestPosition.getZ(), 2)
        );
    }

    /**
     * Get SqrtPow
     * @param satellitePosition
     * @return
     */
    private double getSqrtPow(SatellitePosition satellitePosition){
        return Math.sqrt(Math.pow(satellitePosition.getX(),2) +
                Math.pow(satellitePosition.getY(),2) +
                Math.pow(satellitePosition.getZ(),2));
    }


    /**
     * Get angle velocity between two satellites in 1 second.
     * @param satelliteSource
     * @param satelliteSourceSecondAgo
     * @param satelliteDest
     * @param satelliteDestSecondAgo
     * @return
     */
    private double getAngleVelocity(SatellitePosition satelliteSource, SatellitePosition satelliteSourceSecondAgo,
                                    SatellitePosition satelliteDest, SatellitePosition satelliteDestSecondAgo){

        // Get angle for current
//        double distanceCurrent = getDistanceBetweenPair(satelliteSource, satelliteDest);
//        double aSource = getSqrtPow(satelliteSource);
//        double bSource = getSqrtPow(satelliteDest);
//        double angleCurrent = Math.atan(
//                (satelliteSource.getX()*satelliteDest.getX() + satelliteSource.getY()*satelliteDest.getY() + satelliteSource.getZ()*satelliteDest.getZ())/
//                Math.sqrt(Math.pow( satelliteSource.getY()*satelliteDest.getZ() - satelliteSource.getZ()*satelliteDest.getY(), 2 ) +
//                        Math.pow( satelliteSource.getZ()*satelliteDest.getX() - satelliteSource.getX()*satelliteDest.getZ(), 2 ) +
//                        Math.pow( satelliteSource.getX()*satelliteDest.getY() - satelliteSource.getY()*satelliteDest.getX(), 2 )
//                ))*Math.PI/180;

        // Get angle for past second
//        double distanceSecondAgo = getDistanceBetweenPair(satelliteSourceSecondAgo, satelliteDestSecondAgo);
//        double aSourceSecondAgo = getSqrtPow(satelliteSourceSecondAgo);
//        double bSourceSecondAgo = getSqrtPow(satelliteDestSecondAgo);
//        double angleSecondAgo = Math.atan(
//                (satelliteSourceSecondAgo.getX()*satelliteDestSecondAgo.getX() + satelliteSourceSecondAgo.getY()*satelliteDestSecondAgo.getY() + satelliteSourceSecondAgo.getZ()*satelliteDestSecondAgo.getZ())/
//                        Math.sqrt(Math.pow( satelliteSourceSecondAgo.getY()*satelliteDestSecondAgo.getZ() - satelliteSourceSecondAgo.getZ()*satelliteDestSecondAgo.getY(), 2 ) +
//                                Math.pow( satelliteSourceSecondAgo.getZ()*satelliteDestSecondAgo.getX() - satelliteSourceSecondAgo.getX()*satelliteDestSecondAgo.getZ(), 2 ) +
//                                Math.pow( satelliteSourceSecondAgo.getX()*satelliteDestSecondAgo.getY() - satelliteSourceSecondAgo.getY()*satelliteDestSecondAgo.getX(), 2 )
//                        ))*Math.PI/180;

        double a2 = Math.atan2(Math.sqrt(Math.pow( satelliteSource.getY()*satelliteDest.getZ() - satelliteSource.getZ()*satelliteDest.getY(), 2 ) +
                Math.pow( satelliteSource.getZ()*satelliteDest.getX() - satelliteSource.getX()*satelliteDest.getZ(), 2 ) +
                Math.pow( satelliteSource.getX()*satelliteDest.getY() - satelliteSource.getY()*satelliteDest.getX(), 2 )
        ), satelliteSource.getX()*satelliteDest.getX() + satelliteSource.getY()*satelliteDest.getY() + satelliteSource.getZ()*satelliteDest.getZ());

        double a2SecondAgo = Math.atan2(Math.sqrt(Math.pow( satelliteSourceSecondAgo.getY()*satelliteDestSecondAgo.getZ() - satelliteSourceSecondAgo.getZ()*satelliteDestSecondAgo.getY(), 2 ) +
                Math.pow( satelliteSourceSecondAgo.getZ()*satelliteDestSecondAgo.getX() - satelliteSourceSecondAgo.getX()*satelliteDestSecondAgo.getZ(), 2 ) +
                Math.pow( satelliteSourceSecondAgo.getX()*satelliteDestSecondAgo.getY() - satelliteSourceSecondAgo.getY()*satelliteDestSecondAgo.getX(), 2 )
        ), satelliteSourceSecondAgo.getX()*satelliteDestSecondAgo.getX() + satelliteSourceSecondAgo.getY()*satelliteDestSecondAgo.getY() + satelliteSourceSecondAgo.getZ()*satelliteDestSecondAgo.getZ());

//        return (Math.abs(angleCurrent - angleSecondAgo));
        return Math.abs(a2-a2SecondAgo);
    }





}
