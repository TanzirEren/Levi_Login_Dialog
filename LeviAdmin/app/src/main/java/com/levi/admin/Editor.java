package com.levi.admin;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.widget.ProgressBar;
import java.io.File;
import java.io.FileInputStream;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.levi.dialog.Levi;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class Editor {
    final MainActivity A;
    final String ck;
    final JSONObject app;
    JSONObject cfg;
    Levi.Card card;
    LinearLayout controls;
    boolean dirty = false;
    final Handler h = new Handler(Looper.getMainLooper());
    final HashMap<String, Boolean> open = new HashMap<>();
    final Runnable applyRun = new Runnable() {
        @Override public void run() { fit(); card.apply(cfg); }
    };

    static final String[][] THEMES = {
            {"Light glass", "{\"grad\":16,\"gcustom\":false,\"cField\":\"#5214283C\",\"cFieldB\":\"#EBFFFFFF\",\"cGetBg\":\"#61FFFFFF\",\"cVerBg\":\"#61FFFFFF\",\"cVerBg2\":\"#61FFFFFF\",\"cTitle\":\"#000000\",\"cGetT\":\"#000000\",\"cVerT\":\"#000000\",\"cInput\":\"#FFFFFF\"}"},
            {"Midnight", "{\"grad\":5,\"gcustom\":false,\"cField\":\"#66000000\",\"cFieldB\":\"#55FFFFFF\",\"cGetBg\":\"#33FFFFFF\",\"cVerBg\":\"#33FFFFFF\",\"cVerBg2\":\"#33FFFFFF\",\"cTitle\":\"#FFFFFF\",\"cGetT\":\"#FFFFFF\",\"cVerT\":\"#FFFFFF\",\"cInput\":\"#FFFFFF\"}"},
            {"Sunset", "{\"grad\":0,\"gcustom\":false,\"cField\":\"#44FFFFFF\",\"cFieldB\":\"#FFFFFFFF\",\"cGetBg\":\"#55FFFFFF\",\"cVerBg\":\"#55FFFFFF\",\"cVerBg2\":\"#55FFFFFF\",\"cTitle\":\"#FFFFFF\",\"cGetT\":\"#FFFFFF\",\"cVerT\":\"#FFFFFF\",\"cInput\":\"#FFFFFF\"}"},
            {"Mint", "{\"grad\":3,\"gcustom\":false,\"cField\":\"#3300594A\",\"cFieldB\":\"#EEFFFFFF\",\"cGetBg\":\"#66FFFFFF\",\"cVerBg\":\"#66FFFFFF\",\"cVerBg2\":\"#66FFFFFF\",\"cTitle\":\"#0B3D2E\",\"cGetT\":\"#0B3D2E\",\"cVerT\":\"#0B3D2E\",\"cInput\":\"#06352A\"}"},
            {"Neon green", "{\"grad\":14,\"gcustom\":false,\"cAcc\":\"#2AF598\",\"cField\":\"#55000000\",\"cFieldB\":\"#FF2AF598\",\"cGetBg\":\"#44000000\",\"cVerBg\":\"#FF2AF598\",\"cVerBg2\":\"#FF08AEEA\",\"cTitle\":\"#FFFFFF\",\"cGetT\":\"#2AF598\",\"cVerT\":\"#FF04130C\",\"cInput\":\"#FFFFFF\"}"},
            {"Mono", "{\"grad\":13,\"gcustom\":false,\"cField\":\"#55000000\",\"cFieldB\":\"#CCFFFFFF\",\"cGetBg\":\"#44FFFFFF\",\"cVerBg\":\"#44FFFFFF\",\"cVerBg2\":\"#44FFFFFF\",\"cTitle\":\"#FFFFFF\",\"cGetT\":\"#FFFFFF\",\"cVerT\":\"#FFFFFF\",\"cInput\":\"#FFFFFF\"}"}};

    public Editor(MainActivity a, String ck) {
        A = a;
        this.ck = ck;
        app = a.apps.optJSONObject(ck);
        JSONObject c = app.optJSONObject("cfg");
        try { cfg = c == null ? new JSONObject() : new JSONObject(c.toString()); } catch (Exception e) { cfg = new JSONObject(); }
        if (!cfg.has("design") && cfg.has("cBtn")) {   // v1 config -> map old button colours
            try {
                cfg.put("cGetBg", cfg.get("cBtn")); cfg.put("cVerBg", cfg.get("cBtn")); cfg.put("cVerBg2", cfg.get("cBtn"));
                if (cfg.has("cBtnT")) { cfg.put("cGetT", cfg.get("cBtnT")); cfg.put("cVerT", cfg.get("cBtnT")); }
            } catch (Exception e) { /* ignore */ }
        }
        JSONObject dd = Levi.designDefaults(Levi.designOf(cfg));
        Iterator<String> it = dd.keys();
        while (it.hasNext()) { String k = it.next(); if (!cfg.has(k)) { try { cfg.put(k, dd.get(k)); } catch (Exception e) { /* ignore */ } } }
        Levi.dbBase = a.base;
        Levi.dbCk = ck;
    }

    // ================================================================ screen ===================
    public View view() {
        LinearLayout page = UI.col(A);
        TextView save = UI.btn(A, "Save", true, v -> save());
        final Runnable back = () -> {
            if (!dirty) { A.pop(); return; }
            new AlertDialog.Builder(A).setTitle("Discard changes?").setMessage("You have unsaved edits.")
                    .setPositiveButton("Discard", (d, w) -> A.pop()).setNegativeButton("Keep editing", null).show();
        };
        page.setTag(back);
        page.addView(A.topBar("Edit dialog", back, save));

        DisplayMetrics dm = A.getResources().getDisplayMetrics();
        FrameLayout panel = new FrameLayout(A);
        GradientDrawable pg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, UI.dark ? new int[]{0xFF1B1E27, 0xFF11131A} : new int[]{0xFFE9EBF7, 0xFFDCDFF0});
        panel.setBackground(pg);
        card = new Levi.Card(A);
        card.apply(cfg);
        panel.addView(card, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        TextView replay = UI.tv(A, "\u21BB Preview animation", 11, UI.PRI, true);
        replay.setPadding(UI.dp(A, 12), UI.dp(A, 6), UI.dp(A, 12), UI.dp(A, 6));
        replay.setBackground(UI.rr(A, UI.surf(), 99));
        replay.setOnClickListener(v -> card.playEnter());
        FrameLayout.LayoutParams rl = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
        rl.setMargins(0, 0, UI.dp(A, 10), UI.dp(A, 8));
        panel.addView(replay, rl);
        TextView live = UI.tv(A, "LIVE PREVIEW", 10, UI.sub(), true);
        live.setLetterSpacing(.1f);
        FrameLayout.LayoutParams ll = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        ll.setMargins(UI.dp(A, 14), UI.dp(A, 8), 0, 0);
        panel.addView(live, ll);
        int ph = (int) Math.min(dm.heightPixels * 0.40f, UI.dp(A, 330));
        page.addView(panel, new LinearLayout.LayoutParams(-1, ph));

        ScrollView sv = new ScrollView(A);
        sv.setVerticalScrollBarEnabled(false);
        controls = UI.col(A);
        int p = UI.dp(A, 14);
        controls.setPadding(p, p, p, p * 3);
        sv.addView(controls, new FrameLayout.LayoutParams(-1, -2));
        page.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
        build();
        fit();
        card.post(() -> card.playEnter());
        return page;
    }

    void fit() {
        DisplayMetrics dm = A.getResources().getDisplayMetrics();
        int dz = Levi.designOf(cfg);
        float pct = Levi.F(cfg, "cardW", 76) / 100f;
        float real = dz == 3 ? dm.widthPixels * pct : Math.min(dm.widthPixels * pct, UI.dp(A, 420));
        float natH = real * (Levi.DESIGN_H0[dz] + Levi.F(cfg, "cardH", 100) - 100) / 100f;
        float maxH = Math.min(dm.heightPixels * 0.40f, UI.dp(A, 330)) * 0.86f;
        float maxW = dm.widthPixels - UI.dp(A, 28);
        card.scale = Math.min(1f, Math.min(maxH / natH, maxW / real));
    }

    void applyDesign(int i) {
        JSONObject dd = Levi.designDefaults(i);
        Iterator<String> it = dd.keys();
        while (it.hasNext()) {
            String k = it.next();
            if (k.equals("bgType")) continue;
            try { cfg.put(k, dd.get(k)); } catch (Exception e) { /* ignore */ }
        }
        dirty = true;
        build();
        fit();
        card.apply(cfg);
        card.post(() -> card.playEnter());
    }

    void designPicker(LinearLayout p) {
        p.addView(UI.tv(A, "Pick a layout. Each one comes with its own colours, fonts, shapes and animation - then tweak everything below.", 12, UI.sub(), false), UI.lp(A, -1, -2, 0, 4, 0, 10));
        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(A);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = UI.row(A);
        row.setGravity(Gravity.TOP);
        DisplayMetrics dm = A.getResources().getDisplayMetrics();
        int cur = Levi.designOf(cfg);
        for (int i = 0; i < Levi.DESIGN_NAMES.length; i++) {
            final int idx = i;
            JSONObject dd = Levi.designDefaults(i);
            float real = i == 3 ? dm.widthPixels * Levi.F(dd, "cardW", 100) / 100f : Math.min(dm.widthPixels * Levi.F(dd, "cardW", 76) / 100f, UI.dp(A, 420));
            Levi.Card mini = new Levi.Card(A);
            mini.scale = UI.dp(A, 132) / real;
            JSONObject mc = new JSONObject();
            try { mc.put("design", i); mc.put("animOn", false); mc.put("desc", "Short description"); } catch (Exception e) { /* ignore */ }
            mini.apply(mc);
            FrameLayout box = new FrameLayout(A);
            box.addView(mini, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
            View shield = new View(A);
            box.addView(shield, new FrameLayout.LayoutParams(-1, -1));
            shield.setOnClickListener(v -> applyDesign(idx));
            LinearLayout cell = UI.col(A);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(UI.dp(A, 8), UI.dp(A, 8), UI.dp(A, 8), UI.dp(A, 8));
            cell.setBackground(i == cur ? UI.rrs(A, UI.priC(), UI.PRI, 18) : UI.rr(A, UI.surf2(), 18));
            cell.addView(box, new LinearLayout.LayoutParams(UI.dp(A, 140), UI.dp(A, 150)));
            cell.addView(UI.tv(A, Levi.DESIGN_NAMES[i], 12, i == cur ? UI.PRI : UI.tx(), true), UI.lp(A, -2, -2, 0, 6, 0, 0));
            TextView inf = UI.tv(A, Levi.DESIGN_INFO[i], 9.5f, UI.sub(), false);
            inf.setGravity(Gravity.CENTER);
            cell.addView(inf, new LinearLayout.LayoutParams(UI.dp(A, 140), -2));
            row.addView(cell, UI.lp(A, -2, -2, 0, 0, 10, 0));
        }
        hs.addView(row);
        p.addView(hs);
    }

    void put(String k, Object v) {
        try { cfg.put(k, v); } catch (Exception e) { /* ignore */ }
        dirty = true;
        h.removeCallbacks(applyRun);
        h.postDelayed(applyRun, 40);
    }

    // ================================================================ controls =================
    void build() {
        controls.removeAllViews();
        LinearLayout s = sec("Dialog design", true);
        designPicker(s);

        s = sec("Quick colour themes", false);
        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(A);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = UI.row(A);
        for (final String[] t : THEMES) {
            TextView c = UI.btn(A, t[0], false, v -> {
                try {
                    JSONObject j = new JSONObject(t[1]);
                    Iterator<String> it = j.keys();
                    while (it.hasNext()) { String k = it.next(); cfg.put(k, j.get(k)); }
                    put("bgType", "gradient");
                    build();
                } catch (Exception e) { /* ignore */ }
            });
            row.addView(c, UI.lp(A, -2, -2, 0, 0, 8, 0));
        }
        hs.addView(row);
        s.addView(hs);

        s = sec("Background", false);
        media(s, "bgType", "bgSrc", new String[]{"gradient", "image", "video"}, 1080, "Background");
        s.addView(UI.tv(A, "Gradient preset", 13, UI.tx(), true), UI.lp(A, -2, -2, 0, 10, 0, 6));
        gradGrid(s);
        LinearLayout gr = UI.row(A);
        gr.addView(UI.btn(A, "Colours from gallery image", false, v -> A.pick("image/*", u -> new Thread(() -> {
            Bitmap bm = null;
            try { bm = Levi.decode(A.readUri(u), 256); } catch (Exception e) { /* ignore */ }
            final Bitmap fb = bm;
            h.post(() -> applyExtracted(fb));
        }).start())), UI.lp(A, -2, -2, 0, 0, 8, 0));
        String curSrc = Levi.S(cfg, "bgSrc", "");
        if (!curSrc.isEmpty() && "image".equals(Levi.S(cfg, "bgType", "gradient")))
            gr.addView(UI.btn(A, "From current image", false, v -> Levi.loadBitmap(curSrc, 256, b -> applyExtracted(b))));
        android.widget.HorizontalScrollView gs = new android.widget.HorizontalScrollView(A);
        gs.setHorizontalScrollBarEnabled(false);
        gs.addView(gr);
        s.addView(gs, UI.lp(A, -1, -2, 0, 4, 0, 6));
        sw(s, "Custom gradient colours", "gcustom", false);
        clr(s, "Gradient colour 1", "gc1", "#FF9A8B");
        clr(s, "Gradient colour 2", "gc2", "#FF6A88");
        sld(s, "Gradient angle", "gangle", 0, 360, 5, 135, "\u00B0");
        sld(s, "Blur (image background)", "blur", 0, 100, 1, 0, "");
        sld(s, "Noise / grain", "noise", 0, 100, 1, 0, "");
        cho(s, "Background animation", "bgFx", Levi.FX_NAMES, 0, false, 0);
        sld(s, "Animation speed", "bgSpd", 1, 10, 1, 5, "");

        s = sec("Banner / logo", false);
        media(s, "bnType", "bnSrc", new String[]{"image", "video"}, 1100, "Banner");
        sld(s, "Banner corner radius (50 = circle)", "bnR", 0, 50, 0.5f, 5.8f, "");

        s = sec("Text", false);
        txt(s, "Title", "title", "To access this you need access key", false);
        sw(s, "Title in CAPITALS", "caps", true);
        txt(s, "Tag line (small label)", "tag", "", false);
        sw(s, "Tag as filled chip", "tagChip", false);
        txt(s, "Short description", "desc", "", true);
        sw(s, "Description on a dark chip", "descChip", false);
        txt(s, "Input hint", "hint", "enter your key here. . . . .", false);
        txt(s, "Get Key button text", "getTxt", "Get Key", false);
        txt(s, "Verify button text", "verTxt", "Verify", false);
        txt(s, "Get Key button URL", "getUrl", "", false);

        s = sec("Fonts (per category)", false);
        cho(s, "Title font", "fTitle", fontList(), 0, true, 2);
        cho(s, "Tag font", "fTag", fontList(), -1, true, 2);
        cho(s, "Description font", "fDesc", fontList(), -1, true, 2);
        cho(s, "Input font", "fInput", fontList(), -1, true, 2);
        cho(s, "Buttons font", "fBtn", fontList(), -1, true, 2);

        s = sec("Colours (per category)", false);
        clr(s, "Title text", "cTitle", "#000000");
        clr(s, "Tag text", "cTag", "#12B76A");
        clr(s, "Description text", "cDesc", "#FFFFFF");
        clr(s, "Accent / decorations", "cAcc", "#22E4FF");
        clr(s, "Input field fill", "cField", "#5214283C");
        clr(s, "Input field border", "cFieldB", "#EBFFFFFF");
        clr(s, "Input text", "cInput", "#FFFFFFFF");
        clr(s, "Get Key button fill", "cGetBg", "#61FFFFFF");
        clr(s, "Get Key button text", "cGetT", "#FF000000");
        clr(s, "Get Key button border", "cGetB", "#FFFFFFFF");
        clr(s, "Verify button fill", "cVerBg", "#61FFFFFF");
        clr(s, "Verify gradient end", "cVerBg2", "#61FFFFFF");
        clr(s, "Verify button text", "cVerT", "#FF000000");
        clr(s, "Verify button border", "cVerB", "#FFFFFFFF");
        clr(s, "Dialog border", "cCardB", "#FFFFFFFF");

        s = sec("Layout & style", false);
        cho(s, "Text alignment", "tAlign", new String[]{"Center", "Left"}, 0, false, 0);
        cho(s, "Input style", "fieldStyle", new String[]{"Filled", "Underline", "Dashed", "Terminal", "Neon outline"}, 0, false, 0);
        cho(s, "Get Key button style", "btnStyle", new String[]{"Filled", "Flat text", "Outlined", "Bracket", "Link", "Gradient glow"}, 0, false, 0);
        cho(s, "Verify button style", "btnStyleV", new String[]{"Filled", "Flat text", "Outlined", "Bracket", "Link", "Gradient glow"}, 0, false, 0);
        sw(s, "Design decorations (frames, scanlines, orbs...)", "deco", true);

        s = sec("Size & shape", false);
        sld(s, "Dialog width (% of screen)", "cardW", 40, 100, 1, 76, "%");
        sld(s, "Dialog height (100 = design default)", "cardH", 70, 140, 1, 100, "");
        sld(s, "Dialog corner radius (50 = round)", "radius", 0, 50, 0.5f, 10.4f, "");
        sld(s, "Dialog border width", "cardBW", 0, 3, 0.25f, 0, "");
        sld(s, "Title size", "sTitle", 50, 150, 1, 100, "%");
        sld(s, "Tag size", "sTag", 60, 160, 1, 100, "%");
        sld(s, "Description size", "sDesc", 60, 160, 1, 100, "%");
        sld(s, "Input text size", "sInput", 60, 150, 1, 100, "%");
        sld(s, "Input roundness", "fieldR", 0, 100, 1, 100, "%");
        sld(s, "Input border width", "fieldBW", 0, 3, 0.25f, 0.75f, "");
        sld(s, "Button size", "btnS", 70, 140, 1, 100, "%");
        sld(s, "Button roundness", "btnR", 0, 100, 1, 100, "%");
        sld(s, "Button border width", "btnBW", 0, 2, 0.25f, 0, "");
        sld(s, "Background dim behind dialog", "dim", 0, 90, 1, 60, "%");

        s = sec("Animation", false);
        sw(s, "Animations on (master)", "animOn", true);
        cho(s, "Entrance animation", "enter", Levi.ENTER_NAMES, 8, false, 0);
        sld(s, "Entrance duration (ms)", "enterMs", 150, 1500, 50, 450, "");

        s = sec("Extras", false);
        sw(s, "Show \u2715 exit chip on dialog (Back key always exits)", "showExit", false);
    }

    String[] fontList() {
        String[] r = new String[Levi.FONT_NAMES.length + 2];
        r[0] = "Monospace";
        r[1] = "System sans";
        System.arraycopy(Levi.FONT_NAMES, 0, r, 2, Levi.FONT_NAMES.length);
        return r;
    }

    LinearLayout sec(final String title, boolean def) {
        LinearLayout c = UI.card(A);
        final LinearLayout body = UI.col(A);
        boolean o = open.containsKey(title) ? open.get(title) : def;
        final TextView hd = UI.tv(A, (o ? "\u25BE  " : "\u25B8  ") + title, 15, UI.tx(), true);
        body.setVisibility(o ? View.VISIBLE : View.GONE);
        hd.setOnClickListener(v -> {
            boolean now = body.getVisibility() != View.VISIBLE;
            body.setVisibility(now ? View.VISIBLE : View.GONE);
            if (now && UI.anim) { body.setAlpha(0); body.setTranslationY(-UI.dp(A, 10)); body.animate().alpha(1).translationY(0).setDuration(260).start(); }
            hd.setText((now ? "\u25BE  " : "\u25B8  ") + title);
            open.put(title, now);
        });
        hd.setPadding(0, UI.dp(A, 4), 0, UI.dp(A, 4));
        c.addView(hd);
        c.addView(body);
        controls.addView(c, UI.lp(A, -1, -2, 0, 0, 0, 12));
        return body;
    }

    void txt(LinearLayout p, String label, final String k, String def, boolean multi) {
        p.addView(UI.tv(A, label, 12, UI.sub(), true), UI.lp(A, -2, -2, 0, 10, 0, 4));
        final EditText e = UI.edit(A, def, multi);
        e.setText(Levi.S(cfg, k, def.isEmpty() ? "" : def));
        e.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable ed) { put(k, ed.toString()); }
        });
        p.addView(e);
    }

    void sld(LinearLayout p, String label, final String k, final float min, final float max, final float step, float def, final String unit) {
        float cur = Levi.F(cfg, k, def);
        LinearLayout r = UI.row(A);
        r.addView(UI.tv(A, label, 13, UI.tx(), false), new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView val = UI.tv(A, fmt(cur, step) + unit, 12, UI.PRI, true);
        r.addView(val);
        p.addView(r, UI.lp(A, -1, -2, 0, 12, 0, 0));
        SeekBar sb = new SeekBar(A);
        sb.setMax(Math.round((max - min) / step));
        sb.setProgress(Math.round((cur - min) / step));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int pr, boolean u) {
                float v = min + pr * step;
                val.setText(fmt(v, step) + unit);
                if (u) { if (step >= 1) put(k, Math.round(v)); else put(k, (double) v); }
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        p.addView(sb);
    }

    static String fmt(float v, float step) { return step >= 1 ? String.valueOf(Math.round(v)) : String.format(Locale.US, "%.2f", v); }

    void sw(LinearLayout p, String label, final String k, boolean def) {
        Switch s = new Switch(A);
        s.setText(label);
        s.setTextColor(UI.tx());
        s.setTextSize(13);
        s.setChecked(Levi.B(cfg, k, def));
        s.setOnCheckedChangeListener((b, on) -> put(k, on));
        p.addView(s, UI.lp(A, -1, -2, 0, 12, 0, 0));
    }

    void clr(LinearLayout p, String label, final String k, final String def) {
        final LinearLayout r = UI.row(A);
        r.addView(UI.tv(A, label, 13, UI.tx(), false), new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView hx = UI.tv(A, Levi.S(cfg, k, def), 11, UI.sub(), false);
        hx.setTypeface(Typeface.MONOSPACE);
        r.addView(hx, UI.lp(A, -2, -2, 0, 0, 10, 0));
        final View sw = new View(A);
        r.addView(sw, new LinearLayout.LayoutParams(UI.dp(A, 30), UI.dp(A, 30)));
        paintSwatch(sw, Levi.col(cfg, k, def));
        r.setPadding(0, UI.dp(A, 10), 0, UI.dp(A, 2));
        r.setOnClickListener(v -> UI.colorPicker(A, Levi.col(cfg, k, def), c -> {
            put(k, UI.hex(c));
            hx.setText(UI.hex(c));
            paintSwatch(sw, c);
        }));
        p.addView(r);
    }

    void paintSwatch(View v, int c) {
        GradientDrawable g = UI.rr(A, c, 99);
        g.setStroke(UI.dp(A, 1), UI.line());
        v.setBackground(g);
    }

    void cho(LinearLayout p, String label, final String k, final String[] names, final int def, final boolean fonts, final int off) {
        final LinearLayout r = UI.row(A);
        r.addView(UI.tv(A, label, 13, UI.tx(), false), new LinearLayout.LayoutParams(0, -2, 1f));
        int cur = Levi.I(cfg, k, def);
        final TextView val = UI.tv(A, names[Math.max(0, Math.min(names.length - 1, cur + off))] + "  \u25BE", 13, UI.PRI, true);
        r.addView(val);
        r.setPadding(0, UI.dp(A, 12), 0, UI.dp(A, 4));
        r.setOnClickListener(v -> {
            ArrayAdapter<String> ad = new ArrayAdapter<String>(A, android.R.layout.simple_list_item_1, names) {
                @Override public View getView(int pos, View cv, ViewGroup par) {
                    TextView t = (TextView) super.getView(pos, cv, par);
                    t.setTextSize(fonts ? 20 : 16);
                    if (fonts && pos >= off) t.setTypeface(Levi.font(A, pos - off));
                    else if (fonts && pos == 0) t.setTypeface(Typeface.MONOSPACE);
                    else t.setTypeface(Typeface.DEFAULT);
                    return t;
                }
            };
            new AlertDialog.Builder(A).setTitle(label).setAdapter(ad, (d, which) -> {
                put(k, which - off);
                val.setText(names[which] + "  \u25BE");
                if (k.equals("enter")) h.postDelayed(() -> card.playEnter(), 120);
            }).show();
        });
        p.addView(r);
    }

    void gradGrid(LinearLayout p) {
        LinearLayout line = null;
        for (int i = 0; i < Levi.GRADS.length; i++) {
            if (i % 5 == 0) { line = UI.row(A); p.addView(line, UI.lp(A, -1, -2, 0, 0, 0, 8)); }
            final int idx = i;
            LinearLayout cell = UI.col(A);
            cell.setGravity(Gravity.CENTER);
            View sw = new View(A);
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, Levi.GRADS[i]);
            g.setCornerRadius(UI.dp(A, 12));
            if (!Levi.B(cfg, "gcustom", false) && Levi.I(cfg, "grad", 16) == i) g.setStroke(UI.dp(A, 3), UI.PRI);
            sw.setBackground(g);
            cell.addView(sw, new LinearLayout.LayoutParams(-1, UI.dp(A, 44)));
            cell.addView(UI.tv(A, Levi.GRAD_NAMES[i], 9, UI.sub(), false));
            cell.setOnClickListener(v -> {
                try { cfg.put("gcustom", false); } catch (Exception e) { /* ignore */ }
                put("grad", idx);
                if (Levi.I(cfg, "bgFx", 0) == 0) put("bgFx", 20);
                if (!"image".equals(Levi.S(cfg, "bgType", "gradient")) && !"video".equals(Levi.S(cfg, "bgType", "gradient"))) put("bgType", "gradient");
                build();
            });
            LinearLayout.LayoutParams q = new LinearLayout.LayoutParams(0, -2, 1f);
            q.setMargins(UI.dp(A, 3), 0, UI.dp(A, 3), 0);
            line.addView(cell, q);
        }
    }

    // ================================================================ media ====================
    void media(final LinearLayout p, final String typeKey, final String srcKey, final String[] types, final int maxSide, String name) {
        LinearLayout chips = UI.row(A);
        String cur = Levi.S(cfg, typeKey, types[0]);
        for (final String t : types) {
            TextView c = UI.tv(A, t.substring(0, 1).toUpperCase() + t.substring(1), 12, t.equals(cur) ? 0xFFFFFFFF : UI.PRI, true);
            c.setGravity(Gravity.CENTER);
            c.setPadding(UI.dp(A, 14), UI.dp(A, 8), UI.dp(A, 14), UI.dp(A, 8));
            c.setBackground(UI.rr(A, t.equals(cur) ? UI.PRI : UI.priC(), 99));
            c.setOnClickListener(v -> { put(typeKey, t); build(); });
            chips.addView(c, UI.lp(A, -2, -2, 0, 0, 8, 0));
        }
        p.addView(chips);
        String src = Levi.S(cfg, srcKey, "");
        final TextView st = UI.tv(A, src.isEmpty() ? "No file selected" : (src.startsWith("data:") || src.startsWith("db:") ? "Using gallery file" : src), 11, UI.sub(), false);
        st.setSingleLine(true);
        st.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        p.addView(st, UI.lp(A, -1, -2, 0, 10, 0, 4));
        final EditText url = UI.edit(A, "Paste image / video URL", false);
        if (src.startsWith("http")) url.setText(src);
        url.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) {
                String u = e.toString().trim();
                if (u.startsWith("http")) { put(srcKey, u); st.setText(u); }
            }
        });
        p.addView(url);
        LinearLayout r = UI.row(A);
        r.addView(UI.btn(A, "Gallery", false, v -> {
            final boolean vid = "video".equals(Levi.S(cfg, typeKey, types[0]));
            A.pick(vid ? "video/*" : "image/*", u -> {
                if (vid && UI.compress && sizeOf(u) > (long) UI.maxMb * 1024 * 1024) compress(u, typeKey, srcKey, st, 1);
                else process(u, vid, maxSide, typeKey, srcKey, st);
            });
        }), UI.lp(A, -2, -2, 0, 10, 8, 0));
        r.addView(UI.btn(A, "Clear", false, v -> { put(srcKey, ""); if (typeKey.equals("bgType")) put(typeKey, "gradient"); build(); }), UI.lp(A, -2, -2, 0, 10, 0, 0));
        p.addView(r);
        float d = Levi.F(cfg, "cardH", 100) - 100;
        String rec = typeKey.equals("bnType")
                ? String.format(Locale.US, "Recommended banner: 1000 \u00D7 %d px (width \u00D7 height). Portrait photos/videos are centre-cropped.", Math.round(1000 * (46 + d) / 94.6f))
                : String.format(Locale.US, "Recommended background: 1080 \u00D7 %d px. Gallery images are shrunk automatically; gallery video max 6 MB (use a URL for bigger).", Math.round(1080 * (100 + d) / 100f));
        p.addView(UI.tv(A, rec, 11, UI.sub(), false), UI.lp(A, -1, -2, 0, 8, 0, 0));
    }

    void process(final android.net.Uri u, final boolean vid, final int maxSide, final String typeKey, final String srcKey, final TextView st) {
        st.setText("Reading file...");
        new Thread(() -> {
            String out = null, err = null;
            try {
                byte[] raw = A.readUri(u);
                if (vid) {
                    if (raw.length > 6 * 1024 * 1024) err = "Video is larger than 6 MB - use a URL instead";
                    else {
                        String mt = A.getContentResolver().getType(u);
                        out = "data:" + (mt == null ? "video/mp4" : mt) + ";base64," + Base64.encodeToString(raw, Base64.NO_WRAP);
                    }
                } else {
                    Bitmap b = Levi.decode(raw, maxSide);
                    int m = Math.max(b.getWidth(), b.getHeight());
                    if (m > maxSide) { float f = (float) maxSide / m; b = Bitmap.createScaledBitmap(b, Math.round(b.getWidth() * f), Math.round(b.getHeight() * f), true); }
                    ByteArrayOutputStream o = new ByteArrayOutputStream();
                    b.compress(Bitmap.CompressFormat.JPEG, 82, o);
                    out = "data:image/jpeg;base64," + Base64.encodeToString(o.toByteArray(), Base64.NO_WRAP);
                }
            } catch (Exception e) { err = "Can't read file"; }
            final String fo = out, fe = err;
            h.post(() -> {
                if (fe != null) { st.setText(fe); UI.toast(A, fe); return; }
                put(srcKey, fo);
                if (typeKey.equals("bgType")) put(typeKey, vid ? "video" : "image");
                st.setText("Using gallery file");
            });
        }).start();
    }

    long sizeOf(android.net.Uri u) {
        try {
            android.content.res.AssetFileDescriptor f = A.getContentResolver().openAssetFileDescriptor(u, "r");
            long n = f.getLength();
            f.close();
            return n;
        } catch (Exception e) { return 0; }
    }

    /** Auto-compress a big gallery video, then store it like any other media. pass 1 = normal, 2 = harder retry. */
    void compress(final android.net.Uri u, final String typeKey, final String srcKey, final TextView st, final int pass) {
        final long before = sizeOf(u);
        final long target = (long) (UI.maxMb * 1024L * 1024L * (pass == 1 ? 0.9 : 0.55));
        final ProgressBar pb = new ProgressBar(A, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        final TextView msg = UI.tv(A, "Compressing video...  0%", 14, UI.tx(), false);
        LinearLayout box = UI.col(A);
        int p = UI.dp(A, 20);
        box.setPadding(p, p, p, p);
        box.addView(msg);
        box.addView(pb, UI.lp(A, -1, -2, 0, 12, 0, 0));
        box.addView(UI.tv(A, String.format(Locale.US, "Original %.1f MB \u2192 target under %d MB. Please keep the app open.", before / 1048576f, UI.maxMb), 11, UI.sub(), false), UI.lp(A, -1, -2, 0, 10, 0, 0));
        final AlertDialog dlg = new AlertDialog.Builder(A).setTitle("Video too large").setView(box).setCancelable(false).create();
        dlg.show();
        Compressor.run(A, u, target, new Compressor.Cb() {
            @Override public void progress(int pct) { pb.setProgress(pct); msg.setText("Compressing video...  " + pct + "%"); }
            @Override public void done(final File out, String err) {
                if (err != null || out == null) { dlg.dismiss(); st.setText(err); UI.toast(A, err == null ? "Compression failed" : err); return; }
                msg.setText("Finishing...");
                new Thread(() -> {
                    byte[] raw = null;
                    try {
                        FileInputStream in = new FileInputStream(out);
                        raw = Levi.readAll(in);
                    } catch (Exception e) { /* handled below */ }
                    out.delete();
                    final byte[] fr = raw;
                    h.post(() -> {
                        dlg.dismiss();
                        if (fr == null) { UI.toast(A, "Compression failed"); return; }
                        if (fr.length > 6 * 1024 * 1024) {
                            if (pass == 1) { compress(u, typeKey, srcKey, st, 2); return; }
                            st.setText("Still too large after compression - use a URL");
                            UI.toast(A, "Video is still too large - use a shorter clip or a URL");
                            return;
                        }
                        put(srcKey, "data:video/mp4;base64," + Base64.encodeToString(fr, Base64.NO_WRAP));
                        if (typeKey.equals("bgType")) put(typeKey, "video");
                        st.setText("Using gallery file (compressed)");
                        UI.toast(A, String.format(Locale.US, "Compressed %.1f MB \u2192 %.1f MB", before / 1048576f, fr.length / 1048576f));
                    });
                }).start();
            }
        });
    }

    // ================================================================ gradient colours from an image ===
    static int[] extract(Bitmap b) {
        Bitmap s = Bitmap.createScaledBitmap(b, 32, 32, true);
        float[][] acc = new float[12][4];
        float[] hsv = new float[3];
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
            int c = s.getPixel(x, y);
            Color.colorToHSV(c, hsv);
            float w = 0.05f + hsv[1] * hsv[2];
            if (hsv[2] < 0.12f) w *= 0.2f;
            int bk = ((int) (hsv[0] / 30f)) % 12;
            acc[bk][0] += w; acc[bk][1] += Color.red(c) * w; acc[bk][2] += Color.green(c) * w; acc[bk][3] += Color.blue(c) * w;
        }
        int i1 = 0;
        for (int i = 1; i < 12; i++) if (acc[i][0] > acc[i1][0]) i1 = i;
        int i2 = -1;
        for (int i = 0; i < 12; i++) {
            int d = Math.min(Math.abs(i - i1), 12 - Math.abs(i - i1));
            if (d >= 2 && acc[i][0] > 0.4f && (i2 < 0 || acc[i][0] > acc[i2][0])) i2 = i;
        }
        int c1 = Color.rgb((int) (acc[i1][1] / acc[i1][0]), (int) (acc[i1][2] / acc[i1][0]), (int) (acc[i1][3] / acc[i1][0]));
        int c2;
        if (i2 >= 0) c2 = Color.rgb((int) (acc[i2][1] / acc[i2][0]), (int) (acc[i2][2] / acc[i2][0]), (int) (acc[i2][3] / acc[i2][0]));
        else { Color.colorToHSV(c1, hsv); hsv[2] = Math.max(0.2f, hsv[2] * 0.5f); c2 = Color.HSVToColor(hsv); }
        return new int[]{c1, c2};
    }

    void applyExtracted(Bitmap b) {
        if (b == null) { UI.toast(A, "Can't read image"); return; }
        int[] c = extract(b);
        try { cfg.put("gcustom", true); } catch (Exception e) { /* ignore */ }
        put("gc1", UI.hex(c[0] | 0xFF000000));
        put("gc2", UI.hex(c[1] | 0xFF000000));
        put("bgType", "gradient");
        if (Levi.I(cfg, "bgFx", 0) == 0) put("bgFx", 20);
        UI.toast(A, "Gradient colours taken from image");
        build();
    }

    // ================================================================ save =====================
    void save() {
        UI.toast(A, "Saving...");
        final ArrayList<String[]> ups = new ArrayList<>();
        if (Levi.S(cfg, "bgSrc", "").startsWith("data:")) ups.add(new String[]{"bg", "bgSrc"});
        if (Levi.S(cfg, "bnSrc", "").startsWith("data:")) ups.add(new String[]{"banner", "bnSrc"});
        upload(ups, 0);
    }

    void upload(final ArrayList<String[]> ups, final int i) {
        if (i >= ups.size()) { finish(); return; }
        final String[] u = ups.get(i);
        A.req("PUT", "levi_media/" + ck + "/" + u[0], JSONObject.quote(Levi.S(cfg, u[1], "")), (r, e) -> {
            if (e != null) { UI.toast(A, "Upload failed: " + MainActivity.shortErr(e)); return; }
            try { cfg.put(u[1], "db:" + u[0] + "." + (System.currentTimeMillis() / 1000)); } catch (Exception ex) { /* ignore */ }
            upload(ups, i + 1);
        });
    }

    void finish() {
        A.req("PUT", "levi_apps/" + ck + "/cfg", cfg.toString(), (r, e) -> {
            if (e != null) { UI.toast(A, "Save failed: " + MainActivity.shortErr(e)); return; }
            try { app.put("cfg", new JSONObject(cfg.toString())); } catch (Exception ex) { /* ignore */ }
            dirty = false;
            UI.toast(A, "Saved - dialog syncs within seconds");
            h.removeCallbacks(applyRun);
            h.postDelayed(applyRun, 40);
        });
    }
}
