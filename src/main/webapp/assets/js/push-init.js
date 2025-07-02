document.addEventListener('DOMContentLoaded', () => {

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

    const app = firebase.initializeApp(firebaseConfig);
    const messaging = firebase.messaging();

    function requestPermissionAndGetToken() {
        console.log('Requesting permission...');
        Notification.requestPermission().then((permission) => {
            if (permission === 'granted') {
                console.log('Notification permission granted.');

                // VAPID key này bạn lấy từ Firebase Project Settings -> Cloud Messaging -> Web push certificates
                const vapidKey = 'BNjdo4iwQ46YE550eCvfvt9C3_wgP1SVjVAA2pe-eXe-67OFZIV_-EZ9bsIt30GCmKvPryDeap2082vZnaGvL6Q';

                messaging.getToken({ vapidKey: vapidKey }).then((currentToken) => {
                    if (currentToken) {
                        console.log('FCM Token:', currentToken);
                        sendTokenToServer(currentToken);
                    } else {
                        console.log('No registration token available. Request permission to generate one.');
                    }
                }).catch((err) => {
                    console.log('An error occurred while retrieving token. ', err);
                });
            } else {
                console.log('Unable to get permission to notify.');
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
            .then(response => console.log('Token sent to server successfully.'))
            .catch(err => console.error('Error sending token to server: ', err));
    }

    // Tự động hỏi quyền khi trang được tải
    requestPermissionAndGetToken();

    // Xử lý tin nhắn khi người dùng đang ở trên trang
    messaging.onMessage((payload) => {
        console.log('Message received. ', payload);
        // Tùy chọn: hiển thị thông báo tùy chỉnh bằng toast/alert
        alert(payload.notification.title + "\n" + payload.notification.body);
    });
});