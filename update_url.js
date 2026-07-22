
const firebase = require('firebase/compat/app');
require('firebase/compat/database');
const firebaseConfig = {
    apiKey: 'AIzaSyD4GZ-DUfvXgqRKiJSbuiH-6R3YAxYTrLU',
    databaseURL: 'https://hogar-finanzas-2026-default-rtdb.firebaseio.com',
};
firebase.initializeApp(firebaseConfig);
async function run() {
    await firebase.database().ref('config/latest_version_code').set(23);
    await firebase.database().ref('config/latest_apk_url').set('https://files.catbox.moe/zvnwsf.apk');
    console.log('done');
    process.exit(0);
}
run();
