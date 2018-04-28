
var viewer;
var ddSatelliteNumber;
var ddTicket;
var point;
var fadedLine;
$(function(){

    // initComponents();

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


    // var satelliteJson = {"satelliteId":"ViaSat-1-3","satelliteName":"ViaSat-1","satelliteDesc":"satellite monitoring","satelliteAvailability":"2018-04-27T19:00:00Z/2018-04-28T19:40:00Z","satelliteEpoch":"2018-04-27T20:00:00Z","satelliteData":[1.7697,8.2516,3.57794450656E7,1.7725,8.5024,3.57793116526E7,1.7753,8.7532,3.57791779079E7,1.7781,9.004,3.57790438339E7,1.7809,9.2548,3.57789094334E7,1.7836,9.5056,3.57787747088E7,1.7862,9.7564,3.57786396627E7,1.7889,10.0072,3.57785042978E7,1.7915,10.258,3.57783686165E7,1.794,10.5088,3.57782326215E7,1.7966,10.7596,3.57780963153E7,1.7991,11.0105,3.57779597007E7,1.8016,11.2613,3.57778227801E7,1.804,11.5121,3.57776855562E7,1.8064,11.7629,3.57775480317E7,1.8087,12.0138,3.57774102092E7,1.8111,12.2646,3.57772720912E7,1.8134,12.5154,3.57771336804E7,1.8156,12.7663,3.57769949796E7,1.8178,13.0171,3.57768559912E7,1.82,13.2679,3.57767167181E7],"predefindedData":[1.7697,8.2748,3.57690468506E7,1.7663,8.5238,3.57690303915E7,1.7707,8.7747,3.57690832876E7,1.7754,9.0225,3.5769237875E7,1.7776,9.277,3.57690244872E7,1.7806,9.5243,3.57690793054E7,1.7825,9.7752,3.57689466442E7,1.7862,10.0266,3.57689333374E7,1.7895,10.2778,3.57688960612E7,1.7934,10.526,3.57690036048E7,1.7966,10.7773,3.57689559907E7,1.7999,11.027,3.57689697988E7,1.8024,11.2789,3.57688509033E7,1.8075,11.5294,3.57689765667E7,1.8109,11.7811,3.57689328987E7,1.813,12.0284,3.57689722069E7,1.8145,12.2782,3.57688636441E7,1.8155,12.5313,3.5768569665E7,1.8171,12.7814,3.57684660464E7,1.8204,13.0315,3.57684884153E7,1.821,13.2812,3.57683239931E7,1.8231,13.5321,3.57682217492E7],
    //     "timeData":["2018-04-27T19:00:00Z","2018-04-27T19:01:00Z","2018-04-27T19:02:00Z","2018-04-27T19:03:00Z","2018-04-27T19:04:00Z","2018-04-27T19:05:00Z","2018-04-27T19:06:00Z","2018-04-27T19:07:00Z","2018-04-27T19:08:00Z","2018-04-27T19:09:00Z","2018-04-27T19:10:00Z","2018-04-27T19:11:00Z","2018-04-27T19:12:00Z","2018-04-27T19:13:00Z","2018-04-27T19:14:00Z","2018-04-27T19:15:00Z","2018-04-27T19:16:00Z","2018-04-27T19:17:00Z","2018-04-27T19:18:00Z","2018-04-27T19:19:00Z","2018-04-27T19:20:00Z"],"uncertainty":[3.9373E-4,-1.4313E-17,2.8794E-8,-1.4313E-17,1.9023E-4,-1.5962E-12,2.8794E-8,-1.5962E-12,100.0,3.8626E-4,-2.12E-7,6.8389E-4,-2.12E-7,1.9054E-4,1.2819E-4,6.8389E-4,1.2819E-4,99.6789,3.7905E-4,-4.2341E-7,0.0013396,-4.2341E-7,1.9082E-4,2.617E-4,0.0013396,2.617E-4,99.3799,3.7209E-4,-6.3975E-7,0.0019749,-6.3975E-7,1.9111E-4,3.9769E-4,0.0019749,3.9769E-4,99.1019,3.6535E-4,-8.6231E-7,0.0025835,-8.6231E-7,1.9141E-4,5.374E-4,0.0025835,5.374E-4,98.8393,3.5885E-4,-1.0896E-6,0.0031736,-1.0896E-6,1.9173E-4,6.8016E-4,0.0031736,6.8016E-4,98.5946,3.5256E-4,-1.3266E-6,0.0037456,-1.3266E-6,1.9205E-4,8.2794E-4,0.0037456,8.2794E-4,98.3706,3.4649E-4,-1.5742E-6,0.0042971,-1.5742E-6,1.9239E-4,9.8135E-4,0.0042971,9.8135E-4,98.1687,3.4061E-4,-1.8266E-6,0.0048286,-1.8266E-6,1.9274E-4,0.0011367,0.0048286,0.0011367,97.986,3.3491E-4,-2.0817E-6,0.005339,-2.0817E-6,1.9309E-4,0.0012931,0.005339,0.0012931,97.8191,3.2939E-4,-2.3437E-6,0.0058255,-2.3437E-6,1.9345E-4,0.0014539,0.0058255,0.0014539,97.6683,3.2406E-4,-2.6058E-6,0.0062908,-2.6058E-6,1.9382E-4,0.0016149,0.0062908,0.0016149,97.5314,3.189E-4,-2.8727E-6,0.0067377,-2.8727E-6,1.942E-4,0.0017783,0.0067377,0.0017783,97.4143,3.139E-4,-3.1459E-6,0.0071647,-3.1459E-6,1.9459E-4,0.0019452,0.0071647,0.0019452,97.3118,3.0906E-4,-3.42E-6,0.0075687,-3.42E-6,1.9498E-4,0.0021122,0.0075687,0.0021122,97.2258,3.0436E-4,-3.6947E-6,0.0079553,-3.6947E-6,1.9536E-4,0.0022837,0.0079553,0.0022837,97.1586,2.9981E-4,-3.9722E-6,0.0083256,-3.9722E-6,1.9576E-4,0.0024563,0.0083256,0.0024563,97.1073,2.9539E-4,-4.2463E-6,0.008674,-4.2463E-6,1.9615E-4,0.0026331,0.008674,0.0026331,97.0728,2.911E-4,-4.5262E-6,0.0090089,-4.5262E-6,1.9656E-4,0.0028136,0.0090089,0.0028136,97.0506,2.8695E-4,-4.8078E-6,0.0093287,-4.8078E-6,1.9697E-4,0.0029941,0.0093287,0.0029941,97.0464,2.8292E-4,-5.0907E-6,0.0096299,-5.0907E-6,1.9737E-4,0.0031744,0.0096299,0.0031744,97.0571,0.0043598,1.7033E-14,1.1319E-4,1.7033E-14,1.8422E-4,-4.3943E-12,1.1319E-4,-4.3943E-12,99.9999,0.0043303,7.8123E-7,0.0013664,7.8123E-7,1.8424E-4,-2.9505E-4,0.0013664,-2.9505E-4,99.5281,0.0043015,1.5382E-6,0.0025879,1.5382E-6,1.8428E-4,-5.8164E-4,0.0025879,-5.8164E-4,99.0705,0.0042733,2.2669E-6,0.0037727,2.2669E-6,1.8432E-4,-8.5811E-4,0.0037727,-8.5811E-4,98.6293,0.0042457,2.9633E-6,0.0049178,2.9633E-6,1.8439E-4,-0.0011234,0.0049178,-0.0011234,98.2053,0.0042187,3.6372E-6,0.0060395,3.6372E-6,1.8449E-4,-0.0013807,0.0060395,-0.0013807,97.7926,0.0041922,4.2981E-6,0.0071376,4.2981E-6,1.8459E-4,-0.0016321,0.0071376,-0.0016321,97.3943,0.0041662,4.9411E-6,0.0082081,4.9411E-6,1.8472E-4,-0.0018761,0.0082081,-0.0018761,97.0119,0.0041408,5.5667E-6,0.0092633,5.5667E-6,1.8487E-4,-0.0021126,0.0092633,-0.0021126,96.6415,0.0041158,6.183E-6,0.010293,6.183E-6,1.8502E-4,-0.002345,0.010293,-0.002345,96.2863,0.0040914,6.7886E-6,0.011301,6.7886E-6,1.8518E-4,-0.002574,0.011301,-0.002574,95.9419,0.0040675,7.3905E-6,0.01228,7.3905E-6,1.8533E-4,-0.0028026,0.01228,-0.0028026,95.6111,0.0040441,7.9682E-6,0.013231,7.9682E-6,1.855E-4,-0.0030227,0.013231,-0.0030227,95.2936,0.0040212,8.5269E-6,0.014163,8.5269E-6,1.8569E-4,-0.003236,0.014163,-0.003236,94.9881,0.0039988,9.0802E-6,0.015082,9.0802E-6,1.8589E-4,-0.0034471,0.015082,-0.0034471,94.6919,0.0039768,9.6183E-6,0.015982,9.6183E-6,1.8612E-4,-0.0036527,0.015982,-0.0036527,94.4067,0.0039553,1.0142E-5,0.016866,1.0142E-5,1.8635E-4,-0.0038518,0.016866,-0.0038518,94.1344,0.0039343,1.065E-5,0.017731,1.065E-5,1.8659E-4,-0.0040464,0.017731,-0.0040464,93.8721,0.0039137,1.1142E-5,0.018573,1.1142E-5,1.8685E-4,-0.0042349,0.018573,-0.0042349,93.6209,0.0038935,1.1613E-5,0.019392,1.1613E-5,1.8713E-4,-0.0044165,0.019392,-0.0044165,93.3815,0.0038738,1.2076E-5,0.020192,1.2076E-5,1.8741E-4,-0.0045954,0.020192,-0.0045954,93.1527,0.0032535,-5.2377E-14,4.2463E-5,-5.2377E-14,1.8489E-4,9.7485E-13,4.2463E-5,9.7485E-13,99.9999,0.0032441,-5.98E-7,0.0012815,-5.98E-7,1.8498E-4,2.4046E-4,0.0012815,2.4046E-4,99.5031,0.0032348,-9.4775E-7,0.0025378,-9.4775E-7,1.8513E-4,3.8126E-4,0.0025378,3.8126E-4,99.0033,0.0032258,-1.3083E-6,0.0037602,-1.3083E-6,1.8527E-4,5.2655E-4,0.0037602,5.2655E-4,98.5206,0.0032171,-1.6802E-6,0.0049471,-1.6802E-6,1.8542E-4,6.7638E-4,0.0049471,6.7638E-4,98.0562,0.0032087,-2.0662E-6,0.0060989,-2.0662E-6,1.8556E-4,8.3179E-4,0.0060989,8.3179E-4,97.6095,0.0032006,-2.4667E-6,0.0072162,-2.4667E-6,1.857E-4,9.9306E-4,0.0072162,9.9306E-4,97.1799,0.0031927,-2.8854E-6,0.0082952,-2.8854E-6,1.8585E-4,0.0011609,0.0082952,0.0011609,96.7716,0.0031851,-3.3241E-6,0.0093394,-3.3241E-6,1.86E-4,0.0013364,0.0093394,0.0013364,96.3821,0.0031778,-3.7745E-6,0.010351,-3.7745E-6,1.8617E-4,0.0015167,0.010351,0.0015167,96.009,0.0031708,-4.2451E-6,0.011327,-4.2451E-6,1.8634E-4,0.0017051,0.011327,0.0017051,95.6544,0.0031641,-4.724E-6,0.012268,-4.724E-6,1.8653E-4,0.0018968,0.012268,0.0018968,95.3169,0.0031576,-5.2197E-6,0.013176,-5.2197E-6,1.8671E-4,0.0020949,0.013176,0.0020949,94.9979,0.0031514,-5.7235E-6,0.01405,-5.7235E-6,1.8689E-4,0.0022961,0.01405,0.0022961,94.6959,0.0031455,-6.2489E-6,0.014887,-6.2489E-6,1.8707E-4,0.0025057,0.014887,0.0025057,94.4138,0.0031398,-6.7968E-6,0.015687,-6.7968E-6,1.8726E-4,0.0027243,0.015687,0.0027243,94.1503,0.0031345,-7.3534E-6,0.016455,-7.3534E-6,1.8746E-4,0.0029463,0.016455,0.0029463,93.9024,0.0031294,-7.918E-6,0.017194,-7.918E-6,1.8767E-4,0.0031717,0.017194,0.0031717,93.6703,0.0031245,-8.5086E-6,0.017901,-8.5086E-6,1.8787E-4,0.0034077,0.017901,0.0034077,93.4542,0.0031199,-9.1054E-6,0.018582,-9.1054E-6,1.8807E-4,0.0036462,0.018582,0.0036462,93.2526,0.0031155,-9.7051E-6,0.019237,-9.7051E-6,1.8825E-4,0.0038852,0.019237,0.0038852,93.0675]};
    //
    // addSatelliteSimple(satelliteJson);


    point = new Cesium.PointGraphics({
        pixelSize: 5,
        color: Cesium.Color.YELLOW
    });

    fadedLine = new Cesium.StripeMaterialProperty({
        evenColor: Cesium.Color.YELLOW,
        oddColor: Cesium.Color.BLACK,
        repeat: 1,
        offset: 0.2,
        orientation: Cesium.StripeOrientation.VERTICAL
    });



    $.ajax({
        url: '/api/satellite/init',
        method: 'GET'
    }).done(function(data){
        _.map(data, function(data, satelliteName){
           addSatellite(satelliteName, data);
        });
    });

});

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
        Cesium.JulianDate.addSeconds(entityTime, 10, entityTime);
    });

    newEntity.position = positions;
    newEntity.orientation = new Cesium.VelocityOrientationProperty(positions);

    if(true) {
        var path = new Cesium.PathGraphics();
        path.material = fadedLine;
        path.leadTime = new Cesium.ConstantProperty(0);
        path.trailTime = new Cesium.ConstantProperty(3600 * 1);

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

function initComponents() {

    var satelliteNumberDs = _.range(1,35);
    var satelliteTicketDs = _.range(60,660,60);
    ddSatelliteNumber = $("#ddSatelliteNumber").dxSelectBox({
        placeholder: 'Select # of Satelites',
        showClearButton: true,
        items: satelliteNumberDs
    }).dxSelectBox("instance");

    ddTicket = $('#ddTicket').dxSelectBox({
        placeholder: 'Ticket in second',
        showClearButton: true,
        items: satelliteTicketDs
    }).dxSelectBox('instance');

    $("#btnStart").dxButton({
        text: "Start Simulator",
        type: "success",
        onClick: function(e) {
            DevExpress.ui.notify(ddSatelliteNumber.option('value') + ' satellites been added to simulator successfully !!!');
        }
    });
}



/**
 * Add simple satellite (no pre-definded entities)
 * @param satelliteJson
 */
function addSatelliteSimple(satelliteJson){

    var sId = satelliteJson.satelliteId;
    var sName = satelliteJson.satelliteName;

    // Compute position
    var positions = new Cesium.SampledPositionProperty();
    var timeDataArray = satelliteJson.timeData.toString().split(",");
    var cartesian3DataArray = satelliteJson.satelliteData.toString().split(",");

    var point = new Cesium.PointGraphics({
        pixelSize: 5,
        color: Cesium.Color.YELLOW
    });

    // Loop timedata array
    var index = 0;
    _.each(timeDataArray, function(timeData){
        var nodePosition = cartesian3DataArray.slice(index, index+3);
        index = index+3;

        //position.addSample(Cesium.JulianDate.fromIso8601('2012-03-15T10:01:00Z'), new Cesium.Cartesian3(3169722.12564676,-2787480.80604407,-5661647.74541255));
        // Add to position
        positions.addSample(Cesium.JulianDate.fromIso8601(_.trim(timeData, "\"")),
            Cesium.Cartesian3.fromArray(nodePosition[0], nodePosition[1], nodePosition[2]));
    });

    // Compute entity
    var entity = new Cesium.Entity({id: sId});

    entity.point = point;

    // Position
    entity.position = positions;
    entity.orientation = new Cesium.VelocityOrientationProperty(positions);

    var fadedLine = new Cesium.StripeMaterialProperty({
        evenColor: Cesium.Color.YELLOW,
        oddColor: Cesium.Color.BLACK,
        repeat: 1,
        offset: 0.2,
        orientation: Cesium.StripeOrientation.VERTICAL
    });

    if(true) {
        var path = new Cesium.PathGraphics();
        path.material = fadedLine;
        path.leadTime = new Cesium.ConstantProperty(0);
        path.trailTime = new Cesium.ConstantProperty(3600 * 1);

        entity.path = path;
    }



    // Make a smooth path
    entity.position.setInterpolationOptions({
        interpolationDegree : 5,
        interpolationAlgorithm : Cesium.LagrangePolynomialApproximation
    });


    viewer.entities.add(entity);

}