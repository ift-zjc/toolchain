package com.ift.toolchain.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ift.toolchain.Service.*;
import com.ift.toolchain.configuration.Autoconfiguration;
import com.ift.toolchain.dijkstra.Dijkstra;
import com.ift.toolchain.dijkstra.Graph;
import com.ift.toolchain.dijkstra.Node;
import com.ift.toolchain.dto.*;
import com.ift.toolchain.dto.SatelliteXSatellite;
import com.ift.toolchain.model.*;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.*;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * API controller
 */

@RestController
@RequestMapping("/api")
public class DataAccessController {

    @Autowired
    MessageHubService messageHubService;
    @Autowired
    SatelliteService satelliteService;
    @Autowired
    GroundStationService groundStationService;
    @Autowired
    TleService tleService;

    @Autowired
    TrafficeModelGenericService trafficModelGenericService;
    @Autowired
    MSApplicationService msApplicationService;
    @Autowired
    AppTrafficDataService appTrafficDataService;

    @Autowired
    SocketService socketService;
    @Autowired
    MSAApplicationEventService applicationEventService;

    private final double maxAngularVelocity = 0.000087;
    private final double maxDistanceLos = 51591*1000;
    private final double maxCornDistance = 5.5E7;

    @Value("${mininet.ip}")
    private String mininetIP;
    @Value("${mininet.port}")
    private int minietPort;


    /**
     * Get traffic model datagrid source
     *
     * @return
     */
    @PostMapping(value = "/tmlist", produces = "application/json")
    @ResponseBody
    public String getTrafficeModelDataSource() {
        return trafficModelGenericService.getTMList();
    }


    /**
     * Get Traffic Model by ID
     *
     * @param key
     * @return
     */
    @PostMapping(value = "/tm/{key}", produces = "application/json")
    @ResponseBody
    public String getTrafficModelByKey(@PathVariable String key) {
        Optional<TrafficModel> trafficModel = trafficModelGenericService.getByCode(key);

        // Json String
        String response = "{";
        if (trafficModel.isPresent()) {
            TrafficModel model = trafficModel.get();
            response += "\"name\":\"" + model.getName() + "\",";
            response += "\"code\":\"" + model.getCode() + "\",";
            response += "\"desc\":\"" + model.getDescription() + "\",";

            // Get configuration
            List<TrafficModelConfig> trafficModelConfigs = model.getTrafficModelConfigs();
            for (TrafficModelConfig config : trafficModelConfigs) {
                response += "\"" + config.getName() + "\": \"" + config.getValue() + "\",";
            }
        }

        response = response.substring(0, response.length() - 1);

        response += "}";

        return response;
    }


    /**
     * Get object list
     *
     * @return
     */
    @GetMapping(value = "/objectlist", produces = "application/json")
    public List<ObjectDto> getAllObjects() {
        List<ObjectDto> objectDtos = new ArrayList<>();
        // Get satellites
        List<Tle> satellites = tleService.getAllTles();
        // Get ground stations
        List<GroundStation> groundStations = groundStationService.getAll();

        groundStations.forEach(groundStation -> {
            objectDtos.add(new ObjectDto(groundStation.getId(), groundStation.getName(), "Ground station"));
        });

        satellites.forEach(satellite -> {
            objectDtos.add(new ObjectDto(satellite.getId(), satellite.getName(), "Satellite"));
        });


        return objectDtos;
    }


    @PostMapping(value = "/object/{key}")
    @ResponseBody
    public ObjectDto getObjectById(@PathVariable String key) {

        ObjectDto objectDto;

        // Try to find satellite
        Optional<Tle> satellite = tleService.findById(key);
        if (satellite.isPresent()) {
            objectDto = new ObjectDto(satellite.get().getId(), satellite.get().getName(), "Satellite");
        } else {
            GroundStation groundStation = groundStationService.findByName(key);
            objectDto = new ObjectDto(groundStation.getId(), groundStation.getName(), "Ground station");
        }

        return objectDto;
    }



    /**
     * Update file
     */
    @Autowired
    StorageService storageService;

