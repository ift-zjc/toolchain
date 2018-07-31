
var terrainProvider;
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


var simulatorStart;
var simulatorEnd;
var simulatorDelta;
var simulator;


var satellitesArray = ["BIIF-1", "BIIF-2", "BIIF-3", "BIIF-4", "BIIF-5", "BIIF-6", "BIIF-7", "BIIF-8", "BIIF-9", "BIIF-10", "BIIF-11", "BIIF-12",
    "BIIR-1", "BIIR-2", "BIIR-3", "BIIR-4", "BIIR-5", "BIIR-6", "BIIR-7", "BIIR-8", "BIIR-9", "BIIR-10", "BIIR-11", "BIIR-12", "BIIR-13",
    "BIIRM-1", "BIIRM-2", "BIIRM-3", "BIIRM-4", "BIIRM-5", "BIIRM-6", "BIIRM-7", "BIIRM-8"];

var applicationArray = [{
    name: "Application 1", source: "192.168.1.134:8234", sourceType: "Satellite", dest: "192.168.4.214:4623", destType: "Satellite", protocol: "TCP/IP"
},{
    name: "Application 2", source: "192.168.1.194:3564", sourceType: "Satellite", dest: "192.168.4.134:9302", destType: "Ground Station", protocol: "UDP"
}];


