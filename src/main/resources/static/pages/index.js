
var viewer;
var ddSatelliteNumber;
var ddTicket;
var point;
var fadedLine;
var satelliteData;
var orbIndex;
var maxDistance = 51591*1000;       //Meter
var maxAngularVelocity = 0.000087;

var dgStatistics;
$(function(){

    initComponents();

    orbIndex = ['A','B','C','D','E','F'];
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

    viewer.clock.clockRange = Cesium.ClockRange.LOOP_STOP; //Loop at the end
    viewer.clock.multiplier = 60;


    point = new Cesium.PointGraphics({
        pixelSize: 5,
        color: Cesium.Color.YELLOW
    });

    fadedLine = new Cesium.StripeMaterialProperty({
        evenColor: Cesium.Color.YELLOW.withAlpha( 0.15 ),
        oddColor: Cesium.Color.BLACK,
        repeat: 1,
        offset: 0.2,
        orientation: Cesium.StripeOrientation.VERTICAL
    });



    $.ajax({
        url: '/api/satellite/init',
        method: 'GET'
    }).done(function(data){
        satelliteData = data;
        _.map(data, function(satellite){
           addSatellite(satellite.name, satellite.satellites);
        });
    });

    // Tick event
    viewer.clock.onTick.addEventListener(handleTick);

});

function initComponents(){

    var books = [
        { author: 'D. Adams', title: "The Hitchhiker's Guide to the Galaxy", year: 1979, genre: 'Comedy, sci-fi' },
        { author: 'K. Vonnegut', title: "Cat's Cradle", year: 1963, genre: 'Satire, sci-fi' },
        { author: 'M. Mitchell', title: "Gone with the Wind", year: 1936, genre: 'Historical fiction' }
    ];

    dgStatistics = $("#gridStatistics").dxDataGrid({
        dataSource: books
    }).dxDataGrid('instance');
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
        point: point
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

    if(true) {
        var path = new Cesium.PathGraphics();
        path.material = fadedLine;
        path.leadTime = new Cesium.ConstantProperty(0);
        path.trailTime = new Cesium.ConstantProperty(3600 * 24);


        newEntity.path = path;
    }


    // Make a smooth path
    newEntity.position.setInterpolationOptions({
        interpolationDegree : 5,
        interpolationAlgorithm : Cesium.LagrangePolynomialApproximation
    });

    // Add to viewer
    viewer.entities.add(newEntity);
}


var selectedSatellite;
var trackingEntities;

var satelliteOrbConnectionLeft;
var satelliteOrbConnectionRight;
/**
 * Handle tick
 * @param clock
 */
function handleTick(clock){

    // If selected on polyline, do nothing
    if(!_.isUndefined(viewer.selectedEntity) && !_.isUndefined(viewer.selectedEntity.polyline)){
        return;
    }

    // Check for selected entity
    var selectedEntity = viewer.selectedEntity;
    if(_.isUndefined(selectedEntity)){
        selectedSatellite = undefined;

        // Remove all entities from trackingEntities;
        if(!_.isUndefined(trackingEntities)){
            _.map(trackingEntities, function(entity){viewer.entities.remove(entity);})
        }

        if(!_.isUndefined(satelliteOrbConnectionLeft)){
           viewer.entities.remove(satelliteOrbConnectionLeft);
            satelliteOrbConnectionLeft = undefined;
        }

        if(!_.isUndefined(satelliteOrbConnectionRight)){
            viewer.entities.remove(satelliteOrbConnectionRight);
            satelliteOrbConnectionRight = undefined;
        }
        return;
    }

    if(! _.isUndefined(selectedSatellite) && _.isEqual(selectedEntity.id, selectedSatellite.id)){
        // Update connection to adjusted orbs
        orbSatelliteConnect();
        return;
    }

    // Remove all entities from trackingEntities;
    if(!_.isUndefined(trackingEntities)){
        _.map(trackingEntities, function(entity){viewer.entities.remove(entity);})
    }

    selectedSatellite = selectedEntity;
    trackingEntities = new Array();

    // Link it with the others.
    // Get left and right satellite id
    _.map(satelliteData, function(satellite){
        if(_.isEqual(satellite.name, selectedSatellite.name)) {

            // Connect to from current satellite
            var trackEntitya = viewer.entities.add({
                polyline: {
                    followSurface: false,
                    positions: new Cesium.PositionPropertyArray([
                        new Cesium.ReferenceProperty(
                            viewer.entities,
                            selectedEntity.id,
                            [ 'position' ]
                        ),
                        new Cesium.ReferenceProperty(
                            viewer.entities,
                            satellite.leftSatelliteName,
                            [ 'position' ]
                        )
                    ]),
                    material: new Cesium.ColorMaterialProperty(
                        Cesium.Color.RED.withAlpha( 0.75 )
                    )
                }
            });

            trackingEntities.push(trackEntitya);

            var trackEntityb = viewer.entities.add({
                polyline: {
                    followSurface: false,
                    positions: new Cesium.PositionPropertyArray([
                        new Cesium.ReferenceProperty(
                            viewer.entities,
                            selectedEntity.id,
                            [ 'position' ]
                        ),
                        new Cesium.ReferenceProperty(
                            viewer.entities,
                            satellite.rightSatelliteName,
                            [ 'position' ]
                        )
                    ]),
                    material: new Cesium.ColorMaterialProperty(
                        Cesium.Color.RED.withAlpha( 0.75 )
                    )
                }
            });
            trackingEntities.push(trackEntityb);
        }
    });

    orbSatelliteConnect();

}

