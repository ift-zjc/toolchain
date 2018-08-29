
var viewer;
var fileUploader;
var popupFileUploader;
var progressBar;
var toolbar;
var fadedLine;

var dsSatellites;
var treeSatellites;
var dsSatelliteStatus;
var dxSatelliteStatus;

var tickTriggerGap = 10;        //in second
var currentTick;
$(function(){

    popupFileUploader = $("#popupFileUploader").dxPopup({
        height: '400px',
        width: '600px',
        deferRendering: false,
        title: 'Upload TLE and JSON Configuration file'
    }).dxPopup("instance");

    // Bing map key
    // Cesium.BingMapsApi.defaultKey = 'Ak8mO9f0VpoByuNwmMcVvFka1GCZ3Bh8VrpqNLqGtIgsuUYjTrJdw7kDZwAwlC7E';
    // terrainProvider = new Cesium.CesiumTerrainProvider({
    //     url : 'https://assets.agi.com/stk-terrain/world',
    //     requestVertexNormals : true
    // });
    viewer = new Cesium.Viewer('cesiumContainer', {
        terrainProvider : Cesium.createWorldTerrain(),
        baseLayerPicker : false,
        shadows: true
    });

    fadedLine = new Cesium.StripeMaterialProperty({
        evenColor: Cesium.Color.YELLOW.withAlpha( 0.05 ),
        oddColor: Cesium.Color.BLACK,
        repeat: 1,
        offset: 0.2,
        orientation: Cesium.StripeOrientation.VERTICAL
    });

    /**
     * Events
     * @type {jQuery}
     */
    viewer.selectedEntityChanged.addEventListener(handleSelectedEntityChangedEvent);
    viewer.clock.onTick.addEventListener(handleTick);


    // Init toolbar
    $('#satelliteToolbar').dxToolbar({
        items: [{
            location: 'before',
            widget: 'dxButton',
            options: {
                text: 'Add Satellite',
                onClick: function (){

                }
            }
        },{
            location: 'before',
            widget: 'dxButton',
            options: {
                text: 'Remove Satellite',
                onClick: function (){

                }
            }
        }]
    });

    toolbar = $('#toolbar').dxToolbar({
        items: [{
            location: 'center',
            locateInMenu: 'never',
            template: function() {
                return $('<div class="toolbar-label"><b>Tool Chain Dashboard</b></div>');
            }
        },{
            location: 'before',
            widget: 'dxButton',
            options: {
                text: 'upload files',
                icon: 'upload',
                onClick: function(){
                    // Show popup
                    DevExpress.ui.notify('File uploaded');
                    popupFileUploader.show();
                }
            }
        },{
            location: 'before',
            widget: 'dxButton',
            options: {
                text: 'start simulation',
                icon: 'runner',
                disabled: false,
                onClick: function(){
                    $("#progressBar").removeClass("complete");
                    progressBar.option('statusFormat', function(data){return 'Initial Simulator ... ';});
                    $('#progressBar').show();

                //    Start ajax call
                    $.ajax({
                        type: "POST",
                        url: "/api/simulation/start",
                        cache: false,
                        success: function(data){
                            // Remove all entities.
                            viewer.entities.removeAll();
                            // Add entities.
                            _.map(data.satelliteCollections, function(satellite){
                               addSatellite(viewer, satellite);
                            });

                            // Update treeviewer
                            treeSatellites.option('items', data.satelliteItems);
                            // treeSatellites = $('#satalliteTree').dxTreeView({
                            //     items: data.satelliteItems,
                            //     dataStructure: 'plain',
                            //     height: $('#cesiumContainer').height(),
                            //     parentIdExpr: "categoryId",
                            //     keyExpr: 'id',
                            //     displayExpr: 'name',
                            //     searchEnabled: true,
                            //     onItemClick: function (e) {
                            //         var item = e.itemData;
                            //     }
                            //
                            // }).dxTreeView('instance');
                        },
                        error: function (e){

                        }
                    });
                }
            }
        }]
    });

    // File uploader
    fileUploader = $('#fileUploader').dxFileUploader({
        multiple: true,
        accept: "*",
        value: [],
        uploadMode: "useButtons",
        uploadUrl: "/api/file/upload",
        onValueChanged: function(e) {
            var files = e.value;
            if(files.length > 0) {
                $("#selected-files .selected-item").remove();
                $.each(files, function(i, file) {
                    var $selectedItem = $("<div />").addClass("selected-item");
                    $selectedItem.append(
                        $("<span />").html("Name: " + file.name + "<br/>"),
                        $("<span />").html("Size " + file.size + " bytes" + "<br/>"),
                        $("<span />").html("Type " + file.type + "<br/>"),
                        $("<span />").html("Last Modified Date: " + file.lastModifiedDate)
                    );
                    $selectedItem.appendTo($("#selected-files"));
                });
                $("#selected-files").show();
            }
            else
                $("#selected-files").hide();
        }
    }).dxFileUploader('instance');

    progressBar = $('#progressBar').dxProgressBar({
        min: 0,
        max: 100,
        onComplete: function (e){
            e.element.addClass('complete');
            $('#progressBar').hide();
        }
    }).dxProgressBar('instance');

    initSatelliteStatusGrid();
    initSatelliteTree();
    initWebsocket();
    initLayoutEvent();
});

