const firebase = require('firebase/compat/app');
require('firebase/compat/storage');
require('firebase/compat/database');
require('firebase/compat/auth');
const fs = require('fs');

const firebaseConfig = {
    apiKey: "AIzaSyD4GZ-DUfvXgqRKiJSbuiH-6R3YAxYTrLU",
    authDomain: "hogar-finanzas-2026.firebaseapp.com",
    databaseURL: "https://hogar-finanzas-2026-default-rtdb.firebaseio.com",
    projectId: "hogar-finanzas-2026",
    storageBucket: "hogar-finanzas-2026.firebasestorage.app",
    messagingSenderId: "1050884199444",
    appId: "1:1050884199444:android:fcba821b52ea1109d14301"
};

firebase.initializeApp(firebaseConfig);

async function uploadFile() {
    try {
        console.log("Reading APK...");
        const fileData = fs.readFileSync("h:\\Mi unidad\\Nido\\hogar-finanzas\\app\\build\\outputs\\apk\\debug\\app-debug.apk");
        
        console.log("Uploading to Firebase Storage...");
        const storageRef = firebase.storage().ref().child('updates/app-update.apk');
        await storageRef.put(fileData, { contentType: 'application/vnd.android.package-archive' });
        
        const url = await storageRef.getDownloadURL();
        console.log("Uploaded! URL:", url);
        
        console.log("Updating Realtime Database...");
        await firebase.database().ref('config').set({
            latest_version_code: 20,
            latest_apk_url: url
        });
        console.log("OTA Published successfully!");
        process.exit(0);
    } catch (e) {
        console.error("Error:", e);
        process.exit(1);
    }
}

uploadFile();
