
var viewer;
var point;
var fadedLine;
var satelliteData;
var orbIndex;
var maxDistance = 51591*1000;       //Meter
var maxAngularVelocity = 0.000087;
var maxGroundSatelliteDistance = 21100*1000;    // Meter

var dgStatistics;

var afMonitorStations;
$(function(){

    initComponents();
    orbIndex = ['A','B','C','D','E','F'];

    // Init ground stations
    afMonitorStations = [
        {gsId: 'gsAF1', cartesian3: [-5.4631*1000000, -2.4802*1000000, 2.1570*1000000], name: "Hawaii"},
        {gsId: 'gsAF2', cartesian3: [0.9189*1000000, -5.5343*1000000, 3.0242*1000000], name: "Cape Canaveral"},
        {gsId: 'gsAF3', cartesian3: [6.1200*1000000, -1.5663*1000000, -0.8759*1000000], name: "Ascension"},
        {gsId: 'gsAF4', cartesian3: [1.9105*1000000, 6.0311*1000000, -0.8072*1000000], name: "Diego Garcia"},
        {gsId: 'gsAF5', cartesian3: [-6.1610*1000000, 1.3396*1000000, 0.9602*1000000], name: "Kwajalein"},

        {gsId: 'gsMaster1', cartesian3: [-1.2482*1000000, -4.8176*1000000, 3.9758*1000000], name: "Schriever AFB"}
        ];

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
        evenColor: Cesium.Color.YELLOW.withAlpha( 0.05 ),
        oddColor: Cesium.Color.BLACK,
        repeat: 1,
        offset: 0.2,
        orientation: Cesium.StripeOrientation.VERTICAL
    });



    $.ajax({
        url: '/api/satellite/init',
        method: 'GET'
    }).done(function(data){
        satelliteData =  _.sortBy(data, function(satellite) {return satellite.name;});;
        _.map(data, function(satellite){
           addSatellite(satellite.name, satellite.satellites);
        });
        // Init
        _.each(satelliteData, function(satellite){
            satelliteStore.insert({
                satellite: satellite.name,
                LOS: 'N/A',
                AV: 'N/A',
                distance: 'N/A'
            })
        })
    });

    // Tick event
    viewer.clock.onTick.addEventListener(handleTick);

    setInterval(function(){
        updateSatelliteTable();
        }, 800);


    // init ground stations
    _.each(afMonitorStations,function(afMonitor){
        addGroundStation(afMonitor.gsId, Cesium.Cartesian3.fromArray(afMonitor.cartesian3), afMonitor.name);
    });

    // setResetInterval(true);
});

var timer1;
var timer2;
var trackingVisible;
function setResetInterval(bool){
    trackingVisible = bool;

    trackingSwitch.option('text', trackingVisible ? 'Time/Geolocation Synchronization ON' : 'Time/Geolocation Synchronization OFF');
    if(bool){
       timer1 = setInterval(function(){
            gsSatelliteConnect();
        }, 800);

        timer2 = setInterval(function(){
            broadcasting();
        }, 2000);
    }else{
        // remove all from array
        if(!_.isUndefined(gsvssatellitearray)){
            _.each(gsvssatellitearray, function (entityId) {
                viewer.entities.removeById(entityId);
            });
        }

        // Reset
        gsvssatellitearray = new Array();
        clearInterval(timer1);
        timer1 = null;

        if(!_.isUndefined(broadcasteRoutes)){
            _.each(broadcasteRoutes, function(routeId){
                viewer.entities.removeById(routeId);
            })
        }

        if(!_.isUndefined(gsvssatelliteLabelArray)){
            _.each(gsvssatelliteLabelArray, function (entityId) {
                viewer.entities.removeById(entityId);
            });
        }

        if(!_.isUndefined(broadCastUpLinks)){
            _.each(broadCastUpLinks, function(routeId){
                viewer.entities.removeById(routeId);
            })
        }

        broadcasteRoutes = new Array();
        broadcasteEntities = new Array();
        broadcastCenters = new Array();
        gsvssatelliteLabelArray = new Array();
        broadCastUpLinks = new Array();
        clearInterval(timer2);
        timer2 = null;
    }
}

