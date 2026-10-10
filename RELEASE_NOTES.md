<!--
  GitHub release
  Tag:    v2.3.0
  Title:  🌿 Levi v2.3.0 — Multi-User Login Keys
-->

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=12,20,24&height=190&section=header&text=LEVI%20v2.3&fontSize=58&fontColor=ffffff&animation=fadeIn&fontAlignY=38&desc=Multi-User%20Login%20Keys%20%C2%B7%208%20Dialog%20Designs&descAlignY=60&descSize=16"/>

[![Typing SVG](https://readme-typing-svg.demolab.com?font=Fira+Code&weight=600&size=20&duration=2800&pause=800&color=12B76A&center=true&vCenter=true&width=640&lines=Login-key+dialog+for+Android+mods;Many+users,+one+key+per+device;Floating+glass+admin+panel;Auto+video+compression;Animated+gradients+%C2%B7+30+presets)](https://github.com)

![Android](https://img.shields.io/badge/Android-5.0%2B-12b76a?style=for-the-badge&logo=android&logoColor=white)
![Version](https://img.shields.io/badge/version-2.3.0-5eead4?style=for-the-badge)
![Dex](https://img.shields.io/badge/classes.dex-single-0b1f17?style=for-the-badge)

</div>

## ⬇ Download (see **Assets** below)

| | File | Install |
|---|---|---|
| 🔐 **Levi** (dialog) | `Levi-v2.3.0.apk` | Open in **MT Manager** → edit `Levi.smali` |
| 🛰 **Levi Admin** | `Levi-Admin-v2.3.0.apk` | Install on your phone |

> Replace `YOUR_USER/YOUR_REPO` if you copy the direct links:
> `https://github.com/YOUR_USER/YOUR_REPO/releases/download/v2.3.0/Levi-v2.3.0.apk`
> `https://github.com/YOUR_USER/YOUR_REPO/releases/download/v2.3.0/Levi-Admin-v2.3.0.apk`

## ✨ What's new in v2.3.0

### 🔑 Multi-user login keys
Before: one login key per app. Now: **as many keys as you want, one per user**, each with its own name, expiry, device limit and status.

- Tap an app → **Login Keys** page: **＋** generate (user name, quantity 1–50, expiry, devices, prefix, note, draft), **🔍 search**, categories **All · Active · Inactive · Draft · Expired · Deleted**
- **ⓘ** top-right opens the app page (INTERNET permission, databaseURL, App Connect Key, Dialog Show, Edit, Delete) — same as before, without the single login key
- **One key = one device** — the first device that enters a key is bound to it; other devices are refused
- Expiry: 1 day · 7 days · 30 days · 90 days · 1 year · **Never** · **Custom date & time** (auto-filled), optional **countdown from first login**

### ➕ 13 extra features
Bulk generate · tamper-proof server-time expiry · re-check every few minutes (disable/delete/expire = instant lock-out) · devices per key 1–5 + remove/reset · quick extend (+1 d/+7 d/+30 d/+1 y) · share-message template · soft delete + restore · usage stats (logins, first login, last seen, last device) · CSV export · sorting · defaults in Settings · notes · import of the old single key

### ⚠ Before you update
Publish the **new Firebase rules** (two new paths: `levi_keys`, `levi_ping`) — see Setup step 1. Old apps keep working; their single key is accepted until you import it on the Login Keys page.

### Still included
8 premium dialog designs · single `classes.dex` · smali placeholders at the top · modal dialog on every screen · glass admin · auto video compression

## 🚀 Setup in 4 steps

1. **Firebase** → Realtime Database → copy the `databaseURL` → *Rules*:
   ```json
   { "rules": { ".read": false, ".write": false,
     "levi_apps":  { ".read": true, ".write": true },
     "levi_keys":  { ".read": true, ".write": true },
     "levi_ping":  { ".read": true, ".write": true },
     "levi_media": { ".read": true, ".write": true },
     "levi_admin": { ".read": true, ".write": true } } }
   ```
2. **Levi Admin** → paste the databaseURL → create your admin key → **+** add an app
3. Tap the app → **ⓘ** → copy the **App Connect Key**, turn **Dialog Show** on, **Edit** to design → back → **＋** create user keys
4. **MT Manager** → `Levi.apk` → `classes.dex` → `Levi.smali` → at the top replace:

   | Field | Replace with |
   |---|---|
   | `APP_ACCESS_KEY` (`LV-XXX-XXX-ST`) | your App Connect Key |
   | `FIREBASE_DATABASE_URL` | your databaseURL |

   Save → **Sign** → install. In Levi Admin open the app → **＋** and generate a **Login Key** for each user.

For another app, add this at the end of its main Activity `onCreate` (and make sure it has the INTERNET permission):

```smali
invoke-static {p0}, Lcom/levi/dialog/Levi;->show(Landroid/app/Activity;)V
```

## ℹ Good to know
- APKs are signed with the **debug key** (fine for modding/testing).
- Gallery video is stored in the database — limit **6 MB** (larger clips are auto-compressed).
- The login key is checked on the device and the database paths are open: this is a gate for mods, not strong security.
- Dialog re-checks Firebase about every 4 s, so edits appear live.

## 🗒 Full changelog
`v2.0.0` → `v2.3.0`: multi-user login keys · per-key expiry, device lock, status · Login Keys page (search, categories, sort, export, bulk) · server-time expiry · periodic re-check · new Firebase paths `levi_keys` + `levi_ping` · settings defaults for keys.

<div align="center">

Made with 💚 by **TENIx**

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=12,20,24&height=90&section=footer"/>

</div>