// Traffic model
var trafficModelData;

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
    terrainProvider = new Cesium.CesiumTerrainProvider({
        url : 'https://assets.agi.com/stk-terrain/world',
        requestVertexNormals : true
    });
    viewer = new Cesium.Viewer('cesiumContainer', {
        terrainProvider : terrainProvider,
        baseLayerPicker : false,
        shadows: true
    });

    // viewer.clock.startTime = Cesium.JulianDate.fromDate(new Date());
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
           addSatellite(viewer, satellite.name, satellite.satellites);
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


    /**
     * Start Simulation
     */

    // Check for browser support
    simulatorStart = $("#dtSimulateStart").dxDateBox({
        type: "datetime",
        min: viewer.clock.startTime,
        max: viewer.clock.stopTime,
        dateSerializationFormat: "yyyy-MM-ddTHH:mm:ssZ"
    }).dxDateBox("instance");

    simulatorEnd = $("#dtSimulateEnd").dxDateBox({
        type: "datetime",
        min: viewer.clock.startTime,
        max: viewer.clock.stopTime,
        dateSerializationFormat: "yyyy-MM-ddTHH:mm:ssZ"
    }).dxDateBox("instance");

    simulatorDelta = $("#deltaSimulate").dxNumberBox({
        placeholder: 'Enter time delta',
        min: 1,
        max: 720
    }).dxNumberBox("instance");

    $('#popupSimulating').dxPopup({

        showTitle: false,
        width: 600,
        height: 'auto',
        closeOnOutsideClick: true,
        contentTemplate: function(e){
            e.append('<p>Simulating in progress ... ...</p>');
        }
    });

    var appSelector = $('#appSelection').dxSelectBox({

        placeholder: "Select an application...",
        dataSource: new DevExpress.data.DataSource({
            store: new DevExpress.data.ArrayStore({ data: applicationArray})
        }),
        valueExpr: 'name',
        displayExpr: 'name',
        onValueChanged: function(e){
            // Get value

            var selectedAppArray = DevExpress.data.query(applicationArray).filter('name', '=', e.value).toArray();
            var appValue = selectedAppArray[0];
            // display
            $('#appSource').html(appValue.source);
            $('#appDest').html(appValue.dest);
            $('#appSourceType').html(appValue.sourceType);
            $('#appDestType').html(appValue.destType);
            $('#protocol').html(appValue.protocol);
        }
    });

    var btnGenerate = $("#dtBtnGenerate").dxButton({
        text: "Start simulator",
        onClick: function () {
            console.log("Simulator start ....");
            // simulator = new Worker("/js/simulator.js");

            // setTimeout(simulator, 100, Cesium.JulianDate.fromDate(new Date(simulatorStart.option("value"))),
            //     Cesium.JulianDate.fromDate(new Date(simulatorEnd.option("value"))),
            //     simulatorDelta.option("value"));
            // simulator.onmessage = function (event){
            //     console.log(event.data);
            // }

            // var simulatorWindow = window.open("/simulate", '_blank');

            $("#popupSimulating").dxPopup("instance").show();

            /**
             * Calling backend
             */
            var offsetStart = Math.abs(Cesium.JulianDate.secondsDifference(Cesium.JulianDate.fromDate(new Date(simulatorStart.option("value"))),viewer.clock.startTime));
            var offsetEnd = Math.abs(Cesium.JulianDate.secondsDifference(Cesium.JulianDate.fromDate(new Date(simulatorEnd.option("value"))),viewer.clock.startTime));
            var delta = simulatorDelta.option("value");
            var data = {'offsetStart': offsetStart, 'offsetEnd': offsetEnd, 'delta': delta, 'appData': dsApplications.items()};
            var simulatorStartDateTime = viewer.clock.startTime;
            var stepsInseconds = offsetEnd - offsetStart;
            $.ajax({
                url: '/simulate',
                type: 'POST',
                data: JSON.stringify(data),
                contentType: "application/json",
                success: function(data){

                    $("#popupSimulating").dxPopup("instance").hide();
                    // Load to data grid.
                    var dgSimulationResult = $('#dgSimulateResult').dxDataGrid({
                        dataSource: data.simulateResultDtos,
                        searchPanel: {
                            visible: false
                        },
                        filterRow: {
                            visible: true
                        },
                        paging:{
                            pageSize: 6
                        },
                        columns: [
                            {
                                dataField: "satelliteNameSource",
                                caption: "Satellite Source",
                                lookup: {
                                    dataSource: satellitesArray
                                }
                            },{
                                dataField: "satelliteNameDest",
                                caption: "Satellite Destination",
                                lookup: {
                                    dataSource: satellitesArray
                                }
                            },{
                                dataField: "offsetMillionSecond",
                                caption: "Simulation point (local time)",
                                cellTemplate: function (container, options){
                                    var currentTime = Cesium.JulianDate.clone(viewer.clock.startTime);
                                    currentTime = Cesium.JulianDate.addSeconds(simulatorStartDateTime, options.value/1000, currentTime);
                                   $('<span>'+ moment(Cesium.JulianDate.toDate(currentTime).toISOString()).format('MMMM Do YYYY, h:mm:ss a') +'</span>').appendTo(container);
                                }
                            }, "connected",
                            {
                                dataField: "delay",
                                caption: "Delay",
                                cellTemplate: function (container, options){
                                    var delayText = options.value == 0 ? "N/A" : options.value;
                                    $('<span>' + delayText + '</span>').appendTo(container);
                                }

                            }, {
                                dataField: "angelVelocity",
                                caption: "Angluar Velocity"
                            },
                            {
                                dataField: "trafficLoading",
                                caption: "Traffic Loading (MB)",
                                cellTemplate: function (container, options){
                                    var delayText = options.value == 0 ? "N/A" : options.value.toFixed(1);
                                    $('<span>' + delayText + '</span>').appendTo(container);
                                }
                            }],
                        onRowPrepared: function (info){
                            if(info.rowType == "data"){
                                info.rowElement.removeClass("dx-row-alt").addClass("bg-dark text-white");
                            }
                        }
                    });

                    // Load to chart
                    var chartSimulateResult = $('#chartSimulateResult').dxChart({
                        palette: "violet",
                        dataSource: DevExpress.data.query(data.simulateResultDtos)
                            .filter("satelliteNameSource", "=", "BIIF-1")
                            .filter("satelliteNameDest", "=", "BIIF-2").toArray(),
                        commonSeriesSettings: {
                            type: "spline",
                            argumentField: "offsetMillionSecond"
                        },
                        tooltip: {
                            enabled: true,
                            shared: true
                        },
                        argumentAxis: {
                            argumentType: 'number',
                            label: {
                                customizeText: function(obj) {
                                    var currentTime = Cesium.JulianDate.clone(viewer.clock.startTime);
                                    currentTime = Cesium.JulianDate.addSeconds(simulatorStartDateTime, obj.value / 1000, currentTime);
                                    return moment(Cesium.JulianDate.toDate(currentTime).toISOString()).format('MMMM Do YYYY, h:mm:ss a');
                                },
                                font:{
                                    color: '#57962B'
                                }
                            }
                        },
                        series: [
                            {valueField: "delay", name: "Propagation Delay", color: "#E03A16", position: "left", axis: "delay"},
                            {valueField: "angelVelocity", name: "Angular Velocity", color: "#57962B", position: "right", axis: "av"}
                        ],
                        valueAxis:[{
                            name: "delay",
                            position: "left",
                            grid: {
                                visible: true
                            },
                            title: {text: "Propagation Delay (second)"}
                        },{
                            name: "av",
                            position: "right",
                            grid: {
                                visible: true
                            },
                            title: {
                                text: "Angular Velocity (rad/s)"
                            }
                        }],
                        size: {
                            height: 750
                        },
                        title: {
                            text: "END-TO-END DELAY / ANGULAR VELOCITY OF CROSSLINKS",
                            subtitle: {
                                text: "BIIF-1 vs BIIF-2",
                                font: {
                                    color: "#57962B",
                                    size: 20,
                                    weight: 800
                                }
                            },
                            font:{
                                color: "white"
                            }
                        }


                    }).dxChart("instance");

                    var selectFrom = $("#selectFrom").dxSelectBox({
                        dataSource: satellitesArray,
                        value: satellitesArray[0]
                    }).dxSelectBox("instance");

                    var selectTo = $("#selectTo").dxSelectBox({
                        dataSource: satellitesArray,
                        value: satellitesArray[1]
                    }).dxSelectBox("instance");

                    $("#btnRefresh").dxButton({
                        text: "Refresh Chart",
                        onClick: function (e) {
                            var refreshedDs = DevExpress.data.query(data.simulateResultDtos)
                                    .filter(function (dataItem){
                                        return (dataItem.satelliteNameSource == selectFrom.option("value")) ||
                                            (dataItem.satelliteNameSource == selectTo.option("value"));
                                    })
                                    .filter(function (dataItem) {
                                        return (dataItem.satelliteNameDest == selectFrom.option("value")) ||
                                        (dataItem.satelliteNameDest == selectTo.option("value"));
                                    }).toArray();

                            chartSimulateResult.option("dataSource", refreshedDs);
                            chartSimulateResult.option("title.subtitle.text", selectFrom.option("value") + " vs " + selectTo.option("value"));
                        }
                    });

                    // Show application div
                    $('#appDiv').show();

                    /**
                     * Application traffic chart
                     */

                    // Construct datasource
                    var dsTrafficChart="[";
                    for (var i = 0; i < stepsInseconds; i++){
                        // JSON string
                        var jsonStr = '{"offset": ' + i;

                        // Loop the data to find traffic @ i
                        _.map(data.applicationTraffic, function (appTraffic){
                           var name = appTraffic.appName;
                           var trafficVolumnItem = _.find(appTraffic.applicationTrafficDataList, function(trafficDataItem){
                               return trafficDataItem.offsetMillionSecond == i;
                           });
                           var trafficVolumn = 0;
                           if(! _.isUndefined(trafficVolumnItem)){
                               trafficVolumn = trafficVolumnItem.trafficVolumn
                           }
                           jsonStr += ', "' + name + '": ' + trafficVolumn;
                        });

                        jsonStr += "},";

                        dsTrafficChart += jsonStr;
                    }
                    // Remove last ,
                    dsTrafficChart = dsTrafficChart.slice(0, -1) + ']';

                    // Convert to JSON
                    dsTrafficChart = JSON.parse(dsTrafficChart);

                    // Add serial
                    seriesTraffic = "[";
                    _.map(data.applicationTraffic, function (appTrafficS){
                        var name = appTrafficS.appName;
                        seriesTraffic += '{"valueField": "' + name + '", "name": "' + name + '"},'
                    });

                    seriesTraffic = seriesTraffic.slice(0, -1) + "]";
                    seriesTraffic = JSON.parse(seriesTraffic);

                    chartApplications = $("#chartApps").dxChart({
                        palette: "soft",
                        dataSource: dsTrafficChart,
                        commonSeriesSettings: {
                            barPadding: 0.2,
                            argumentField: "offset",
                            type: "bar"
                        },
                        legend: {
                            verticalAlignment: "bottom",
                            horizontalAlignment: "center"
                        },
                        title: {
                            text: "Applications Traffic",
                            font: {
                                color: "white"
                            }
                        },
                        series: seriesTraffic,
                        argumentAxis: {
                            argumentType: 'number',
                            label: {
                                customizeText: function(obj) {
                                    var currentTime = Cesium.JulianDate.clone(viewer.clock.startTime);
                                    currentTime = Cesium.JulianDate.addSeconds(simulatorStartDateTime, 1, currentTime);
                                    return moment(Cesium.JulianDate.toDate(currentTime).toISOString()).format('MMMM Do YYYY, h:mm:ss a');
                                },
                                font:{
                                    color: '#57962B'
                                }
                            }
                        }
                    })
                }

            })
        }
    });

    initTrafficModule();

});