var satelliteStore;
var trackingSwitch;
function initComponents(){

    // satelliteDs = [
    //
    // ];

    satelliteStore = new DevExpress.data.ArrayStore({
        // data: satelliteDs,
        key: ['satellite'],
        onLoaded: function () {
            // Event handling commands go here

        },
        onUpdated: function() {
            dgStatistics.refresh();
        },
        onInserted: function() {
            dgStatistics.refresh();
        },
        onRemoved: function() {
            dgStatistics.refresh();
        }
    })

    dgStatistics = $("#gridStatistics").dxDataGrid({
        dataSource: satelliteStore,
        showColumnLines: true,
        showRowLines: true,
        showBorders: true,
        paging:{
            pageSize: 100
        },
        pager: {
            showPageSizeSelector: false,
            allowedPageSizes: [100],
            visible: false,
            showInfo: false,
            showNavigationButtons: false
        },
        columns: [
            {
                dataField: "satellite",
                caption: "Satellite ID",
                width: 90,
                alignment: "center"
            }, {
                dataField: "LOS",
                caption: "Line of Sight",
                alignment: "center",
                width: 100,
                cellTemplate: function(container, options){
                    $('<span>' + (options.value ? 'yes' : 'no') + '</span>').appendTo(container);
                }
            }, {
                dataField: "AV",
                caption: "Angular Velocity (mrad/s)",
                cellTemplate: function(container, options){
                    $('<span>'+options.value*1000 + '</span>').appendTo(container);
                }
            }, {
                dataField: "distance",
                caption: "Distance (km)"
            }],
        onRowClick: function(info){
            // console.log(info.data.satellite);
            viewer.selectedEntity = viewer.entities.getById(info.data.satellite);
        },
        onCellPrepared: function(e){

            // Get selected satellite (data)
            if(e.rowType == 'data'){
                var leftSatelliteId;
                var rightSatelliteId;
                if(!_.isUndefined(selectedSatellite)){
                    var selectedSatelliteData = _.filter(satelliteData, function(sd){ return sd.name == selectedSatellite.id;})
                    if(!_.isUndefined(selectedSatelliteData)){
                        // Get left, right
                        leftSatelliteId = selectedSatelliteData[0].leftSatelliteName;
                        rightSatelliteId = selectedSatelliteData[0].rightSatelliteName;
                    }
                }

                if(!_.isUndefined(leftSatelliteId)){
                    if(e.data.satellite == leftSatelliteId){
                        e.cellElement.addClass('armySelectedRow');
                    }else{

                    }
                }

                if(!_.isUndefined(rightSatelliteId)){
                    if(e.data.satellite == rightSatelliteId){
                        e.cellElement.addClass('armySelectedRow');
                    }else{

                    }
                }
                if(!_.isUndefined(leftSatelliteOrb)){
                    if(e.data.satellite == leftSatelliteOrb.name){
                        e.cellElement.addClass('armySelectedRow');
                    }else{

                    }
                }

                if(!_.isUndefined(rightSatelliteOrb)){
                    if(e.data.satellite == rightSatelliteOrb.name){
                        e.cellElement.addClass('armySelectedRow');
                    }else{

                    }
                }

                if(e.column.dataField == 'satellite'){
                    e.cellElement.addClass('armyGreen');
                }
                if(e.column.dataField == 'LOS'){
                    if(e.data.LOS) {
                        e.cellElement.addClass('armyGreen');
                    }else{
                        e.cellElement.addClass('armyRed');
                    }
                }
                if(e.column.dataField == 'AV'){
                    if(e.data.AV <= maxAngularVelocity){
                        e.cellElement.addClass('armyGreen');
                    }else{
                        e.cellElement.addClass('armyRed');
                    }
                }
                if(e.column.dataField == 'distance'){
                    if(e.data.distance <= (maxDistance/1000)){
                        e.cellElement.addClass('armyGreen');
                    }else{
                        e.cellElement.addClass('armyRed');
                    }
                }
            }
        }
    }).dxDataGrid('instance');

    // trackingSwitch = $('#trackingSwitch').dxSwitch({
    //     onText: 'Turn on tracking',
    //     offText: 'Turn off tracking',
    //     width: 120,
    //     onOptionChanged: function(info){
    //         if(info.value){
    //             setResetInterval(true);
    //         }else{
    //             setResetInterval(false);
    //         }
    //     }
    // }).dxSwitch('instance');

    trackingSwitch = $('#trackingSwitch').dxButton({
        text: "Time/Geolocation Synchronization OFF",
        template: function (e){
            return $("<strong />").text(e.text)
                .css("color", "green");
        },
        height: "40px",
        onClick: function(e){
            if(_.isUndefined(trackingVisible)){
                setResetInterval(true);
            }else{
                trackingVisible = !trackingVisible;
                setResetInterval(trackingVisible);
            }
        }
    }).dxButton('instance');

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

    // Ignore ground station selection.
    if(!_.isUndefined(viewer.selectedEntity) && _.startsWith(viewer.selectedEntity.id, 'gs')){
        return;
    }

    // Check for label click
    if(!_.isUndefined(viewer.selectedEntity) && (_.indexOf(gsvssatelliteLabelArray, viewer.selectedEntity.id) > -1 || _.indexOf(broadCastUpLinks, viewer.selectedEntity.id) > -1)){
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

        if(!_.isUndefined(leftSatelliteOrb)){
            leftSatelliteOrb = undefined;
        }

        if(!_.isUndefined(rightSatelliteOrb)){
            rightSatelliteOrb = undefined;
        }

        // Clean all
        $('.selectedSatellite').html('[N/A]');
        $('#leftSatellite').html('[N/A]');
        $('#rightSatellite').html('[N/A]');
        $('#leftOrbSatellite').html('[N/A]');
        $('#rightOrbSatellite').html('[N/A]');

        return;
    }

    if(! _.isUndefined(selectedSatellite) && _.isEqual(selectedEntity.id, selectedSatellite.id)){
        // Update connection to adjusted orbs
        orbSatelliteConnect();
        // updateSatelliteTable();
        return;
    }

    // Remove all entities from trackingEntities;
    if(!_.isUndefined(trackingEntities)){
        _.map(trackingEntities, function(entity){viewer.entities.remove(entity);})
    }

    $('.selectedSatellite').html('['+viewer.selectedEntity.id+']');

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
            $('#leftSatellite').html('['+satellite.leftSatelliteName+']');

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
            $('#rightSatellite').html('['+satellite.rightSatelliteName+']');
        }
    });

    orbSatelliteConnect();

}