function initSatelliteStatusGrid(){

    dsSatelliteStatus = new DevExpress.data.ArrayStore({
        // data: satelliteDs,
        key: ['name'],
        onLoaded: function () {
            // Event handling commands go here

        },
        onUpdated: function() {
            dxSatelliteStatus.refresh();
        },
        onInserted: function() {
            dxSatelliteStatus.refresh();
        },
        onRemoved: function() {
            dxSatelliteStatus.refresh();
        }
    });

    dxSatelliteStatus = $('#satelliteStatus').dxDataGrid({
        dataSource: dsSatelliteStatus,
        showColumnLines:true,
        showRowLines:true,
        showBorders:true,
        width: 'inherit',
        height: '100%',
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
                dataField: "name",
                caption: "Property",
                width:150,
                alignment: "left"
            },{
                dataField: "value",
                caption: "Value",
                alignment: "left",
                width:200
            }
        ],
        onCellPrepared: function (e) {
            if(e.rowType == 'data'){
                if(e.column.dataField == 'name' || e.column.dataField == 'value'){
                    e.cellElement.addClass('armyGreen');
                }
            }
        }
    }).dxDataGrid('instance');
}

function initSatelliteTree(){

    treeSatellites = $('#satalliteTree').dxTreeView({
        // dataSource: treeSatellites,
        dataStructure: 'plain',
        height: '100%',
        parentIdExpr: "categoryId",
        keyExpr: 'id',
        displayExpr: 'name',
        searchEnabled: true,
        onItemClick: function (e) {
            var item = e.itemData;

            // Change selected entity on cesium viewer.
            viewer.selectedEntity = viewer.entities.getById(item.name);
        }

    }).dxTreeView('instance');
}

/**
 * Web socket initial to /simulation-websocket
 */
function initWebsocket(){
    var socket = new SockJS('/simulation-websocket');
    var stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame){
        stompClient.subscribe('/topic/simulation/start', function (e){
            var data = JSON.parse(e.body);
            progressBar.option('value', data.percentage);
            progressBar.option('statusFormat', function(event){return data.message + ": " + data.percentage + "%";});
        });
    })
}

/**
 * Add satellite data to cesium visualization
 * @param satelliteName
 * @param satelliteData
 */
function addSatellite(viewer, satellite){
    var entityTime = Cesium.JulianDate.clone(viewer.clock.startTime, entityTime);

    // Create new entity
    var newEntity = new Cesium.Entity({
        id: satellite.name,
        name: satellite.name,
        label: new Cesium.LabelGraphics({
            text: satellite.name,
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
    _.each(satellite.satellites, function(data){
        // Add point as sample
        positions.addSample(data.julianDate,
            Cesium.Cartesian3.fromArray(data.cartesian3));

        // // 60" for each record
        // Cesium.JulianDate.addSeconds(entityTime, 60, entityTime);
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

function initLayoutEvent(){
    // Sync table height with viewer height.
    // $(window).on('resize', function(){
    //     dxSatelliteStatus.option('height', $('#cesiumContainer').height());
    //     treeSatellites.option('height', $('#cesiumContainer').height());
    // });
}

function handleTick(clock){

    if((! _.isUndefined(viewer.selectedEntity)) && (_.isUndefined(currentTick) ||
        clock.currentTime.dayNumber != currentTick.dayNumber ||
        Math.abs(clock.currentTime.secondsOfDay-currentTick.secondsOfDay) > tickTriggerGap)){
        // Update table
        // Calling orekit for detailed data
        var data = {'satelliteName': viewer.selectedEntity.name, 'time': clock.currentTime.toString()};
        currentTick = clock.currentTime;
        $.ajax({
            method: 'POST',
            url: '/api/satellite/currentstatus',
            cache: true,
            data: JSON.stringify(data),
            contentType: "application/json",
            success: function (data){

                _.each(data, function(item){
                    dsSatelliteStatus.update({name: item.name}, {
                        name: item.name,
                        value: item.value
                    })
                })

            },
            error: function (data){

            }
        })
    }

}


function handleSelectedEntityChangedEvent(event){

    // unselected entity.
    if(_.isUndefined(event)) return;

    var selectedSatelliteName = event.name;
    // Calling orekit for detailed data
    var data = {'satelliteName': selectedSatelliteName, 'time': viewer.clock.currentTime.toString()};
    $.ajax({
        method: 'POST',
        url: '/api/satellite/currentstatus',
        cache: true,
        data: JSON.stringify(data),
        contentType: "application/json",
        success: function (data){

            // Check datasource for inserting or update
            dsSatelliteStatus.totalCount().done(function (count){
                if(count == 0){
                    _.each(data, function(item){
                        dsSatelliteStatus.insert({
                            name: item.name,
                            value: item.value
                        })
                    })
                }else{
                    _.each(data, function(item){
                        dsSatelliteStatus.update({name: item.name}, {
                            name: item.name,
                            value: item.value
                        })
                    })
                }
            });

        },
        error: function (data){

        }
    })
}