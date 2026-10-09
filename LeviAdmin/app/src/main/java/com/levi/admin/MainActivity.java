package com.levi.admin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.levi.dialog.Levi;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    public static MainActivity A;
    static boolean session = false;

    public SharedPreferences sp;
    public FrameLayout root;
    public String base = "";
    public JSONObject apps = new JSONObject();
    final ArrayList<View> stack = new ArrayList<>();
    final Handler h = new Handler(Looper.getMainLooper());
    final HashMap<String, Bitmap> iconCache = new HashMap<>();

    FrameLayout homeContent;
    LinearLayout navBar;
    View fab;
    int tab = 0;

    public interface PickCb { void got(Uri u); }
    PickCb pend;

    static final String PERM = "<uses-permission android:name=\"android.permission.INTERNET\"/>";
    static final String RULES = "{\n  \"rules\": {\n    \".read\": false,\n    \".write\": false,\n    \"levi_apps\":  { \".read\": true, \".write\": true },\n    \"levi_media\": { \".read\": true, \".write\": true },\n    \"levi_admin\": { \".read\": true, \".write\": true }\n  }\n}";

    // =============================================================== lifecycle =================
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        A = this;
        sp = getSharedPreferences("levi_admin", 0);
        UI.dark = sp.getBoolean("dark", false);
        UI.anim = sp.getBoolean("anim", true);
        base = sp.getString("db", "");
        Levi.dbBase = base;
        root = new FrameLayout(this);
        setContentView(root);
        theme();
        if (base.isEmpty()) push(connectScreen());
        else if (session) push(home());
        else gate();
    }

    void theme() {
        root.setBackgroundColor(UI.bg());
        getWindow().setStatusBarColor(UI.bg());
        getWindow().setNavigationBarColor(UI.bg());
        getWindow().getDecorView().setSystemUiVisibility(UI.dark ? 0 : (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | 0x10));
    }

    @Override public void onBackPressed() {
        if (stack.size() <= 1) { super.onBackPressed(); return; }
        View top = stack.get(stack.size() - 1);
        if (top.getTag() instanceof Runnable) ((Runnable) top.getTag()).run();
        else pop();
    }

    @Override protected void onActivityResult(int rq, int rs, Intent d) {
        super.onActivityResult(rq, rs, d);
        if (rq == 77 && rs == RESULT_OK && d != null && d.getData() != null && pend != null) { PickCb c = pend; pend = null; c.got(d.getData()); }
    }

    public void pick(String mime, PickCb cb) {
        pend = cb;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(mime);
        startActivityForResult(i, 77);
    }

    public byte[] readUri(Uri u) throws Exception {
        InputStream in = getContentResolver().openInputStream(u);
        return Levi.readAll(in);
    }

    // =============================================================== navigation ===============
    public void push(final View v) {
        v.setBackgroundColor(UI.bg());
        root.addView(v, new FrameLayout.LayoutParams(-1, -1));
        stack.add(v);
        if (UI.anim && stack.size() > 1) {
            v.setTranslationX(root.getWidth() > 0 ? root.getWidth() : 1000);
            v.setAlpha(.6f);
            v.animate().translationX(0).alpha(1).setDuration(280).start();
        }
    }

    public void pop() {
        if (stack.size() <= 1) return;
        final View v = stack.remove(stack.size() - 1);
        if (UI.anim) v.animate().translationX(root.getWidth()).alpha(.5f).setDuration(220).withEndAction(() -> root.removeView(v)).start();
        else root.removeView(v);
    }

    public void replace(View v) {
        root.removeAllViews();
        stack.clear();
        push(v);
    }

    void refreshTheme() {
        theme();
        replace(home());
    }

    // =============================================================== api =======================
    public String api(String path) { return base + "/" + path + ".json"; }
    public void req(String m, String path, String body, Levi.Res cb) { Levi.async(m, api(path), body, cb); }

    public void loadApps(final Runnable done) {
        req("GET", "levi_apps", null, (r, e) -> {
            try { apps = (r == null || r.equals("null")) ? new JSONObject() : new JSONObject(r); } catch (Exception ex) { apps = new JSONObject(); }
            if (e != null) UI.toast(this, "Offline: " + shortErr(e));
            done.run();
        });
    }

    static String shortErr(String e) { return e.length() > 80 ? e.substring(0, 80) : e; }

    List<String> sortedKeys() {
        final ArrayList<String> ks = new ArrayList<>();
        Iterator<String> it = apps.keys();
        while (it.hasNext()) ks.add(it.next());
        Collections.sort(ks, (a, b) -> Long.compare(apps.optJSONObject(b) == null ? 0 : apps.optJSONObject(b).optLong("created", 0),
                apps.optJSONObject(a) == null ? 0 : apps.optJSONObject(a).optLong("created", 0)));
        return ks;
    }

    // =============================================================== shared widgets ============
    View hero(String title, String sub) {
        LinearLayout l = UI.col(this);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.setPadding(0, UI.dp(this, 36), 0, UI.dp(this, 18));
        TextView logo = UI.tv(this, "L", 34, 0xFFFFFFFF, true);
        logo.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{UI.PRI, UI.PRI2});
        g.setShape(GradientDrawable.OVAL);
        logo.setBackground(g);
        logo.setElevation(UI.dp(this, 6));
        l.addView(logo, new LinearLayout.LayoutParams(UI.dp(this, 78), UI.dp(this, 78)));
        l.addView(UI.tv(this, title, 24, UI.tx(), true), UI.lp(this, -2, -2, 0, 16, 0, 2));
        TextView s = UI.tv(this, sub, 13, UI.sub(), false);
        s.setGravity(Gravity.CENTER);
        l.addView(s);
        if (UI.anim) { logo.setScaleX(.2f); logo.setScaleY(.2f); logo.animate().scaleX(1).scaleY(1).setDuration(520).setInterpolator(new android.view.animation.OvershootInterpolator(2.4f)).start(); }
        return l;
    }

    ScrollView scroll(LinearLayout c) {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        int p = UI.dp(this, 18);
        c.setPadding(p, p, p, p);
        sv.addView(c, new FrameLayout.LayoutParams(-1, -2));
        return sv;
    }

    View topBar(String title, final Runnable back, View right) {
        LinearLayout r = UI.row(this);
        r.setPadding(UI.dp(this, 8), UI.dp(this, 8), UI.dp(this, 14), UI.dp(this, 8));
        TextView b = UI.tv(this, "\u2190", 22, UI.tx(), true);
        b.setGravity(Gravity.CENTER);
        b.setOnClickListener(v -> back.run());
        r.addView(b, new LinearLayout.LayoutParams(UI.dp(this, 46), UI.dp(this, 46)));
        r.addView(UI.tv(this, title, 18, UI.tx(), true), new LinearLayout.LayoutParams(0, -2, 1f));
        if (right != null) r.addView(right);
        return r;
    }

    TextView label(String s) { TextView t = UI.tv(this, s.toUpperCase(), 11, UI.sub(), true); t.setLetterSpacing(.08f); return t; }

    View copyRow(String title, String value, boolean mono) {
        final String val = value;
        LinearLayout c = UI.card(this);
        c.addView(label(title));
        LinearLayout r = UI.row(this);
        TextView v = UI.tv(this, value, mono ? 13 : 14, UI.tx(), !mono);
        if (mono) v.setTypeface(Typeface.MONOSPACE);
        v.setTextIsSelectable(true);
        r.addView(v, new LinearLayout.LayoutParams(0, -2, 1f));
        r.addView(UI.btn(this, "Copy", false, x -> UI.copy(this, val)));
        c.addView(r, UI.lp(this, -1, -2, 0, 8, 0, 0));
        return c;
    }

    public View iconView(String ck, JSONObject app, int sizeDp) {
        int px = UI.dp(this, sizeDp);
        FrameLayout f = new FrameLayout(this);
        final float rad = px * 0.28f;
        f.setClipToOutline(true);
        f.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), rad); }
        });
        String ic = app.optString("icon", "");
        Bitmap bm = null;
        if (!ic.isEmpty()) {
            String k = ck + ic.length();
            bm = iconCache.get(k);
            if (bm == null) { try { bm = Levi.decode(Levi.bytes(ic), 256); iconCache.put(k, bm); } catch (Exception e) { bm = null; } }
        }
        if (bm != null) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(bm);
            f.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        } else {
            TextView t = UI.tv(this, app.optString("name", "?").length() > 0 ? app.optString("name", "?").substring(0, 1).toUpperCase() : "?", sizeDp / 2.2f, 0xFFFFFFFF, true);
            t.setGravity(Gravity.CENTER);
            t.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{UI.PRI, UI.PRI2}));
            f.addView(t, new FrameLayout.LayoutParams(-1, -1));
        }
        f.setLayoutParams(new LinearLayout.LayoutParams(px, px));
        return f;
    }

    TextView chip(String s, boolean on) {
        TextView t = UI.tv(this, s, 11, on ? UI.GRN : UI.sub(), true);
        t.setPadding(UI.dp(this, 10), UI.dp(this, 4), UI.dp(this, 10), UI.dp(this, 4));
        t.setBackground(UI.rr(this, on ? (UI.dark ? 0xFF16382A : 0xFFDDF5E8) : UI.surf2(), 99));
        return t;
    }

    View appRow(final String ck, JSONObject app, int idx) {
        LinearLayout c = UI.card(this);
        c.setOrientation(LinearLayout.HORIZONTAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.addView(iconView(ck, app, 52));
        LinearLayout m = UI.col(this);
        m.addView(UI.tv(this, app.optString("name", "App"), 16, UI.tx(), true));
        TextView d = UI.tv(this, app.optString("desc", ""), 12, UI.sub(), false);
        d.setSingleLine(true);
        d.setEllipsize(android.text.TextUtils.TruncateAt.END);
        m.addView(d);
        m.addView(UI.tv(this, app.optString("date", ""), 11, UI.sub(), false));
        c.addView(m, UI.lp(this, 0, -2, 14, 0, 8, 0));
        ((LinearLayout.LayoutParams) m.getLayoutParams()).weight = 1f;
        c.addView(chip(app.optBoolean("enabled", false) ? "LIVE" : "OFF", app.optBoolean("enabled", false)));
        c.setOnClickListener(v -> push(detail(ck)));
        UI.press(c);
        UI.pop(c, idx);
        c.setLayoutParams(UI.lp(this, -1, -2, 0, 0, 0, 12));
        return c;
    }

    // =============================================================== connect ===================
    View connectScreen() {
        LinearLayout c = UI.col(this);
        c.addView(hero("Levi Admin", "Connect your Firebase Realtime Database"));
        LinearLayout card = UI.card(this);
        card.addView(label("Firebase databaseURL"));
        final EditText et = UI.edit(this, "https://your-project-default-rtdb.firebaseio.com", false);
        card.addView(et, UI.lp(this, -1, -2, 0, 8, 0, 8));
        card.addView(UI.tv(this, "Firebase console \u2192 Realtime Database \u2192 copy the URL shown at the top. Rules: see Guide tab after login.", 12, UI.sub(), false));
        final TextView err = UI.tv(this, "", 12, UI.RED, false);
        card.addView(err, UI.lp(this, -2, -2, 0, 8, 0, 0));
        final TextView go = UI.btn(this, "Connect", true, null);
        go.setOnClickListener(v -> {
            String u = et.getText().toString().trim().replaceAll("\\.json.*$", "").replaceAll("/+$", "");
            if (!u.startsWith("http")) u = "https://" + u;
            if (u.length() < 14 || !u.contains(".")) { err.setText("Enter a valid databaseURL"); return; }
            final String fu = u;
            err.setText("");
            go.setText("Connecting...");
            Levi.async("GET", fu + "/levi_admin.json", null, (r, e) -> {
                go.setText("Connect");
                if (e != null) { err.setText(e.contains("401") || e.contains("403") ? "Permission denied - set the database rules (see Guide)." : "Can't reach database: " + shortErr(e)); return; }
                base = fu;
                Levi.dbBase = fu;
                sp.edit().putString("db", fu).apply();
                gate();
            });
        });
        card.addView(go, UI.lp(this, -1, -2, 0, 14, 0, 0));
        c.addView(card);
        UI.pop(card, 1);
        return scroll(c);
    }

    // =============================================================== key gate ==================
    void gate() {
        FrameLayout w = new FrameLayout(this);
        w.addView(UI.tv(this, "Connecting...", 14, UI.sub(), false), new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        replace(w);
        req("GET", "levi_admin/hash", null, (r, e) -> {
            if (e != null) { UI.toast(this, "Can't reach database"); replace(connectScreen()); return; }
            String hash = Levi.unq(r);
            if (!hash.isEmpty() && sp.getBoolean("remember", false) && hash.equals(sp.getString("rhash", ""))) { session = true; replace(home()); return; }
            replace(keyScreen(hash));
        });
    }

    View keyScreen(final String hash) {
        final boolean create = hash.isEmpty();
        LinearLayout c = UI.col(this);
        c.addView(hero(create ? "Create admin key" : "Welcome back", create ? "This key protects your admin panel" : "Enter your admin key to continue"));
        LinearLayout card = UI.card(this);
        card.addView(label(create ? "New admin key" : "Admin key"));
        final UI.Pw pw = new UI.Pw(this, create ? "choose a key (min 4 chars)" : "enter admin key");
        card.addView(pw, UI.lp(this, -1, -2, 0, 8, 0, 8));
        if (create) card.addView(UI.btn(this, "Generate strong key", false, v -> { pw.et.setText(Levi.randKey("ADM")); }), UI.lp(this, -2, -2, 0, 0, 0, 8));
        final Switch rem = new Switch(this);
        rem.setText("Stay signed in");
        rem.setTextColor(UI.sub());
        rem.setChecked(sp.getBoolean("remember", false));
        card.addView(rem, UI.lp(this, -1, -2, 0, 4, 0, 4));
        final TextView err = UI.tv(this, "", 12, UI.RED, false);
        card.addView(err);
        final TextView go = UI.btn(this, create ? "Create & enter" : "Unlock", true, null);
        go.setOnClickListener(v -> {
            final String k = pw.get();
            if (create) {
                if (k.length() < 4) { err.setText("Key must be at least 4 characters"); return; }
                final String hs = Levi.sha256(k + "|levi");
                go.setText("Saving...");
                req("PUT", "levi_admin/hash", JSONObject.quote(hs), (r, e) -> {
                    go.setText("Create & enter");
                    if (e != null) { err.setText("Failed: " + shortErr(e)); return; }
                    sp.edit().putBoolean("remember", rem.isChecked()).putString("rhash", hs).apply();
                    session = true;
                    UI.toast(this, "Admin key created - keep it safe");
                    replace(home());
                });
            } else {
                String hs = Levi.sha256(k + "|levi");
                if (!hs.equals(hash)) { err.setText("Wrong admin key"); card.animate().translationX(0).start(); shake(card); return; }
                sp.edit().putBoolean("remember", rem.isChecked()).putString("rhash", hs).apply();
                session = true;
                replace(home());
            }
        });
        card.addView(go, UI.lp(this, -1, -2, 0, 10, 0, 0));
        c.addView(card);
        TextView chg = UI.tv(this, "Use a different database", 13, UI.PRI, true);
        chg.setGravity(Gravity.CENTER);
        chg.setPadding(0, UI.dp(this, 18), 0, 0);
        chg.setOnClickListener(v -> { sp.edit().remove("db").apply(); base = ""; replace(connectScreen()); });
        c.addView(chg);
        UI.pop(card, 1);
        return scroll(c);
    }

    void shake(View v) { android.animation.ObjectAnimator.ofFloat(v, "translationX", 0, -16, 16, -10, 10, -4, 4, 0).setDuration(360).start(); }

    // =============================================================== home ======================
    View home() {
        FrameLayout outer = new FrameLayout(this);
        LinearLayout page = UI.col(this);
        homeContent = new FrameLayout(this);
        page.addView(homeContent, new LinearLayout.LayoutParams(-1, 0, 1f));
        navBar = UI.row(this);
        navBar.setBackgroundColor(UI.surf());
        navBar.setElevation(UI.dp(this, 8));
        String[] ic = {"\u2302", "\u25A4", "\u2754", "\u2699"};
        String[] nm = {"Home", "Apps", "Guide", "Settings"};
        for (int i = 0; i < 4; i++) {
            final int k = i;
            LinearLayout t = UI.col(this);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, UI.dp(this, 9), 0, UI.dp(this, 9));
            t.addView(UI.tv(this, ic[i], 20, UI.sub(), false));
            t.addView(UI.tv(this, nm[i], 11, UI.sub(), true));
            t.setOnClickListener(v -> selectTab(k));
            navBar.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        page.addView(navBar, new LinearLayout.LayoutParams(-1, -2));
        outer.addView(page, new FrameLayout.LayoutParams(-1, -1));
        TextView f = UI.tv(this, "+", 30, 0xFFFFFFFF, false);
        f.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{UI.PRI, UI.PRI2});
        g.setShape(GradientDrawable.OVAL);
        f.setBackground(g);
        f.setElevation(UI.dp(this, 8));
        f.setOnClickListener(v -> addDialog());
        UI.press(f);
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(UI.dp(this, 60), UI.dp(this, 60), Gravity.BOTTOM | Gravity.END);
        fl.setMargins(0, 0, UI.dp(this, 20), UI.dp(this, 84));
        outer.addView(f, fl);
        fab = f;
        if (UI.anim) { f.setScaleX(0); f.setScaleY(0); f.animate().scaleX(1).scaleY(1).setStartDelay(300).setDuration(420).setInterpolator(new android.view.animation.OvershootInterpolator(3f)).start(); }
        selectTab(tab);
        return outer;
    }

    void selectTab(final int i) {
        tab = i;
        for (int k = 0; k < navBar.getChildCount(); k++) {
            LinearLayout t = (LinearLayout) navBar.getChildAt(k);
            for (int j = 0; j < t.getChildCount(); j++) ((TextView) t.getChildAt(j)).setTextColor(k == i ? UI.PRI : UI.sub());
        }
        fab.setVisibility(i <= 1 ? View.VISIBLE : View.GONE);
        homeContent.removeAllViews();
        if (i == 2) { show(guide()); return; }
        if (i == 3) { show(settings()); return; }
        TextView ld = UI.tv(this, "Loading...", 13, UI.sub(), false);
        homeContent.addView(ld, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        loadApps(() -> { if (tab != i) return; homeContent.removeAllViews(); show(i == 0 ? dashboard() : appsTab()); });
    }

    void show(View v) {
        homeContent.addView(v, new FrameLayout.LayoutParams(-1, -1));
        if (UI.anim) { v.setAlpha(0); v.animate().alpha(1).setDuration(220).start(); }
    }

    void refreshHome() { if (homeContent != null) selectTab(tab); }

    // ---------------------------------------------------------------- dashboard ---------------
    View dashboard() {
        LinearLayout c = UI.col(this);
        LinearLayout hero = UI.col(this);
        hero.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{UI.PRI, UI.PRI2}) {{ setCornerRadius(UI.dp(MainActivity.this, 28)); }});
        hero.setPadding(UI.dp(this, 22), UI.dp(this, 26), UI.dp(this, 22), UI.dp(this, 26));
        hero.addView(UI.tv(this, "Dashboard", 12, 0xCCFFFFFF, true));
        hero.addView(UI.tv(this, "Levi Admin", 28, 0xFFFFFFFF, true));
        hero.addView(UI.tv(this, base.replace("https://", ""), 12, 0xCCFFFFFF, false), UI.lp(this, -2, -2, 0, 6, 0, 0));
        c.addView(hero, UI.lp(this, -1, -2, 0, 0, 0, 16));
        UI.pop(hero, 0);
        int total = apps.length(), on = 0;
        Iterator<String> it = apps.keys();
        while (it.hasNext()) { JSONObject a = apps.optJSONObject(it.next()); if (a != null && a.optBoolean("enabled", false)) on++; }
        LinearLayout r1 = UI.row(this), r2 = UI.row(this);
        r1.addView(stat("Total apps", String.valueOf(total), UI.PRI), UI.lp(this, 0, -2, 0, 0, 6, 12));
        r1.addView(stat("Dialog ON", String.valueOf(on), UI.GRN), UI.lp(this, 0, -2, 6, 0, 0, 12));
        final TextView ping = UI.tv(this, "tap", 24, UI.PRI2, true);
        r2.addView(stat("Dialog OFF", String.valueOf(total - on), UI.sub()), UI.lp(this, 0, -2, 0, 0, 6, 12));
        LinearLayout pc = UI.card(this);
        pc.addView(label("DB latency"));
        pc.addView(ping);
        pc.setOnClickListener(v -> doPing(ping));
        r2.addView(pc, UI.lp(this, 0, -2, 6, 0, 0, 12));
        for (LinearLayout r : new LinearLayout[]{r1, r2}) for (int i = 0; i < 2; i++) ((LinearLayout.LayoutParams) r.getChildAt(i).getLayoutParams()).weight = 1f;
        c.addView(r1);
        c.addView(r2);
        doPing(ping);
        UI.pop(r1, 1);
        UI.pop(r2, 2);
        TextView rt = UI.tv(this, "Recent apps", 16, UI.tx(), true);
        c.addView(rt, UI.lp(this, -2, -2, 2, 6, 0, 10));
        List<String> ks = sortedKeys();
        if (ks.isEmpty()) {
            LinearLayout e = UI.card(this);
            e.setGravity(Gravity.CENTER_HORIZONTAL);
            e.addView(UI.tv(this, "No apps yet", 16, UI.tx(), true));
            e.addView(UI.tv(this, "Add your first app to get a Connect Key and Login Key.", 12, UI.sub(), false));
            e.addView(UI.btn(this, "Add app", true, v -> addDialog()), UI.lp(this, -2, -2, 0, 12, 0, 0));
            c.addView(e);
        }
        for (int i = 0; i < Math.min(3, ks.size()); i++) c.addView(appRow(ks.get(i), apps.optJSONObject(ks.get(i)), i + 3));
        c.addView(UI.space(this, 70));
        return scroll(c);
    }

    View stat(String name, String val, int color) {
        LinearLayout c = UI.card(this);
        c.addView(label(name));
        c.addView(UI.tv(this, val, 28, color, true));
        return c;
    }

    void doPing(final TextView out) {
        final long t0 = System.currentTimeMillis();
        out.setText("...");
        req("GET", "levi_admin/hash", null, (r, e) -> out.setText(e != null ? "offline" : (System.currentTimeMillis() - t0) + " ms"));
    }

    // ---------------------------------------------------------------- apps tab ----------------
    View appsTab() {
        LinearLayout c = UI.col(this);
        c.addView(UI.tv(this, "Your apps", 24, UI.tx(), true));
        final EditText q = UI.edit(this, "Search apps", false);
        c.addView(q, UI.lp(this, -1, -2, 0, 12, 0, 14));
        final LinearLayout list = UI.col(this);
        c.addView(list);
        c.addView(UI.space(this, 90));
        final Runnable fill = () -> {
            list.removeAllViews();
            String f = q.getText().toString().toLowerCase().trim();
            int n = 0;
            for (String k : sortedKeys()) {
                JSONObject a = apps.optJSONObject(k);
                if (a == null) continue;
                if (!f.isEmpty() && !(a.optString("name", "") + a.optString("desc", "") + k).toLowerCase().contains(f)) continue;
                list.addView(appRow(k, a, n++));
            }
            if (n == 0) {
                TextView t = UI.tv(this, apps.length() == 0 ? "No apps yet - tap + to add one." : "No match.", 14, UI.sub(), false);
                t.setGravity(Gravity.CENTER);
                t.setPadding(0, UI.dp(this, 40), 0, 0);
                list.addView(t);
            }
        };
        q.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) { fill.run(); }
            @Override public void afterTextChanged(Editable e) { }
        });
        fill.run();
        return scroll(c);
    }

    // ---------------------------------------------------------------- add app -----------------
    public static String dataIcon(Bitmap b) {
        int s = Math.min(b.getWidth(), b.getHeight());
        Bitmap sq = Bitmap.createBitmap(b, (b.getWidth() - s) / 2, (b.getHeight() - s) / 2, s, s);
        sq = Bitmap.createScaledBitmap(sq, 128, 128, true);
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        sq.compress(Bitmap.CompressFormat.JPEG, 88, o);
        return "data:image/jpeg;base64," + Base64.encodeToString(o.toByteArray(), Base64.NO_WRAP);
    }

    void addDialog() {
        LinearLayout v = UI.col(this);
        int p = UI.dp(this, 20);
        v.setPadding(p, p, p, 0);
        final EditText name = UI.edit(this, "App name", false);
        final EditText desc = UI.edit(this, "Short detail", true);
        final EditText url = UI.edit(this, "Icon URL (optional)", false);
        final String[] icon = {""};
        final ImageView prev = new ImageView(this);
        prev.setScaleType(ImageView.ScaleType.CENTER_CROP);
        prev.setBackground(UI.rr(this, UI.surf2(), 16));
        prev.setClipToOutline(true);
        final String date = new SimpleDateFormat("dd MMM yyyy", Locale.US).format(new Date());
        v.addView(name);
        v.addView(desc, UI.lp(this, -1, -2, 0, 10, 0, 0));
        LinearLayout ir = UI.row(this);
        ir.addView(prev, new LinearLayout.LayoutParams(UI.dp(this, 60), UI.dp(this, 60)));
        ir.addView(UI.btn(this, "Gallery icon", false, x -> pick("image/*", u -> {
            try { Bitmap b = Levi.decode(readUri(u), 512); icon[0] = dataIcon(b); prev.setImageBitmap(b); } catch (Exception ex) { UI.toast(this, "Can't read image"); }
        })), UI.lp(this, -2, -2, 14, 0, 0, 0));
        v.addView(ir, UI.lp(this, -1, -2, 0, 10, 0, 0));
        v.addView(url, UI.lp(this, -1, -2, 0, 10, 0, 0));
        v.addView(UI.tv(this, "Date: " + date + " (auto)", 12, UI.sub(), false), UI.lp(this, -2, -2, 2, 10, 0, 6));
        final AlertDialog d = new AlertDialog.Builder(this).setTitle("Add app").setView(v)
                .setPositiveButton("Add", null).setNegativeButton("Cancel", null).create();
        d.show();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x -> {
            final String nm = name.getText().toString().trim();
            if (nm.isEmpty()) { UI.toast(this, "Enter app name"); return; }
            final String ds = desc.getText().toString().trim(), us = url.getText().toString().trim();
            d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            if (icon[0].isEmpty() && !us.isEmpty()) {
                Levi.loadBitmap(us, 512, b -> { if (b != null) icon[0] = dataIcon(b); else UI.toast(this, "Icon URL failed - continuing without"); createApp(nm, ds, icon[0], date, d); });
            } else createApp(nm, ds, icon[0], date, d);
        });
    }

    void createApp(String nm, String ds, String icon, String date, final AlertDialog d) {
        try {
            final String ck = Levi.randKey("LV");
            JSONObject a = new JSONObject();
            a.put("name", nm).put("desc", ds).put("date", date).put("icon", icon).put("loginKey", Levi.randKey("LEVI"))
                    .put("enabled", false).put("created", System.currentTimeMillis());
            a.put("cfg", new JSONObject().put("bgType", "gradient").put("grad", 16).put("enter", 8));
            final JSONObject fa = a;
            req("PUT", "levi_apps/" + ck, a.toString(), (r, e) -> {
                if (e != null) { UI.toast(this, "Failed: " + shortErr(e)); d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); return; }
                d.dismiss();
                try { apps.put(ck, fa); } catch (Exception ex) { /* ignore */ }
                UI.toast(this, "App added");
                refreshHome();
            });
        } catch (Exception ex) { UI.toast(this, "Error"); }
    }

    // =============================================================== detail ====================
    View detail(final String ck) {
        final JSONObject app = apps.optJSONObject(ck);
        LinearLayout page = UI.col(this);
        TextView edit = UI.btn(this, "\u270E Edit", true, v -> push(new Editor(this, ck).view()));
        page.addView(topBar("App", this::pop, edit));
        LinearLayout c = UI.col(this);
        LinearLayout hc = UI.card(this);
        hc.setGravity(Gravity.CENTER_HORIZONTAL);
        hc.addView(iconView(ck, app, 84));
        hc.addView(UI.tv(this, app.optString("name"), 20, UI.tx(), true), UI.lp(this, -2, -2, 0, 12, 0, 2));
        TextView ds = UI.tv(this, app.optString("desc"), 13, UI.sub(), false);
        ds.setGravity(Gravity.CENTER);
        hc.addView(ds);
        hc.addView(UI.tv(this, "Added " + app.optString("date"), 11, UI.sub(), false), UI.lp(this, -2, -2, 0, 6, 0, 0));
        c.addView(hc, UI.lp(this, -1, -2, 0, 0, 0, 12));
        UI.pop(hc, 0);
        View[] rows = {
                copyRow("App Connect Key", ck, true),
                loginRow(ck, app),
                copyRow("Firebase databaseURL", base, true),
                copyRow("INTERNET permission", PERM, true)};
        for (int i = 0; i < rows.length; i++) { c.addView(rows[i], UI.lp(this, -1, -2, 0, 0, 0, 12)); UI.pop(rows[i], i + 1); }

        LinearLayout tg = UI.card(this);
        tg.setOrientation(LinearLayout.HORIZONTAL);
        tg.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout tt = UI.col(this);
        tt.addView(UI.tv(this, "Dialog Show", 15, UI.tx(), true));
        tt.addView(UI.tv(this, "Enable / Disable the login dialog in the app", 12, UI.sub(), false));
        tg.addView(tt, new LinearLayout.LayoutParams(0, -2, 1f));
        final Switch sw = new Switch(this);
        sw.setChecked(app.optBoolean("enabled", false));
        sw.setOnCheckedChangeListener((b, on) -> req("PUT", "levi_apps/" + ck + "/enabled", String.valueOf(on), (r, e) -> {
            if (e != null) { UI.toast(this, "Failed"); return; }
            try { app.put("enabled", on); } catch (Exception ex) { /* ignore */ }
            UI.toast(this, on ? "Dialog enabled" : "Dialog disabled");
        }));
        tg.addView(sw);
        c.addView(tg, UI.lp(this, -1, -2, 0, 0, 0, 12));

        LinearLayout ex = UI.card(this);
        ex.addView(label("More"));
        LinearLayout r1 = UI.row(this);
        r1.addView(UI.btn(this, "Copy all", false, v -> UI.copy(this, "App Connect Key: " + ck + "\nLogin Key: " + app.optString("loginKey") + "\ndatabaseURL: " + base + "\n" + PERM)), UI.lp(this, 0, -2, 0, 0, 4, 0));
        r1.addView(UI.btn(this, "Duplicate", false, v -> duplicate(ck, app)), UI.lp(this, 0, -2, 4, 0, 0, 0));
        LinearLayout r2 = UI.row(this);
        r2.addView(UI.btn(this, "Export config", false, v -> exportCfg(app)), UI.lp(this, 0, -2, 0, 0, 4, 0));
        r2.addView(UI.btn(this, "Import config", false, v -> importCfg(ck, app)), UI.lp(this, 0, -2, 4, 0, 0, 0));
        for (LinearLayout r : new LinearLayout[]{r1, r2}) for (int i = 0; i < 2; i++) ((LinearLayout.LayoutParams) r.getChildAt(i).getLayoutParams()).weight = 1f;
        ex.addView(r1, UI.lp(this, -1, -2, 0, 8, 0, 8));
        ex.addView(r2);
        c.addView(ex, UI.lp(this, -1, -2, 0, 0, 0, 12));

        TextView del = UI.btn(this, "Delete app", false, v -> new AlertDialog.Builder(this).setTitle("Delete app?")
                .setMessage("This removes the app and its dialog config from Firebase. Apps using this connect key will stop showing the dialog.")
                .setPositiveButton("Delete", (dd, w) -> req("DELETE", "levi_apps/" + ck, null, (r, e) -> {
                    req("DELETE", "levi_media/" + ck, null, (r2x, e2) -> { });
                    apps.remove(ck);
                    pop();
                    refreshHome();
                })).setNegativeButton("Cancel", null).show());
        del.setTextColor(UI.RED);
        del.setBackground(UI.rr(this, UI.dark ? 0xFF3A1D20 : 0xFFFDE8E9, 99));
        c.addView(del, UI.lp(this, -1, -2, 0, 0, 0, 30));
        page.addView(scroll(c), new LinearLayout.LayoutParams(-1, 0, 1f));
        return page;
    }

    View loginRow(final String ck, final JSONObject app) {
        final LinearLayout c = UI.card(this);
        c.addView(label("Login Key"));
        LinearLayout r = UI.row(this);
        final TextView v = UI.tv(this, app.optString("loginKey"), 13, UI.tx(), false);
        v.setTypeface(Typeface.MONOSPACE);
        v.setTextIsSelectable(true);
        r.addView(v, new LinearLayout.LayoutParams(0, -2, 1f));
        r.addView(UI.btn(this, "Copy", false, x -> UI.copy(this, app.optString("loginKey"))));
        c.addView(r, UI.lp(this, -1, -2, 0, 8, 0, 6));
        c.addView(UI.btn(this, "Regenerate (users must verify again)", false, x -> {
            final String nk = Levi.randKey("LEVI");
            req("PUT", "levi_apps/" + ck + "/loginKey", JSONObject.quote(nk), (rr, e) -> {
                if (e != null) { UI.toast(this, "Failed"); return; }
                try { app.put("loginKey", nk); } catch (Exception ex) { /* ignore */ }
                v.setText(nk);
                UI.toast(this, "New login key");
            });
        }));
        return c;
    }

    void duplicate(final String ck, JSONObject app) {
        try {
            final String nk = Levi.randKey("LV");
            JSONObject a = new JSONObject(app.toString());
            a.put("name", app.optString("name") + " copy").put("loginKey", Levi.randKey("LEVI")).put("enabled", false).put("created", System.currentTimeMillis());
            final JSONObject fa = a;
            req("PUT", "levi_apps/" + nk, a.toString(), (r, e) -> {
                if (e != null) { UI.toast(this, "Failed"); return; }
                for (final String m : new String[]{"bg", "banner"}) req("GET", "levi_media/" + ck + "/" + m, null, (mr, me) -> {
                    if (me == null && mr != null && !mr.equals("null")) req("PUT", "levi_media/" + nk + "/" + m, mr, (x, y) -> { });
                });
                try { apps.put(nk, fa); } catch (Exception ex) { /* ignore */ }
                UI.toast(this, "Duplicated");
                pop();
                refreshHome();
            });
        } catch (Exception ex) { UI.toast(this, "Error"); }
    }

    void exportCfg(JSONObject app) {
        try {
            JSONObject c = new JSONObject(app.optJSONObject("cfg") == null ? "{}" : app.optJSONObject("cfg").toString());
            for (String k : new String[]{"bgSrc", "bnSrc"}) if (c.optString(k).startsWith("db:")) c.remove(k);
            UI.copy(this, c.toString());
        } catch (Exception e) { UI.toast(this, "Nothing to export"); }
    }

    void importCfg(final String ck, final JSONObject app) {
        final EditText et = UI.edit(this, "Paste exported config JSON", true);
        et.setMinLines(6);
        new AlertDialog.Builder(this).setTitle("Import config").setView(et).setPositiveButton("Import", (d, w) -> {
            try {
                final JSONObject c = new JSONObject(et.getText().toString().trim());
                req("PUT", "levi_apps/" + ck + "/cfg", c.toString(), (r, e) -> {
                    if (e != null) { UI.toast(this, "Failed"); return; }
                    try { app.put("cfg", c); } catch (Exception ex) { /* ignore */ }
                    UI.toast(this, "Config imported");
                });
            } catch (Exception ex) { UI.toast(this, "Invalid JSON"); }
        }).setNegativeButton("Cancel", null).show();
    }

    // =============================================================== guide =====================
    View guide() {
        LinearLayout c = UI.col(this);
        c.addView(UI.tv(this, "Guide", 24, UI.tx(), true), UI.lp(this, -2, -2, 0, 0, 0, 12));
        String[][] g = {
                {"1  Firebase setup", "Create a Firebase project \u2192 Realtime Database \u2192 Create. Copy the databaseURL. Open the Rules tab and paste the rules below, then Publish.\n\nTip: these rules are open for the levi_* paths only. For tighter security move to Firebase Auth later.", RULES},
                {"2  Connect + admin key", "Open Levi Admin, paste the databaseURL, tap Connect. First time you create an admin key (use the eye icon to see it). Next time you open the app, enter that key to get in.", null},
                {"3  Add an app", "Tap + and give a name, short detail and an icon (gallery or URL). The date is automatic. Every app gets its own App Connect Key (LV-XXX-XXX-ST) and Login Key (LEVI-XXX-XXX-ST).", null},
                {"4  Put the dialog in your APK", "Add Levi.java (package com.levi.dialog) + assets/fonts to the target app and call Levi.show(this) in the main activity. Or use the Levi APK as a donor. Make sure the app has the INTERNET permission line shown on the app page.\n\nWith MT Manager: open classes.dex \u2192 com/levi/dialog/Levi.smali \u2192 replace the string \"https://YOUR-PROJECT-default-rtdb.firebaseio.com\" with your databaseURL and \"LV-XXX-XXX-ST\" with the App Connect Key. Save + sign.", null},
                {"5  Enable + design", "Open the app page \u2192 turn on Dialog Show. Tap Edit to design the dialog: live preview on top, media, colours, fonts, shape and 20 entrance animations. Save, and the dialog updates itself within a few seconds (it re-checks every 4 s while visible).", null},
                {"6  How verification works", "Until the user types the correct Login Key the dialog stays and Back closes the app. After success it is remembered on the device. Regenerate the Login Key to force everyone to verify again. Dialog Show off = no dialog.", null},
                {"7  Troubleshooting", "\u2022 Permission denied \u2192 check the rules.\n\u2022 Dialog never shows \u2192 Dialog Show is off or the connect key in smali is wrong.\n\u2022 Video too big \u2192 use a URL or keep it under 6 MB.\n\u2022 Fonts missing \u2192 assets/fonts must contain the .ttf files.", null}};
        for (int i = 0; i < g.length; i++) {
            final LinearLayout card = UI.card(this);
            TextView t = UI.tv(this, g[i][0], 15, UI.tx(), true);
            card.addView(t);
            final LinearLayout body = UI.col(this);
            body.setVisibility(View.GONE);
            body.addView(UI.tv(this, g[i][1], 13, UI.sub(), false), UI.lp(this, -1, -2, 0, 8, 0, 0));
            if (g[i][2] != null) {
                final String code = g[i][2];
                TextView cv = UI.tv(this, code, 11, UI.tx(), false);
                cv.setTypeface(Typeface.MONOSPACE);
                cv.setBackground(UI.rr(this, UI.surf2(), 12));
                cv.setPadding(UI.dp(this, 12), UI.dp(this, 10), UI.dp(this, 12), UI.dp(this, 10));
                body.addView(cv, UI.lp(this, -1, -2, 0, 10, 0, 8));
                body.addView(UI.btn(this, "Copy rules", false, x -> UI.copy(this, code)));
            }
            card.addView(body);
            card.setOnClickListener(x -> body.setVisibility(body.getVisibility() == View.GONE ? View.VISIBLE : View.GONE));
            c.addView(card, UI.lp(this, -1, -2, 0, 0, 0, 12));
            UI.pop(card, i);
        }
        return scroll(c);
    }

    // =============================================================== settings ==================
    View settings() {
        LinearLayout c = UI.col(this);
        c.addView(UI.tv(this, "Settings", 24, UI.tx(), true), UI.lp(this, -2, -2, 0, 0, 0, 12));
        LinearLayout a = UI.card(this);
        a.addView(label("Appearance"));
        a.addView(switchRow("Dark mode", "Easy on the eyes at night", UI.dark, (b, on) -> { sp.edit().putBoolean("dark", on).apply(); UI.dark = on; refreshTheme(); }));
        a.addView(switchRow("Animations", "Screen transitions and effects", UI.anim, (b, on) -> { sp.edit().putBoolean("anim", on).apply(); UI.anim = on; }));
        c.addView(a, UI.lp(this, -1, -2, 0, 0, 0, 12));
        UI.pop(a, 0);
        LinearLayout s = UI.card(this);
        s.addView(label("Security"));
        s.addView(switchRow("Stay signed in", "Skip the admin key on this device", sp.getBoolean("remember", false), (b, on) -> {
            sp.edit().putBoolean("remember", on).apply();
            if (!on) sp.edit().remove("rhash").apply();
            else UI.toast(this, "Applies after next unlock");
        }));
        s.addView(UI.btn(this, "Change admin key", false, v -> changeKey()), UI.lp(this, -1, -2, 0, 10, 0, 0));
        c.addView(s, UI.lp(this, -1, -2, 0, 0, 0, 12));
        UI.pop(s, 1);
        c.addView(copyRow("Connected database", base, true), UI.lp(this, -1, -2, 0, 0, 0, 12));
        LinearLayout d = UI.card(this);
        d.addView(label("Session"));
        d.addView(UI.btn(this, "Lock now", false, v -> { session = false; sp.edit().remove("rhash").putBoolean("remember", false).apply(); gate(); }), UI.lp(this, -1, -2, 0, 8, 0, 8));
        TextView dis = UI.btn(this, "Disconnect database", false, v -> { sp.edit().remove("db").remove("rhash").apply(); session = false; base = ""; replace(connectScreen()); });
        dis.setTextColor(UI.RED);
        d.addView(dis);
        c.addView(d, UI.lp(this, -1, -2, 0, 0, 0, 12));
        TextView about = UI.tv(this, "Levi Admin 1.0  \u2022  TENIx", 12, UI.sub(), false);
        about.setGravity(Gravity.CENTER);
        c.addView(about, UI.lp(this, -1, -2, 0, 8, 0, 20));
        return scroll(c);
    }

    View switchRow(String t, String sub, boolean on, android.widget.CompoundButton.OnCheckedChangeListener l) {
        LinearLayout r = UI.row(this);
        LinearLayout m = UI.col(this);
        m.addView(UI.tv(this, t, 15, UI.tx(), true));
        m.addView(UI.tv(this, sub, 12, UI.sub(), false));
        r.addView(m, new LinearLayout.LayoutParams(0, -2, 1f));
        Switch sw = new Switch(this);
        sw.setChecked(on);
        sw.setOnCheckedChangeListener(l);
        r.addView(sw);
        r.setPadding(0, UI.dp(this, 8), 0, UI.dp(this, 4));
        return r;
    }

    void changeKey() {
        LinearLayout v = UI.col(this);
        int p = UI.dp(this, 20);
        v.setPadding(p, p, p, 0);
        final UI.Pw o = new UI.Pw(this, "current key"), n = new UI.Pw(this, "new key");
        v.addView(o);
        v.addView(n, UI.lp(this, -1, -2, 0, 10, 0, 0));
        new AlertDialog.Builder(this).setTitle("Change admin key").setView(v).setPositiveButton("Change", (d, w) -> {
            final String nk = n.get();
            if (nk.length() < 4) { UI.toast(this, "New key too short"); return; }
            req("GET", "levi_admin/hash", null, (r, e) -> {
                if (e != null || !Levi.unq(r).equals(Levi.sha256(o.get() + "|levi"))) { UI.toast(this, "Current key is wrong"); return; }
                final String hs = Levi.sha256(nk + "|levi");
                req("PUT", "levi_admin/hash", JSONObject.quote(hs), (r2, e2) -> {
                    if (e2 != null) { UI.toast(this, "Failed"); return; }
                    if (sp.getBoolean("remember", false)) sp.edit().putString("rhash", hs).apply();
                    UI.toast(this, "Admin key changed");
                });
            });
        }).setNegativeButton("Cancel", null).show();
    }
}