/**
 * Add ground station
 * @param gsId
 * @param cartesian3
 */
function addGroundStation(gsId, cartesian3, name){
    viewer.entities.add({
        id: gsId,
        position : cartesian3,
        billboard:{
            image: "/Image/groundstation.png",
            show: true
        },
        label: new Cesium.LabelGraphics({
            text: name,
            scale: 0.5,
            pixelOffset: new Cesium.Cartesian2(35,-25),
            scaleByDistance: new Cesium.NearFarScalar(20000000, 1.2, 100000000, 0.5)
        })

    });
}
var leftSatelliteOrb;
var rightSatelliteOrb;
function orbSatelliteConnect(){

    // Get adjusted orb name;
    var selectedSatelliteData = _.filter(satelliteData, function(satellite){ return satellite.name == selectedSatellite.id;});
    var orbName = selectedSatelliteData[0].orbName;

    // Get left, right orb
    var currentOrbIndex = _.indexOf(orbIndex, orbName);
    var leftOrbIndex = currentOrbIndex == 0 ? orbIndex.length-1 : currentOrbIndex-1;
    var rightOrbIndex = currentOrbIndex == orbIndex.length -1 ? 0 : currentOrbIndex + 1;

    var leftSatellite = getSatelliteBasedOnAugAndDistance(orbIndex[leftOrbIndex], selectedSatellite);
    var rightSatellite = getSatelliteBasedOnAugAndDistance(orbIndex[rightOrbIndex], selectedSatellite);

    leftSatelliteOrb = leftSatellite;
    rightSatelliteOrb = rightSatellite;

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

        $('#leftOrbSatellite').html('['+leftSatellite.name+']');
    }else{
        viewer.entities.remove(satelliteOrbConnectionLeft);
        satelliteOrbConnectionLeft = undefined;
        $('#leftOrbSatellite').html('[N/A]');
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

        $('#rightOrbSatellite').html('['+rightSatellite.name+']');
    }else{
        viewer.entities.remove(satelliteOrbConnectionRight);
        satelliteOrbConnectionRight = undefined;

        $('#rightOrbSatellite').html('[N/A]');
    }

}

