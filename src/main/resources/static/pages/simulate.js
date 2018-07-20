
var viewer;
var startTime;

var maxDistance = 51591*1000;       //Meter
var maxAngularVelocity = 0.000087;

$(function(){

    // Bing map key
    Cesium.BingMapsApi.defaultKey = 'Ak8mO9f0VpoByuNwmMcVvFka1GCZ3Bh8VrpqNLqGtIgsuUYjTrJdw7kDZwAwlC7E';
    var terrainProvider = new Cesium.CesiumTerrainProvider({
        url : 'https://assets.agi.com/stk-terrain/world',
        requestVertexNormals : true
    });
    viewer = new Cesium.Viewer('cesiumContainer', {
        terrainProvider : terrainProvider,
        baseLayerPicker : false,
        shadows: true
    });

    // var startTime = Cesium.JulianDate.fromDate(new Date(), startTime);
    // var endTime = startTime;
    // endTime = Cesium.JulianDate.addDays(startTime, 1, endTime);
    // viewer.clock.startTime = startTime;
    // viewer.clock.endTime = endTime;
    // viewer.clock.ClockStep = Cesium.ClockStep.TICK_DEPENDENT;
    viewer.clock.clockRange = Cesium.ClockRange.LOOP_STOP; //Loop at the end
    viewer.clock.multiplier = 60;
    startTime = Cesium.JulianDate.clone(viewer.clock.startTime, startTime);

    /**
     * Init all satellites.
     */
    $.ajax({
        url: '/api/satellite/init',
        method: 'GET'
    }).done(function(data){
        satelliteData =  _.sortBy(data, function(satellite) {return satellite.name;});;
        _.map(data, function(satellite){
            addSatellite(satellite.name, satellite.satellites);
        });
        calculateStep();
    });

    // Tick event
    // viewer.clock.onTick.addEventListener(handleTick);



});

var log = _.bind(console.log, console);

function calculateStep(){

    var orbSatellites = viewer.entities.values;

    // Do calculate then add step.
    // console.log(viewer.clock.currentTime);
    orbSatellites.forEach(function calculateBetweenTwoObject(item, index){
        console.log("For Satellite: " + item.id);
       // Loop entities again
        orbSatellites.forEach(function calculateBetweenTwoObjectDeep(itemDeep, indexDeep){
           // Avoid duplicated calculate
           if(index >= indexDeep){
               // Skip
               return;
           }

           try {
               // Reset time.
               viewer.clock.currentTime = Cesium.JulianDate.clone(startTime, viewer.clock.startTime);
               while(true) {
                   // Calculate distance
                   var distance = Cesium.Cartesian3.distance(item.position.getValue(viewer.clock.currentTime),
                       itemDeep.position.getValue(viewer.clock.currentTime));

                   // Check for angular velocity
                   var postSecond = Cesium.JulianDate.clone(viewer.clock.currentTime);
                   postSecond = Cesium.JulianDate.addSeconds(postSecond, 1, postSecond);

                   var angularVelocity = Cesium.Cartesian3.angleBetween(item.position.getValue(viewer.clock.currentTime),
                       itemDeep.position.getValue(viewer.clock.currentTime)) -
                       Cesium.Cartesian3.angleBetween(item.position.getValue(postSecond),
                           itemDeep.position.getValue(postSecond));

                   if(distance <= maxDistance && Math.abs(angularVelocity)<=maxAngularVelocity){
                       var data = {'sourceid': item.id, 'destid': itemDeep.id, 'distance': distance, 'angularvelocity': Math.abs(angularVelocity)};
                       // Ajax call send to back
                       $.ajax({
                           type: "POST",
                           url: "/api/event/trigger",
                           data: JSON.stringify(data),
                           contentType: "application/json",
                           success: function(data){
                               console.log(data);
                           }
                       })
                   }
                   console.log("Distance between: " + item.id + " | " + itemDeep.id + " : " + distance +
                   "\n\rAngular velocity: " + angularVelocity);

                   viewer.clock.currentTime = Cesium.JulianDate.addMinutes(viewer.clock.currentTime,
                       30,
                       viewer.clock.currentTime);
               }
           }catch(ex){
               console.error("END!");
           }
       })
    });
}

/**
 * Add satellite data to cesium visualization
 * @param satelliteName
 * @param satelliteData
 */
function addSatellite(satelliteName, satelliteData){
    var entityTime = Cesium.JulianDate.clone(viewer.clock.startTime, entityTime);

    // Create new entity
    var newEntity = new Cesium.Entity({
        id: satelliteName,
        name: satelliteName,
        label: new Cesium.LabelGraphics({
            text: satelliteName,
            scale: 0.5,
            pixelOffset: new Cesium.Cartesian2(35,15),
            scaleByDistance: new Cesium.NearFarScalar(70000000, 1.2, 100000000, 0.5)
        }),
        billboard:{
            image: "/Image/satellite-1.png",
            show: true
        }

    });

    // Time and position
    var positions = new Cesium.SampledPositionProperty();
    _.each(satelliteData, function(data){

        // Add point as sample
        positions.addSample(Cesium.JulianDate.clone(entityTime),
            Cesium.Cartesian3.fromArray(data.cartesian3));

        // 60" for each record
        Cesium.JulianDate.addSeconds(entityTime, 60, entityTime);
    });

    newEntity.position = positions;
    newEntity.orientation = new Cesium.VelocityOrientationProperty(positions);

    // if(true) {
    //     var path = new Cesium.PathGraphics();
    //     path.material = fadedLine;
    //     path.leadTime = new Cesium.ConstantProperty(0);
    //     path.trailTime = new Cesium.ConstantProperty(3600 * 24);
    //
    //
    //     newEntity.path = path;
    // }


    // Make a smooth path
    newEntity.position.setInterpolationOptions({
        interpolationDegree : 5,
        interpolationAlgorithm : Cesium.LagrangePolynomialApproximation
    });

    // Add to viewer
    viewer.entities.add(newEntity);
}


/**
 * Handle tick
 * @param clock
 */
function handleTick(clock){

}