var applicationsData;
var dgApplications;
var dsApplications;
var protocols = [{name: "TCP", value: "TCP"}, {name: "UDP", value: "UDP"}];
var tmDatasource;
var chartApplications;
function initTrafficModule(){

    tmDatasource = new DevExpress.data.CustomStore({
        load: function (loadOptions){
            var deferred = $.Deferred();

            $.ajax({
                url: "/api/tmlist",
                type: "POST",
                success: function(result){
                    deferred.resolve(result.items, {totalCount: result.totalCount});
                },
                error: function(){
                    deferred.reject("TrafficeModel Loading Error");
                },
                timeout: 5000
            });

            return deferred.promise();
        },

        byKey: function(key){
            var d = new $.Deferred();
            $.post("/api/tm/"+key)
                .done(function (result){
                d.resolve(result)
            });

            return d.promise();
        }
    });

    applicationsData = [{
        id: "abc", name: "File transfer", source: "BIIF-1", dest: "BIIF-2", protocol: "TCP",
        tm: "TM1", startOffset: "23", endOffset: "540"
    }];

    dsApplications = new DevExpress.data.DataSource({
        store: {
            type: "local",
            name: "AppDataLocalStore",
            key: "id",
            data: applicationsData
        }
    });




    // Initial datagrid
    dgApplications = $('#dgApps').dxDataGrid({
        dataSource: dsApplications,
        noDataText: "No application data ...",
        editing: {
            allowUpdating: true,
            allowAdding: true,
            allowDeleting: true,
            mode: "popup",
            popup: {
                title: "Application Information",
                showTitle: true,
                width: 800,
                height: 520
            },
            form: {
                items: [
                    {itemType: "group",
                    caption: "Application Information",
                    items: ["name", "tm"]
                    },
                    {
                    itemType: "group",
                    caption: "Crosslink information",
                    items: ["source","dest", "protocol"]
                },{
                    itemType: "group",
                        colSpan: 2,
                        colCount: 2,
                        caption: "Execution Time (in seconds)",
                        items: ["startOffset", "endOffset"]
                    }]
            }
        },
        searchPanel: {
            visible: true
        },
        columns: ["name", {
            caption: "Source & Destination",
            columns: [
                {
                    dataField: "source",
                    caption: "Source",
                    dataType: "String",
                    validationRules: [{type: "required"}],
                    lookup: {dataSource: satellitesArray}
                }, {
                    dataField: "dest",
                    caption: "Destination",
                    dataType: "String",
                    validationRules: [{type: "required"}],
                    lookup: {dataSource: satellitesArray}
                }
            ]
        },{
            dataField: "protocol",
            caption: "Protocol",
            dataType: "String",
            validationRules: [{ type: "required" }],
            lookup: {
                dataSource: protocols,
                valueExpr: "name",
                displayExpr: "name"
            }
        }, {
            dataField: "tm",
            caption: "Traffic Model",
            dataType: "String",
            lookup: {
                dataSource: tmDatasource,
                valueExpr: "tmCode",
                displayExpr: "tmName"
            }
        },{
            caption: "Time Range",
            // columns: [{
            //     dataField: "startOffset",
            //     caption: "Start Offset",
            //     dataType: "number"
            // }, {
            //     dataField: "endOffset",
            //     caption: "End Offset",
            //     dataType: "number"
            // }],
            cellTemplate: function (container, options) {
                $('<span>Start at ' + options.data.startOffset + ' seconds to ' + options.data.endOffset  + ' seconds</span>').appendTo(container);
            },
            formItem: {
                visible: false
            }
        },{
                dataField: "startOffset",
                caption: "Start Offset",
                dataType: "number",
                visible: false
            },
            {
                dataField: "endOffset",
                caption: "End Offset",
                dataType: "number",
                visible: false
        }],
        onRowPrepared: function (info){
            if(info.rowType == "data"){
                info.rowElement.removeClass("dx-row-alt").addClass("bg-dark text-white");
            }
        }
    }).dxDataGrid("instance");

}

function simulator(start, end, count){
    var _viewer = new Cesium.Viewer('_cesiumContainer', {
        terrainProvider : terrainProvider,
        baseLayerPicker : false,
        shadows: true
    });

    _viewer.clock.clockRange = Cesium.ClockRange.LOOP_STOP; //Loop at the end
    _viewer.clock.multiplier = 60;

    /**
     * Init all satellites.
     */
    $.ajax({
        url: '/api/satellite/init',
        method: 'GET'
    }).done(function(data){
        satelliteData =  _.sortBy(data, function(satellite) {return satellite.name;});;
        _.map(data, function(satellite){
            addSatellite(_viewer, satellite.name, satellite.satellites);
        });
        calculateStep(_viewer, start, end, count);
    });
}

/**
 * Simulate
 * @param viewer
 * @param startTime
 * @param endTime
 * @param count
 */
function calculateStep(viewer, startTime, endTime, count){

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

                    viewer.clock.currentTime = Cesium.JulianDate.addSeconds(viewer.clock.currentTime,
                        count,
                        viewer.clock.currentTime);
                }
            }catch(ex){
                console.error(ex.message);
            }
        })
    });
}

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
function addSatellite(viewer, satelliteName, satelliteData){
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