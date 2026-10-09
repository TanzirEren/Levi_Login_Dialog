<!--
  GitHub release
  Tag:    v2.0.0
  Title:  🌿 Levi v2.0.0 — 8 Premium Dialog Designs
-->

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=12,20,24&height=190&section=header&text=LEVI%20v2&fontSize=58&fontColor=ffffff&animation=fadeIn&fontAlignY=38&desc=8%20Premium%20Dialog%20Designs%20%C2%B7%20Glass%20Admin&descAlignY=60&descSize=16"/>

[![Typing SVG](https://readme-typing-svg.demolab.com?font=Fira+Code&weight=600&size=20&duration=2800&pause=800&color=12B76A&center=true&vCenter=true&width=640&lines=Login-key+dialog+for+Android+mods;Floating+glass+admin+panel;Auto+video+compression;Animated+gradients+%C2%B7+30+presets)](https://github.com)

![Android](https://img.shields.io/badge/Android-5.0%2B-12b76a?style=for-the-badge&logo=android&logoColor=white)
![Version](https://img.shields.io/badge/version-2.0.0-5eead4?style=for-the-badge)
![Dex](https://img.shields.io/badge/classes.dex-single-0b1f17?style=for-the-badge)

</div>

## ⬇ Download (see **Assets** below)

| | File | Install |
|---|---|---|
| 🔐 **Levi** (dialog) | `Levi-v2.0.0.apk` | Open in **MT Manager** → edit `Levi.smali` |
| 🛰 **Levi Admin** | `Levi-Admin-v2.0.0.apk` | Install on your phone |

> Replace `YOUR_USER/YOUR_REPO` if you copy the direct links:
> `https://github.com/YOUR_USER/YOUR_REPO/releases/download/v2.0.0/Levi-v2.0.0.apk`
> `https://github.com/YOUR_USER/YOUR_REPO/releases/download/v2.0.0/Levi-Admin-v2.0.0.apk`

## ✨ What's new in v2

### 🖼 8 premium dialog designs — each completely different
| | Design | Vibe |
|---|---|---|
| 0 | **Classic Glass** | soft glass card + pill input |
| 1 | **Neon Cyber** | pulsing neon frame, scanlines, corner brackets |
| 2 | **Hero Banner** | full-bleed photo, amber gradient CTA |
| 3 | **Bottom Sheet** | slides up from the bottom, underline input |
| 4 | **iOS Frost** | frosted alert with split action buttons |
| 5 | **Ticket Pass** | punched-notch boarding pass with barcode |
| 6 | **Terminal** | hacker window, `$` prompt, `[ bracket ]` buttons |
| 7 | **Aurora Orb** | glowing orb, breathing rings, gradient glow CTA |

- Pick a design in **Edit → Dialog design** (live mini previews of all 8), then edit **everything**: media, gradients (30 presets + colours from an image), 20 fonts + monospace, per-category colours, shapes, button & input styles, 20 entrance animations, 21 background animations
- New controls: tag line + chip, text alignment, separate **Get Key / Verify** button styles and colours, accent colour, decorations on/off
- v1 configs still work (open as Classic Glass)
- Everything from v1.1 stays: single `classes.dex`, smali placeholders at the top, modal dialog on every screen, glass admin, floating nav, auto video compression

## 🚀 Setup in 4 steps

1. **Firebase** → Realtime Database → copy the `databaseURL` → *Rules*:
   ```json
   { "rules": { ".read": false, ".write": false,
     "levi_apps":  { ".read": true, ".write": true },
     "levi_media": { ".read": true, ".write": true },
     "levi_admin": { ".read": true, ".write": true } } }
   ```
2. **Levi Admin** → paste the databaseURL → create your admin key → **+** add an app
3. Open the app card → copy the **App Connect Key** → turn **Dialog Show** on → **Edit** to design
4. **MT Manager** → `Levi.apk` → `classes.dex` → `Levi.smali` → at the top replace:

   | Field | Replace with |
   |---|---|
   | `APP_ACCESS_KEY` (`LV-XXX-XXX-ST`) | your App Connect Key |
   | `FIREBASE_DATABASE_URL` | your databaseURL |

   Save → **Sign** → install. Give your user the **Login Key** (`LEVI-XXX-XXX-ST`).

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
`v1.1.0` → `v2.0.0`: 8 dialog designs · design picker with live previews · per-button styles · tag line · monospace font · bottom-sheet and ticket layouts · design ornaments (neon frame, scanlines, orb rings, barcode, notches).

<div align="center">

Made with 💚 by **TENIx**

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=12,20,24&height=90&section=footer"/>

</div>
