package com.ift.toolchain.controller;

import com.ift.toolchain.Service.*;
import com.ift.toolchain.configuration.Autoconfiguration;
import com.ift.toolchain.dto.*;
import com.ift.toolchain.dto.SatellitePosition;
import com.ift.toolchain.dto.SatelliteXSatellite;
import com.ift.toolchain.model.*;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.joda.time.DateTime;
import org.json.simple.JSONObject;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.InterSatellitesRange;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.*;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sun.util.resources.LocaleData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
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


    /**
     * Get traffic model datagrid source
     * @return
     */
    @PostMapping(value = "/tmlist", produces = "application/json")
    @ResponseBody
    public String getTrafficeModelDataSource(){
        return trafficModelGenericService.getTMList();
    }


    /**
     * Get Traffic Model by ID
     * @param key
     * @return
     */
    @PostMapping(value = "/tm/{key}", produces = "application/json")
    @ResponseBody
    public String getTrafficModelByKey(@PathVariable String key){
        Optional<TrafficModel> trafficModel =  trafficModelGenericService.getByCode(key);

        // Json String
        String response = "{";
        if(trafficModel.isPresent()){
            TrafficModel model = trafficModel.get();
            response += "\"name\":\"" + model.getName() + "\",";
            response += "\"code\":\"" + model.getCode() + "\",";
            response += "\"desc\":\"" + model.getDescription() + "\",";

            // Get configuration
            List<TrafficModelConfig> trafficModelConfigs = model.getTrafficModelConfigs();
            for(TrafficModelConfig config : trafficModelConfigs){
                response += "\"" + config.getName() + "\": \"" + config.getValue() + "\",";
            }
        }

        response = response.substring(0, response.length()-1);

        response += "}";

        return response;
    }


    /**
     * Get object list
     * @return
     */
    @GetMapping(value = "/objectlist", produces = "application/json")
    public List<ObjectDto> getAllObjects(){
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
    public ObjectDto getObjectById(@PathVariable String key){

        ObjectDto objectDto;

        // Try to find satellite
        Optional<Tle> satellite = tleService.findById(key);
        if(satellite.isPresent()){
            objectDto = new ObjectDto(satellite.get().getId(), satellite.get().getName(), "Satellite");
        }else{
            GroundStation groundStation = groundStationService.findByName(key);
            objectDto = new ObjectDto(groundStation.getId(), groundStation.getName(), "Ground station");
        }

        return objectDto;
    }


    @Autowired
    StorageService storageService;
    @PostMapping("/file/upload")
    public void handleFileUpload(@RequestParam("files[]") MultipartFile[] files){
        // manipulate file name
        // Save to storage
        Arrays.stream(files).forEach(file -> {
            String filename = storageService.store(file);

            // Save to database
            // Get TLE file
//            ConfigFile tleFile= configFileService.getConfigFile("TLE");
            Path path = storageService.load(filename);
            List<String> tleList = new ArrayList<>();
            try (Stream<String> stream = Files.lines(path)){
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
            for(Tle item : tleDtos){
                try{
                    tleService.save(item);
                }catch(Exception ex){
                    // TODO handle DB exception, ignore now.
                    // Database error, most for duplicated record
                }

            }
        });
    }

    private final String categoryId = "GPS_SATELLITES";

    @Autowired
    SimpMessagingTemplate webSocket;
    @Autowired
    ConfigFileService configFileService;

    @PostMapping("/simulation/populateobjects")
    public SatellitePopulated populateobjects() throws Exception{

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
        for(Tle item : tleDtos){

            satelliteItems.add(new SatelliteItem(UUID.randomUUID().toString(), item.getName(), false, categoryId));

            sendMessage("Generating satellite data (" + item.getName() +  ") ...", 11f + (100f-11f)/(1 + (satelliteCnt - index++)));
            // Start generate orbits
            try {
                satelliteCollections.add(createOrbit(item));
            } catch (OrekitException e) {
                e.printStackTrace();
            }
        }

        sendMessage("TLE data acquired ...", 100f);

        SatellitePopulated satellitePopulated = new SatellitePopulated();
        satellitePopulated.setSatelliteCollections(satelliteCollections);
        satellitePopulated.setSatelliteItems(satelliteItems);

        return satellitePopulated;

    }

    /**
     * Add satellites via 2-line element string
     * @param payload
     * @return
     */
    @PostMapping(value = "/satellite/add", consumes = "application/json")
    public SatellitePopulated addSatellites(@RequestBody Map<String, Object> payload){

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
        for(Tle item : tleDtos){
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
            }catch(Exception e){
                // TODO handle DB exception, ignore now.
            }
        }

        SatellitePopulated satellitePopulated = new SatellitePopulated();
        satellitePopulated.setSatelliteCollections(satelliteCollections);
        satellitePopulated.setSatelliteItems(satelliteItems);

        return satellitePopulated;
    }

    @PostMapping(value = "/simulation/start", consumes = "application/json")
    public SimulateData startSimulator(@RequestBody Map<String, Object> payload) throws OrekitException {

        Autoconfiguration.configureOrekit();
        int step = 30;      // second
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

        // Remove all messageses/msa application settings before start
        messageHubService.removeAll();
        msApplicationService.removeAll();


        for (int i = 0; i<tleDtos.size()-1; i++){
            Orbit orbit1Init = this.getInitialOrb(tleDtos.get(i));
            AbsoluteDate absoluteDateCurrentStartDate = absoluteStartDate;
            propagator1 = new KeplerianPropagator(orbit1Init);
            propagator1.setSlaveMode();

            for(int j = i+1; j<tleDtos.size(); j++){

                MessageHub messageHub = new MessageHub();
                Orbit orbit2Init = this.getInitialOrb(tleDtos.get(j));
                propagator2 = new KeplerianPropagator(orbit2Init);
                propagator2.setSlaveMode();
                // Loop between time.
                while(absoluteDateCurrentStartDate.compareTo(absoluteEndDate) <= 0){
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
                    boolean distanceChanged = Math.abs((messageHub.getDistance()-distance)/messageHub.getDistance())>=delta;

                    boolean needUpdate = absoluteDateCurrentStartDate.compareTo(absoluteStartDate) == 0 ||
                            distanceChanged || losChanged;

                    if(needUpdate) {
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
                        }else {
                            if (losChanged) {
                                messageHub.setMsgType(1);
                                messageHub.setSubMsgType(3);                // Connectivity updated
                            } else {
                                if(los){
                                    if(distanceChanged){
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

        for(HashMap<String, Object> hashMap : applicationDtos){
            // Get current absolute date based on start time.
            String _startTime = hashMap.get("startTime").toString();
            String _endTime = hashMap.get("endTime").toString();
            DateTime _start = DateTime.parse( _startTime);
            DateTime _end = DateTime.parse(_endTime);
            String appName = hashMap.get("name").toString();

            ApplicationTraffic applicationTraffic = new ApplicationTraffic();
            applicationTraffic.setAppName(appName);

            // Get traffic model for this application.
            HashMap<String, String> tmData =  (HashMap<String, String>)hashMap.get("tm");
            String tmCode = tmData.get("code");
            // Find traffic model
            Optional<TrafficModel> trafficModel = trafficModelGenericService.getByCode(tmCode);
            // Skip to next application if this application's traffic model is not presented.
            if(!trafficModel.isPresent()){
                continue;
            }

            // Save to database before proc.
            MSAApplication msaApplication = new MSAApplication();
            msaApplication.setAppName(appName);
            msaApplication.setStartTime(_start.toDate());
            msaApplication.setEndTime(_end.toDate());
            msaApplication.setSourceObj(hashMap.get("source").toString());
            msaApplication.setDestObj(hashMap.get("dest").toString());
            msaApplication.setTrafficModelCode(tmCode);
            msaApplication.setTrafficModelConfig((new JSONObject(tmData)).toJSONString());
            msApplicationService.add(msaApplication);

            TrafficModel trafficModelExtracted = trafficModel.get();
            // Loop traffic model attributes and update if necessary
            trafficModelExtracted.getTrafficModelConfigs().stream().forEach(tmConfig -> {
                if(tmData.containsKey(tmConfig.getName())){
                    tmConfig.setValue(String.valueOf(tmData.get(tmConfig.getName())));
                }
            });

            TrafficeModelService trafficeModelService = this.getTrafficModelService(tmCode);

            List<ApplicationTrafficData> applicationTrafficDataList = trafficeModelService.simulate(_start, _end, trafficModelExtracted.getTrafficModelConfigs(), applicationTraffic.getAppName());
            applicationTraffic.setApplicationTrafficDataList(applicationTrafficDataList);
            applicationTraffics.add(applicationTraffic);
        }

        SimulateData simulateData = new SimulateData();
        simulateData.setSimulateResultDtos(null);
        simulateData.setApplicationTraffic(applicationTraffics);

        return simulateData;
    }

    /**
     * Disable satellite from database
     * @param payload
     * @return
     */
    @PostMapping(value = "/satellite/disable", consumes="application/json")
    public NameValue disableSatellite(@RequestBody Map<String, Object> payload){
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
        DateTime dateTime = DateTime.parse(julianTime.length()>23 ? julianTime.substring(0, 23) : julianTime);        // cut time string
        AbsoluteDate absoluteDate = new AbsoluteDate(dateTime.toDate(), TimeScalesFactory.getUTC());

        // Get orbit status
        SpacecraftState currentState = keplerianPropagator.propagate(absoluteDate);
        KeplerianOrbit o = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(currentState.getOrbit());

        List<NameValue> nameValues = new ArrayList<>();

        // Populate status
        nameValues.add( new NameValue("Satellite Name", satelliteName));
        nameValues.add( new NameValue("Satellite Id", tle.getNumber()));
        nameValues.add( new NameValue("Classification", tle.getClassification()));
        nameValues.add( new NameValue("COSPAR ID", tle.getLaunchYear()+"-"+tle.getLaunchNumber()+tle.getLaunchPiece()));
        nameValues.add( new NameValue("Inclination", FastMath.toDegrees(o.getI()) + "\u00B0"));
        nameValues.add( new NameValue("Eccentricity", o.getEccentricAnomaly()));
        nameValues.add( new NameValue("RA ascending node", o.getRightAscensionOfAscendingNode()));
        nameValues.add( new NameValue("Argument perihelion", FastMath.toDegrees(o.getPerigeeArgument()) + "\u00B0"));
        nameValues.add( new NameValue("Mean anomaly", FastMath.toDegrees(o.getMeanAnomaly())  + "\u00B0"));
        nameValues.add( new NameValue("Position X[m]", o.getPVCoordinates().getPosition().getX()) );
        nameValues.add( new NameValue("Position Y[m]", o.getPVCoordinates().getPosition().getY()));
        nameValues.add( new NameValue("Position Z[m]", o.getPVCoordinates().getPosition().getZ()));
        nameValues.add( new NameValue("Angular Velocity X", o.getPVCoordinates().getAngularVelocity().getX()));
        nameValues.add( new NameValue("Angular Velocity Y", o.getPVCoordinates().getAngularVelocity().getY()));
        nameValues.add( new NameValue("Angular Velocity Z", o.getPVCoordinates().getAngularVelocity().getZ()));
        nameValues.add( new NameValue("Acceleration X", o.getPVCoordinates().getAcceleration().getX()));
        nameValues.add( new NameValue("Acceleration Y", o.getPVCoordinates().getAcceleration().getY()));
        nameValues.add( new NameValue("Acceleration Z", o.getPVCoordinates().getAcceleration().getZ()));

        selectedSatelliteDto.setSatelliteProperties(nameValues);

        // Find 4 nearest satellites
        List<SatelliteXSatellite> satelliteXSatellites = new ArrayList<>();
        // Get all the objects from database
        List<Tle> tleDtos = tleService.getAllTles();
        for(int i = 0; i<tleDtos.size(); i++){
            Tle currentTle = tleDtos.get(i);
            if(currentTle.getName().equalsIgnoreCase(satelliteName)){
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

            if(los){
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
     * Reset data
     * @return
     */
    @PostMapping(value = "/simulation/reset")
    public boolean resetSimulation(){
        boolean success = false;

        try {
            // Delete all records from TLE table
            tleService.removeRecords();
            // Delete all records from message table
            messageHubService.removeAll();
            success = true;
        }catch(Exception ex){

        }

        return success;
    }



    private void julianFractionConverter(Tle tleDto){

        // Get year
        int epochYear = tleDto.getEpochYear();
        int year = epochYear > 70 ? 1900 + epochYear : 2000 + epochYear;
        OffsetDateTime epochCalendarNewStyleActOf2017 = LocalDate.of ( year , Month.JANUARY , 1 ).atStartOfDay ().atOffset ( ZoneOffset.UTC );

        BigDecimal bd = new BigDecimal(tleDto.getJulianFraction());
        long days = bd.toBigInteger().longValue();
        BigDecimal fractionOfADay = bd.subtract ( new BigDecimal ( days ) ); // Extract the fractional number, separate from the integer number.
        BigDecimal secondsFractional = new BigDecimal ( TimeUnit.DAYS.toSeconds ( 1 ) ).multiply ( fractionOfADay );
        long secondsWhole = secondsFractional.longValue ();
        long nanos = secondsFractional.subtract ( new BigDecimal ( secondsWhole ) ).multiply ( new BigDecimal ( 1_000_000_000L ) ).longValue ();
        Duration duration = Duration.ofDays ( days ).plusSeconds ( secondsWhole ).plusNanos ( nanos );
        OffsetDateTime odt = epochCalendarNewStyleActOf2017.plus ( duration );

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
             extrapDate = extrapDate.shiftedBy(stepT)){

            SpacecraftState currentState = keplerianPropagator.propagate(extrapDate);
//            System.out.println("step " + cpt);
//            System.out.println(" time : " + currentState.getDate());
//            System.out.println(" " + currentState.getOrbit());

            KeplerianOrbit o = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(currentState.getOrbit());
            CartesianOrbit cartesianOrbit = new CartesianOrbit(o);
            SatelliteDto satelliteDto = new SatelliteDto();
            satelliteDto.setTime((long) (stepT*cpt++));
            satelliteDto.setJulianDate(cartesianOrbit.getDate().toString()+"Z");
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
        double a = Math.pow(u, 1d/3) / Math.pow((2*tleDto.getMeanMotion()*Math.PI/86400), 2d/3);                             //24396159;                    // semi major axis in meters
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

//        public void init(final SpacecraftState s0, final AbsoluteDate t) {
//            System.out.println("          date                a           e" +
//                    "           i         \u03c9          \u03a9" +
//                    "          \u03bd");
//        }


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

    private void sendMessage(String message, float percentage){
        webSocket.convertAndSend("/topic/simulation/start", new SimulationMessage(message + "     ", percentage));
    }

    /**
     * Convert Strings to TLE obje
     * @param tleContent
     * @return
     */
    private List<Tle> convertToTleDto(List<String> tleContent){
        // Load to TLE Dto （every 3 lines)
        List<Tle> tleDtos = new ArrayList<>();
        int i = 0;
        Tle tleDto = new Tle();
        String tleLine1 = "";
        String tleLine2;
        for(String tleLine : tleContent){
            if(i%3 == 0){
                i = 0;
            }

            if(i == 0){
                tleDto = new Tle();
                tleDto.setName(tleLine.substring(0,24).trim());
            }
            if(i == 1){
                // Line 1
                tleDto.setClassification(tleLine.substring(7, 8).trim().charAt(0));
                int year2digits = Integer.parseInt(tleLine.substring(9,11).trim());
                tleDto.setLaunchYear(year2digits > 50 ? 1900 + year2digits : 2000 + year2digits);
                tleDto.setLaunchNumber(Integer.parseInt(tleLine.substring(11, 14).trim()));
                tleDto.setLaunchPiece(tleLine.substring(14, 17).trim());
                tleDto.setEpochYear(Integer.parseInt(tleLine.substring(18, 20).trim()));
                tleDto.setJulianFraction(Double.parseDouble(tleLine.substring(20, 32).trim()));

                tleLine1 = tleLine;
            }
            if(i == 2){
                // Line 2
                tleDto.setNumber(tleLine.substring(2,7).trim());
                tleDto.setInclination(Double.parseDouble(tleLine.substring(8, 16).trim()));
                tleDto.setAscensionAscending(Double.parseDouble(tleLine.substring(17, 25).trim()));
                tleDto.setEccentricity(Double.parseDouble("0."+tleLine.substring(26, 33).trim()));
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
     * @param satelliteSource
     * @param satelliteDest
     * @return
     */
    private double getAngularVelocity(SpacecraftState satelliteSource, SpacecraftState satelliteDest){

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

        double a2 = Math.atan2(Math.sqrt(Math.pow( sourceY*destZ - sourceZ*destY, 2 ) +
                Math.pow( sourceZ*destX - sourceX*destZ, 2 ) +
                Math.pow( sourceX*destY - sourceY*destX, 2 )
        ), sourceX*destX + sourceY*destY + sourceZ*destZ);

        double a2SecondAgo = Math.atan2(Math.sqrt(Math.pow( sourceYAgo*destZAgo - sourceZAgo*destYAgo, 2 ) +
                Math.pow( sourceZAgo*destXAgo - sourceXAgo*destZAgo, 2 ) +
                Math.pow( sourceXAgo*destYAgo - sourceYAgo*destXAgo, 2 )
        ), sourceXAgo*destXAgo + sourceYAgo*destYAgo + sourceZAgo*destZAgo);

//        return (Math.abs(angleCurrent - angleSecondAgo));
        return Math.abs(a2-a2SecondAgo);
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

    private TrafficeModelService getTrafficModelService(String trafficModelCode){

        if(trafficModelCode.equalsIgnoreCase("TM1")) {
            return oneTimeDataTrasmissionTrafficeModel;
        }

        if(trafficModelCode.equalsIgnoreCase("TM2")) {
            return staticPeriodicalDataTramsmissionTrafficModel;
        }

        if(trafficModelCode.equalsIgnoreCase("TM3")){
            return regularRandomDataTransmissionTrafficModel;
        }

        if(trafficModelCode.equalsIgnoreCase("TM4")) {
            return smallDataShortIntervalTransmissionTrafficModel;
        }

        if(trafficModelCode.equalsIgnoreCase("TM5")) {
            return smallDataRegularIntervalTransmission;
        }

        return null;
    }
}