var gsvssatellitearray;
var gsvssatelliteLabelArray;
function  gsSatelliteConnect(){

    // Find entity gsAF2
    var gsEntity = viewer.entities.getById('gsAF2');

    // remove all from array
    if(!_.isUndefined(gsvssatellitearray)){
        _.each(gsvssatellitearray, function (entityId) {
            viewer.entities.removeById(entityId);
        });
    }
    if(!_.isUndefined(gsvssatelliteLabelArray)){
        _.each(gsvssatelliteLabelArray, function (entityId) {
            viewer.entities.removeById(entityId);
        });
    }

    // Reset
    gsvssatellitearray = new Array();
    gsvssatelliteLabelArray = new Array();

    // Get distance between gsEntity and other satellites
    _.each(satelliteData, function (satellite) {
        var distance = Cesium.Cartesian3.distance(gsEntity.position.getValue(viewer.clock.currentTime), viewer.entities.getById(satellite.name).position.getValue(viewer.clock.currentTime));
        if(distance<maxGroundSatelliteDistance){
            // Connect
                var pair = new Cesium.PositionPropertyArray([
                    new Cesium.ReferenceProperty(
                        viewer.entities,
                        gsEntity.id,
                        ['position']
                    ),
                    new Cesium.ReferenceProperty(
                        viewer.entities,
                        satellite.name,
                        ['position']
                    )
                ]);

                viewer.entities.add({
                    id: gsEntity.id + '-' + satellite.name,
                    polyline: {
                        followSurface: false,
                        positions: pair,
                        width: 2,
                        material: new Cesium.ColorMaterialProperty(
                            Cesium.Color.GREEN.withAlpha(0.85)
                        )
                    }
                });

                var cartesian3Gs = gsEntity.position.getValue(viewer.clock.currentTime);
                var cartesian3Satellite = viewer.entities.getById(satellite.name).position.getValue(viewer.clock.currentTime);

                var lableCartesian3 = new Cesium.Cartesian3.fromElements((cartesian3Gs.x+cartesian3Satellite.x)/2, (cartesian3Gs.y+cartesian3Satellite.y)/2, (cartesian3Gs.z+cartesian3Satellite.z)/2);

                var labelEntity = viewer.entities.add({
                    position: lableCartesian3,
                    label: {
                        text: "Downlink DataRate: " + Math.floor((Math.random()*500) + 50) +"Mb/s",
                        scaleByDistance: new Cesium.NearFarScalar(70000000, 0.7, 120000000, 0.3),
                        fillColor : Cesium.Color.GREEN
                    }
                });

                gsvssatelliteLabelArray.push(labelEntity.id);

                gsvssatellitearray.push(gsEntity.id + '-' + satellite.name);
        }
    });
}



function updateSatelliteTable(){

    if(_.isUndefined(selectedSatellite)){

        _.map(satelliteData, function (satellite) {
            satelliteStore.update({satellite: satellite.name}, {
                satellite: satellite.name,
                LOS: 'N/A',
                AV: 'N/A',
                distance: 'N/A'
            })
        });
    }else{
        _.map(satelliteData, function (satellite) {
            var distance = Cesium.Cartesian3.distance(selectedSatellite.position.getValue(viewer.clock.currentTime), viewer.entities.getById(satellite.name).position.getValue(viewer.clock.currentTime));
            var los = distance <= maxDistance;

            // Check for angular velocity
            var postSecond = Cesium.JulianDate.clone(viewer.clock.currentTime);
            postSecond = Cesium.JulianDate.addSeconds(postSecond, 1, postSecond);
            var angularVelocity = Cesium.Cartesian3.angleBetween(selectedSatellite.position.getValue(viewer.clock.currentTime), viewer.entities.getById(satellite.name).position.getValue(viewer.clock.currentTime)) -
                Cesium.Cartesian3.angleBetween(selectedSatellite.position.getValue(postSecond), viewer.entities.getById(satellite.name).position.getValue(postSecond));

            satelliteStore.update({satellite: satellite.name}, {
                satellite: satellite.name,
                LOS: los,
                AV: Math.abs(angularVelocity),
                distance: distance/1000
            })

        });
    }

}

