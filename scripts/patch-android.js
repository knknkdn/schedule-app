// Adds the notification permissions and the notification icon to the generated Android project.
const fs = require('fs');
const path = require('path');

const mf = path.join('android', 'app', 'src', 'main', 'AndroidManifest.xml');
let x = fs.readFileSync(mf, 'utf8');
const perms = [
  ['android.permission.POST_NOTIFICATIONS', '<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />'],
  ['android.permission.USE_EXACT_ALARM', '<uses-permission android:name="android.permission.USE_EXACT_ALARM" />'],
  ['android.permission.SCHEDULE_EXACT_ALARM', '<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" android:maxSdkVersion="32" />'],
  ['android.permission.RECEIVE_BOOT_COMPLETED', '<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />'],
];
perms.forEach(([name, tag]) => {
  if (!x.includes(name)) x = x.replace('</manifest>', '    ' + tag + '\n</manifest>');
});
fs.writeFileSync(mf, x);

const dir = path.join('android', 'app', 'src', 'main', 'res', 'drawable');
fs.mkdirSync(dir, { recursive: true });
fs.copyFileSync(path.join('android-extra', 'ic_stat_schedule.xml'), path.join(dir, 'ic_stat_schedule.xml'));
console.log('Android project patched');
