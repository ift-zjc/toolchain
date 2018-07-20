importScripts("/pages/index.js");

self.addEventListener('message', function (e){
    var viewer = e.data;
    console.log(viewer.selectedEntity.id);
}, false);