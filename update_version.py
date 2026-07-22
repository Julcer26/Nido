import firebase_admin
from firebase_admin import credentials
from firebase_admin import db
import os

# Initialize Firebase
cred = credentials.Certificate("C:/Users/julio/Downloads/hogar-finanzas-2026-firebase-adminsdk-fbsvc-992db21ea5.json")
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://hogar-finanzas-2026-default-rtdb.firebaseio.com/'
})

ref = db.reference('update_config')
ref.set({
    'latest_version_code': 18,
    'latest_apk_url': 'https://files.catbox.moe/7rvjer.apk'
})
print("Update config written to Firebase successfully.")
