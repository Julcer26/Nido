const { execSync } = require('child_process');
try {
  const versionCode = 12;
  execSync(`npx firebase database:set /config/latest_version_code -d "${versionCode}" -f`, { stdio: 'inherit' });
  console.log("Version code updated to 11.");
} catch (e) { console.error(e.message); process.exit(1); }
