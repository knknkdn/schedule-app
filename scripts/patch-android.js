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
fs.copyFileSync(path.join('android-extra', 'ic_stat_schedule.png'), path.join(dir, 'ic_stat_schedule.png'));

// Dark launch colours: no white/system flash before the app's own opening screen.
const resDir = path.join('android', 'app', 'src', 'main', 'res', 'values');
fs.writeFileSync(path.join(resDir, 'jadwal_colors.xml'), '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="jadwal_dark">#0A0D12</color>\n</resources>\n');
fs.writeFileSync(path.join(resDir, 'styles.xml'), `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="AppTheme" parent="Theme.AppCompat.Light.DarkActionBar">
        <item name="colorPrimary">@color/colorPrimary</item>
        <item name="colorPrimaryDark">@color/colorPrimaryDark</item>
        <item name="colorAccent">@color/colorAccent</item>
    </style>

    <style name="AppTheme.NoActionBar" parent="Theme.AppCompat.DayNight.NoActionBar">
        <item name="windowActionBar">false</item>
        <item name="windowNoTitle">true</item>
        <item name="android:background">@null</item>
        <item name="android:windowBackground">@color/jadwal_dark</item>
        <item name="android:statusBarColor">@color/jadwal_dark</item>
        <item name="android:navigationBarColor">@color/jadwal_dark</item>
    </style>

    <style name="AppTheme.NoActionBarLaunch" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">@color/jadwal_dark</item>
        <item name="postSplashScreenTheme">@style/AppTheme.NoActionBar</item>
        <item name="android:background">@drawable/splash</item>
    </style>
</resources>
`);
console.log('Android project patched');
