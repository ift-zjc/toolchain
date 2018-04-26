
var viewer;
var ddSateliteNumber;
$(function(){

    initComponents();
    // Bing map key
    Cesium.BingMapsApi.defaultKey = 'Ak8mO9f0VpoByuNwmMcVvFka1GCZ3Bh8VrpqNLqGtIgsuUYjTrJdw7kDZwAwlC7E';
    viewer = new Cesium.Viewer('cesiumContainer');

});


function initComponents() {

    var sateliteNumberDs = [1,2,3,4,5,6,7,8,9,10];
    ddSateliteNumber = $("#ddSateliteNumber").dxSelectBox({
        items: sateliteNumberDs
    }).dxSelectBox("instance");
}