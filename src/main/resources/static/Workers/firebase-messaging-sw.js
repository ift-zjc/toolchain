importScripts('https://www.gstatic.com/firebasejs/5.2.0/firebase-app.js');
importScripts('https://www.gstatic.com/firebasejs/5.2.0/firebase-messaging.js');

// Initialize Firebase
var config = {
    apiKey: "AIzaSyBw3VVc0dUO_hydYZ2My8XTxjsN0leQwio",
    authDomain: "ift-demo-20180724.firebaseapp.com",
    databaseURL: "https://ift-demo-20180724.firebaseio.com",
    projectId: "ift-demo-20180724",
    storageBucket: "ift-demo-20180724.appspot.com",
    messagingSenderId: "361001276618"
};
firebase.initializeApp(config);
const messaging = firebase.messaging();