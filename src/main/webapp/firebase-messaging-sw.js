// Import and initialize the Firebase SDK
importScripts('https://www.gstatic.com/firebasejs/9.2.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.2.0/firebase-messaging-compat.js');

// Dán đối tượng cấu hình Firebase của bạn vào đây
const firebaseConfig = {
    apiKey : "AIzaSyDMXBz2qaPXBDYFLari-sIs41kMuDHcoek" ,
    authDomain : "lunch-431e0.firebaseapp.com" ,
    projectId : "lunch-431e0" ,
    storageBucket : "lunch-431e0.firebasestorage.app" ,
    messagingSenderId : "369289433588" ,
    appId : "1:369289433588:web:40c496185b13994f67e9a1" ,
    measurementId : "G-QKCLJBMZTE"
};

firebase.initializeApp(firebaseConfig);

const messaging = firebase.messaging();

// Background message handler
messaging.onBackgroundMessage((payload) => {
    console.log(
        '[firebase-messaging-sw.js] Received background message ',
        payload,
    );
    const notificationTitle = payload.notification.title;
    const notificationOptions = {
        body: payload.notification.body,
        icon: '/assets/images/codegym_logo.png',
    };

    self.registration.showNotification(notificationTitle, notificationOptions);
});