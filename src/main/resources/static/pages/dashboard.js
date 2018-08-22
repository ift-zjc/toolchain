
var viewer;
var fileUploader;
var popupFileUploader;
var progressBar;
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

    // Init toolbar
    $('#toolbar').dxToolbar({
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