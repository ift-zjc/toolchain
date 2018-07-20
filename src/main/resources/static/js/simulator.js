
var viewer;
var startTime;
var endTime;
var delta;

onmessage = function (event) {
    console.log("initiating simulator ..." + event.data);
    viewer = event.data[0];
    startTime = event.data[1];
    endTime = event.data[2];
    delta = event.data[3];

    console.log("start simulator ...");
    simulate();
}

function simulate(){
    postMessage("Initial Cesium Viewer ...");

    console.log(viewer.clock.currentTime);

    postMessage("Cesium Viewer Ready");
}
