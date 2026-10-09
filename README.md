# Levi + Levi Admin (one repo, one GitHub Action)

```
Levi/        -> login dialog app  (MainActivity.java + Levi.java, package com.levi.dialog)
LeviAdmin/   -> admin panel app   (package com.levi.admin, contains a copy of Levi.java for the live preview)
.github/workflows/build.yml  -> builds both APKs
```

## Build
1. Create a GitHub repo, upload everything in this folder (keep the `.github` folder), push.
2. Actions tab -> "Build Levi + Levi Admin" -> download artifacts **Levi** and **Levi-Admin**.

## Replace the icon
Put your round PNG as `ic_launcher.png` and `ic_launcher_round.png` (512x512) in
`Levi/app/src/main/res/mipmap-xxhdpi/` and `LeviAdmin/app/src/main/res/mipmap-xxhdpi/`.

## Firebase rules (Realtime Database -> Rules)
```
{ "rules": { ".read": false, ".write": false,
  "levi_apps":  { ".read": true, ".write": true },
  "levi_media": { ".read": true, ".write": true },
  "levi_admin": { ".read": true, ".write": true } } }
```

## Use
1. Open Levi Admin -> paste databaseURL -> create admin key -> + add app.
2. App page shows App Connect Key, Login Key, databaseURL, INTERNET line. Turn on **Dialog Show**, tap **Edit** to design.
3. In the Levi APK (MT Manager) open `classes.dex -> com/levi/dialog/Levi.smali` and replace
   `https://YOUR-PROJECT-default-rtdb.firebaseio.com` and `LV-XXX-XXX-ST`. Make sure the app has the INTERNET permission.

## Data layout
`levi_apps/<connectKey>` = name, desc, date, icon, loginKey, enabled, cfg{...}  |  `levi_media/<connectKey>/{bg,banner}` = data URLs
`levi_admin/hash` = SHA-256 of the admin key
