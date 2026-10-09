package com.levi.admin;

import android.app.AlertDialog;
import android.graphics.Bitmap;
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
            {"Light glass", "{\"grad\":16,\"cField\":\"#5214283C\",\"cFieldB\":\"#EBFFFFFF\",\"cBtn\":\"#61FFFFFF\",\"cTitle\":\"#000000\",\"cBtnT\":\"#000000\",\"cInput\":\"#FFFFFF\",\"gcustom\":false}"},
            {"Midnight", "{\"grad\":5,\"cField\":\"#66000000\",\"cFieldB\":\"#55FFFFFF\",\"cBtn\":\"#33FFFFFF\",\"cTitle\":\"#FFFFFF\",\"cBtnT\":\"#FFFFFF\",\"cInput\":\"#FFFFFF\",\"gcustom\":false}"},
            {"Sunset", "{\"grad\":0,\"cField\":\"#44FFFFFF\",\"cFieldB\":\"#FFFFFFFF\",\"cBtn\":\"#55FFFFFF\",\"cTitle\":\"#FFFFFF\",\"cBtnT\":\"#FFFFFF\",\"cInput\":\"#FFFFFF\",\"gcustom\":false}"},
            {"Mint", "{\"grad\":3,\"cField\":\"#3300594A\",\"cFieldB\":\"#EEFFFFFF\",\"cBtn\":\"#66FFFFFF\",\"cTitle\":\"#0B3D2E\",\"cBtnT\":\"#0B3D2E\",\"cInput\":\"#06352A\",\"gcustom\":false}"},
            {"Neon", "{\"grad\":14,\"bgFx\":17,\"cField\":\"#55000000\",\"cFieldB\":\"#FF2AF598\",\"cBtn\":\"#44000000\",\"cBtnB\":\"#FF2AF598\",\"btnBW\":0.5,\"cTitle\":\"#FFFFFF\",\"cBtnT\":\"#2AF598\",\"cInput\":\"#FFFFFF\",\"gcustom\":false}"},
            {"Mono", "{\"grad\":13,\"cField\":\"#55000000\",\"cFieldB\":\"#CCFFFFFF\",\"cBtn\":\"#44FFFFFF\",\"cTitle\":\"#FFFFFF\",\"cBtnT\":\"#FFFFFF\",\"cInput\":\"#FFFFFF\",\"gcustom\":false}"}};

    public Editor(MainActivity a, String ck) {
        A = a;
        this.ck = ck;
        app = a.apps.optJSONObject(ck);
        JSONObject c = app.optJSONObject("cfg");
        try { cfg = c == null ? new JSONObject() : new JSONObject(c.toString()); } catch (Exception e) { cfg = new JSONObject(); }
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
        float real = Math.min(dm.widthPixels * Levi.F(cfg, "cardW", 76) / 100f, UI.dp(A, 420));
        float natH = real * Levi.F(cfg, "cardH", 100) / 100f;
        float maxH = Math.min(dm.heightPixels * 0.40f, UI.dp(A, 330)) * 0.86f;
        card.scale = Math.min(1f, maxH / natH);
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
        LinearLayout s = sec("Quick themes", true);
        LinearLayout row = UI.row(A);
        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(A);
        hs.setHorizontalScrollBarEnabled(false);
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

        s = sec("Background", true);
        media(s, "bgType", "bgSrc", new String[]{"gradient", "image", "video"}, 1080, "Background");
        s.addView(UI.tv(A, "Gradient preset", 13, UI.tx(), true), UI.lp(A, -2, -2, 0, 10, 0, 6));
        gradGrid(s);
        sw(s, "Custom gradient colours", "gcustom", false);
        clr(s, "Gradient colour 1", "gc1", "#FF9A8B");
        clr(s, "Gradient colour 2", "gc2", "#FF6A88");
        sld(s, "Gradient angle", "gangle", 0, 360, 5, 135, "\u00B0");
        sld(s, "Blur (image background)", "blur", 0, 100, 1, 0, "");
        sld(s, "Noise / grain", "noise", 0, 100, 1, 0, "");
        cho(s, "Background animation", "bgFx", Levi.FX_NAMES, 0, false, 0);
        sld(s, "Animation speed", "bgSpd", 1, 10, 1, 5, "");

        s = sec("Banner", false);
        media(s, "bnType", "bnSrc", new String[]{"image", "video"}, 1100, "Banner");
        sld(s, "Banner corner radius", "bnR", 0, 20, 0.2f, 5.8f, "");

        s = sec("Text", false);
        txt(s, "Title", "title", "To access this you need access key", false);
        sw(s, "Title in CAPITALS", "caps", true);
        txt(s, "Short description (shown on banner)", "desc", "", true);
        txt(s, "Input hint", "hint", "enter your key here. . . . .", false);
        txt(s, "Get Key button text", "getTxt", "Get Key", false);
        txt(s, "Verify button text", "verTxt", "Verify", false);
        txt(s, "Get Key button URL", "getUrl", "", false);

        s = sec("Fonts (per category)", false);
        cho(s, "Title font", "fTitle", fontList(), 0, true, 1);
        cho(s, "Description font", "fDesc", fontList(), -1, true, 1);
        cho(s, "Input font", "fInput", fontList(), -1, true, 1);
        cho(s, "Buttons font", "fBtn", fontList(), -1, true, 1);

        s = sec("Colours (per category)", false);
        clr(s, "Title text", "cTitle", "#000000");
        clr(s, "Description text", "cDesc", "#FFFFFF");
        clr(s, "Input field fill", "cField", "#5214283C");
        clr(s, "Input field border", "cFieldB", "#EBFFFFFF");
        clr(s, "Input text", "cInput", "#FFFFFFFF");
        clr(s, "Button fill", "cBtn", "#61FFFFFF");
        clr(s, "Button border", "cBtnB", "#FFFFFFFF");
        clr(s, "Button text", "cBtnT", "#FF000000");
        clr(s, "Dialog border", "cCardB", "#FFFFFFFF");

        s = sec("Size & shape", false);
        sld(s, "Dialog width (% of screen)", "cardW", 40, 95, 1, 76, "%");
        sld(s, "Dialog height (100 = square)", "cardH", 70, 140, 1, 100, "");
        sld(s, "Dialog corner radius (50 = round)", "radius", 0, 50, 0.5f, 10.4f, "");
        sld(s, "Dialog border width", "cardBW", 0, 3, 0.25f, 0, "");
        sld(s, "Title size", "sTitle", 50, 150, 1, 100, "%");
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
        String[] r = new String[Levi.FONT_NAMES.length + 1];
        r[0] = "System sans";
        System.arraycopy(Levi.FONT_NAMES, 0, r, 1, Levi.FONT_NAMES.length);
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
                    if (fonts && pos > 0) t.setTypeface(Levi.font(A, pos - 1));
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
            A.pick(vid ? "video/*" : "image/*", u -> process(u, vid, maxSide, typeKey, srcKey, st));
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
