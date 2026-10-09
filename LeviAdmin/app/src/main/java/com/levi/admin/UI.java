package com.levi.admin;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.levi.dialog.Levi;

public final class UI {
    public static boolean dark = false, anim = true;

    public static int bg() { return dark ? 0xFF0E1014 : 0xFFF4F5FA; }
    public static int surf() { return dark ? 0xFF181B22 : 0xFFFFFFFF; }
    public static int surf2() { return dark ? 0xFF20242D : 0xFFEEF0F8; }
    public static int tx() { return dark ? 0xFFECEEF4 : 0xFF15171C; }
    public static int sub() { return dark ? 0xFF9AA1B0 : 0xFF6A7080; }
    public static int priC() { return dark ? 0xFF2B2C5C : 0xFFE7E8FF; }
    public static int line() { return dark ? 0xFF2A2E39 : 0xFFE6E8F1; }
    public static final int PRI = 0xFF5B5BF0, PRI2 = 0xFFEC4899, RED = 0xFFE5484D, GRN = 0xFF2FB36B;

    public static int dp(Context c, float v) { return Levi.dp(c, v); }

    public static GradientDrawable rr(Context c, int color, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, r));
        return g;
    }

    public static GradientDrawable rrs(Context c, int color, int stroke, float r) {
        GradientDrawable g = rr(c, color, r);
        g.setStroke(dp(c, 1), stroke);
        return g;
    }

    public static TextView tv(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    public static LinearLayout.LayoutParams lp(Context c, int w, int h, float l, float t, float r, float b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        return p;
    }

    public static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = col(c);
        l.setBackground(rr(c, surf(), 22));
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        l.setElevation(dark ? 0 : dp(c, 2));
        return l;
    }

    public static View space(Context c, int h) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, h)));
        return v;
    }

    public static void press(final View v) {
        v.setOnTouchListener((x, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) x.animate().scaleX(.96f).scaleY(.96f).setDuration(80).start();
            else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) x.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
            return false;
        });
    }

    public static TextView btn(Context c, String s, boolean filled, View.OnClickListener cl) {
        TextView t = tv(c, s, 14, filled ? 0xFFFFFFFF : PRI, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 18), dp(c, 12), dp(c, 18), dp(c, 12));
        t.setBackground(filled ? rr(c, PRI, 99) : rr(c, priC(), 99));
        t.setOnClickListener(cl);
        t.setClickable(true);
        press(t);
        return t;
    }

    public static EditText edit(Context c, String hint, boolean multi) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setTextSize(14);
        e.setTextColor(tx());
        e.setHintTextColor(sub());
        e.setBackground(rrs(c, surf2(), line(), 14));
        e.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        if (multi) { e.setMinLines(2); e.setGravity(Gravity.TOP); }
        else e.setSingleLine(true);
        return e;
    }

    public static void toast(Context c, String s) { Toast.makeText(c, s, Toast.LENGTH_SHORT).show(); }

    public static void copy(Context c, String t) {
        ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("levi", t));
        toast(c, "Copied");
    }

    /** staggered entrance */
    public static void pop(View v, int i) {
        if (!anim) return;
        v.setAlpha(0);
        v.setTranslationY(dp(v.getContext(), 22));
        v.animate().alpha(1).translationY(0).setStartDelay(Math.min(i, 10) * 45L).setDuration(320).start();
    }

    // ---------------------------------------------------------------- password field with eye ----
    public static class Pw extends LinearLayout {
        public final EditText et;
        final Levi.Eye eye;

        public Pw(Context c, String hint) {
            super(c);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setBackground(rrs(c, surf2(), line(), 14));
            setPadding(dp(c, 14), 0, dp(c, 12), 0);
            et = new EditText(c);
            et.setBackground(null);
            et.setHint(hint);
            et.setTextSize(14);
            et.setTextColor(tx());
            et.setHintTextColor(sub());
            et.setSingleLine(true);
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            et.setTransformationMethod(PasswordTransformationMethod.getInstance());
            addView(et, new LayoutParams(0, dp(c, 48), 1f));
            eye = new Levi.Eye(c);
            eye.color = PRI;
            addView(eye, new LayoutParams(dp(c, 24), dp(c, 24)));
            eye.setOnClickListener(v -> {
                eye.shown = !eye.shown;
                et.setTransformationMethod(eye.shown ? null : PasswordTransformationMethod.getInstance());
                et.setSelection(et.length());
                eye.invalidate();
            });
        }

        public String get() { return et.getText().toString().trim(); }
    }

    // ---------------------------------------------------------------- colour picker ---------------
    public interface IntCb { void got(int v); }

    static final int[] SWATCH = {0xFFFFFFFF, 0xFF000000, 0xFF111827, 0xFF6B7280, 0xFFEF4444, 0xFFF97316, 0xFFF59E0B, 0xFFEAB308,
            0xFF84CC16, 0xFF22C55E, 0xFF10B981, 0xFF14B8A6, 0xFF06B6D4, 0xFF0EA5E9, 0xFF3B82F6, 0xFF6366F1,
            0xFF8B5CF6, 0xFFA855F7, 0xFFD946EF, 0xFFEC4899, 0xFFF43F5E, 0xFFFDE68A, 0xFFBFDBFE, 0xFFFBCFE8};

    public static String hex(int c) { return String.format("#%08X", c); }

    public static void colorPicker(final Context c, int init, final IntCb cb) {
        final int[] v = {Color.alpha(init), Color.red(init), Color.green(init), Color.blue(init)};
        LinearLayout root = col(c);
        int p = dp(c, 18);
        root.setPadding(p, p, p, 0);
        final View sw = new View(c);
        root.addView(sw, new LinearLayout.LayoutParams(-1, dp(c, 54)));
        final EditText hx = edit(c, "#AARRGGBB", false);
        root.addView(hx, lp(c, -1, -2, 0, 10, 0, 6));
        final SeekBar[] bars = new SeekBar[4];
        final boolean[] upd = {false};
        final Runnable sync = () -> {
            upd[0] = true;
            int col = Color.argb(v[0], v[1], v[2], v[3]);
            GradientDrawable g = rr(c, col, 14);
            g.setStroke(dp(c, 1), line());
            sw.setBackground(g);
            if (!hx.hasFocus()) hx.setText(hex(col));
            for (int i = 0; i < 4; i++) bars[i].setProgress(v[i]);
            upd[0] = false;
        };
        String[] nm = {"A", "R", "G", "B"};
        for (int i = 0; i < 4; i++) {
            final int k = i;
            LinearLayout r = row(c);
            r.addView(tv(c, nm[i], 12, sub(), true), new LinearLayout.LayoutParams(dp(c, 18), -2));
            bars[i] = new SeekBar(c);
            bars[i].setMax(255);
            bars[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar s, int pr, boolean u) { if (u) { v[k] = pr; sync.run(); } }
                @Override public void onStartTrackingTouch(SeekBar s) { }
                @Override public void onStopTrackingTouch(SeekBar s) { }
            });
            r.addView(bars[i], new LinearLayout.LayoutParams(0, -2, 1f));
            root.addView(r);
        }
        hx.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) { }
            @Override public void afterTextChanged(Editable e) {
                if (upd[0]) return;
                try {
                    String t = e.toString().trim();
                    if (t.length() == 7 || t.length() == 9) {
                        int col = Color.parseColor(t);
                        if (t.length() == 7) col = (v[0] << 24) | (col & 0xFFFFFF);
                        v[0] = Color.alpha(col); v[1] = Color.red(col); v[2] = Color.green(col); v[3] = Color.blue(col);
                        sync.run();
                    }
                } catch (Exception ex) { /* ignore */ }
            }
        });
        TextView lab = tv(c, "Presets", 12, sub(), true);
        root.addView(lab, lp(c, -2, -2, 0, 10, 0, 6));
        LinearLayout line = null;
        for (int i = 0; i < SWATCH.length; i++) {
            if (i % 8 == 0) { line = row(c); root.addView(line, lp(c, -1, -2, 0, 0, 0, 6)); }
            final int sc = SWATCH[i];
            View s = new View(c);
            GradientDrawable g = rr(c, sc, 99);
            g.setStroke(dp(c, 1), 0x33808080);
            s.setBackground(g);
            s.setOnClickListener(x -> { v[1] = Color.red(sc); v[2] = Color.green(sc); v[3] = Color.blue(sc); hx.clearFocus(); sync.run(); });
            line.addView(s, lp(c, 0, dp(c, 30) , 0, 0, 0, 0));
            LinearLayout.LayoutParams q = new LinearLayout.LayoutParams(0, dp(c, 30), 1f);
            q.setMargins(dp(c, 2), 0, dp(c, 2), 0);
            s.setLayoutParams(q);
        }
        sync.run();
        new AlertDialog.Builder(c).setTitle("Pick colour").setView(root)
                .setPositiveButton("Apply", (d, w) -> cb.got(Color.argb(v[0], v[1], v[2], v[3])))
                .setNegativeButton("Cancel", null).show();
    }
}
