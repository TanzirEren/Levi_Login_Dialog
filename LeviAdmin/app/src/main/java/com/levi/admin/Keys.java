package com.levi.admin;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.levi.dialog.Levi;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/** Login keys page of one app: many keys per app, each with user, expiry, device limit, status. */
public class Keys {
    final MainActivity A;
    final String ck;
    final JSONObject app;
    JSONObject keys = new JSONObject();
    String cat = "all", q = "";
    int sort = 0;
    boolean searching = false, loaded = false;
    LinearLayout list, chipsRow, statRow, legacyBox;
    EditText search;
    TextView sortTv;
    final Handler h = new Handler(Looper.getMainLooper());

    static final String[] CATS = {"all", "active", "inactive", "draft", "expired", "deleted"};
    static final String[] CAT_NAMES = {"All", "Active", "Inactive", "Draft", "Expired", "Deleted"};
    static final String[] SORTS = {"Newest", "Oldest", "Expiring soon", "Name A-Z"};
    static final String[] EXP_NAMES = {"1 day", "7 days", "30 days", "90 days", "1 year", "Never", "Custom"};
    static final int[] EXP_DAYS = {1, 7, 30, 90, 365, 0, -1};
    static final long DAY = 86400000L;

    interface Mut { void apply(JSONObject o) throws Exception; }

    public Keys(MainActivity a, String ck) {
        A = a;
        this.ck = ck;
        app = a.apps.optJSONObject(ck) == null ? new JSONObject() : a.apps.optJSONObject(ck);
        Levi.dbBase = a.base;
        Levi.dbCk = ck;
    }

    // ================================================================ helpers ==================
    static String fmt(long t) { return new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(new Date(t)); }

    static String rel(long ms) {
        long a = Math.abs(ms);
        String v;
        if (a >= DAY) v = (a / DAY) + "d " + ((a % DAY) / 3600000L) + "h";
        else if (a >= 3600000L) v = (a / 3600000L) + "h " + ((a % 3600000L) / 60000L) + "m";
        else v = Math.max(1, a / 60000L) + "m";
        return ms < 0 ? v + " ago" : v + " left";
    }

    String stateOf(JSONObject k, long now) {
        String st = k.optString("status", "active");
        if (st.equals("deleted") || st.equals("draft") || st.equals("inactive")) return st;
        long exp = k.optLong("expiry", 0);
        return exp > 0 && exp < now ? "expired" : "active";
    }

    int stateColor(String st) {
        switch (st) {
            case "active": return UI.GRN;
            case "draft": return 0xFFF59E0B;
            case "expired": return UI.RED;
            case "deleted": return 0xFF9CA3AF;
            default: return UI.sub();
        }
    }

    String expText(JSONObject k, long now) {
        long exp = k.optLong("expiry", 0), dur = k.optLong("dur", 0);
        if (exp > 0) return (exp < now ? "Expired " : "Expires ") + fmt(exp) + "  (" + rel(exp - now) + ")";
        if (dur > 0) return "Countdown starts at first login (" + (dur / DAY) + " days)";
        return "Never expires";
    }

    int devCount(JSONObject k) { JSONObject d = k.optJSONObject("devices"); return d == null ? 0 : d.length(); }

    String shareText(JSONObject k) {
        long exp = k.optLong("expiry", 0), dur = k.optLong("dur", 0);
        String valid = exp > 0 ? "Valid until: " + fmt(exp) : dur > 0 ? "Valid for " + (dur / DAY) + " days after first login" : "Valid: lifetime";
        return "\uD83D\uDD11 " + app.optString("name", "App") + " access\nUser: " + k.optString("user") + "\nLogin key: " + k.optString("key")
                + "\n" + valid + "\nDevices: " + Math.max(1, k.optInt("maxDevices", 1))
                + "\n\nOpen the app and enter this key in the login dialog.";
    }

