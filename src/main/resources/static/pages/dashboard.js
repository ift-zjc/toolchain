
var viewer;
var fileUploader;
var popupFileUploader;
var progressBar;
var toolbar;
var fadedLine;
$(function(){

    popupFileUploader = $("#popupFileUploader").dxPopup({
        height: '400px',
        width: '600px',
        deferRendering: false,
        title: 'Upload TLE and JSON Configuration file'
    }).dxPopup("instance");

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

    fadedLine = new Cesium.StripeMaterialProperty({
        evenColor: Cesium.Color.YELLOW.withAlpha( 0.05 ),
        oddColor: Cesium.Color.BLACK,
        repeat: 1,
        offset: 0.2,
        orientation: Cesium.StripeOrientation.VERTICAL
    });

    // Init toolbar
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
                            // Data will be send through websocket.
                            console.log(data);
                            // Remove all entities.
                            viewer.entities.removeAll();
                            // Add entities.
                            _.map(data, function(satellite){
                               addSatellite(viewer, satellite);
                            });
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

    initWebsocket();
});

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