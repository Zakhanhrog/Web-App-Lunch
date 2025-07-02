document.addEventListener('DOMContentLoaded', () => {

    const firebaseConfig = {
        apiKey: "AIzaSyDMXBz2qaPXBDYFLari-sIs41kMuDHcoek",
        authDomain: "lunch-431e0.firebaseapp.com",
        projectId: "lunch-431e0",
        storageBucket: "lunch-431e0.appspot.com",
        messagingSenderId: "369289433588",
        appId: "1:369289433588:web:40c496185b13994f67e9a1",
        measurementId: "G-QKCLJBMZTE"
    };

    if (firebase.apps.length === 0) {
        firebase.initializeApp(firebaseConfig);
    }

    const messaging = firebase.messaging();

    function requestPermissionAndGetToken() {
        Notification.requestPermission().then((permission) => {
            if (permission === 'granted') {
                const vapidKey = 'BNjdo4iwQ46YE550eCvfvt9C3_wgP1SVjVAA2pe-eXe-67OFZIV_-EZ9bsIt30GCmKvPryDeap2082vZnaGvL6Q';

                messaging.getToken({ vapidKey: vapidKey }).then((currentToken) => {
                    if (currentToken) {
                        sendTokenToServer(currentToken);
                    }
                }).catch((err) => {
                    console.error('An error occurred while retrieving token. ', err);
                });
            }
        });
    }

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
            .catch(err => console.error('Error sending token to server: ', err));
    }

    if ('serviceWorker' in navigator) {
        navigator.serviceWorker.register('/firebase-messaging-sw.js')
            .then(function(registration) {
                messaging.useServiceWorker(registration);
                requestPermissionAndGetToken();
            }).catch(function(err) {
            console.error('Service Worker registration failed: ', err);
        });
    }

    messaging.onMessage((payload) => {
        console.log('Foreground message received. ', payload);
        const notificationTitle = payload.notification.title;
        const notificationOptions = {
            body: payload.notification.body,
            icon: '/assets/images/codegym_logo.png'
        };
        new Notification(notificationTitle, notificationOptions);
    });
});