function orbSatelliteConnect(){

    // Get adjusted orb name;
    var selectedSatelliteData = _.filter(satelliteData, function(satellite){ return satellite.name == selectedSatellite.id;});
    var orbName = selectedSatelliteData[0].orbName;

    // Get left, right orb
    var currentOrbIndex = _.indexOf(orbIndex, orbName);
    var leftOrbIndex = currentOrbIndex == 0 ? orbIndex.length-1 : currentOrbIndex-1;
    var rightOrbIndex = currentOrbIndex == orbIndex.length -1 ? 0 : currentOrbIndex + 1;

    var leftSatellite = getSatelliteBasedOnAugAndDistance(orbIndex[leftOrbIndex]);
    var rightSatellite = getSatelliteBasedOnAugAndDistance(orbIndex[rightOrbIndex]);

    // Remove entities before add new.
    // if(!_.isUndefined(trackingEntitiesOrb)){
    //     _.map(trackingEntitiesOrb, function(entity){viewer.entities.remove(entity);})
    // }

    // Connect current satellite
    // Connect to from current satellite

    if(!_.isUndefined(leftSatellite)) {
        var positionLeft = new Cesium.PositionPropertyArray([
            new Cesium.ReferenceProperty(
                viewer.entities,
                selectedSatellite.id,
                ['position']
            ),
            new Cesium.ReferenceProperty(
                viewer.entities,
                leftSatellite.name,
                ['position']
            )
        ]);

        if(_.isUndefined(satelliteOrbConnectionLeft)){
            satelliteOrbConnectionLeft = viewer.entities.add({
                polyline: {
                    followSurface: false,
                    positions: positionLeft,
                    material: new Cesium.ColorMaterialProperty(
                        Cesium.Color.RED.withAlpha( 0.75 )
                    )
                }
            })
        }else{
            satelliteOrbConnectionLeft.polyline.positions = positionLeft;
        }
    }

    if(!_.isUndefined(rightSatellite)){

        var positionRight = new Cesium.PositionPropertyArray([
            new Cesium.ReferenceProperty(
                viewer.entities,
                selectedSatellite.id,
                [ 'position' ]
            ),
            new Cesium.ReferenceProperty(
                viewer.entities,
                rightSatellite.name,
                [ 'position' ]
            )
        ]);

        if(_.isUndefined(satelliteOrbConnectionRight)){
            satelliteOrbConnectionRight = viewer.entities.add({
                polyline: {
                    followSurface: false,
                    positions: positionRight,
                    material: new Cesium.ColorMaterialProperty(
                        Cesium.Color.RED.withAlpha( 0.75 )
                    )
                }
            })
        }else{
            satelliteOrbConnectionRight.polyline.positions = positionRight;
        }
    }

}

/**
 * calculated based on shortest distance and within angular velocity range.
 * @param orbName
 * @returns {*}
 */
function getSatelliteBasedOnAugAndDistance(orbName){
    // Find all satellites on orb
    var orbSatellites = _.filter(satelliteData, function(satellite) { return satellite.orbName == orbName; });

    // For each satellite, find the distance.
    var satelliteDistance;
    var satelliteChoosen;
    _.map(orbSatellites, function(satellite){
        try {
            distance = Cesium.Cartesian3.distance(selectedSatellite.position.getValue(viewer.clock.currentTime), viewer.entities.getById(satellite.name).position.getValue(viewer.clock.currentTime));
            if (distance <= maxDistance) {
                // Check for angular velocity
                var postSecond = Cesium.JulianDate.clone(viewer.clock.currentTime);
                postSecond = Cesium.JulianDate.addSeconds(postSecond, 1, postSecond);

                var angularVelocity = Cesium.Cartesian3.angleBetween(selectedSatellite.position.getValue(viewer.clock.currentTime), viewer.entities.getById(satellite.name).position.getValue(viewer.clock.currentTime)) -
                    Cesium.Cartesian3.angleBetween(selectedSatellite.position.getValue(postSecond), viewer.entities.getById(satellite.name).position.getValue(postSecond));

                // console.log(satellite.name + ":" + angularVelocity);

                if(Math.abs(angularVelocity) <= maxAngularVelocity) {
                    if (_.isUndefined(satelliteDistance)) {
                        satelliteDistance = distance;
                        satelliteChoosen = satellite;
                    } else {
                        if (distance < satelliteDistance) {
                            satelliteDistance = distance;
                            satelliteChoosen = satellite;
                        }
                    }
                }
            }
        }catch(err){
            // console.error(err.message);
        }
    });

    // ang = Cesium.Cartesian3.angleBetween(selectedSatellite.position.getValue(viewer.clock.currentTime), viewer.entities.getById(satelliteChoosen.name).position.getValue(viewer.clock.currentTime));
    // console.log(satelliteChoosen.name + " : " + ang);

    return satelliteChoosen;
}