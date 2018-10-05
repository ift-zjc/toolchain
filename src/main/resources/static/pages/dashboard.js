
// Max AngularVelocity value
var maxAngularVelocity = 0.000087;

var viewer;
var fileUploader;
var popupFileUploader;
var popupAddSatellite;
var progressBar;
var toolbar;
var fadedLine;

var dsSatellites;
var treeSatellites;
var dsSatelliteStatus;
var dxSatelliteStatus;

var tickTriggerGap = 10;        //in second
var currentTick;
var connectedObjectPolylines;
var connectedObjectPolylinesCurrent;

var dgApplications;
var chartApps;

//Stations
var afMonitorStations;
$(function(){

    // Init ground stations
    afMonitorStations = [
        {gsId: 'gsAF1', cartesian3: [-5.4631*1000000, -2.4802*1000000, 2.1570*1000000], name: "Hawaii"},
        {gsId: 'gsAF2', cartesian3: [0.9189*1000000, -5.5343*1000000, 3.0242*1000000], name: "Cape Canaveral"},
        {gsId: 'gsAF3', cartesian3: [6.1200*1000000, -1.5663*1000000, -0.8759*1000000], name: "Ascension"},
        {gsId: 'gsAF4', cartesian3: [1.9105*1000000, 6.0311*1000000, -0.8072*1000000], name: "Diego Garcia"},
        {gsId: 'gsAF5', cartesian3: [-6.1610*1000000, 1.3396*1000000, 0.9602*1000000], name: "Kwajalein"},

        {gsId: 'gsMaster1', cartesian3: [-1.2482*1000000, -4.8176*1000000, 3.9758*1000000], name: "Schriever AFB"}
    ];

    popupFileUploader = $("#popupFileUploader").dxPopup({
        height: '400px',
        width: '600px',
        deferRendering: false,
        title: 'Upload TLE and JSON Configuration file',
        onHiding: function(){
            fileUploader.reset();
        }
    }).dxPopup("instance");

    popupAddSatellite = $('#addSatellitePopup').dxPopup({
        height: '350px',
        width: '700px',
        deferRendering: false,
        // visible: true,
        showTitle: true,
        onHiding: function () {
            // empty textarea value
            $('#taTle').dxTextArea('instance').option('value','');
        },
        title: 'Add Satellites (two-line TLE format)',
        contentTemplate: $("<div id='addSatelliteForm'></div>"),
        toolbarItems: [
            {
                toolbar: 'bottom',
                location: 'after',
                widget: 'dxButton',
                options:{
                    text: 'Add Satellite(s)',
                    onClick: function(){
                        // Ajax call
                        var data = {tle: $('#taTle').dxTextArea('instance').option('value')};
                        $.ajax({
                            type: 'POST',
                            url: 'api/satellite/add',
                            data: JSON.stringify(data),
                            contentType: "application/json",
                            cache: false,
                            success: function (data){
                                // Add entities.
                                _.map(data.satelliteCollections, function(satellite){
                                    addSatellite(viewer, satellite);
                                });

                                if (treeSatellites.option('items').length == 0){
                                    treeSatellites.option('items', data.satelliteItems);
                                }else{
                                    // _.map(data.satelliteItems, function(item){
                                    //     // Check whether has categoryId
                                    //     if(item.categoryId){
                                    //         // this is not category
                                    //     }else{
                                    //         // this is category.
                                    //         // Check whether category existing
                                    //         var catetoryCnt = _.find(treeSatellites.option('items'), function(treeItem){return item.id==treeItem.id;});
                                    //         if(catetoryCnt == 0){
                                    //             treeSatellites.option('items', treeSatellites.option('items'));
                                    //         }
                                    //     }
                                    // })
                                    var _items = treeSatellites.option('items');
                                    var unionList = _.union(_items, data.satelliteItems);
                                    var updatedList = _.uniq(unionList, function(item, key, name){return item.name;});
                                    treeSatellites.option('items', updatedList);
                                }

                                // Hide popup
                                popupAddSatellite.hide();
                            }
                        }).always(function (jqXHR){
                            if(jqXHR.status == 500){
                                DevExpress.ui.dialog.alert("Please confirm your input the right two-line TLE format include satellite name at first line", "Something wrong");
                            }
                        })
                    }
                }
            }
        ]
    }).dxPopup('instance');

    $('#addSatelliteForm').dxForm({
        colCount: 1,
        labelLocation: "top",
        items: [
            {
                dataField: "TLE Content",
                editorType: "dxTextArea",
                editorOptions: {
                    height: '180px',
                    elementAttr:{
                        id: 'taTle'
                    }
                }
            }
        ]
    });
    // Bing map key
    Cesium.BingMapsApi.defaultKey = 'Ak8mO9f0VpoByuNwmMcVvFka1GCZ3Bh8VrpqNLqGtIgsuUYjTrJdw7kDZwAwlC7E';
    terrainProvider = new Cesium.CesiumTerrainProvider({
        url : 'https://assets.agi.com/stk-terrain/world',
        requestVertexNormals : true
    });

    viewer = new Cesium.Viewer('cesiumContainer', {
        terrainProvider: terrainProvider,
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
                icon: 'add',
                elementAttr:{
                  id: 'btnAddSatellite'
                },
                onClick: function (){
                    // This button always enabled
                    popupAddSatellite.show();
                }
            }
        },{
            location: 'before',
            widget: 'dxButton',
            options: {
                text: 'Remove Satellite',
                icon: 'remove',
                disabled: true,
                elementAttr: {
                    id: 'btnRemoveSatellite'
                },
                onClick: function (){
                    // Remove selected satellite from topology
                    if(!_.isUndefined(selectedNode)){
                        var _satelliteName = selectedNode.name;
                        var _data = {name: _satelliteName};
                        var _result = DevExpress.ui.dialog.confirm("Are you sure you want to remove object: " + _satelliteName + "?", "Confirm remove");
                        _result.done(function (dialogResult) {
                            if(dialogResult){
                                // Delete from server
                                $.ajax({
                                    type : 'POST',
                                    url : 'api/satellite/disable',
                                    data: JSON.stringify(_data),
                                    contentType: "application/json",
                                    cache: false,
                                    success: function (data){
                                        // Remove object from cesium
                                        viewer.entities.removeById(data.value);
                                        // Remove polyline
                                        removePolylines();
                                        connectedObjectPolylinesCurrent = [];
                                        connectedObjectPolylines = [];
                                        // Remove node from treeview
                                        var _items = treeSatellites.option('items');
                                        // Filter out the removed node
                                        treeSatellites.option('items', _.filter(_items, function(_item){ return _item.name != data.value;}));

                                        // Disable the remove satellite button
                                        $('#btnRemoveSatellite').dxButton('instance').option('disabled', true);

                                    }
                                })
                            }
                        });
                    }
                }
            }
        }]
    });

    $('#toolbarTop').dxToolbar({
        items: [{
            location: 'center',
            locateInMenu: 'never',
            template: function(){
                return $('<div class="toolbar-label"><b>ToolChain Dashboard</b></div>')
            }
        }]
    })

    toolbar = $('#toolbar').dxToolbar({
        items: [{
            location: 'before',
            widget: 'dxButton',
            options: {
                text: 'Reset Simulator',
                icon: 'revert',
                onClick: function(){
                    var _result = DevExpress.ui.dialog.confirm("Are you sure you want to reset simulator?", "Confirm Reset");
                    _result.done(function (dialogResult){
                        if(dialogResult){
                            $.ajax({
                                type: 'POST',
                                url: '/api/simulation/reset',
                                success: function (data) {
                                    if(data) {
                                        // Remove all satellites
                                        viewer.entities.removeAll()
                                        // Remove TreeView
                                        treeSatellites.option('items', {});
                                        // Remove datagrid data.
                                        // Automatically removed.
                                        DevExpress.ui.notify('Simulator Reset, all data erased !!!');
                                    }else{

                                    }
                                }
                            })
                        }else{
                            // Do nothing
                        }
                    });
                }
            }
        },{
            location: 'before',
            widget: 'dxButton',
            options: {
                text: 'Upload files',
                icon: 'upload',
                onClick: function(){
                    // Show popup
                    // DevExpress.ui.notify('File uploaded');
                    popupFileUploader.show();
                }
            }
        },{
            location: 'before',
            widget: 'dxButton',
            options: {
                text: 'Propagate objects',
                icon: 'runner',
                disabled: false,
                onClick: function(){
                    $("#progressBar").removeClass("complete");
                    progressBar.option('statusFormat', function(data){return 'Initial Simulator ... ';});
                    $('#progressBar').show();

                //    Start ajax call
                    $.ajax({
                        type: "POST",
                        url: "/api/simulation/populateobjects",
                        cache: false,
                        success: function(data){
                            // Remove all entities.
                            viewer.entities.removeAll();
                            // Add entities.
                            _.map(data.satelliteCollections, function(satellite){
                               addSatellite(viewer, satellite);
                            });

                            // Add groundstations.
                            addGroundStations();

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

    $('#toolbarSimulator').dxToolbar({
        items: [{
            template: function () {
                return $('<span class="h6">Start time&nbsp;</span>');
            },
            location: 'before'

        },{
            location: 'before',
            widget: 'dxDateBox',
            options: {
                type: 'datetime',
                value: new Date(),
                elementAttr: {
                    id: 'timeStart'
                },
            }
        },{
            template: function () {
                return $('<span class="h6">End time&nbsp;</span>');
            },
            location: 'before'
        },{
            location: 'before',
            widget: 'dxDateBox',
            options: {
                type: 'datetime',
                value: new Date(),
                elementAttr: {
                    id: 'timeEnd'
                },
            }
        },{
            location: 'before',
            widget: 'dxButton',
            options: {
                text: 'Start simulation',
                icon: 'runner',
                disabled: true,
                elementAttr: {
                    id: 'btnStartSimulation'
                },
                onClick: function(){
                    var _startTime = $('#timeStart').dxDateBox('instance').option('value').toISOString();
                    var _endTime = $('#timeEnd').dxDateBox('instance').option('value').toISOString();
                    var _data = {timeStart: _startTime, timeEnd: _endTime, appData: dsApplications.items()};
                    // Disable button
                    $('#btnStartSimulation').dxButton('instance').option('disabled', true);

                    // Data
                    var data = {'appData': dsApplications.items()};
                    $.ajax({
                        type: 'POST',
                        url: '/api/simulation/start',
                        data: JSON.stringify(_data),
                        contentType: "application/json",
                        success: function (data){
                            /**
                             * Application traffic chart
                             */

                                // Get all application data from datasource
                                // var dataArray = dsApplications.store()._array.slice(0);
                                // Get min date
                                // var startDate = _.min(_.pluck(dataArray, 'startTime'));
                                // var endDate = _.max(_.pluck(dataArray, 'endTime'));

                                // Construct datasource
                            var dsTrafficChart = [];
                            _.each(data.applicationTraffic, function(traffic){
                                dsTrafficChart = dsTrafficChart.concat(traffic.applicationTrafficDataList);
                            });
                            // var dsTrafficChart="[";
                            // for (var i = 0; i < 1000; i++){
                            //     // JSON string
                            //     var jsonStr = '{"offset": ' + i;
                            //
                            //     // Loop the data to find traffic @ i
                            //     _.map(data.applicationTraffic, function (appTraffic){
                            //         var name = appTraffic.appName;
                            //         var trafficVolumnItem = _.find(appTraffic.applicationTrafficDataList, function(trafficDataItem){
                            //             return trafficDataItem.offsetMillionSecond == i;
                            //         });
                            //         var trafficVolumn = 0;
                            //         if(! _.isUndefined(trafficVolumnItem)){
                            //             trafficVolumn = trafficVolumnItem.trafficVolumn
                            //         }
                            //         jsonStr += ', "' + name + '": ' + trafficVolumn;
                            //     });
                            //
                            //     jsonStr += "},";
                            //
                            //     dsTrafficChart += jsonStr;
                            // }
                            // // Remove last ,
                            // dsTrafficChart = dsTrafficChart.slice(0, -1) + ']';
                            //
                            // // Convert to JSON
                            // dsTrafficChart = JSON.parse(dsTrafficChart);
                            //
                            // // Add serial
                            // seriesTraffic = "[";
                            // _.map(data.applicationTraffic, function (appTrafficS){
                            //     var name = appTrafficS.appName;
                            //     seriesTraffic += '{"valueField": "' + name + '", "name": "' + name + '"},'
                            // });


                            //
                            // seriesTraffic = seriesTraffic.slice(0, -1) + "]";
                            // seriesTraffic = JSON.parse(seriesTraffic);

                            // dsTrafficChart = JSON.stringify(dsTrafficChart);
                            // var dsRaw = dsTrafficChart.replace('[[','[').replace(']]',']');
                            // var ds = new DevExpress.data.DataSource({
                            //     store: {
                            //         type: "local",
                            //         name: "MyLocalData",
                            //         key: "appName",
                            //         data: dsTrafficChart
                            //     }
                            // });
                            chartApps.option('dataSource', dsTrafficChart);
                        },
                        error: function (data){

                        }
                    }).done(function(){
                        // Enable button
                        $('#btnStartSimulation').dxButton('instance').option('disabled', false);
                    });
                }
            }
        }]
    })

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

    // init ground stations
    addGroundStations();

    initSatelliteStatusGrid();
    initSatelliteTree();
    initApplicationComponents();
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

var selectedNode;

function initSatelliteTree(){

    treeSatellites = $('#satalliteTree').dxTreeView({
        // dataSource: treeSatellites,
        dataStructure: 'plain',
        height: $('#cesiumContainer').height() - $('#satelliteToolbar').height()-18,
        parentIdExpr: "categoryId",
        keyExpr: 'id',
        displayExpr: 'name',
        searchEnabled: true,
        selectByClick: true,
        onItemSelectionChanged: function (e) {
            var item = e.node.itemData;
            selectedNode = item;

            if(_.isNull(item.categoryId) || _.isUndefined(item.categoryId)){
                // Enable remove satellite button
                $('#btnRemoveSatellite').dxButton('instance').option('disabled', true);
                // Remove selected node
                selectedNode = undefined;
            }else{
                // Change selected entity on cesium viewer.
                viewer.selectedEntity = viewer.entities.getById(item.name);

                // Enable remove satellite button
                $('#btnRemoveSatellite').dxButton('instance').option('disabled', false);
            }

        }

    }).dxTreeView('instance');
}

var dsObjects;
var tmDatasource;
var protocols = [{name: "TCP", value: "TCP"}, {name: "UDP", value: "UDP"}];
var editors = {};
var tmObject;
var editingId;

/**
 * Application related components
 */
function initApplicationComponents(){

    dsApplications = new DevExpress.data.DataSource({
        store: {
            type: "local",
            name: "AppDataLocalStore",
            key: "id"

        },
        onChanged: function(){
            if(dsApplications.totalCount() > 0){
                $('#btnStartSimulation').dxButton('instance').option('disabled', false);
            }else{
                $('#btnStartSimulation').dxButton('instance').option('disabled', true);
            }
        }
    });

    // Object datasource
    dsObjects = new DevExpress.data.CustomStore({
        load: function(loadOptions){
            var deferred = $.Deferred();

            $.ajax({
                url: "/api/objectlist",
                type: "GET",
                success: function(result){
                    deferred.resolve(result, {totalCount: result.length});
                },
                error: function(){
                    deferred.reject ("Object data loading error ...");
                },
                timeout: 1000
            });

            return deferred.promise();
        },

        byKey: function(key){
            var d = new $.Deferred();
            $.post("api/object/" + key)
                .done(function(result){
                    d.resolve(result)
                });

            return d.promise();
        }
    });

    tmDatasource = new DevExpress.data.CustomStore({
        load: function (loadOptions){
            var deferred = $.Deferred();

            $.ajax({
                url: "/api/tmlist",
                type: "POST",
                success: function(result){
                    deferred.resolve(result, {totalCount: result.length});
                },
                error: function(){
                    deferred.reject("Traffic Model Loading Error");
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

    popTMAttributes = $("#formTrafficModelPopup").dxPopup({
        title: 'Traffic Model Attributes',
        visible: false,
        height: "auto",
        width: "auto",
        deferRendering: false,
        toolbarItems: [
            {
                toolbar: 'bottom',
                location: 'after',
                widget: 'dxButton',
                options: {
                    text: 'Update',
                    onClick: function(){
                        if(!_.isUndefined(tmObject)) {
                            dsApplications.store().byKey(editingId).done(function(_item){_item.tm = tmObject;});
                        }

                        popTMAttributes.hide();
                    }
                }
            },
            {
                toolbar: 'bottom',
                location: 'after',
                widget: 'dxButton',
                options: {
                    text: 'Cancel',
                    onClick: function(){
                        popTMAttributes.hide();
                    }
                }
            }
        ],
        onHiding: function ( e ){
            // Save to TM data object
            tmObject = $('#formTrafficModel').dxForm('instance').option('formData');
            editors.tmCode.option('selectedItem', tmObject);
        }
    }).dxPopup('instance');

    // Initial data grid
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
                        items: ["name", "tmCode", "btnAdjust"]
                    },
                    {
                        itemType: "group",
                        caption: "Crosslink information",
                        items: ["source","dest", "protocol"]
                    },{
                        itemType: "group",
                        colSpan: 2,
                        colCount: 2,
                        caption: "Execution Time Frame",
                        items: ["startTime", "endTime"]
                    }]
            }
        },
        searchPanel: {
            visible: true
        },
        columns: [{
            name: "name",
            dataField: "name"
        }, {
            caption: "Source & Destination",
            columns: [
                {
                    name: "source",
                    dataField: "source",
                    caption: "Source",
                    dataType: "String",
                    validationRules: [{type: "required"}],
                    lookup: {
                        dataSource: dsObjects,
                        valueExpr: "id",
                        grouped: true,
                        displayExpr: "name"
                    }
                }, {
                    name: "dest",
                    dataField: "dest",
                    caption: "Destination",
                    dataType: "String",
                    validationRules: [{type: "required"}],
                    lookup: {
                        dataSource: dsObjects,
                        valueExpr: "id",
                        grouped: true,
                        displayExpr: "name"
                    }
                }
            ]
        },{
            name: "protocol",
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
            name: "tmCode",
            dataField: "tmCode",
            caption: "Traffic Model",
            dataType: "String",
            lookup: {
                dataSource: tmDatasource,
                valueExpr: "code",
                displayExpr: "name"
            }
        },{
            caption: "Time Range",
            columns: [
                {
                    name: "startTime",
                    dataField: "startTime",
                    caption: "Start Time",
                    dataType: "datetime"
                    // visible: false
                },
                {
                    name: "endTime",
                    dataField: "endTime",
                    caption: "End Time",
                    dataType: "datetime"
                    // visible: false
                }
            ],
            // columns: [{
            //     dataField: "startOffset",
            //     caption: "Start Offset",
            //     dataType: "number"
            // }, {
            //     dataField: "endOffset",
            //     caption: "End Offset",
            //     dataType: "number"
            // }],
            // cellTemplate: function (container, options) {
            //     $('<span>Start at ' + options.data.startTime + ' to ' + options.data.endTime  + '</span>').appendTo(container);
            // },
            // formItem: {
            //     visible: false
            // }
        },{
            name: "btnAdjust",
            dataField: "btnAdjust",
            caption: "Config",
            visible: false,
            editCellTemplate: function(cellElement, cellInfo){
                $('<div />').dxButton({
                    text: "Config Traffic Model",
                    elementAttr: {
                        id: "btnAdjust"
                    },
                    onClick: function (e) {
                        // if(_.isUndefined(selectedTMCode) || selectedTMCode == editingRow.tm.code) {
                        var formData;

                        selectedTMItem = editors.tmCode.option("selectedItem");



                        if (!_.isUndefined(tmObject) && !_.isNull(tmObject) && selectedTMItem.code == tmObject.code){
                            formData = tmObject;
                        }else{
                            formData = selectedTMItem;
                        }
                        // Create form
                        $('#formTrafficModel').dxForm({
                            formData: formData,
                            customizeItem: function(item){
                                if (item.dataField === "name" || item.dataField === "code" || item.dataField === "desc"){
                                    item.editorOptions = {
                                        disabled: true
                                    };
                                }
                            }
                        });
                        // }

                        popTMAttributes.show();

                    }
                }).appendTo(cellElement);
            }
        }],
        onEditingStart: function (e) {
            tmObject = e.data.tm;
            editingId = e.key;
        },
        onRowPrepared: function (info){
            if(info.rowType == "data"){
                info.rowElement.removeClass("dx-row-alt").addClass("bg-dark text-white");
            }
        },
        // onEditorPreparing: function (e) {
        //     var component = e.component,
        //         rowIndex = e.row && e.row.rowIndex;
        //     if(e.dataField === "id"){
        //         var onClick = e.editorOptions.onClick;
        //         e.editorOptions.OnClick = function (e) {
        //
        //         }
        //     }
        // }
        onEditorPrepared: function( e ){
            // var tmSelectBox;
            // if(e.parentType == 'dataRow' && e.dataField == "tm.code"){
            //     e.editorElement.dxSelectBox('instance').option('onValueChanged', function (e) {
            //         selectedTMCode = e.value;
            //         // Set data to popup form fields
            //         $('#formTrafficModel').dxForm({
            //             formData: e.component.option('selectedItem')
            //         });
            //     });
            // }
            if (e.parentType == 'dataRow') {
                editors[e.name] = e.editorElement[e.editorElement.data().dxComponents[0]]('instance');
            }
        },
        onRowUpdated: function(e) {
            editors = {};
        },
        onRowInserted: function(e) {
            editors = {};
            if(!_.isUndefined(tmObject)) {
                dsApplications.store().byKey(e.key).done(function(_item){_item.tm = tmObject;});
            }
        }
    }).dxDataGrid("instance");

    chartApps = $("#chartApps").dxChart({
        palette: "soft",
        commonSeriesSettings: {
            argumentField: "timeString",
            valueField: "trafficVolumn",
            barPadding: 0.2,
            type: "bar"
        },argumentAxis: {
            argumentType: 'datetime'
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
        seriesTemplate: {
            nameField: "appName"
        }
        // argumentAxis: {
        //     argumentType: 'number',
        //     label: {
        //         customizeText: function(obj) {
        //             var currentTime = Cesium.JulianDate.clone(viewer.clock.startTime);
        //             currentTime = Cesium.JulianDate.addSeconds(simulatorStartDateTime, obj.value, currentTime);
        //             return moment(Cesium.JulianDate.toDate(currentTime).toISOString()).format('MMMM Do YYYY, h:mm:ss a');
        //         },
        //         font:{
        //             color: '#57962B'
        //         }
        //     }
        // },
        // onLegendClick: function (e) {
        //     var series = e.target;
        //     if (series.isVisible()) {
        //         series.hide();
        //     } else {
        //         series.show();
        //     }
        // }
    }).dxChart('instance');
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

/**
 * Add fixed ground stations.
 */
function addGroundStations(){
    // init ground stations
    _.each(afMonitorStations,function(afMonitor){
        addGroundStation(afMonitor.gsId, Cesium.Cartesian3.fromArray(afMonitor.cartesian3), afMonitor.name);
    });
}

function initLayoutEvent(){
    // Sync table height with viewer height.
    // $(window).on('resize', function(){
    //     dxSatelliteStatus.option('height', $('#cesiumContainer').height());
    //     treeSatellites.option('height', $('#cesiumContainer').height());
    // });
    $(window).on('resize', function(){
        $('#mapContainer').height($('#cesiumContainer').height());
        treeSatellites.option('height', $('#cesiumContainer').height() - $('#satelliteToolbar').height()-18)
    })
}

function handleTick(clock){

    try {
        if ((!_.isUndefined(viewer.selectedEntity)) && (_.isUndefined(currentTick) ||
            clock.currentTime.dayNumber != currentTick.dayNumber ||
            Math.abs(clock.currentTime.secondsOfDay - currentTick.secondsOfDay) > tickTriggerGap)) {
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
                success: function (data) {

                    _.each(data.satelliteProperties, function (item) {
                        dsSatelliteStatus.update({name: item.name}, {
                            name: item.name,
                            value: item.value
                        })
                    });

                    // Filter the array within max angularvelocity range
                    var angularvelocitySatellites = _.filter(data.satelliteXSatellites, function (_item) {
                        return _item.angularVelocity <= maxAngularVelocity
                    });
                    var closestSatellites = _.chain(angularvelocitySatellites).sortBy(function (item) {
                        return item.distance;
                    }).first(4).value();
                    // Clean all connected object and linked again
                    connectedObjectPolylinesCurrent = [];
                    _.each(closestSatellites, function (_sitem) {
                        // TODO connect to source;
                        // Connecting satellites
                        connectObject(_sitem.source, _sitem.destination, false);
                    });

                    // Check for the difference between connectedObjectPolylines  and connectedObjectPolylinesCurrent.
                    if (_.isUndefined(connectedObjectPolylines)) {
                        connectedObjectPolylines = connectedObjectPolylinesCurrent;
                    }
                    else {
                        // Find the line in connectedObjectPolylines that doesn't existing in connectedObjectPolylinesCurrent and remove it animatedly.
                        var diffArray = _.difference(connectedObjectPolylines, connectedObjectPolylinesCurrent);

                        // Remove those additional lines.
                        _.each(diffArray, function (_id) {
                            viewer.entities.removeById(_id);
                        });

                        connectedObjectPolylines = connectedObjectPolylinesCurrent;
                    }

                },
                error: function (data) {

                }
            })
        }
    }catch(ex){
        // Do nothing now.
    }

}

/**
 * Connect two objects
 * @param sourceName
 * @param destName
 */
function connectObject(sourceName, destName, followSurface){

    var _id = sourceName + '-' + destName;
    connectedObjectPolylinesCurrent.push(_id);
    // If already existing
    if(!_.isUndefined(viewer.entities.getById(_id))){
        return;
    }
    var positionArray = new Cesium.PositionPropertyArray([
        new Cesium.ReferenceProperty(
            viewer.entities,
            sourceName,
            ['position']
        ),
        new Cesium.ReferenceProperty(
            viewer.entities,
            destName,
            ['position']
        )
    ]);

    // Connect two objects
    viewer.entities.add({
        id: sourceName + '-' + destName,
        polyline: {
            followSurface: followSurface,
            positions: positionArray,
            material: new Cesium.ColorMaterialProperty(
                Cesium.Color.RED.withAlpha( 0.75 )
            )
        }
    });
}


function handleSelectedEntityChangedEvent(event){

    // unselected entity.
    if(_.isUndefined(event)) {
        // Remove satellite property table content
        dsSatelliteStatus.clear();
        dsSatelliteStatus.remove();

        return;
    };

    removePolylines();
    connectedObjectPolylines = [];
    connectedObjectPolylinesCurrent = [];
    currentTick = undefined;

    // Make

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
                    _.each(data.satelliteProperties, function(item){
                        dsSatelliteStatus.insert({
                            name: item.name,
                            value: item.value
                        })
                    })
                }else{
                    _.each(data.satelliteProperties, function(item){
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

/**
 * Remove all polylines connecting objects.
 */
function removePolylines(){
    _.each(connectedObjectPolylines, function(_id){
        viewer.entities.removeById(_id);
    })
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