/**
 * calculated based on shortest distance and within angular velocity range.
 * @param orbName
 * @returns {*}
 */
function getSatelliteBasedOnAugAndDistance(orbName, selectedSatellite){
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

/**
 * Broadcasting from ground station gsAF1
 */

var broadcasteEntities;
var broadcasteRoutes;
var broadcastCenters;
var broadCastUpLinks;
function broadcasting(){

    if(!_.isUndefined(broadcasteRoutes)){
        _.each(broadcasteRoutes, function(routeId){
            viewer.entities.removeById(routeId);
        })
    }

    if(!_.isUndefined(broadCastUpLinks)){
        _.each(broadCastUpLinks, function(routeId){
            viewer.entities.removeById(routeId);
        })
    }

    broadcasteRoutes = new Array();
    broadcasteEntities = new Array();
    broadcastCenters = new Array();
    broadCastUpLinks = new Array();

    var groundStation = viewer.entities.getById('gsAF1');
    var shortestDistance;
    var closestSatelliteId;
    // Get the closest satellite
    _.each(satelliteData, function(satellite){

        var distance = Cesium.Cartesian3.distance(groundStation.position.getValue(viewer.clock.currentTime), viewer.entities.getById(satellite.name).position.getValue(viewer.clock.currentTime));

        if(_.isUndefined(shortestDistance)){
            shortestDistance = distance;
        }else{
            if(distance < shortestDistance){
                closestSatelliteId = satellite.name;
                shortestDistance = distance;
            }
        }
    });

    broadcasteEntities.push(closestSatelliteId);
    // Connect to shortest closest satellite;
    connectEntities('gsAF1', closestSatelliteId);

    var cartesian3Gs = groundStation.position.getValue(viewer.clock.currentTime);
    var cartesian3Satellite = viewer.entities.getById(closestSatelliteId).position.getValue(viewer.clock.currentTime);

    var lableCartesian3 = new Cesium.Cartesian3.fromElements((cartesian3Gs.x+cartesian3Satellite.x)/2, (cartesian3Gs.y+cartesian3Satellite.y)/2, (cartesian3Gs.z+cartesian3Satellite.z)/2);

    var labelEntity = viewer.entities.add({
        position: lableCartesian3,
        label: {
            text: "Uplink DataRate: " + Math.floor((Math.random()*500) + 50) +"Mb/s",
            scaleByDistance: new Cesium.NearFarScalar(70000000, 0.7, 120000000, 0.3),
            fillColor : Cesium.Color.YELLOWGREEN
        }
    });

    broadCastUpLinks.push(labelEntity.id);
    // From closest satellite to nearest satellites;
    broadcastingRecuriting(closestSatelliteId);

}


function broadcastingRecuriting(satelliteId){

    if(_.isUndefined(satelliteId)){return ;}
    // Connect to neibo
    var sData = _.filter(satelliteData, function(satellite) {return satellite.name == satelliteId;});
    if(_.isUndefined(sData) || _.size(sData) == 0){return;}

    broadcastCenters.push(satelliteId);
    // Get neibo
    var leftSatellite = sData[0].leftSatelliteName;
    var rightSatellite = sData[0].rightSatelliteName;

    // Connect to neibo

    if(_.indexOf(broadcasteEntities, leftSatellite) == -1){
        connectEntities(satelliteId, leftSatellite);
        broadcasteEntities.push(leftSatellite);
    }

    if(_.indexOf(broadcasteEntities, rightSatellite) == -1){
        connectEntities(satelliteId, rightSatellite);
        broadcasteEntities.push(rightSatellite);
    }

    var orbName = sData[0].orbName;

    // Get left, right orb
    var currentOrbIndex = _.indexOf(orbIndex, orbName);
    var leftOrbIndex = currentOrbIndex == 0 ? orbIndex.length-1 : currentOrbIndex-1;
    var rightOrbIndex = currentOrbIndex == orbIndex.length -1 ? 0 : currentOrbIndex + 1;

    var leftOrbSatellite = getSatelliteBasedOnAugAndDistance(orbIndex[leftOrbIndex], viewer.entities.getById(satelliteId));
    var rightOrbSatellite = getSatelliteBasedOnAugAndDistance(orbIndex[rightOrbIndex], viewer.entities.getById(satelliteId));
    var leftOrbSatelliteId = leftOrbSatellite.name;
    var rightOrbSatelliteId = rightOrbSatellite.name;

    if(!_.isUndefined(leftOrbSatellite) && _.indexOf(broadcasteEntities, leftOrbSatelliteId) == -1){
        connectEntities(satelliteId, leftOrbSatelliteId);
        broadcasteEntities.push(leftOrbSatelliteId);
    }

    if(!_.isUndefined(rightOrbSatellite) && _.indexOf(broadcasteEntities, rightOrbSatelliteId) == -1){
        connectEntities(satelliteId, rightOrbSatelliteId);
        broadcasteEntities.push(rightOrbSatelliteId);
    }

    // Get shortest distance.
    var currentSatellite = viewer.entities.getById(satelliteId);
    var distanceLeft = Cesium.Cartesian3.distance(currentSatellite.position.getValue(viewer.clock.currentTime), viewer.entities.getById(leftSatellite).position.getValue(viewer.clock.currentTime));
    var distanceRight = Cesium.Cartesian3.distance(currentSatellite.position.getValue(viewer.clock.currentTime), viewer.entities.getById(rightSatellite).position.getValue(viewer.clock.currentTime));
    var distanceOrbLeft = Cesium.Cartesian3.distance(currentSatellite.position.getValue(viewer.clock.currentTime), viewer.entities.getById(leftOrbSatelliteId).position.getValue(viewer.clock.currentTime));
    var distanceOrbRight = Cesium.Cartesian3.distance(currentSatellite.position.getValue(viewer.clock.currentTime), viewer.entities.getById(rightOrbSatelliteId).position.getValue(viewer.clock.currentTime));

    var minimumDistance = Number.MAX_SAFE_INTEGER;
    var minimumSatellite;

    if(_.indexOf(broadcastCenters, leftSatellite)==-1 && distanceLeft < minimumDistance ){
        minimumDistance = distanceLeft;
        minimumSatellite = leftSatellite;
    }

    if(_.indexOf(broadcastCenters, rightSatellite)==-1 && distanceRight < minimumDistance ){
        minimumDistance = distanceRight;
        minimumSatellite = rightSatellite;
    }

    if(_.indexOf(broadcastCenters, leftOrbSatelliteId)==-1 && distanceOrbLeft < minimumDistance){
        minimumDistance = distanceOrbLeft;
        minimumSatellite = leftOrbSatelliteId;
    }

    if(_.indexOf(broadcastCenters, rightOrbSatelliteId)==-1 && distanceOrbRight < minimumDistance){
        minimumDistance = distanceOrbRight;
        minimumSatellite = rightOrbSatelliteId;
    }

    // Loop only minimumSatellite still present
    if(!_.isUndefined(minimumSatellite)) {
        broadcastingRecuriting(minimumSatellite);
    }

}


function connectEntities(leftEntity, rightEntity){

    // Connect entities pair;
    var pair = new Cesium.PositionPropertyArray([
        new Cesium.ReferenceProperty(
            viewer.entities,
            leftEntity,
            ['position']
        ),
        new Cesium.ReferenceProperty(
            viewer.entities,
            rightEntity,
            ['position']
        )
    ]);

    var route;
    if(leftEntity == "gsAF1"){
        route = viewer.entities.add({
            polyline: {
                followSurface: false,
                positions: pair,
                width: 2,
                material: new Cesium.ColorMaterialProperty(
                    Cesium.Color.BLUE.withAlpha(0.55)
                )
            }
        });
    }else{
        route = viewer.entities.add({
            polyline: {
                followSurface: false,
                positions: pair,
                width: 2,
                material: new Cesium.ColorMaterialProperty(
                    Cesium.Color.DARKGOLDENROD.withAlpha(0.35)
                )
            }
        });
    }


    broadcasteRoutes.push(route.id);

    return route;
}