    TextView small(String s, int color, View.OnClickListener cl) {
        TextView t = UI.tv(A, s, 12, color, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(UI.dp(A, 12), UI.dp(A, 8), UI.dp(A, 12), UI.dp(A, 8));
        t.setBackground(UI.rr(A, UI.priC(), 99));
        t.setOnClickListener(cl);
        UI.press(t);
        return t;
    }

    View icon(int type, int color, View.OnClickListener cl) {
        UI.NavIcon ic = new UI.NavIcon(A, type);
        ic.color = color;
        FrameLayout f = new FrameLayout(A);
        f.addView(ic, new FrameLayout.LayoutParams(UI.dp(A, 24), UI.dp(A, 24), Gravity.CENTER));
        f.setOnClickListener(cl);
        f.setLayoutParams(new LinearLayout.LayoutParams(UI.dp(A, 44), UI.dp(A, 44)));
        return f;
    }

    // ================================================================ screen ===================
    public View view() {
        FrameLayout outer = new FrameLayout(A);
        LinearLayout page = UI.col(A);

        LinearLayout bar = UI.row(A);
        bar.setPadding(UI.dp(A, 8), UI.dp(A, 8), UI.dp(A, 8), UI.dp(A, 4));
        TextView back = UI.tv(A, "\u2190", 22, UI.tx(), true);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> A.pop());
        bar.addView(back, new LinearLayout.LayoutParams(UI.dp(A, 44), UI.dp(A, 44)));
        bar.addView(A.iconView(ck, app, 38), UI.lp(A, UI.dp(A, 38), UI.dp(A, 38), 0, 0, 10, 0));
        LinearLayout t = UI.col(A);
        TextView nm = UI.tv(A, app.optString("name", "App"), 17, UI.tx(), true);
        nm.setSingleLine(true);
        t.addView(nm);
        t.addView(UI.tv(A, "Login keys", 11, UI.sub(), false));
        bar.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        bar.addView(icon(4, UI.tx(), v -> toggleSearch()));
        bar.addView(icon(5, UI.PRI, v -> A.push(A.detail(ck))));
        page.addView(bar);

        search = UI.edit(A, "Search by user, key or note", false);
        search.setVisibility(View.GONE);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { q = s.toString().trim().toLowerCase(); if (loaded) render(); }
            @Override public void afterTextChanged(Editable e) { }
        });
        page.addView(search, UI.lp(A, -1, -2, 16, 4, 16, 4));

        statRow = UI.row(A);
        page.addView(statRow, UI.lp(A, -1, -2, 16, 6, 16, 6));

        HorizontalScrollView hs = new HorizontalScrollView(A);
        hs.setHorizontalScrollBarEnabled(false);
        chipsRow = UI.row(A);
        hs.addView(chipsRow);
        page.addView(hs, UI.lp(A, -1, -2, 16, 0, 16, 4));

        LinearLayout tools = UI.row(A);
        sortTv = small("Sort: " + SORTS[sort] + " \u25BE", UI.PRI, v -> pickSort());
        tools.addView(sortTv, UI.lp(A, -2, -2, 0, 0, 8, 0));
        tools.addView(small("Export", UI.PRI, v -> exportMenu()));
        page.addView(tools, UI.lp(A, -1, -2, 16, 4, 16, 6));

        ScrollView sv = new ScrollView(A);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout body = UI.col(A);
        body.setPadding(UI.dp(A, 16), UI.dp(A, 4), UI.dp(A, 16), UI.dp(A, 110));
        legacyBox = UI.col(A);
        body.addView(legacyBox);
        list = UI.col(A);
        body.addView(list);
        sv.addView(body, new FrameLayout.LayoutParams(-1, -2));
        page.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
        outer.addView(page, new FrameLayout.LayoutParams(-1, -1));

        TextView fab = UI.tv(A, "+", 30, 0xFFFFFFFF, false);
        fab.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{UI.PRI, UI.PRI2});
        g.setShape(GradientDrawable.OVAL);
        g.setStroke(UI.dp(A, 2), 0xAAFFFFFF);
        fab.setBackground(g);
        fab.setElevation(UI.dp(A, 10));
        fab.setOnClickListener(v -> generate());
        UI.press(fab);
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(UI.dp(A, 60), UI.dp(A, 60), Gravity.BOTTOM | Gravity.END);
        fl.setMargins(0, 0, UI.dp(A, 22), UI.dp(A, 26));
        outer.addView(fab, fl);
        if (UI.anim) { fab.setScaleX(0); fab.setScaleY(0); fab.animate().scaleX(1).scaleY(1).setStartDelay(250).setDuration(450).setInterpolator(new android.view.animation.OvershootInterpolator(3f)).start(); }

        TextView ld = UI.tv(A, "Loading keys...", 13, UI.sub(), false);
        ld.setGravity(Gravity.CENTER);
        ld.setPadding(0, UI.dp(A, 40), 0, 0);
        list.addView(ld);
        load();
        return outer;
    }

    void toggleSearch() {
        searching = !searching;
        search.setVisibility(searching ? View.VISIBLE : View.GONE);
        if (searching) {
            search.requestFocus();
            if (UI.anim) { search.setAlpha(0); search.animate().alpha(1).setDuration(220).start(); }
        } else { search.setText(""); q = ""; if (loaded) render(); }
    }

    void load() {
        A.req("GET", "levi_keys/" + ck, null, (r, e) -> {
            if (e != null) {
                list.removeAllViews();
                list.addView(UI.tv(A, "Can't load keys: " + MainActivity.shortErr(e) + "\nPublish the new Firebase rules (levi_keys, levi_ping) - see Guide.", 13, UI.RED, false));
                return;
            }
            try { keys = (r == null || r.equals("null")) ? new JSONObject() : new JSONObject(r); } catch (Exception ex) { keys = new JSONObject(); }
            loaded = true;
            render();
        });
    }

    // ================================================================ render ===================
    List<String> ids() {
        ArrayList<String> r = new ArrayList<>();
        Iterator<String> it = keys.keys();
        while (it.hasNext()) r.add(it.next());
        return r;
    }

    void render() {
        final long now = System.currentTimeMillis();
        int[] cnt = new int[CATS.length];
        int bound = 0, total = 0;
        ArrayList<String> show = new ArrayList<>();
        for (String id : ids()) {
            JSONObject k = keys.optJSONObject(id);
            if (k == null) continue;
            String st = stateOf(k, now);
            if (!st.equals("deleted")) { cnt[0]++; total++; bound += devCount(k); }
            for (int i = 1; i < CATS.length; i++) if (CATS[i].equals(st)) cnt[i]++;
            boolean in = cat.equals("all") ? !st.equals("deleted") : cat.equals(st);
            if (in && !q.isEmpty() && !(k.optString("user", "") + " " + k.optString("key", id) + " " + k.optString("note", "")).toLowerCase().contains(q)) in = false;
            if (in) show.add(id);
        }
        // stats
        statRow.removeAllViews();
        statRow.addView(stat("Keys", String.valueOf(total), UI.PRI), UI.lp(A, 0, -2, 0, 0, 6, 0));
        statRow.addView(stat("Active", String.valueOf(cnt[1]), UI.GRN), UI.lp(A, 0, -2, 3, 0, 3, 0));
        statRow.addView(stat("Devices", String.valueOf(bound), UI.PRI2), UI.lp(A, 0, -2, 6, 0, 0, 0));
        for (int i = 0; i < 3; i++) ((LinearLayout.LayoutParams) statRow.getChildAt(i).getLayoutParams()).weight = 1f;
        // chips
        chipsRow.removeAllViews();
        for (int i = 0; i < CATS.length; i++) {
            final String c = CATS[i];
            boolean on = c.equals(cat);
            TextView ch = UI.tv(A, CAT_NAMES[i] + " " + cnt[i], 12, on ? 0xFFFFFFFF : UI.PRI, true);
            ch.setPadding(UI.dp(A, 14), UI.dp(A, 8), UI.dp(A, 14), UI.dp(A, 8));
            ch.setBackground(UI.rr(A, on ? UI.PRI : UI.priC(), 99));
            ch.setOnClickListener(v -> { cat = c; render(); });
            chipsRow.addView(ch, UI.lp(A, -2, -2, 0, 0, 8, 0));
        }
        // sorting
        final int so = sort;
        Collections.sort(show, (a, b) -> {
            JSONObject x = keys.optJSONObject(a), y = keys.optJSONObject(b);
            if (so == 1) return Long.compare(x.optLong("created", 0), y.optLong("created", 0));
            if (so == 2) {
                long ex = x.optLong("expiry", 0) == 0 ? Long.MAX_VALUE : x.optLong("expiry", 0), ey = y.optLong("expiry", 0) == 0 ? Long.MAX_VALUE : y.optLong("expiry", 0);
                return Long.compare(ex, ey);
            }
            if (so == 3) return x.optString("user", "").compareToIgnoreCase(y.optString("user", ""));
            return Long.compare(y.optLong("created", 0), x.optLong("created", 0));
        });
        // legacy single key banner
        legacyBox.removeAllViews();
        final String legacy = app.optString("loginKey", "");
        if (!legacy.isEmpty()) {
            LinearLayout lc = UI.card(A);
            lc.addView(UI.tv(A, "Old single login key found", 14, UI.tx(), true));
            TextView lk = UI.tv(A, legacy, 12, UI.sub(), false);
            lk.setTypeface(Typeface.MONOSPACE);
            lc.addView(lk, UI.lp(A, -2, -2, 0, 4, 0, 8));
            lc.addView(small("Import as a key", UI.PRI, v -> importLegacy(legacy)));
            legacyBox.addView(lc, UI.lp(A, -1, -2, 0, 0, 0, 12));
        }
        // list
        list.removeAllViews();
        if (show.isEmpty()) {
            TextView e = UI.tv(A, loaded && total == 0 && cat.equals("all") ? "No login keys yet.\nTap + to generate keys for your users." : "Nothing here.", 14, UI.sub(), false);
            e.setGravity(Gravity.CENTER);
            e.setPadding(0, UI.dp(A, 40), 0, 0);
            list.addView(e);
        }
        int n = 0;
        for (String id : show) list.addView(keyCard(id, keys.optJSONObject(id), now, n++), UI.lp(A, -1, -2, 0, 0, 0, 12));
        if (sortTv != null) sortTv.setText("Sort: " + SORTS[sort] + " \u25BE");
    }

    View stat(String name, String val, int color) {
        LinearLayout c = UI.card(A);
        c.setPadding(UI.dp(A, 12), UI.dp(A, 10), UI.dp(A, 12), UI.dp(A, 10));
        c.addView(UI.tv(A, name.toUpperCase(), 10, UI.sub(), true));
        c.addView(UI.tv(A, val, 22, color, true));
        return c;
    }

    View keyCard(final String id, final JSONObject k, long now, int idx) {
        final String st = stateOf(k, now);
        LinearLayout c = UI.card(A);
        LinearLayout r1 = UI.row(A);
        TextView nm = UI.tv(A, k.optString("user", "User"), 16, UI.tx(), true);
        nm.setSingleLine(true);
        r1.addView(nm, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView chip = UI.tv(A, st.toUpperCase(), 10, stateColor(st), true);
        chip.setPadding(UI.dp(A, 10), UI.dp(A, 4), UI.dp(A, 10), UI.dp(A, 4));
        chip.setBackground(UI.rr(A, UI.surf2(), 99));
        r1.addView(chip);
        c.addView(r1);
        LinearLayout r2 = UI.row(A);
        TextView kv = UI.tv(A, k.optString("key", id), 14, UI.PRI, true);
        kv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        kv.setTextIsSelectable(true);
        r2.addView(kv, new LinearLayout.LayoutParams(0, -2, 1f));
        r2.addView(icon(7, UI.PRI, v -> UI.copy(A, k.optString("key", id))));
        c.addView(r2, UI.lp(A, -1, -2, 0, 4, 0, 0));
        int max = Math.max(1, k.optInt("maxDevices", 1));
        String info = expText(k, now) + "\nDevices " + devCount(k) + "/" + max + "  \u2022  " + k.optInt("uses", 0) + " logins"
                + (k.optLong("lastSeen", 0) > 0 ? "  \u2022  last seen " + rel(k.optLong("lastSeen", 0) - now) : "");
        c.addView(UI.tv(A, info, 12, UI.sub(), false), UI.lp(A, -1, -2, 0, 2, 0, 0));
        if (!k.optString("note", "").isEmpty()) c.addView(UI.tv(A, "\u201C" + k.optString("note") + "\u201D", 12, UI.tx(), false), UI.lp(A, -1, -2, 0, 4, 0, 0));
        LinearLayout r3 = UI.row(A);
        if (st.equals("deleted")) {
            r3.addView(small("Restore", UI.PRI, v -> update(id, o -> { o.put("status", "inactive"); o.remove("deletedAt"); }, null)), UI.lp(A, -2, -2, 0, 0, 8, 0));
            r3.addView(small("Delete forever", UI.RED, v -> confirm("Delete forever?", "This key is removed permanently.", () -> A.req("DELETE", "levi_keys/" + ck + "/" + id, null, (r, e) -> { keys.remove(id); render(); }))));
        } else {
            r3.addView(small("Copy", UI.PRI, v -> UI.copy(A, k.optString("key", id))), UI.lp(A, -2, -2, 0, 0, 8, 0));
            r3.addView(small("Share", UI.PRI, v -> UI.copy(A, shareText(k))), UI.lp(A, -2, -2, 0, 0, 8, 0));
            r3.addView(small("Edit", UI.PRI, v -> editDialog(id, k)), UI.lp(A, -2, -2, 0, 0, 8, 0));
            r3.addView(small("More \u25BE", UI.PRI, v -> moreMenu(id, k, st)));
        }
        HorizontalScrollView hs = new HorizontalScrollView(A);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(r3);
        c.addView(hs, UI.lp(A, -1, -2, 0, 10, 0, 0));
        c.setOnClickListener(v -> details(id, k));
        UI.pop(c, idx);
        return c;
    }

    // ================================================================ firebase writes ==========
    void update(final String id, final Mut m, final Runnable done) {
        new Thread(() -> {
            String err = null;
            JSONObject res = null;
            try {
                String url = A.api("levi_keys/" + ck + "/" + id);
                for (int i = 0; i < 3 && res == null; i++) {
                    Levi.Resp r = Levi.httpR("GET", url, null, null, true);
                    if (r.code >= 400) throw new Exception("HTTP " + r.code);
                    JSONObject o = r.body.trim().equals("null") ? new JSONObject() : new JSONObject(r.body);
                    m.apply(o);
                    Levi.Resp w = Levi.httpR("PUT", url, o.toString(), r.etag, false);
                    if (w.code == 412) continue;
                    if (w.code >= 400) throw new Exception("HTTP " + w.code);
                    res = o;
                }
            } catch (Exception e) { err = e.getMessage() == null ? e.toString() : e.getMessage(); }
            final JSONObject fr = res;
            final String fe = err;
            h.post(() -> {
                if (fr == null) { UI.toast(A, "Failed: " + (fe == null ? "busy, try again" : MainActivity.shortErr(fe))); return; }
                try { keys.put(id, fr); } catch (Exception ex) { /* ignore */ }
                render();
                if (done != null) done.run();
            });
        }).start();
    }

    void confirm(String title, String msg, final Runnable yes) {
        new AlertDialog.Builder(A).setTitle(title).setMessage(msg).setPositiveButton("Yes", (d, w) -> yes.run()).setNegativeButton("Cancel", null).show();
    }

    // ================================================================ menus ====================
    void pickSort() {
        new AlertDialog.Builder(A).setTitle("Sort keys").setItems(SORTS, (d, w) -> { sort = w; render(); }).show();
    }

    void moreMenu(final String id, final JSONObject k, final String st) {
        final ArrayList<String> items = new ArrayList<>();
        final ArrayList<Runnable> acts = new ArrayList<>();
        if (st.equals("active") || st.equals("expired")) { items.add("Deactivate"); acts.add(() -> update(id, o -> o.put("status", "inactive"), null)); }
        else { items.add("Activate"); acts.add(() -> update(id, o -> o.put("status", "active"), null)); }
        if (k.optLong("expiry", 0) > 0) {
            for (final int dd : new int[]{1, 7, 30, 365}) {
                items.add("Extend +" + (dd == 365 ? "1 year" : dd + (dd == 1 ? " day" : " days")));
                acts.add(() -> update(id, o -> { long base = Math.max(System.currentTimeMillis(), o.optLong("expiry", 0)); o.put("expiry", base + dd * DAY); }, null));
            }
        }
        items.add("Reset devices (unbind all)");
        acts.add(() -> confirm("Reset devices?", "The next device that enters this key will be bound again.", () -> update(id, o -> o.remove("devices"), null)));
        items.add("Details & devices");
        acts.add(() -> details(id, k));
        items.add("Delete");
        acts.add(() -> confirm("Delete this key?", "It moves to Deleted. The user is locked out within minutes. You can restore it later.", () -> update(id, o -> { o.put("status", "deleted"); o.put("deletedAt", System.currentTimeMillis()); }, null)));
        new AlertDialog.Builder(A).setTitle(k.optString("user", "Key")).setItems(items.toArray(new String[0]), (d, w) -> acts.get(w).run()).show();
    }

    void exportMenu() {
        final String[] it = {"Copy CSV (current list)", "Copy active keys (one per line)", "Copy all keys with users"};
        new AlertDialog.Builder(A).setTitle("Export").setItems(it, (d, w) -> {
            long now = System.currentTimeMillis();
            StringBuilder b = new StringBuilder();
            if (w == 0) b.append("user,key,status,expiry,devices,logins,note\n");
            for (String id : ids()) {
                JSONObject k = keys.optJSONObject(id);
                if (k == null) continue;
                String st = stateOf(k, now);
                if (w == 1 && !st.equals("active")) continue;
                if (w != 0 && st.equals("deleted")) continue;
                if (w == 0) {
                    boolean in = cat.equals("all") ? !st.equals("deleted") : cat.equals(st);
                    if (!in) continue;
                    b.append(csv(k.optString("user"))).append(',').append(k.optString("key", id)).append(',').append(st).append(',')
                            .append(k.optLong("expiry", 0) > 0 ? fmt(k.optLong("expiry", 0)) : "never").append(',').append(devCount(k)).append(',')
                            .append(k.optInt("uses", 0)).append(',').append(csv(k.optString("note"))).append('\n');
                } else if (w == 1) b.append(k.optString("key", id)).append('\n');
                else b.append(k.optString("user")).append(" - ").append(k.optString("key", id)).append('\n');
            }
            UI.copy(A, b.toString().trim());
        }).show();
    }

    static String csv(String s) { return "\"" + s.replace("\"", "\"\"") + "\""; }

    void importLegacy(final String legacy) {
        try {
            JSONObject o = new JSONObject();
            o.put("key", legacy).put("user", "Legacy key").put("created", System.currentTimeMillis()).put("status", "active").put("maxDevices", 1).put("uses", 0);
            A.req("PUT", "levi_keys/" + ck + "/" + legacy, o.toString(), (r, e) -> {
                if (e != null) { UI.toast(A, "Failed"); return; }
                A.req("DELETE", "levi_apps/" + ck + "/loginKey", null, (r2, e2) -> { });
                app.remove("loginKey");
                try { keys.put(legacy, o); } catch (Exception ex) { /* ignore */ }
                UI.toast(A, "Imported");
                render();
            });
        } catch (Exception ex) { UI.toast(A, "Error"); }
    }

    // ================================================================ expiry picker ============
    class Exp {
        int preset = 2;
        long custom = 0;
        boolean first = false;
        TextView info;
        LinearLayout chips;

        Exp(int p) { preset = p; }

        long[] value() {   // {expiry, dur}
            if (preset == 5) return new long[]{0, 0};
            if (preset == 6) return new long[]{custom, 0};
            long ms = EXP_DAYS[preset] * DAY;
            return first ? new long[]{0, ms} : new long[]{System.currentTimeMillis() + ms, 0};
        }

        void refresh() {
            chips.removeAllViews();
            for (int i = 0; i < EXP_NAMES.length; i++) {
                final int k = i;
                boolean on = i == preset;
                TextView c = UI.tv(A, EXP_NAMES[i], 12, on ? 0xFFFFFFFF : UI.PRI, true);
                c.setPadding(UI.dp(A, 12), UI.dp(A, 7), UI.dp(A, 12), UI.dp(A, 7));
                c.setBackground(UI.rr(A, on ? UI.PRI : UI.priC(), 99));
                c.setOnClickListener(v -> {
                    preset = k;
                    if (k == 6) pickDate(); else refresh();
                });
                chips.addView(c, UI.lp(A, -2, -2, 0, 0, 6, 0));
            }
            long[] v = value();
            if (preset == 5) info.setText("This key never expires");
            else if (preset == 6) info.setText(custom > 0 ? "Expires: " + fmt(custom) : "Pick a date and time");
            else if (first) info.setText("Countdown of " + EXP_NAMES[preset] + " starts at the user's first login");
            else info.setText("Expires: " + fmt(v[0]) + "  (auto set)");
        }

        void pickDate() {
            final Calendar c = Calendar.getInstance();
            if (custom > 0) c.setTimeInMillis(custom); else c.add(Calendar.DAY_OF_YEAR, 30);
            new DatePickerDialog(A, (dp, y, m, d) -> {
                c.set(y, m, d);
                new TimePickerDialog(A, (tp, hh, mm) -> {
                    c.set(Calendar.HOUR_OF_DAY, hh);
                    c.set(Calendar.MINUTE, mm);
                    c.set(Calendar.SECOND, 0);
                    custom = c.getTimeInMillis();
                    refresh();
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        }

        View build() {
            LinearLayout box = UI.col(A);
            box.addView(UI.tv(A, "Login expiry", 12, UI.sub(), true), UI.lp(A, -2, -2, 0, 12, 0, 4));
            HorizontalScrollView hs = new HorizontalScrollView(A);
            hs.setHorizontalScrollBarEnabled(false);
            chips = UI.row(A);
            hs.addView(chips);
            box.addView(hs);
            info = UI.tv(A, "", 12, UI.tx(), false);
            box.addView(info, UI.lp(A, -2, -2, 0, 6, 0, 0));
            Switch sw = new Switch(A);
            sw.setText("Start countdown on first login");
            sw.setTextColor(UI.tx());
            sw.setTextSize(13);
            sw.setOnCheckedChangeListener((b, on) -> { first = on; refresh(); });
            box.addView(sw, UI.lp(A, -1, -2, 0, 6, 0, 0));
            refresh();
            return box;
        }
    }

    // ================================================================ generate =================
    void generate() {
        final LinearLayout v = UI.col(A);
        int p = UI.dp(A, 20);
        v.setPadding(p, p, p, 0);
        final EditText user = UI.edit(A, "User name", false);
        v.addView(user);
        final int[] qty = {1}, dev = {A.sp.getInt("defdev", 1)};
        final TextView qt = UI.tv(A, "Quantity: 1", 13, UI.tx(), true);
        v.addView(qt, UI.lp(A, -2, -2, 0, 12, 0, 0));
        SeekBar qs = new SeekBar(A);
        qs.setMax(49);
        qs.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int pr, boolean u) { qty[0] = pr + 1; qt.setText("Quantity: " + qty[0] + (qty[0] > 1 ? "  (names get a number)" : "")); }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        v.addView(qs);
        final Exp ex = new Exp(Math.min(6, A.sp.getInt("defexp", 2)));
        v.addView(ex.build());
        final TextView dt = UI.tv(A, "Devices per key: " + dev[0], 13, UI.tx(), true);
        v.addView(dt, UI.lp(A, -2, -2, 0, 12, 0, 0));
        SeekBar ds = new SeekBar(A);
        ds.setMax(4);
        ds.setProgress(dev[0] - 1);
        ds.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int pr, boolean u) { dev[0] = pr + 1; dt.setText("Devices per key: " + dev[0] + (dev[0] == 1 ? "  (one key = one device)" : "")); }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        v.addView(ds);
        final EditText prefix = UI.edit(A, "Key prefix", false);
        prefix.setText(A.sp.getString("defprefix", "LEVI"));
        v.addView(prefix, UI.lp(A, -1, -2, 0, 12, 0, 0));
        final EditText note = UI.edit(A, "Note (optional)", false);
        v.addView(note, UI.lp(A, -1, -2, 0, 10, 0, 0));
        final Switch draft = new Switch(A);
        draft.setText("Save as draft (not active yet)");
        draft.setTextColor(UI.tx());
        draft.setTextSize(13);
        v.addView(draft, UI.lp(A, -1, -2, 0, 10, 0, 6));
        ScrollView sv = new ScrollView(A);
        sv.addView(v);
        final AlertDialog d = new AlertDialog.Builder(A).setTitle("Generate login key").setView(sv)
                .setPositiveButton("Generate", null).setNegativeButton("Cancel", null).create();
        d.show();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x -> {
            final String nm = user.getText().toString().trim();
            if (nm.isEmpty()) { UI.toast(A, "Enter a user name"); return; }
            long[] val = ex.value();
            if (ex.preset == 6 && val[0] <= System.currentTimeMillis()) { UI.toast(A, "Pick a future expiry date"); return; }
            String pf = prefix.getText().toString().trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
            if (pf.isEmpty()) pf = "LEVI";
            A.sp.edit().putString("defprefix", pf).apply();
            d.dismiss();
            run(nm, qty[0], pf, val[0], val[1], dev[0], note.getText().toString().trim(), draft.isChecked());
        });
    }

    void run(final String name, final int n, final String pf, final long expiry, final long dur, final int devices, final String note, final boolean draft) {
        final TextView msg = UI.tv(A, "Generating 0/" + n, 14, UI.tx(), false);
        msg.setPadding(UI.dp(A, 22), UI.dp(A, 18), UI.dp(A, 22), UI.dp(A, 8));
        final AlertDialog pd = new AlertDialog.Builder(A).setTitle("Generating").setView(msg).setCancelable(false).create();
        pd.show();
        new Thread(() -> {
            final ArrayList<JSONObject> made = new ArrayList<>();
            String err = null;
            long t0 = System.currentTimeMillis();
            try {
                for (int i = 0; i < n; i++) {
                    String key;
                    do { key = Levi.randKey(pf); } while (keys.has(key));
                    JSONObject o = new JSONObject();
                    o.put("key", key).put("user", n > 1 ? name + " " + (i + 1) : name).put("created", t0 + i).put("status", draft ? "draft" : "active")
                            .put("maxDevices", devices).put("uses", 0);
                    if (expiry > 0) o.put("expiry", expiry);
                    if (dur > 0) o.put("dur", dur);
                    if (!note.isEmpty()) o.put("note", note);
                    Levi.Resp r = Levi.httpR("PUT", A.api("levi_keys/" + ck + "/" + key), o.toString(), null, false);
                    if (r.code >= 400) throw new Exception("HTTP " + r.code + " (publish the new Firebase rules)");
                    made.add(o);
                    keys.put(key, o);
                    final int done = i + 1;
                    h.post(() -> msg.setText("Generating " + done + "/" + n));
                }
            } catch (Exception e) { err = e.getMessage() == null ? e.toString() : e.getMessage(); }
            final String fe = err;
            h.post(() -> {
                pd.dismiss();
                render();
                if (fe != null) UI.toast(A, "Stopped: " + MainActivity.shortErr(fe));
                if (made.isEmpty()) return;
                showResult(made);
            });
        }).start();
    }

    void showResult(final ArrayList<JSONObject> made) {
        final StringBuilder all = new StringBuilder();
        for (JSONObject k : made) all.append(k.optString("user")).append(" - ").append(k.optString("key")).append('\n');
        LinearLayout v = UI.col(A);
        int p = UI.dp(A, 20);
        v.setPadding(p, p, p, 0);
        ScrollView sv = new ScrollView(A);
        TextView t = UI.tv(A, all.toString().trim(), 14, UI.tx(), false);
        t.setTypeface(Typeface.MONOSPACE);
        t.setTextIsSelectable(true);
        sv.addView(t);
        v.addView(sv, new LinearLayout.LayoutParams(-1, Math.min(UI.dp(A, 260), UI.dp(A, 40) + made.size() * UI.dp(A, 22))));
        new AlertDialog.Builder(A).setTitle(made.size() == 1 ? "Key created" : made.size() + " keys created").setView(v)
                .setPositiveButton("Copy", (d, w) -> UI.copy(A, made.size() == 1 ? made.get(0).optString("key") : all.toString().trim()))
                .setNeutralButton(made.size() == 1 ? "Share text" : "Close", (d, w) -> { if (made.size() == 1) UI.copy(A, shareText(made.get(0))); })
                .setNegativeButton("Done", null).show();
    }

    // ================================================================ edit / details ===========
    void editDialog(final String id, final JSONObject k) {
        LinearLayout v = UI.col(A);
        int p = UI.dp(A, 20);
        v.setPadding(p, p, p, 0);
        final EditText user = UI.edit(A, "User name", false);
        user.setText(k.optString("user"));
        v.addView(user);
        final EditText note = UI.edit(A, "Note", false);
        note.setText(k.optString("note"));
        v.addView(note, UI.lp(A, -1, -2, 0, 10, 0, 0));
        final int[] dev = {Math.max(1, k.optInt("maxDevices", 1))};
        final TextView dt = UI.tv(A, "Devices per key: " + dev[0], 13, UI.tx(), true);
        v.addView(dt, UI.lp(A, -2, -2, 0, 12, 0, 0));
        SeekBar ds = new SeekBar(A);
        ds.setMax(4);
        ds.setProgress(dev[0] - 1);
        ds.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int pr, boolean u) { dev[0] = pr + 1; dt.setText("Devices per key: " + dev[0]); }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        v.addView(ds);
        final Exp ex = new Exp(5);
        long exp = k.optLong("expiry", 0), dur = k.optLong("dur", 0);
        if (exp > 0) { ex.preset = 6; ex.custom = exp; }
        else if (dur > 0) {
            ex.first = true;
            ex.preset = 2;
            for (int i = 0; i < 5; i++) if (EXP_DAYS[i] * DAY == dur) ex.preset = i;
        }
        v.addView(ex.build());
        ScrollView sv = new ScrollView(A);
        sv.addView(v);
        new AlertDialog.Builder(A).setTitle("Edit key").setView(sv).setPositiveButton("Save", (d, w) -> {
            final long[] val = ex.value();
            final String un = user.getText().toString().trim(), nt = note.getText().toString().trim();
            update(id, o -> {
                o.put("user", un.isEmpty() ? "User" : un);
                if (nt.isEmpty()) o.remove("note"); else o.put("note", nt);
                o.put("maxDevices", dev[0]);
                if (ex.preset == 6 && ex.custom == o.optLong("expiry", 0)) { /* unchanged */ }
                else if (val[0] > 0) { o.put("expiry", val[0]); o.remove("dur"); }
                else if (val[1] > 0) { if (o.optLong("firstUse", 0) > 0) o.put("expiry", o.optLong("firstUse") + val[1]); else o.remove("expiry"); o.put("dur", val[1]); }
                else { o.remove("expiry"); o.remove("dur"); }
            }, null);
        }).setNegativeButton("Cancel", null).show();
    }

    void details(final String id, final JSONObject k) {
        long now = System.currentTimeMillis();
        LinearLayout v = UI.col(A);
        int p = UI.dp(A, 20);
        v.setPadding(p, p, p, 0);
        StringBuilder b = new StringBuilder();
        b.append("Key: ").append(k.optString("key", id)).append("\nUser: ").append(k.optString("user")).append("\nStatus: ").append(stateOf(k, now))
                .append("\nCreated: ").append(fmt(k.optLong("created", now))).append("\n").append(expText(k, now))
                .append("\nFirst login: ").append(k.optLong("firstUse", 0) > 0 ? fmt(k.optLong("firstUse")) : "not used yet")
                .append("\nLogins: ").append(k.optInt("uses", 0))
                .append("\nLast seen: ").append(k.optLong("lastSeen", 0) > 0 ? fmt(k.optLong("lastSeen")) + " (" + k.optString("lastDev") + ")" : "-")
                .append("\nDevice limit: ").append(Math.max(1, k.optInt("maxDevices", 1)));
        if (!k.optString("note").isEmpty()) b.append("\nNote: ").append(k.optString("note"));
        TextView t = UI.tv(A, b.toString(), 13, UI.tx(), false);
        t.setTextIsSelectable(true);
        v.addView(t);
        v.addView(UI.tv(A, "Bound devices", 12, UI.sub(), true), UI.lp(A, -2, -2, 0, 14, 0, 6));
        JSONObject devs = k.optJSONObject("devices");
        final AlertDialog[] dd = new AlertDialog[1];
        if (devs == null || devs.length() == 0) v.addView(UI.tv(A, "None yet - the first device that enters this key gets bound.", 12, UI.sub(), false));
        else {
            Iterator<String> it = devs.keys();
            while (it.hasNext()) {
                final String did = it.next();
                JSONObject dv = devs.optJSONObject(did);
                LinearLayout r = UI.row(A);
                r.addView(UI.tv(A, (dv == null ? did : dv.optString("m", did)) + (dv == null ? "" : "\n" + fmt(dv.optLong("t", now))), 12, UI.tx(), false), new LinearLayout.LayoutParams(0, -2, 1f));
                r.addView(small("Remove", UI.RED, x -> { if (dd[0] != null) dd[0].dismiss(); update(id, o -> { JSONObject ds2 = o.optJSONObject("devices"); if (ds2 != null) ds2.remove(did); }, null); }));
                v.addView(r, UI.lp(A, -1, -2, 0, 0, 0, 6));
            }
        }
        ScrollView sv = new ScrollView(A);
        sv.addView(v);
        dd[0] = new AlertDialog.Builder(A).setTitle(k.optString("user", "Key")).setView(sv)
                .setPositiveButton("Copy key", (d, w) -> UI.copy(A, k.optString("key", id))).setNegativeButton("Close", null).create();
        dd[0].show();
    }
}
