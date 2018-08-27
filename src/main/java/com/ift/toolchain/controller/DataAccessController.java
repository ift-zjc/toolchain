package com.ift.toolchain.controller;

import com.ift.toolchain.Service.*;
import com.ift.toolchain.configuration.Autoconfiguration;
import com.ift.toolchain.dto.*;
import com.ift.toolchain.model.*;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
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
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sun.util.resources.LocaleData;

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
    TrafficeModelGenericService trafficeModelGenericService;

    @PostMapping(value = "/event/trigger")
    public void saveEvent(@RequestBody ObjectEvent objectEvent){

        MessageHub messageHub = messageHubService.create(objectEvent);

    }


    /**
     * Get traffic model datagrid source
     * @return
     */
    @PostMapping(value = "/tmlist", produces = "application/json")
    @ResponseBody
    public String getTrafficeModelDataSource(){
        return trafficeModelGenericService.getTMList();
    }


    /**
     * Get Traffic Model by ID
     * @param key
     * @return
     */
    @PostMapping(value = "/tm/{key}", produces = "application/json")
    @ResponseBody
    public String getTrafficModelByKey(@PathVariable String key){
        Optional<TrafficModel> trafficModel =  trafficeModelGenericService.getByCode(key);

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
        List<Satellite> satellites = satelliteService.getAll();
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
        Satellite satellite = satelliteService.findByName(key);
        if(satellite != null){
            objectDto = new ObjectDto(satellite.getId(), satellite.getName(), "Satellite");
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
            storageService.store(file);
        });
    }


    @Autowired
    SimpMessagingTemplate webSocket;
    @Autowired
    ConfigFileService configFileService;

    @PostMapping("/simulation/start")
    public List<SatelliteCollection> simulate() throws Exception{

        sendMessage("Start reading TLE data ...", 1f);
        // Get TLE file
        ConfigFile tleFile= configFileService.getConfigFile("TLE");
        Path path = storageService.load(tleFile.getFileName());
        List<String> tleList = new ArrayList<>();
        try (Stream<String> stream = Files.lines(path)){
            tleList = stream.collect(Collectors.toList());
        }

        // Load to TLE Dto （every 3 lines)
        List<Tle> tleDtos = new ArrayList<>();
        int i = 0;
        Tle tleDto = new Tle();
        for(String tleLine : tleList){
            if(i%3 == 0){
                i = 0;
            }

            if(i == 0){
                tleDto = new Tle();
                tleDto.setName(tleLine.substring(0,24).trim());
            }
            if(i == 1){
                // Line 1
                tleDto.setClassification(tleLine.substring(7, 8).trim());
                tleDto.setEpochYear(Integer.parseInt(tleLine.substring(18, 20).trim()));
                tleDto.setJulianFraction(Double.parseDouble(tleLine.substring(20, 32).trim()));
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

                julianFractionConverter(tleDto);

                tleDtos.add(tleDto);
            }

            i++;
        }
        sendMessage("TLE data acquired ...", 10f);


        /**
         * Generate orbit
         */
        sendMessage("Generating satellite data ...", 11.0f);
        List<SatelliteCollection> satelliteCollections = new ArrayList<>();
        Autoconfiguration.configureOrekit();

        int satelliteCnt = tleDtos.size();
        float index = 1f;
        for(Tle item : tleDtos){
            tleService.save(item);

            sendMessage("Generating satellite data (" + item.getName() +  ") ...", 11f + (100f-11f)/(1 + (satelliteCnt - index++)));
            // Start generate orbits
            try {
                satelliteCollections.add(createOrbit(item));
            } catch (OrekitException e) {
                e.printStackTrace();
            }
        }

        webSocket.convertAndSend("/topic/simulation/start", new SimulationMessage("TLE data acquired ...", 100.0f));

        return satelliteCollections;

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
        double duration = 6000.;
        AbsoluteDate finalDate = new AbsoluteDate(new Date(), TimeScalesFactory.getUTC()).shiftedBy(duration);
        double stepT = 60.;
        int cpt = 1;
        for (AbsoluteDate extrapDate = new AbsoluteDate(new Date(), TimeScalesFactory.getUTC());
             extrapDate.compareTo(finalDate) <= 0;
             extrapDate = extrapDate.shiftedBy(stepT)){

            SpacecraftState currentState = keplerianPropagator.propagate(extrapDate);
            System.out.println("step " + cpt);
            System.out.println(" time : " + currentState.getDate());
            System.out.println(" " + currentState.getOrbit());

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
}