    @PostMapping("/file/upload")
    public void handleFileUpload(@RequestParam("files[]") MultipartFile[] files) {
        // manipulate file name
        // Save to storage
        Arrays.stream(files).forEach(file -> {
            String filename = storageService.store(file);

            // Save to database
            // Get TLE file
//            ConfigFile tleFile= configFileService.getConfigFile("TLE");
            Path path = storageService.load(filename);
            if(path.toString().endsWith("tle")) {
                List<String> tleList = new ArrayList<>();
                try (Stream<String> stream = Files.lines(path)) {
                    tleList = stream.collect(Collectors.toList());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                List<Tle> tleDtos = convertToTleDto(tleList);

                /**
                 * Generate orbit
                 */
                List<SatelliteCollection> satelliteCollections = new ArrayList<>();
                Autoconfiguration.configureOrekit();

                int satelliteCnt = tleDtos.size();
                float index = 1f;
                for (Tle item : tleDtos) {
                    try {
                        tleService.save(item);
                    } catch (Exception ex) {
                        // TODO handle DB exception, ignore now.
                        // Database error, most for duplicated record
                    }

                }
            }

            // Check for configuration file
            if(path.toString().endsWith("configuration.json")){
                // Parsing the configuration file.
                JSONParser jsonParser = new JSONParser();
                try {
                    JSONObject jsonObject = (JSONObject) jsonParser.parse(Files.newBufferedReader(path));
                    JSONObject rootJsonObj = (JSONObject) jsonObject.get("SatcomScnDef");
                    JSONArray satelliteJsonArray = (JSONArray) rootJsonObj.get("sateDef");
                    // Loop satellites description and write to db
                    for(Object satelliteObj : satelliteJsonArray){
                        JSONObject satelliteJsonObj = (JSONObject) satelliteObj;
                        Satellite satellite = new Satellite();
                        satellite.setName(satelliteJsonObj.get("satName").toString());
                        satellite.setSatelliteId(satelliteJsonObj.get("satID").toString());

                        List<Parameter> parameters = new ArrayList<>();
                        // Get all parameters.
                        JSONArray configJsonArray = (JSONArray) satelliteJsonObj.get("Config");
                        // Loop
                        for(Object configObj : configJsonArray){
                            JSONObject configJsonObj = (JSONObject) configObj;

                            Parameter parameter = new Parameter();
                            parameter.setSatellite(satellite);
                            parameter.setName(configJsonObj.get("paraName").toString());
                            parameter.setValue(configJsonObj.get("value").toString());

                            parameters.add(parameter);
                        }

                        satellite.setSatelliteParams(parameters);
                        satelliteService.save(satellite);
                    }

                    System.out.println("Parsed");

                    // Send notification
                    try {
                        socketService.sendDataToSocket(mininetIP, minietPort, "s");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    /**
     * Nothing happens here.
     *
     * @param files
     */
    @PostMapping("/file/dummyupload")
    public void handleDummyFileUpload(@RequestParam("files[]") MultipartFile[] files) {

    }

    private final String categoryId = "GPS_SATELLITES";

    @Autowired
    SimpMessagingTemplate webSocket;
    @Autowired
    ConfigFileService configFileService;

    @PostMapping("/simulation/populateobjects")
    public SatellitePopulated populateobjects() throws Exception {

        sendMessage("Start reading TLE data ...", 1f);

        List<Tle> tleDtos = tleService.getAllTles();

        sendMessage("TLE data acquired ...", 10f);

        List<SatelliteItem> satelliteItems = new ArrayList<>();
        satelliteItems.add(new SatelliteItem(categoryId, "GPS Satellites", true));

        /**
         * Generate orbit
         */
        sendMessage("Generating satellite data ...", 11.0f);
        List<SatelliteCollection> satelliteCollections = new ArrayList<>();
        Autoconfiguration.configureOrekit();

        int satelliteCnt = tleDtos.size();
        float index = 1f;
        for (Tle item : tleDtos) {

            satelliteItems.add(new SatelliteItem(UUID.randomUUID().toString(), item.getName(), false, categoryId));

            sendMessage("Generating satellite data (" + item.getName() + ") ...", 11f + (100f - 11f) / (1 + (satelliteCnt - index++)));
            // Start generate orbits
            try {
                satelliteCollections.add(createOrbit(item));
            } catch (OrekitException e) {
                e.printStackTrace();
            }
        }

        /**
         * Geterate ground station objects
         */
        List<GroundStation> groundStations = groundStationService.getAll();
        satelliteItems.add(new SatelliteItem("GPS_GROUND_STATIONS", "Ground Stations", true));

        groundStations.forEach(groundStation -> {
            satelliteItems.add(new SatelliteItem(UUID.randomUUID().toString(), groundStation.getName(), false, "GPS_GROUND_STATIONS"));
        });

        sendMessage("TLE data and Ground station data acquired ...", 100f);

        SatellitePopulated satellitePopulated = new SatellitePopulated();
        satellitePopulated.setSatelliteCollections(satelliteCollections);
        satellitePopulated.setSatelliteItems(satelliteItems);

        return satellitePopulated;

    }

    /**
     * Add satellites via 2-line element string
     *
     * @param payload
     * @return
     */
    @PostMapping(value = "/satellite/add", consumes = "application/json")
    public SatellitePopulated addSatellites(@RequestBody Map<String, Object> payload) {

        // Read satellite TLE content
        String tleContent = payload.get("tle").toString();
        List<String> tleList = new BufferedReader(new StringReader(tleContent)).lines().collect(Collectors.toList());

        List<Tle> tleDtos = convertToTleDto(tleList);

        /**
         * Generate orbit
         */
        List<SatelliteItem> satelliteItems = new ArrayList<>();
        List<SatelliteCollection> satelliteCollections = new ArrayList<>();
        Autoconfiguration.configureOrekit();

        satelliteItems.add(new SatelliteItem(categoryId, "GPS Satellites", true));

        int satelliteCnt = tleDtos.size();
        float index = 1f;
        for (Tle item : tleDtos) {
            try {
                // Save to database
                tleService.save(item);
                // Start generate orbits
                satelliteItems.add(new SatelliteItem(UUID.randomUUID().toString(), item.getName(), false, categoryId));
                try {
                    satelliteCollections.add(createOrbit(item));
                } catch (OrekitException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                // TODO handle DB exception, ignore now.
            }
        }

        SatellitePopulated satellitePopulated = new SatellitePopulated();
        satellitePopulated.setSatelliteCollections(satelliteCollections);
        satellitePopulated.setSatelliteItems(satelliteItems);

        return satellitePopulated;
    }

    @Value("${simulation.step}")
    private int step;
    /**
     * Start simulator
     *
     * @param payload
     * @return
     * @throws OrekitException
     */
    @PostMapping(value = "/simulation/start", consumes = "application/json")
    public SimulateData startSimulator(@RequestBody Map<String, Object> payload) throws OrekitException {

        Autoconfiguration.configureOrekit();
        float delta = 0.05f;  // 10%;
        String startTime = payload.get("timeStart").toString();
        String endTime = payload.get("timeEnd").toString();

        List<HashMap<String, Object>> applicationDtos = (List<HashMap<String, Object>>) payload.get("appData");

        // Convert to datetime
        DateTime startDateTime = DateTime.parse(startTime);
        DateTime endDateTime = DateTime.parse(endTime);
        AbsoluteDate absoluteStartDate = new AbsoluteDate(startDateTime.toDate(), TimeScalesFactory.getUTC());
        AbsoluteDate absoluteEndDate = new AbsoluteDate(endDateTime.toDate(), TimeScalesFactory.getUTC());

        // Propagation every n second
        // Get all the objects from database
        List<Tle> tleDtos = tleService.getAllTles();
        KeplerianPropagator propagator1;
        KeplerianPropagator propagator2;

        // Remove all messages/msa application settings before start
        messageHubService.removeAll();
        msApplicationService.removeAll();


        for (int i = 0; i < tleDtos.size() - 1; i++) {
            Orbit orbit1Init = this.getInitialOrb(tleDtos.get(i));
            AbsoluteDate absoluteDateCurrentStartDate = absoluteStartDate;
            propagator1 = new KeplerianPropagator(orbit1Init);
            propagator1.setSlaveMode();

            for (int j = i + 1; j < tleDtos.size(); j++) {

                MessageHub messageHub = new MessageHub();
                Orbit orbit2Init = this.getInitialOrb(tleDtos.get(j));
                propagator2 = new KeplerianPropagator(orbit2Init);
                propagator2.setSlaveMode();
                // Loop between time.
                while (absoluteDateCurrentStartDate.compareTo(absoluteEndDate) <= 0) {
                    // propagate

                    SpacecraftState currentState1 = propagator1.propagate(absoluteDateCurrentStartDate);
                    SpacecraftState currentState2 = propagator2.propagate(absoluteDateCurrentStartDate);

                    // Calculate line of sight
                    double eta = FastMath.asin(Constants.WGS84_EARTH_EQUATORIAL_RADIUS / currentState1.getPVCoordinates().getPosition().getNorm());
                    double gamma = Vector3D.angle(currentState1.getPVCoordinates().getPosition().negate(), currentState2.getPVCoordinates().getPosition().subtract(currentState1.getPVCoordinates().getPosition()));
                    double distance = Vector3D.distance(currentState1.getPVCoordinates().getPosition(), currentState2.getPVCoordinates().getPosition());
                    boolean los = (gamma > eta
                            || (
                            gamma < eta
                                    && distance < currentState1.getPVCoordinates().getPosition().getNorm()));


                    boolean losChanged = los != messageHub.isLos();
                    boolean distanceChanged = Math.abs((messageHub.getDistance() - distance) / messageHub.getDistance()) >= delta;

                    boolean needUpdate = absoluteDateCurrentStartDate.compareTo(absoluteStartDate) == 0 ||
                            distanceChanged || losChanged;

                    if (needUpdate) {
                        messageHub = new MessageHub();
                        messageHub.setSourceId(tleDtos.get(i).getId());
                        messageHub.setDestinationId(tleDtos.get(j).getId());
                        messageHub.setDate(absoluteDateCurrentStartDate.toDate(TimeScalesFactory.getUTC()));
                        messageHub.setLos(los);
                        messageHub.setDistance(distance);
                        if (absoluteDateCurrentStartDate.compareTo(absoluteStartDate) == 0) {
                            // First record always present
                            messageHub.setMsgType(0);   // Init
                            messageHub.setSubMsgType(0);    // Init
                        } else {
                            if (losChanged) {
                                messageHub.setMsgType(1);
                                messageHub.setSubMsgType(3);                // Connectivity updated
                            } else {
                                if (los) {
                                    if (distanceChanged) {
                                        messageHub.setMsgType(1);
                                        messageHub.setSubMsgType(1);        // Delay updated
                                    }
                                }
                            }
                        }

                        // Save to database
                        messageHubService.save(messageHub);
                    }


                    // add 60 seconds
                    absoluteDateCurrentStartDate = absoluteDateCurrentStartDate.shiftedBy(step);
                }
            }
        }


        /**
         * Simulate application traffic model
         */
        List<ApplicationTraffic> applicationTraffics = new ArrayList<>();

        // TODO write JSON to file system with (new ObjectMapper()).writeValueAsString(applicationDtos), then provide a link for user to download.

        for (HashMap<String, Object> hashMap : applicationDtos) {
            // Get current absolute date based on start time.
            String _startTime = hashMap.get("startTime").toString();
            String _endTime = hashMap.get("endTime").toString();
            DateTime _start = DateTime.parse(_startTime);
            DateTime _end = DateTime.parse(_endTime);
            String appName = hashMap.get("name").toString();

            ApplicationTraffic applicationTraffic = new ApplicationTraffic();
            applicationTraffic.setAppName(appName);

            // Get traffic model for this application.
            HashMap<String, String> tmData = (HashMap<String, String>) hashMap.get("tm");
            String tmCode = tmData.get("code");
            // Find traffic model
            Optional<TrafficModel> trafficModel = trafficModelGenericService.getByCode(tmCode);
            // Skip to next application if this application's traffic model is not presented.
            if (!trafficModel.isPresent()) {
                continue;
            }

            // Convert to datetime
            DateTime appStartDateTie = DateTime.parse(hashMap.get("startTime").toString());
            DateTime appEndDateTime = DateTime.parse(hashMap.get("endTime").toString());
            AbsoluteDate appAbsoluteStartDate = new AbsoluteDate(appStartDateTie.toDate(), TimeScalesFactory.getUTC());
            AbsoluteDate appAbsoluteEndDate = new AbsoluteDate(appEndDateTime.toDate(), TimeScalesFactory.getUTC());

            // Save to database before proc.
            MSAApplication msaApplication = new MSAApplication();
            msaApplication.setAppName(appName);
            msaApplication.setProtocol(hashMap.get("protocol").toString());
            msaApplication.setStartTime(DateTime.parse(appAbsoluteStartDate.toString()).toDate());
            msaApplication.setEndTime(DateTime.parse(appAbsoluteEndDate.toString()).toDate());
            msaApplication.setSourceObj(hashMap.get("source").toString());
            msaApplication.setDestObj(hashMap.get("dest").toString());
            msaApplication.setTrafficModelCode(tmCode);
            msaApplication.setTrafficModelConfig((new JSONObject(tmData)).toJSONString());
            msaApplication = msApplicationService.add(msaApplication);

            TrafficModel trafficModelExtracted = trafficModel.get();
            // Loop traffic model attributes and update if necessary
            trafficModelExtracted.getTrafficModelConfigs().stream().forEach(tmConfig -> {
                if (tmData.containsKey(tmConfig.getName())) {
                    tmConfig.setValue(String.valueOf(tmData.get(tmConfig.getName())));
                }
            });

            TrafficeModelService trafficeModelService = this.getTrafficModelService(tmCode);

            List<ApplicationTrafficData> applicationTrafficDataList = trafficeModelService.simulate(_start, _end, trafficModelExtracted.getTrafficModelConfigs(), applicationTraffic.getAppName());
            applicationTraffic.setApplicationTrafficDataList(applicationTrafficDataList);


            ObjectMapper objectMapper = new ObjectMapper();
            // Loop on step base.
            while (appAbsoluteStartDate.compareTo(appAbsoluteEndDate) <= 0) {

                MSAApplicationEvent applicationEvent = new MSAApplicationEvent();
                applicationEvent.setMsaApplication(msaApplication);
                applicationEvent.setTick(appAbsoluteStartDate.toString());

                // Get routing
                ShortestPath shortestPath = getShortestPath(hashMap.get("source").toString(), hashMap.get("dest").toString(), appAbsoluteStartDate);
                // Save to database
                try {
                    String routingJson = objectMapper.writeValueAsString(shortestPath);
                    applicationEvent.setRouting(routingJson);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

                LinkedList<Double> distanceLinkedList = new LinkedList<>();
                try{
                    // Get distance
                    for(int i = 0; i<shortestPath.getPathById().size()-1; i++){
                        String idFirst = shortestPath.getPathById().get(i);
                        String idNext = shortestPath.getPathById().get(i+1);

                        // Distance
                        distanceLinkedList.add(getDistance(idFirst, idNext, appAbsoluteStartDate));
                    }
                }catch(OrekitException ex){
                    ex.printStackTrace();
                }

                try {
                    applicationEvent.setDistanceJson(objectMapper.writeValueAsString(distanceLinkedList));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

                // Save to database
                applicationEventService.save(applicationEvent);

                // Moveforward step seconds
                appAbsoluteStartDate = appAbsoluteStartDate.shiftedBy(step);
            }

            // Save those data to database before end loop
            AppTrafficData appTrafficData = null;
            for (ApplicationTrafficData appData : applicationTrafficDataList) {
                appTrafficData = new AppTrafficData();
                appTrafficData.setApplicationTrafficModel(msaApplication);
                appTrafficData.setDataTime(appData.getTimeString());
                appTrafficData.setVolume(appData.getTrafficVolumn());

                appTrafficDataService.save(appTrafficData);
            }
            applicationTraffics.add(applicationTraffic);

        }

        SimulateData simulateData = new SimulateData();
        simulateData.setSimulateResultDtos(null);
        simulateData.setApplicationTraffic(applicationTraffics);

        try {
            socketService.sendDataToSocket(mininetIP, minietPort, "e");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return simulateData;
    }


    /**
     * Generate json data file for LOS for time period
     * @param payload
     */
    @PostMapping(value = "/file/los/generate",
            consumes = "application/json")
    @ResponseBody
    public String startLosFileGenerate(@RequestBody Map<String, Object> payload) throws OrekitException {
        System.out.println("Generating");

        // Config Orekit
        Autoconfiguration.configureOrekit();
        // Step as interval
        int step = Integer.parseInt(payload.get("timeInterval").toString());
        String startTime = payload.get("timeStart").toString();
        String endTime = payload.get("timeEnd").toString();

        // Convert to datetime
        AbsoluteDate startDate = new AbsoluteDate(DateTime.parse(payload.get("timeStart").toString()).toDate(), TimeScalesFactory.getUTC());
        AbsoluteDate endDate = new AbsoluteDate(DateTime.parse(payload.get("timeEnd").toString()).toDate(), TimeScalesFactory.getUTC());

        // Get all TLEs
        List<Tle> tleDtos = tleService.getAllTles();

        // Get all base stations
        List<GroundStation> groundStations = groundStationService.getAll();

        // Propagator
        KeplerianPropagator propagator1;
        KeplerianPropagator propagator2;

//        JSONObject losJson = new JSONObject();
        JSONObject losData = new JSONObject();
        losData.put("simTime", startDate.toString());
        losData.put("simTimeEnd", endDate.toString());
        losData.put("simDuration", endDate.durationFrom(startDate));
        losData.put("simPeriodSecs", step);
        losData.put("satNodeCount", tleDtos.size());
        losData.put("gsNodeCount", groundStations.size());


        // JSON Array
        JSONArray stateArray = new JSONArray();


        // Loop time step interval
        while (startDate.compareTo(endDate) <= 0) {
            // Loop through
            for (int i = 0; i < tleDtos.size() - 0; i++) {

                Tle tleSource = tleDtos.get(i);
                Orbit orbit1Init = this.getInitialOrb(tleDtos.get(i));
                propagator1 = new KeplerianPropagator(orbit1Init);
                propagator1.setSlaveMode();
                SpacecraftState state1 = propagator1.propagate(startDate);

                System.out.println("Working on Satellite: " + tleSource.getName()+ " | " + tleSource.getNumber());

                // JSON Object
                JSONObject sourceObj = new JSONObject();
                sourceObj.put("sateName", tleSource.getName());
                sourceObj.put("satID", tleSource.getNumber());
                sourceObj.put("PosX", state1.getPVCoordinates().getPosition().getX());
                sourceObj.put("PosY", state1.getPVCoordinates().getPosition().getY());
                sourceObj.put("PosZ", state1.getPVCoordinates().getPosition().getZ());
                sourceObj.put("Time", startDate.toString());

                JSONArray losArray = new JSONArray();

                for (int j = i + 1; j < tleDtos.size(); j++) {
                    Tle tleDest = tleDtos.get(j);
                    Orbit orbit2Init = this.getInitialOrb(tleDest);
                    propagator2 = new KeplerianPropagator(orbit2Init);
                    propagator2.setSlaveMode();

                    SpacecraftState state2 = propagator2.propagate(startDate);

//                    // Calculate line of sight
//                    double eta = FastMath.asin(Constants.WGS84_EARTH_EQUATORIAL_RADIUS / state1.getPVCoordinates().getPosition().getNorm());
//                    double gamma = Vector3D.angle(state1.getPVCoordinates().getPosition().negate(), state2.getPVCoordinates().getPosition().subtract(state1.getPVCoordinates().getPosition()));
//                    double distance = Vector3D.distance(state1.getPVCoordinates().getPosition(), state2.getPVCoordinates().getPosition());
//                    boolean los = (gamma > eta
//                            || (
//                            gamma < eta
//                                    && distance < state1.getPVCoordinates().getPosition().getNorm()));
                    boolean los = checkLoS(state1, state2, "SS");
                    // Get los
                    JSONObject losObj = new JSONObject();
                    losObj.put("sateName", tleDest.getName());
                    losObj.put("satID", tleDest.getNumber());
                    losObj.put("PosX", state2.getPVCoordinates().getPosition().getX());
                    losObj.put("PosY", state2.getPVCoordinates().getPosition().getY());
                    losObj.put("PosZ", state2.getPVCoordinates().getPosition().getZ());
                    losObj.put("LoS", los);

                    losArray.add(losObj);
                }

                sourceObj.put("visibilityGraph", losArray);
                stateArray.add(sourceObj);
            }

            // Check based on ground station
            for(int g = 0; g < groundStations.size(); g++){

                GroundStation gs = groundStations.get(g);
                // JSON Object
                JSONObject sourceObj = new JSONObject();
                sourceObj.put("BSName", gs.getName());
                sourceObj.put("BSID", gs.getGsId());
                sourceObj.put("PosX", gs.getX());
                sourceObj.put("PosY", gs.getY());
                sourceObj.put("PosZ", gs.getZ());
                sourceObj.put("Time", startDate.toString());

                JSONArray losArray = new JSONArray();

                for (int j = 0; j < tleDtos.size(); j++) {
                    Tle tleDest = tleDtos.get(j);
                    Orbit orbit2Init = this.getInitialOrb(tleDest);
                    propagator2 = new KeplerianPropagator(orbit2Init);
                    propagator2.setSlaveMode();

                    SpacecraftState state2 = propagator2.propagate(startDate);
                    boolean los = checkLoS(gs, state2, "GS");
                    // Get los
                    JSONObject losObj = new JSONObject();
                    losObj.put("sateName", tleDest.getName());
                    losObj.put("satID", tleDest.getNumber());
                    losObj.put("PosX", state2.getPVCoordinates().getPosition().getX());
                    losObj.put("PosY", state2.getPVCoordinates().getPosition().getY());
                    losObj.put("PosZ", state2.getPVCoordinates().getPosition().getZ());
                    losObj.put("LoS", los);

                    losArray.add(losObj);
                }

                sourceObj.put("visibilityGraph", losArray);
                stateArray.add(sourceObj);
            }


            // Shifting by step.
            startDate = startDate.shiftedBy(step);
        }

        losData.put("SateDef", stateArray);

        try {
            Files.write(Paths.get("/var/upload/LOS.json"), losData.toJSONString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "done";
    }


    /**
     * Check for LoS
     * @param source
     * @param dest
     * @return
     */
    private boolean checkLoS(Object source, SpacecraftState dest, String type){

        Double sourceX = 0d;
        Double sourceY = 0d;
        Double sourceZ = 0d;

        Double destX = 0d;
        Double destY = 0d;
        Double destZ = 0d;

        if(type.equalsIgnoreCase("SS")) {
            Vector3D satelliteSourcePosition = ((SpacecraftState)source).getPVCoordinates().getPosition();

            sourceX = satelliteSourcePosition.getX();
            sourceY = satelliteSourcePosition.getY();
            sourceZ = satelliteSourcePosition.getZ();


        }else if(type.equalsIgnoreCase("GS")){
            GroundStation gs = (GroundStation) source;

            sourceX = gs.getX();
            sourceY = gs.getY();
            sourceZ = gs.getZ();
        }

        Vector3D satelliteDestPosition = dest.getPVCoordinates().getPosition();
        destX = satelliteDestPosition.getX();
        destY = satelliteDestPosition.getY();
        destZ = satelliteDestPosition.getZ();

        double distance = Math.sqrt(
                Math.pow(sourceX - destX, 2)
                        + Math.pow(sourceY - destY, 2)
                        + Math.pow(sourceZ - destZ, 2)
        );

        double sourceDist = Math.sqrt(
                Math.pow(sourceX, 2)
                        + Math.pow(sourceY, 2)
                        + Math.pow(sourceZ, 2)
        );

        double destDist = Math.sqrt(
                Math.pow(destX, 2)
                        + Math.pow(destY, 2)
                        + Math.pow(destZ, 2)
        );

        double p = 0.5*(distance+sourceDist+destDist);

        double h = 2*Math.sqrt(p*(p-distance)*(p-sourceDist)*(p-destDist))/distance;

//        if(h<=6357000){System.out.println("LOSSSS");}
        return h>6357000;
    }

    @GetMapping(value = "/download/los")
    public ResponseEntity<Resource> downloadLoS() throws FileNotFoundException {

        File file = new File("/var/upload/LOS.json");
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        headers.add("Content-Disposition", "attachment; filename=\"los.json\"");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/json"))
                .body(resource);
    }


    @PostMapping(value = "/app/routing", consumes = "application/json")
    public List<ShortestPath> estimateRouting(@RequestBody Map<String, Object> payload) throws OrekitException {

        List<ShortestPath> shortestPaths = new ArrayList<>();
        String julianTime = payload.get("time").toString();

        // Get current absolute date based on juliandate
        DateTime dateTime = DateTime.parse(julianTime.length() > 23 ? julianTime.substring(0, 23)+"Z" : julianTime);        // cut time string
        AbsoluteDate absoluteDateCurrent = new AbsoluteDate(dateTime.toDate(), TimeScalesFactory.getUTC());

        // Get start/end time of app model

        List<HashMap<String, Object>> applicationDtos = (List<HashMap<String, Object>>) payload.get("appData");
        for (HashMap<String, Object> hashMap : applicationDtos) {

            // Get current absolute date based on start time.
            String _startTime = hashMap.get("startTime").toString();
            String _endTime = hashMap.get("endTime").toString();
            DateTime _start = DateTime.parse(_startTime);
            DateTime _end = DateTime.parse(_endTime);

            AbsoluteDate absoluteDateStart = new AbsoluteDate(_start.toDate(), TimeScalesFactory.getUTC());
            AbsoluteDate absoluteDateEnd = new AbsoluteDate(_end.toDate(), TimeScalesFactory.getUTC());

            if(absoluteDateStart.compareTo(absoluteDateCurrent) <=0 &&
                    absoluteDateEnd.compareTo(absoluteDateCurrent) >=0){

                // Find the shortest path
                String source = hashMap.get("source").toString();
                String dest = hashMap.get("dest").toString();

                shortestPaths.add(getShortestPath(source, dest, absoluteDateCurrent));
            }
        }

        return shortestPaths;
        // getShortestPath(hashMap.get("source").toString(), hashMap.get("dest").toString(), absoluteStartDate);
    }

    /**
     * Disable satellite from database
     *
     * @param payload
     * @return
     */
    @PostMapping(value = "/satellite/disable", consumes = "application/json")
    public NameValue disableSatellite(@RequestBody Map<String, Object> payload) {
        // Get satellite name
        String satelliteName = payload.get("name").toString();

        // Get satellite
        Tle tle = tleService.getByName(satelliteName);
        tle.setEnabled(false);

        tleService.save(tle);

        return new NameValue("name", tle.getName());
    }

    /**
     * Get satellite status in any given time.
     *
     * @param payload
     * @return
     * @throws OrekitException
     */
    @PostMapping(value = "/satellite/currentstatus", consumes = "application/json")
    public SelectedSatelliteDto getSatelliteStatus(@RequestBody Map<String, Object> payload) throws OrekitException {
        String satelliteName = payload.get("satelliteName").toString();
        String julianTime = payload.get("time").toString();

        SelectedSatelliteDto selectedSatelliteDto = new SelectedSatelliteDto();

        // Get Tle entity based on satellite name
        Tle tle = tleService.getByName(satelliteName);
        // Get initial orb
        Autoconfiguration.configureOrekit();
        Orbit initialOrbit = getInitialOrb(tle);

        /**
         * Slave mode
         */
        KeplerianPropagator keplerianPropagator = new KeplerianPropagator(initialOrbit);
        // Set to slave mode
        keplerianPropagator.setSlaveMode();

        // Get current absolute date based on juliandate
        DateTime dateTime = DateTime.parse(julianTime.length() > 23 ? julianTime.substring(0, 23) + "Z": julianTime);        // cut time string
        AbsoluteDate absoluteDate = new AbsoluteDate(dateTime.toDate(), TimeScalesFactory.getUTC());

        // Get orbit status
        SpacecraftState currentState = keplerianPropagator.propagate(absoluteDate);
        KeplerianOrbit o = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(currentState.getOrbit());

        List<NameValue> nameValues = new ArrayList<>();

        // Populate status
        nameValues.add(new NameValue("Satellite Name", satelliteName));
        nameValues.add(new NameValue("Satellite Id", tle.getNumber()));
        nameValues.add(new NameValue("Classification", tle.getClassification()));
        nameValues.add(new NameValue("COSPAR ID", tle.getLaunchYear() + "-" + tle.getLaunchNumber() + tle.getLaunchPiece()));
        nameValues.add(new NameValue("Inclination", FastMath.toDegrees(o.getI()) + "\u00B0"));
        nameValues.add(new NameValue("Eccentricity", o.getEccentricAnomaly()));
        nameValues.add(new NameValue("RA ascending node", o.getRightAscensionOfAscendingNode()));
        nameValues.add(new NameValue("Argument perihelion", FastMath.toDegrees(o.getPerigeeArgument()) + "\u00B0"));
        nameValues.add(new NameValue("Mean anomaly", FastMath.toDegrees(o.getMeanAnomaly()) + "\u00B0"));
        nameValues.add(new NameValue("Position X[m]", o.getPVCoordinates().getPosition().getX()));
        nameValues.add(new NameValue("Position Y[m]", o.getPVCoordinates().getPosition().getY()));
        nameValues.add(new NameValue("Position Z[m]", o.getPVCoordinates().getPosition().getZ()));
        nameValues.add(new NameValue("Angular Velocity X", o.getPVCoordinates().getAngularVelocity().getX()));
        nameValues.add(new NameValue("Angular Velocity Y", o.getPVCoordinates().getAngularVelocity().getY()));
        nameValues.add(new NameValue("Angular Velocity Z", o.getPVCoordinates().getAngularVelocity().getZ()));
        nameValues.add(new NameValue("Acceleration X", o.getPVCoordinates().getAcceleration().getX()));
        nameValues.add(new NameValue("Acceleration Y", o.getPVCoordinates().getAcceleration().getY()));
        nameValues.add(new NameValue("Acceleration Z", o.getPVCoordinates().getAcceleration().getZ()));

        selectedSatelliteDto.setSatelliteProperties(nameValues);

        // Find 4 nearest satellites
        List<SatelliteXSatellite> satelliteXSatellites = new ArrayList<>();
        // Get all the objects from database
        List<Tle> tleDtos = tleService.getAllTles();
        for (int i = 0; i < tleDtos.size(); i++) {
            Tle currentTle = tleDtos.get(i);
            if (currentTle.getName().equalsIgnoreCase(satelliteName)) {
                continue;
            }

            // init orbit
            Orbit orbit1Init = this.getInitialOrb(currentTle);
            KeplerianPropagator keplerianPropagatorLoop = new KeplerianPropagator(orbit1Init);
            keplerianPropagatorLoop.setSlaveMode();

            // Get spacecraft state
            SpacecraftState currentStateLoop = keplerianPropagatorLoop.propagate(absoluteDate);

            // Calculate line of sight
            double eta = FastMath.asin(Constants.WGS84_EARTH_EQUATORIAL_RADIUS / currentState.getPVCoordinates().getPosition().getNorm());
            double gamma = Vector3D.angle(currentState.getPVCoordinates().getPosition().negate(), currentStateLoop.getPVCoordinates().getPosition().subtract(currentState.getPVCoordinates().getPosition()));
            double distance = Vector3D.distance(currentState.getPVCoordinates().getPosition(), currentStateLoop.getPVCoordinates().getPosition());
            boolean los = (gamma > eta
                    || (
                    gamma < eta
                            && distance < currentState.getPVCoordinates().getPosition().getNorm()));

            if (los) {
                SatelliteXSatellite satelliteXSatellite = new SatelliteXSatellite();
                satelliteXSatellite.setSource(satelliteName);
                satelliteXSatellite.setDestination(currentTle.getName());
                satelliteXSatellite.setDistance(distance);
                satelliteXSatellite.setAngularVelocity(getAngularVelocity(currentState, currentStateLoop));
                satelliteXSatellites.add(satelliteXSatellite);

            }
        }

        selectedSatelliteDto.setSatelliteXSatellites(satelliteXSatellites);
        return selectedSatelliteDto;

    }

    /**
     * Get satellite id by name.
     *
     * @param name
     * @return
     */
    @GetMapping("/satellite/getidbyname/{name}")
    public String getObjectIdByName(@PathVariable("name") String name) {

        Tle tle = tleService.getByName(name);
        if (tle != null) {
            return tle.getId();
        }

        return "N/A";
    }


    /**
     * Reset data
     *
     * @return
     */
    @PostMapping(value = "/simulation/reset")
    public boolean resetSimulation() {
        boolean success = false;

        try {
            // Delete all records from TLE table
            tleService.removeRecords();
            // Delete all records from message table
            messageHubService.removeAll();
            success = true;
        } catch (Exception ex) {

        }

        return success;
    }


    private void julianFractionConverter(Tle tleDto) {

        // Get year
        int epochYear = tleDto.getEpochYear();
        int year = epochYear > 70 ? 1900 + epochYear : 2000 + epochYear;
        OffsetDateTime epochCalendarNewStyleActOf2017 = LocalDate.of(year, Month.JANUARY, 1).atStartOfDay().atOffset(ZoneOffset.UTC);

        BigDecimal bd = new BigDecimal(tleDto.getJulianFraction());
        long days = bd.toBigInteger().longValue();
        BigDecimal fractionOfADay = bd.subtract(new BigDecimal(days)); // Extract the fractional number, separate from the integer number.
        BigDecimal secondsFractional = new BigDecimal(TimeUnit.DAYS.toSeconds(1)).multiply(fractionOfADay);
        long secondsWhole = secondsFractional.longValue();
        long nanos = secondsFractional.subtract(new BigDecimal(secondsWhole)).multiply(new BigDecimal(1_000_000_000L)).longValue();
        Duration duration = Duration.ofDays(days).plusSeconds(secondsWhole).plusNanos(nanos);
        OffsetDateTime odt = epochCalendarNewStyleActOf2017.plus(duration);

//        System.out.println ( "bd: " + bd );
//        System.out.println ( "days: " + days );
//        System.out.println ( "fractionOfADay.toString(): " + fractionOfADay );
//        System.out.println ( "secondsFractional: " + secondsFractional );
//        System.out.println ( "secondsWhole: " + secondsWhole );
//        System.out.println ( "nanos: " + nanos );
//        System.out.println ( "duration.toString(): " + duration );
//        System.out.println ( "duration.toDays(): " + duration.toDays () );
//        System.out.println ( "odt.toString(): " + odt );

        tleDto.setYear(year);
        tleDto.setMonth(odt.getMonthValue());
        tleDto.setDay(odt.getDayOfMonth());
        tleDto.setHour(odt.getHour());
        tleDto.setMinute(odt.getMinute());
        tleDto.setSecond(odt.getSecond());
        tleDto.setJulianDateStr(odt.toString());

    }

    private final double u = 3.986004418 * 10e14;

    /**
     * Create orbit
     *
     * @param tleDto
     * @throws OrekitException
     */
    private SatelliteCollection createOrbit(Tle tleDto) throws OrekitException {

        Orbit initialOrbit = getInitialOrb(tleDto);

        /**
         * Slave mode
         */
        KeplerianPropagator keplerianPropagator = new KeplerianPropagator(initialOrbit);
        // Set to slave mode
        keplerianPropagator.setSlaveMode();

        SatelliteCollection satelliteCollection = new SatelliteCollection();
        satelliteCollection.setName(tleDto.getName());
        List<SatelliteDto> satelliteDtos = new ArrayList<>();
        // Propagation
        double duration = 3600. * 24;
        AbsoluteDate finalDate = new AbsoluteDate(new Date(), TimeScalesFactory.getUTC()).shiftedBy(duration);
        double stepT = 120.;
        int cpt = 1;
        for (AbsoluteDate extrapDate = new AbsoluteDate(new Date(), TimeScalesFactory.getUTC()).shiftedBy(-60);
             extrapDate.compareTo(finalDate) <= 0;
             extrapDate = extrapDate.shiftedBy(stepT)) {

            SpacecraftState currentState = keplerianPropagator.propagate(extrapDate);
//            System.out.println("step " + cpt);
//            System.out.println(" time : " + currentState.getDate());
//            System.out.println(" " + currentState.getOrbit());

            KeplerianOrbit o = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(currentState.getOrbit());
            CartesianOrbit cartesianOrbit = new CartesianOrbit(o);
            SatelliteDto satelliteDto = new SatelliteDto();
            satelliteDto.setTime((long) (stepT * cpt++));
            satelliteDto.setJulianDate(cartesianOrbit.getDate().toString() + "Z");
            satelliteDto.setCartesian3(cartesianOrbit.getPVCoordinates().getPosition().toArray());
            satelliteDtos.add(satelliteDto);
        }

        satelliteCollection.setSatellites(satelliteDtos);

        return satelliteCollection;

        /**
         * Master Mode
         */
//        // Initial state definition
//        SpacecraftState initialState = new SpacecraftState(initialOrbit);
//
//        // Adaptive step integrator
//        // with a minimum step of 0.001 and a maximum step of 1000
//        double minStep = 0.001;
//        double maxstep = 1000;
//        double positionTolerance = 10.0;
//        OrbitType propagationType = OrbitType.KEPLERIAN;
//        double[][] tolerances =
//                NumericalPropagator.tolerances(positionTolerance, initialOrbit, propagationType);
//        AdaptiveStepsizeIntegrator integrator =
//                new DormandPrince853Integrator(minStep, maxstep, tolerances[0], tolerances[1]);
//
//        NumericalPropagator propagator = new NumericalPropagator(integrator);
//        propagator.setOrbitType(propagationType);
//
//        NormalizedSphericalHarmonicsProvider provider =
//                GravityFieldFactory.getNormalizedProvider(10, 10);
//        ForceModel holmesFeatherstone =
//                new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010,
//                        true),
//                        provider);
//
//        TutorialStepHandler tutorialStepHandler = new TutorialStepHandler();
//        propagator.setMasterMode(600., tutorialStepHandler);
//
//        propagator.setInitialState(initialState);
//
//        SpacecraftState finalState =
//                propagator.propagate(new AbsoluteDate(initialDate, 63000.));

    }

    /**
     * Get intial orb
     *
     * @param tleDto
     * @return
     * @throws OrekitException
     */
    private Orbit getInitialOrb(Tle tleDto) throws OrekitException {
        Frame inertialFrame = FramesFactory.getEME2000();
        // Init date
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate initialDate = new AbsoluteDate(tleDto.getYear(), tleDto.getMonth(), tleDto.getDay(), tleDto.getHour(), tleDto.getMinute(), tleDto.getSecond(), utc);
        // Center attraction coefficient
        double mu = 3.986004415e+14;
        // Initial orbit
        double a = Math.pow(u, 1d / 3) / Math.pow((2 * tleDto.getMeanMotion() * Math.PI / 86400), 2d / 3);                             //24396159;                    // semi major axis in meters
        double e = tleDto.getEccentricity();    // 0.72831215;                  // eccentricity
        double i = FastMath.toRadians(tleDto.getInclination());       // inclination
        double omega = FastMath.toRadians(tleDto.getPerigeeArgument()); // perigee argument
        double raan = FastMath.toRadians(tleDto.getAscensionAscending());  // right ascention of ascending node
        double lM = tleDto.getMeanAnomaly();                          // mean anomaly
        Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.MEAN,
                inertialFrame, initialDate, mu);

        return initialOrbit;
    }

    private static class TutorialStepHandler implements OrekitFixedStepHandler {
        public void handleStep(SpacecraftState currentState, boolean isLast) {
            KeplerianOrbit o = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(currentState.getOrbit());
            System.out.format(Locale.US, "%s %12.3f %10.8f %10.6f %10.6f %10.6f %10.6f%n",
                    currentState.getDate(),
                    o.getA(), o.getE(),
                    FastMath.toDegrees(o.getI()),
                    FastMath.toDegrees(o.getPerigeeArgument()),
                    FastMath.toDegrees(o.getRightAscensionOfAscendingNode()),
                    FastMath.toDegrees(o.getTrueAnomaly()));
            if (isLast) {
                System.out.println("this was the last step ");
                System.out.println();
            }

            CartesianOrbit cartesianOrbit = new CartesianOrbit(o);
            System.out.format("%s", cartesianOrbit.getPVCoordinates().toString());
        }

    }

    private void sendMessage(String message, float percentage) {
        webSocket.convertAndSend("/topic/simulation/start", new SimulationMessage(message + "     ", percentage));
    }

    /**
     * Convert Strings to TLE obje
     *
     * @param tleContent
     * @return
     */
    private List<Tle> convertToTleDto(List<String> tleContent) {
        // Load to TLE Dto （every 3 lines)
        List<Tle> tleDtos = new ArrayList<>();
        int i = 0;
        Tle tleDto = new Tle();
        String tleLine1 = "";
        String tleLine2;
        for (String tleLine : tleContent) {
            if (i % 3 == 0) {
                i = 0;
            }

            if (i == 0) {
                tleDto = new Tle();
                tleDto.setName(tleLine.substring(0, 24).trim());
            }
            if (i == 1) {
                // Line 1
                tleDto.setClassification(tleLine.substring(7, 8).trim().charAt(0));
                int year2digits = Integer.parseInt(tleLine.substring(9, 11).trim());
                tleDto.setLaunchYear(year2digits > 50 ? 1900 + year2digits : 2000 + year2digits);
                tleDto.setLaunchNumber(Integer.parseInt(tleLine.substring(11, 14).trim()));
                tleDto.setLaunchPiece(tleLine.substring(14, 17).trim());
                tleDto.setEpochYear(Integer.parseInt(tleLine.substring(18, 20).trim()));
                tleDto.setJulianFraction(Double.parseDouble(tleLine.substring(20, 32).trim()));

                tleLine1 = tleLine;
            }
            if (i == 2) {
                // Line 2
                tleDto.setNumber(tleLine.substring(2, 7).trim());
                tleDto.setInclination(Double.parseDouble(tleLine.substring(8, 16).trim()));
                tleDto.setAscensionAscending(Double.parseDouble(tleLine.substring(17, 25).trim()));
                tleDto.setEccentricity(Double.parseDouble("0." + tleLine.substring(26, 33).trim()));
                tleDto.setPerigeeArgument(Double.parseDouble(tleLine.substring(34, 42).trim()));
                tleDto.setMeanAnomaly(Double.parseDouble(tleLine.substring(43, 51).trim()));
                tleDto.setMeanMotion(Double.parseDouble(tleLine.substring(52, 63).trim()));

                tleLine2 = tleLine;

//                //TODO switch to TLE class
//                try {
//                    TLE tle = new TLE(tleLine1, tleLine2);
//
////                    tleDto.setClassification(tle.getClassification());
////                    tleDto.setLaunchYear(tle.getLaunchYear());
////                    tleDto.setLaunchNumber(tle.getLaunchNumber());
////                    tleDto.setLaunchPiece(tle.getLaunchPiece());
////                    tleDto.setName(tle.getElementNumber());
//
//                    System.out.println(tle.toString());
//
//                } catch (OrekitException e) {
//                    e.printStackTrace();
//                }

                julianFractionConverter(tleDto);

                tleDtos.add(tleDto);
            }

            i++;
        }

        return tleDtos;
    }

    /**
     * Get angle velocity between two satellites in 1 second.
     *
     * @param satelliteSource
     * @param satelliteDest
     * @return
     */
    private double getAngularVelocity(SpacecraftState satelliteSource, SpacecraftState satelliteDest) {

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

        SpacecraftState satelliteSourceAgo = satelliteSource.shiftedBy(-1);
        SpacecraftState satelliteDestAgo = satelliteDest.shiftedBy(-1);

        double sourceX = satelliteSource.getPVCoordinates().getPosition().getX();
        double sourceY = satelliteSource.getPVCoordinates().getPosition().getY();
        double sourceZ = satelliteSource.getPVCoordinates().getPosition().getZ();
        double sourceXAgo = satelliteSourceAgo.getPVCoordinates().getPosition().getX();
        double sourceYAgo = satelliteSourceAgo.getPVCoordinates().getPosition().getY();
        double sourceZAgo = satelliteSourceAgo.getPVCoordinates().getPosition().getZ();

        double destX = satelliteDest.getPVCoordinates().getPosition().getX();
        double destY = satelliteDest.getPVCoordinates().getPosition().getY();
        double destZ = satelliteDest.getPVCoordinates().getPosition().getZ();
        double destXAgo = satelliteDestAgo.getPVCoordinates().getPosition().getX();
        double destYAgo = satelliteDestAgo.getPVCoordinates().getPosition().getY();
        double destZAgo = satelliteDestAgo.getPVCoordinates().getPosition().getZ();

        double a2 = Math.atan2(Math.sqrt(Math.pow(sourceY * destZ - sourceZ * destY, 2) +
                Math.pow(sourceZ * destX - sourceX * destZ, 2) +
                Math.pow(sourceX * destY - sourceY * destX, 2)
        ), sourceX * destX + sourceY * destY + sourceZ * destZ);

        double a2SecondAgo = Math.atan2(Math.sqrt(Math.pow(sourceYAgo * destZAgo - sourceZAgo * destYAgo, 2) +
                Math.pow(sourceZAgo * destXAgo - sourceXAgo * destZAgo, 2) +
                Math.pow(sourceXAgo * destYAgo - sourceYAgo * destXAgo, 2)
        ), sourceXAgo * destXAgo + sourceYAgo * destYAgo + sourceZAgo * destZAgo);

//        return (Math.abs(angleCurrent - angleSecondAgo));
        return Math.abs(a2 - a2SecondAgo);
    }

    @Qualifier("oneTimeDataTransmissionTrafficModel")
    @Autowired
    TrafficeModelService oneTimeDataTrasmissionTrafficeModel;

    @Qualifier("staticPeriodicalDataTramsmission")
    @Autowired
    TrafficeModelService staticPeriodicalDataTramsmissionTrafficModel;

    @Qualifier("regularRandomDataTransmission")
    @Autowired
    TrafficeModelService regularRandomDataTransmissionTrafficModel;

    @Qualifier("smallDataShortIntervalTransmission")
    @Autowired
    TrafficeModelService smallDataShortIntervalTransmissionTrafficModel;

    @Qualifier("smallDataRegularIntervalTransmission")
    @Autowired
    TrafficeModelService smallDataRegularIntervalTransmission;

    private TrafficeModelService getTrafficModelService(String trafficModelCode) {

        if (trafficModelCode.equalsIgnoreCase("TM1")) {
            return oneTimeDataTrasmissionTrafficeModel;
        }

        if (trafficModelCode.equalsIgnoreCase("TM2")) {
            return staticPeriodicalDataTramsmissionTrafficModel;
        }

        if (trafficModelCode.equalsIgnoreCase("TM3")) {
            return regularRandomDataTransmissionTrafficModel;
        }

        if (trafficModelCode.equalsIgnoreCase("TM4")) {
            return smallDataShortIntervalTransmissionTrafficModel;
        }

        if (trafficModelCode.equalsIgnoreCase("TM5")) {
            return smallDataRegularIntervalTransmission;
        }

        return null;
    }


    /**
     * Set shortest path
     * @param source
     * @param dest
     * @param absoluteDate
     * @return
     * @throws OrekitException
     */
    private ShortestPath getShortestPath(String source, String dest, AbsoluteDate absoluteDate) throws OrekitException {

        ShortestPath shortestPath = new ShortestPath();

        // Get ground stations.
        List<GroundStation> groundStations = groundStationService.getAll();
        // Get satellites
        List<Tle> tles = tleService.getAllTles();

        Graph graph = new Graph();
        // Add node to graph
        groundStations.forEach(gs -> graph.addNode(new Node(gs.getId(), "gs")));

        tles.forEach(tle -> graph.addNode(new Node(tle.getId(), "satellite")));

        // Get source node
        Node sourceNode = null;
        Node destNode = null;
        for (Node node : graph.getNodes()) {

            if(sourceNode != null && destNode != null){
                break;
            }
            if (node.getName().equalsIgnoreCase(source)) {
                sourceNode = node;
                continue;
            }
            if (node.getName().equalsIgnoreCase(dest)) {
                destNode = node;
                continue;
            }
        }

        // Get initial orb
        Autoconfiguration.configureOrekit();

        // Init source nodes
        Set<Node> sourceNodes = new HashSet<>();
        sourceNodes.add(sourceNode);
        // Start weight assignment
        setWeight(sourceNodes, graph, new HashSet<Node>(), absoluteDate);
//        sourceNode.addDestination(destNode, 200);
        Dijkstra.calculateShortestPathFromSource(graph, sourceNode);

        // Get shortest path to destination
        destNode.getShortestPath().forEach(node -> {
            // Check for ground station, need get name back
            if(node.getType().equals("gs")){
                GroundStation gs = groundStationService.findById(node.getName());
                shortestPath.getPath().add(gs.getName());
                shortestPath.getPathById().add(gs.getStationId());
            }else{
                Optional<Tle> tleOptional = tleService.findById(node.getName());
                if(tleOptional.isPresent()) {
                    shortestPath.getPath().add(tleOptional.get().getName());
                    shortestPath.getPathById().add(tleOptional.get().getNumber());
                }
            }
        });

        if(destNode.getType().equals("gs")){
            GroundStation gs = groundStationService.findById(destNode.getName());
            shortestPath.getPath().add(gs.getName());
            shortestPath.getPathById().add(gs.getStationId());
        }else{
            Optional<Tle> tleOptional = tleService.findById(destNode.getName());
            if(tleOptional.isPresent()) {
                shortestPath.getPath().add(tleOptional.get().getName());
                shortestPath.getPathById().add(tleOptional.get().getNumber());
            }
        }

        return shortestPath;

    }


    /**
     * Set weight from source to all nodes
     * @param sourceNodes
     * @param graph
     * @param evaluatedNodes
     * @param absoluteDate
     */
    private void setWeight(Set<Node> sourceNodes,  Graph graph,  Set<Node> evaluatedNodes, AbsoluteDate absoluteDate){

        if(sourceNodes.size() == 0){
            return;
        }

        Set<Node> innerSourceNodes = new HashSet<>();
        for(Node sourceNode : sourceNodes){
            for(Node targetNode : graph.getNodes()){
                // skip if target node already in sourceNodes or both ground station
                if(evaluatedNodes.contains(targetNode)
                        || sourceNode.getName().equals(targetNode.getName())
                        || (sourceNode.getType().equals("gs") && targetNode.getType().equals("gs"))){
                    continue;
                }

                // for both satellite
                if(sourceNode.getType().equals("satellite")){
                    if(targetNode.getType().equals("satellite")){
                        // Get both tle
                        Optional<Tle> tleSourceOptional = tleService.findById(sourceNode.getName());
                        Optional<Tle> tleDestOptional = tleService.findById(targetNode.getName());

                        if(tleSourceOptional.isPresent() && tleDestOptional.isPresent()){
                            Tle sourceTle = tleSourceOptional.get();
                            Tle destTle = tleDestOptional.get();
                            try{
                                Orbit initialOrbit = getInitialOrb(sourceTle);
                                /**
                                 * Slave mode
                                 */
                                KeplerianPropagator keplerianPropagator = new KeplerianPropagator(initialOrbit);
                                // Set to slave mode
                                keplerianPropagator.setSlaveMode();
                                SpacecraftState sourceState = keplerianPropagator.propagate(absoluteDate);

                                Orbit initialOrbitDest = getInitialOrb(destTle);
                                keplerianPropagator= new KeplerianPropagator(initialOrbitDest);
                                keplerianPropagator.setSlaveMode();
                                SpacecraftState destState = keplerianPropagator.propagate(absoluteDate);

                                // Calculate line of sight
                                double eta = FastMath.asin(Constants.WGS84_EARTH_EQUATORIAL_RADIUS / sourceState.getPVCoordinates().getPosition().getNorm());
                                double gamma = Vector3D.angle(sourceState.getPVCoordinates().getPosition().negate(), destState.getPVCoordinates().getPosition().subtract(sourceState.getPVCoordinates().getPosition()));
                                double distance = Vector3D.distance(sourceState.getPVCoordinates().getPosition(), destState.getPVCoordinates().getPosition());
//                                boolean los = (gamma > eta
//                                        || (
//                                        gamma < eta
//                                                && distance < sourceState.getPVCoordinates().getPosition().getNorm()));
                                double angularVelocity = getAngularVelocity(sourceState, destState);

                                /**
                                 * Only the link in LOS and met angular velocity requirement.
                                 */
                                if(distance < maxDistanceLos && angularVelocity<maxAngularVelocity){

                                    // Reduce to 100KM scale.
                                    sourceNode.addDestination(targetNode, distance/100000);
                                    // Add to source Nodes for next evaluation
                                    innerSourceNodes.add(targetNode);
                                }

                            }catch (OrekitException e){
                                //TODO deal with error or throw
                            }
                        }
                    }else{
                        // Source target is ground station.
                        // Get source tle
                        Optional<Tle> tleSourceOptional = tleService.findById(sourceNode.getName());
                        // Get target gs
                        GroundStation groundStation = groundStationService.findById(targetNode.getName());
                        if(tleSourceOptional.isPresent()) {
                            Tle sourceTle = tleSourceOptional.get();
                            Orbit initialOrbit = null;
                            try {
                                initialOrbit = getInitialOrb(sourceTle);
                                /**
                                 * Slave mode
                                 */
                                KeplerianPropagator keplerianPropagator = new KeplerianPropagator(initialOrbit);
                                // Set to slave mode
                                keplerianPropagator.setSlaveMode();
                                SpacecraftState sourceState = keplerianPropagator.propagate(absoluteDate);

                                Vector3D satellitePosition = sourceState.getPVCoordinates().getPosition();
                                Vector3D gs3D = new Vector3D(groundStation.getX(), groundStation.getY(), groundStation.getZ());
                                double distance = Vector3D.distance(satellitePosition, gs3D);

                                // Calculate the max distance
                                double maxDistance = Math.sqrt(
                                        Math.pow(satellitePosition.getX(),2) - Math.pow(gs3D.getX(), 2) +
                                                Math.pow(satellitePosition.getY(),2) - Math.pow(gs3D.getY(), 2) +
                                                Math.pow(satellitePosition.getZ(),2) - Math.pow(gs3D.getZ(), 2));
                                if(distance<maxDistance){
                                    sourceNode.addDestination(targetNode, distance/100000);
                                    innerSourceNodes.add(targetNode);
                                }


                            } catch (OrekitException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }else{

                    if(targetNode.getType().equals("satellite")){
                        // Source target is ground station.
                        // Get source tle
                        Optional<Tle> tleTargetOptional = tleService.findById(targetNode.getName());
                        // Get target gs
                        GroundStation groundStation = groundStationService.findById(sourceNode.getName());
                        if(tleTargetOptional.isPresent()) {
                            Tle targetTle = tleTargetOptional.get();
                            Orbit initialOrbit = null;
                            try {
                                initialOrbit = getInitialOrb(targetTle);
                                /**
                                 * Slave mode
                                 */
                                KeplerianPropagator keplerianPropagator = new KeplerianPropagator(initialOrbit);
                                // Set to slave mode
                                keplerianPropagator.setSlaveMode();
                                SpacecraftState targetState = keplerianPropagator.propagate(absoluteDate);

                                Vector3D satellitePosition = targetState.getPVCoordinates().getPosition();
                                Vector3D gs3D = new Vector3D(groundStation.getX(), groundStation.getY(), groundStation.getZ());
                                double distance = Vector3D.distance(satellitePosition, gs3D);

                                // Calculate the max distance
                                double maxDistance = Math.sqrt(
                                        Math.pow(satellitePosition.getX(),2) - Math.pow(gs3D.getX(), 2) +
                                                Math.pow(satellitePosition.getY(),2) - Math.pow(gs3D.getY(), 2) +
                                                Math.pow(satellitePosition.getZ(),2) - Math.pow(gs3D.getZ(), 2));

                                if(distance<maxDistance){
                                    sourceNode.addDestination(targetNode, distance/100000);
                                    innerSourceNodes.add(targetNode);
                                }

                            } catch (OrekitException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            // Add evaluated node to evaluated nodes
            evaluatedNodes.add(sourceNode);
            sourceNodes = innerSourceNodes;
        }

        setWeight(sourceNodes, graph, evaluatedNodes, absoluteDate);
    }



    private void weightNode(
            boolean isSourceSatellite,
            Node sourceNode,
            SpacecraftState spacecraftStateSource,
            AbsoluteDate absoluteDate,
            Set<Node> evalutedNodes,
            Set<Node> totalNodes) {

        for (Node targetNode : totalNodes) {
            if (evalutedNodes.contains(targetNode) || sourceNode.getName().equals(targetNode.getName())) {
                continue;
            }

            if (isSourceSatellite) {

                // Check for satellite
                if (targetNode.getType().equals("satellite")) {
                    // Check for LoS
                    Optional<Tle> tleOptional = tleService.findById(targetNode.getName());
                    if (tleOptional.isPresent()) {
                        Tle tle = tleOptional.get();

                        try {
                            Orbit orbit1Init = this.getInitialOrb(tle);

                            KeplerianPropagator keplerianPropagatorLoop = new KeplerianPropagator(orbit1Init);
                            keplerianPropagatorLoop.setSlaveMode();

                            // Get spacecraft state
                            SpacecraftState currentStateLoop = keplerianPropagatorLoop.propagate(absoluteDate);

                            // Calculate line of sight
                            double eta = FastMath.asin(Constants.WGS84_EARTH_EQUATORIAL_RADIUS / spacecraftStateSource.getPVCoordinates().getPosition().getNorm());
                            double gamma = Vector3D.angle(spacecraftStateSource.getPVCoordinates().getPosition().negate(), currentStateLoop.getPVCoordinates().getPosition().subtract(spacecraftStateSource.getPVCoordinates().getPosition()));
                            double distance = Vector3D.distance(spacecraftStateSource.getPVCoordinates().getPosition(), currentStateLoop.getPVCoordinates().getPosition());
                            boolean los = (gamma > eta
                                    || (
                                    gamma < eta
                                            && distance < spacecraftStateSource.getPVCoordinates().getPosition().getNorm()));

                            if (!los) {
                                continue;
                            }

                            sourceNode.addDestination(targetNode, (int) distance);

                        } catch (OrekitException e) {
                            e.printStackTrace();
                        }

                    }
                } else {

                }

            } else {
                // Ground station
                if(targetNode.getType().equals("gs")){
                    // No connection
                    continue;
                }

                // Satellite
            }
        }
    }

    /**
     * Get Distance between any two object
     * @param sourceId
     * @param destId
     * @param absoluteDate
     * @return
     * @throws OrekitException
     */
    private double getDistance(String sourceId, String destId, AbsoluteDate absoluteDate) throws OrekitException {

        double distance;
        Object source;
        Object dest;
        Optional<Satellite> satellite = Optional.ofNullable(satelliteService.findBySatelliteId(sourceId));
        if(!satellite.isPresent()){
            GroundStation groundStation = groundStationService.findByGroundStationId(sourceId);
            source = groundStation;
        }else{
            source = satellite.get();
        }

        Optional<Satellite> satelliteDest = Optional.ofNullable(satelliteService.findBySatelliteId(destId));
        if(!satelliteDest.isPresent()){
            dest = groundStationService.findByGroundStationId(destId);
        }else{
            dest = satelliteDest.get();
        }

        Vector3D sourceVector3D = getVector3D(source, absoluteDate);
        Vector3D destVector3D = getVector3D(dest, absoluteDate);

        distance = Vector3D.distance(sourceVector3D, destVector3D);

        return distance;
    }


    /**
     * Get Vector3D based on object and time
     * @param obj
     * @param absoluteDate
     * @return
     * @throws OrekitException
     */
    private Vector3D getVector3D(Object obj, AbsoluteDate absoluteDate) throws OrekitException {

        Vector3D vector3D = null;
        if(obj instanceof Satellite){
            Optional<Tle> currentTleOpt = tleService.findBySatelliteId(((Satellite)obj).getSatelliteId());
            if(currentTleOpt.isPresent()) {
                Tle currentTle = currentTleOpt.get();
                // init orbit
                Orbit orbit1Init = this.getInitialOrb(currentTle);
                KeplerianPropagator keplerianPropagator = new KeplerianPropagator(orbit1Init);
                keplerianPropagator.setSlaveMode();

                // Get spacecraft state
                SpacecraftState currentState = keplerianPropagator.propagate(absoluteDate);
                vector3D = currentState.getPVCoordinates().getPosition();
            }
        }else{
            GroundStation groundStation = (GroundStation) obj;
            vector3D = new Vector3D(groundStation.getX(),groundStation.getY(),groundStation.getZ());
        }

        return vector3D;

    }

    /**
     * Get initial settings
     * @return
     */
    @GetMapping(value = "/settings/init", produces = "application/json")
    public SettingsInit getSettingInit() {

        SettingsInit settingsInit = new SettingsInit();
        List<SatelliteSettings> satelliteSettings = new ArrayList<>();

        List<Satellite> satellites = satelliteService.getAll();


        satellites.forEach(satellite -> {
            SatelliteSettings satelliteSetting = new SatelliteSettings();
            satelliteSetting.setSatID(satellite.getSatelliteId());
            satelliteSetting.setSatName(satellite.getName());

            // Properties
            List<Parameter> parameters = satellite.getSatelliteParams();
            parameters.forEach(parameter -> {
                if(parameter.getName().equalsIgnoreCase("outLinkMax")){
                    satelliteSetting.setOutLinkMax(Float.parseFloat(parameter.getValue()));
                }

                if(parameter.getName().equalsIgnoreCase("inLinkMax")){
                    satelliteSetting.setInLinkMax(Float.parseFloat(parameter.getValue()));
                }

                if(parameter.getName().equalsIgnoreCase("netIfnum")){
                    satelliteSetting.setNetIfnum(Float.parseFloat(parameter.getValue()));
                }
            });

            satelliteSettings.add(satelliteSetting);
        });

        // ground station data
        List<GroundStation> groundStations = groundStationService.getAll();
        List<BaseStationSettings> baseStationSettings = new ArrayList<>();
        groundStations.forEach(groundStation -> {
            BaseStationSettings bs = new BaseStationSettings();
            bs.setName(groundStation.getName());
            bs.setId(groundStation.getStationId());

            baseStationSettings.add(bs);
        });

        settingsInit.setSatelliteSettings(satelliteSettings);
        settingsInit.setBaseStationSettings(baseStationSettings);

        return settingsInit;
    }


    /**
     * Get list data for Mininet
     * @return
     */
    @GetMapping(value = "/data/applications", produces = "application/json")
    public List<ApplicationDataMininetDto> getApplications(){

        List<ApplicationDataMininetDto> dataMininetDtos = new ArrayList<>();
        List<MSAApplication> applications = msApplicationService.getApplications();

        applications.forEach(app->{
            ApplicationDataMininetDto applicationDataMininetDto = new ApplicationDataMininetDto();
            applicationDataMininetDto.setAppName(app.getAppName());
            applicationDataMininetDto.setStartTime(app.getStartTime().toString());
            applicationDataMininetDto.setEndTime(app.getEndTime().toString());
            applicationDataMininetDto.setProtocol(app.getProtocol());
            List<ApplicationEventDto> applicationEventDtos = new ArrayList<>();
            app.getApplicationEvents().forEach(applicationEvent -> {
                ApplicationEventDto eventDto = new ApplicationEventDto();
                eventDto.setPath(applicationEvent.getRouting());
                eventDto.setDatavol(new Random().nextInt(50));
                eventDto.setThroughput(98+ new Random().nextInt(2));
                eventDto.setTimetick(applicationEvent.getTick());
                eventDto.setDistance(applicationEvent.getDistanceJson());
                applicationEventDtos.add(eventDto);
            });
            applicationDataMininetDto.setEvents(applicationEventDtos);
            dataMininetDtos.add(applicationDataMininetDto);
        });

        return dataMininetDtos;
    }


    @PostMapping(value = "/connectionDisplay")
    @ResponseStatus(HttpStatus.OK)
    public void setConnectionDisplay(@RequestBody ConnDisplayDto connectionDisplay){

        List<ConnDisplayItemDto> links = connectionDisplay.getLinks();

        // Remove all those element with thpt == 0
        links.removeIf(link -> link.getThpt() == 0f);

        // Update the id
        links.forEach(item -> {

            String sourceId;
            String destId;

            Optional<Tle> tleSource = tleService.getTleByNumber(item.getSrce());
            if(tleSource.isPresent()){
                sourceId = tleSource.get().getName();
            }else{
                // Try ground station
                Optional<GroundStation> gs = groundStationService.findByGroundStationIdExternal(Integer.parseInt(item.getSrce()));
                if(gs.isPresent()){
                    sourceId = gs.get().getName();
                }else{
                    return;
                }
            }
            Optional<Tle> tleDest = tleService.getTleByNumber(item.getDest());
            if(tleDest.isPresent()){
                destId = tleDest.get().getName();
            }else{
                // Try ground station
                Optional<GroundStation> gs = groundStationService.findByGroundStationIdExternal(Integer.parseInt(item.getDest()));
                if(gs.isPresent()){
                    destId = gs.get().getName();
                }else{
                    return;
                }
            }

            if(sourceId != null & destId != null){
                item.setIdSource(sourceId);
                item.setIdDest(destId);
            }

            // Adjust the color
            if(item.getPdelay()<20f){
                item.setColor("green");
            }else if(item.getPdelay()>400f){
                item.setColor("red");
            }else{
                item.setColor("yellow");
            }

            // Adjust the width
            item.setThpt((float)Math.log10(item.getThpt()) + 1f);
        });
        System.out.println("Connection Display");
        // Send data to websock for display.
        try{
            webSocket.convertAndSend("/topic/api/update", new WebSocketMessage(objectMapper.writeValueAsString(connectionDisplay)));
        }catch(JsonProcessingException e){
            e.printStackTrace();
        }

    }

    ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping(value = "/mininet/update")
    @ResponseStatus(HttpStatus.OK)
    public void updateMininetResult(@RequestBody MininetDataDto mininetDataDto){

        System.out.println(mininetDataDto.toString());

        try {
            webSocket.convertAndSend("/topic/mininet/update", new MessageMininet(objectMapper.writeValueAsString(mininetDataDto)));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }

    @PostMapping(value = "/mininet/routing", produces = "application/json")
    public void routingConnet(@RequestBody RoutingDto routingDto){

        // Send data to front
        try{
            webSocket.convertAndSend("/topic/mininet/vis", new MessageMininet(objectMapper.writeValueAsString(routingDto)));
        }catch (JsonProcessingException e){
            e.printStackTrace();
        }
    }
}
