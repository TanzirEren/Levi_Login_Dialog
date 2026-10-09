package com.levi.admin;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
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
    public static boolean dark = false, anim = true, aurora = true, compress = true;
    public static int glass = 70, fontIdx = 9, maxMb = 5, accent = 0;
    public static int PRI = 0xFF12B76A, PRI2 = 0xFF5EEAD4;
    public static final int RED = 0xFFE5484D, GRN = 0xFF12B76A;
    public static final String[] ACCENT_NAMES = {"Emerald", "Forest", "Mint", "Lime", "Jade"};
    static final int[][] ACCENTS = {{0xFF12B76A, 0xFF5EEAD4}, {0xFF15803D, 0xFF4ADE80}, {0xFF14B8A6, 0xFF99F6E4}, {0xFF65A30D, 0xFFBEF264}, {0xFF059669, 0xFFA7F3D0}};
    public static final int[] UI_FONTS = {9, 11, 10, 5, 4, 15, 13, 1, 3, 6, -1};
    public static final String[] UI_FONT_NAMES = {"Righteous", "Audiowide", "Orbitron", "Lobster", "Pacifico", "Playfair Display", "Cinzel", "Bebas Neue", "Oswald", "Dancing Script", "System"};

    public static void setAccent(int i) { accent = Math.max(0, Math.min(ACCENTS.length - 1, i)); PRI = ACCENTS[accent][0]; PRI2 = ACCENTS[accent][1]; }

    static int mix(int a, int b, float t) {
        return Color.argb(255, (int) (Color.red(a) * (1 - t) + Color.red(b) * t), (int) (Color.green(a) * (1 - t) + Color.green(b) * t), (int) (Color.blue(a) * (1 - t) + Color.blue(b) * t));
    }
    static int alpha(int c, float f) { return (c & 0xFFFFFF) | ((int) (Color.alpha(c) * f) << 24); }

    public static int bg() { return dark ? 0xFF06120D : 0xFFF1FAF5; }
    public static int surf() { return dark ? Color.argb(100 + glass * 12 / 10, 20, 40, 31) : Color.argb(90 + glass * 14 / 10, 255, 255, 255); }
    public static int surf2() { return dark ? 0x22FFFFFF : (PRI & 0xFFFFFF) | 0x1A000000; }
    public static int tx() { return dark ? 0xFFE8F5EE : 0xFF0F2A1D; }
    public static int sub() { return dark ? 0xFF8FB0A0 : 0xFF5B7A69; }
    public static int priC() { return dark ? mix(0xFF0B1A13, PRI, 0.28f) : mix(0xFFFFFFFF, PRI, 0.16f); }
    public static int line() { return dark ? 0x33FFFFFF : (PRI & 0xFFFFFF) | 0x44000000; }
    public static int glassEdge() { return dark ? 0x30FFFFFF : 0xB3FFFFFF; }

    public static Typeface hf(Context c) {
        int i = fontIdx >= 0 && fontIdx < UI_FONTS.length ? UI_FONTS[fontIdx] : -1;
        Typeface f = i >= 0 ? Levi.font(c, i) : null;
        return f != null ? f : Typeface.DEFAULT_BOLD;
    }

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
        if (bold) t.setTypeface(hf(c));
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
        l.setBackground(glassBg(c, 24));
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        return l;
    }

    public static GradientDrawable glassBg(Context c, float r) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{surf(), alpha(surf(), 0.72f)});
        g.setCornerRadius(dp(c, r));
        g.setStroke(dp(c, 1), glassEdge());
        return g;
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
        if (filled) t.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{PRI, mix(PRI, PRI2, 0.55f)}) {{ setCornerRadius(dp(c, 99)); }});
        else t.setBackground(rr(c, priC(), 99));
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

    // ---------------------------------------------------------------- animated aurora backdrop ---
    public static class Aurora extends View {
        float t;
        ValueAnimator va;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        public Aurora(Context c) { super(c); }

        public void restart() {
            if (va != null) { va.cancel(); va = null; }
            if (aurora && anim && isAttachedToWindow()) {
                va = ValueAnimator.ofFloat(0, 1);
                va.setDuration(26000);
                va.setRepeatCount(ValueAnimator.INFINITE);
                va.setInterpolator(new android.view.animation.LinearInterpolator());
                va.addUpdateListener(a -> { t = (Float) a.getAnimatedValue(); invalidate(); });
                va.start();
            }
            invalidate();
        }

        @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); restart(); }
        @Override protected void onDetachedFromWindow() { super.onDetachedFromWindow(); if (va != null) va.cancel(); }

        @Override protected void onDraw(Canvas cv) {
            int w = getWidth(), h = getHeight();
            if (w == 0) return;
            p.setShader(new LinearGradient(0, 0, 0, h, dark ? new int[]{0xFF0A1A13, 0xFF04100B} : new int[]{0xFFFFFFFF, 0xFFE0F4E8}, null, Shader.TileMode.CLAMP));
            cv.drawRect(0, 0, w, h, p);
            if (!aurora) { p.setShader(null); return; }
            double a = t * Math.PI * 2;
            int[] cols = {PRI, PRI2, dark ? 0xFF0F766E : 0xFFFFFFFF};
            float[] al = dark ? new float[]{.30f, .20f, .30f} : new float[]{.34f, .38f, .90f};
            for (int i = 0; i < 3; i++) {
                float cx = w * (0.5f + 0.42f * (float) Math.sin(a + i * 2.1)), cy = h * (0.5f + 0.38f * (float) Math.cos(a * (i == 1 ? 2 : 1) + i * 1.7));
                int c = (cols[i] & 0xFFFFFF) | ((int) (255 * al[i]) << 24);
                p.setShader(new RadialGradient(cx, cy, Math.max(w, h) * (0.55f + 0.1f * i), c, c & 0xFFFFFF, Shader.TileMode.CLAMP));
                cv.drawRect(0, 0, w, h, p);
            }
            p.setShader(null);
        }
    }

    // ---------------------------------------------------------------- nav icons (drawn, no fonts) ---
    public static class NavIcon extends View {
        final int type;
        public int color = 0xFF000000;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final android.graphics.Path path = new android.graphics.Path();

        public NavIcon(Context c, int type) { super(c); this.type = type; }

        @Override protected void onDraw(Canvas cv) {
            float s = getWidth();
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(s * 0.09f);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setColor(color);
            if (type == 0) {
                path.reset();
                path.moveTo(.14f * s, .48f * s); path.lineTo(.5f * s, .16f * s); path.lineTo(.86f * s, .48f * s);
                path.moveTo(.24f * s, .42f * s); path.lineTo(.24f * s, .84f * s); path.lineTo(.76f * s, .84f * s); path.lineTo(.76f * s, .42f * s);
                cv.drawPath(path, p);
                cv.drawLine(.43f * s, .84f * s, .43f * s, .62f * s, p);
                cv.drawLine(.57f * s, .84f * s, .57f * s, .62f * s, p);
            } else if (type == 1) {
                float r = .09f * s;
                cv.drawRoundRect(.14f * s, .14f * s, .45f * s, .45f * s, r, r, p);
                cv.drawRoundRect(.55f * s, .14f * s, .86f * s, .45f * s, r, r, p);
                cv.drawRoundRect(.14f * s, .55f * s, .45f * s, .86f * s, r, r, p);
                cv.drawRoundRect(.55f * s, .55f * s, .86f * s, .86f * s, r, r, p);
            } else if (type == 2) {
                cv.drawRoundRect(.2f * s, .12f * s, .8f * s, .88f * s, .12f * s, .12f * s, p);
                cv.drawLine(.34f * s, .38f * s, .66f * s, .38f * s, p);
                cv.drawLine(.34f * s, .56f * s, .58f * s, .56f * s, p);
            } else {
                cv.drawCircle(.5f * s, .5f * s, .17f * s, p);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    cv.drawLine(.5f * s + (float) Math.cos(a) * .30f * s, .5f * s + (float) Math.sin(a) * .30f * s,
                            .5f * s + (float) Math.cos(a) * .42f * s, .5f * s + (float) Math.sin(a) * .42f * s, p);
                }
            }
        }
    }
}
