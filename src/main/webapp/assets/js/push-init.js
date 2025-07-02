// Import các hàm cần thiết từ Firebase SDK
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import { getMessaging, getToken, onMessage } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-messaging.js";

const firebaseConfig = {
    apiKey: "AIzaSyDMXBz2qaPXBDYFLari-sIs41kMuDHcoek",
    authDomain: "lunch-431e0.firebaseapp.com",
    projectId: "lunch-431e0",
    storageBucket: "lunch-431e0.appspot.com",
    messagingSenderId: "369289433588",
    appId: "1:369289433588:web:40c496185b13994f67e9a1",
    measurementId: "G-QKCLJBMZTE"
};

// Khởi tạo Firebase
const app = initializeApp(firebaseConfig);
const messaging = getMessaging(app);

// Hàm yêu cầu quyền và lấy token
function requestPermissionAndGetToken() {
    console.log('Requesting permission...');
    Notification.requestPermission().then((permission) => {
        if (permission === 'granted') {
            console.log('Notification permission granted.');
            const vapidKey = 'BNjdo4iwQ46YE550eCvfvt9C3_wgP1SVjVAA2pe-eXe-67OFZIV_-EZ9bsIt30GCmKvPryDeap2082vZnaGvL6Q';

            getToken(messaging, { vapidKey: vapidKey }).then((currentToken) => {
                if (currentToken) {
                    console.log('FCM Token:', currentToken);
                    sendTokenToServer(currentToken);
                } else {
                    console.log('No registration token available. Request permission to generate one.');
                }
            }).catch((err) => {
                console.error('An error occurred while retrieving token. ', err);
            });
        } else {
            console.log('Unable to get permission to notify.');
        }
    });
}

// Hàm gửi token lên server
function sendTokenToServer(token) {
    fetch('/api/push/subscribe', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ token: token }),
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Server responded with an error.');
            }
            return response.json();
        })
        .then(data => {
            console.log('Token sent to server response.', data);
        })
        .catch((error) => {
            console.error('Error sending token to server:', error);
        });
}

onMessage(messaging, (payload) => {
    console.log('Foreground message received. ', payload);
    const notificationTitle = payload.notification.title;
    const notificationOptions = {
        body: payload.notification.body,
        icon: '/assets/images/codegym_logo.png'
    };
    new Notification(notificationTitle, notificationOptions);
});

if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/firebase-messaging-sw.js')
        .then((registration) => {
            console.log('Service Worker registered successfully with scope:', registration.scope);
            requestPermissionAndGetToken();
        }).catch((err) => {
        console.error('Service Worker registration failed: ', err);
    });
}