package com.levi.dialog;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Random;

/**
 * Levi - login-key dialog.
 * Host usage:  Levi.show(this);
 *
 * MT Manager: open classes.dex -> com/levi/dialog/Levi.smali and replace the two strings
 *   "https://YOUR-PROJECT-default-rtdb.firebaseio.com"   (Firebase RTDB databaseURL)
 *   "LV-XXX-XXX-ST"                                      (App Connect Key from the Levi Admin app)
 * Each of them exists exactly once in the smali (inside dbUrl() / connectKey()).
 */
public class Levi {

    // ------------------------------------------------------------------ REPLACE ME (smali) ----
    static String dbUrl() { return "https://YOUR-PROJECT-default-rtdb.firebaseio.com"; }
    static String connectKey() { return "LV-XXX-XXX-ST"; }
    // -----------------------------------------------------------------------------------------

    public static String dbBase = "";   // used by media loader (set by show() / by the admin app)
    public static String dbCk = "";
    static final Handler main = new Handler(Looper.getMainLooper());
    static boolean showing = false;

    // ================================================================ catalogues ================
    public static final String[] FONT_NAMES = {"League Gothic", "Bebas Neue", "Anton", "Oswald", "Pacifico", "Lobster",
            "Dancing Script", "Caveat", "Bangers", "Righteous", "Orbitron", "Audiowide", "Press Start 2P", "Cinzel",
            "Abril Fatface", "Playfair Display", "Russo One", "Monoton", "Creepster", "Bungee"};
    public static final String[] FONT_FILES = {"league_gothic", "bebas_neue", "anton", "oswald", "pacifico", "lobster",
            "dancing_script", "caveat", "bangers", "righteous", "orbitron", "audiowide", "press_start_2p", "cinzel",
            "abril_fatface", "playfair_display", "russo_one", "monoton", "creepster", "bungee"};
    public static final String[] GRAD_NAMES = {"Sunset", "Ocean", "Peach", "Mint", "Lavender", "Midnight", "Aurora",
            "Candy", "Ember", "Forest", "Sky", "Rose", "Gold", "Mono", "Cyber", "Coral", "Cotton", "Dusk", "Neon", "Slate"};
    public static final int[][] GRADS = {
            {0xFFFF9A8B, 0xFFFF6A88}, {0xFF2193B0, 0xFF6DD5ED}, {0xFFFFECD2, 0xFFFCB69F}, {0xFF84FAB0, 0xFF8FD3F4},
            {0xFFA18CD1, 0xFFFBC2EB}, {0xFF141E30, 0xFF243B55}, {0xFF00C9FF, 0xFF92FE9D}, {0xFFFF9EEA, 0xFF8EC5FC},
            {0xFFF12711, 0xFFF5AF19}, {0xFF134E5E, 0xFF71B280}, {0xFF89F7FE, 0xFF66A6FF}, {0xFFFFC3A0, 0xFFFFAFBD},
            {0xFFF7971E, 0xFFFFD200}, {0xFFBDC3C7, 0xFF2C3E50}, {0xFF7F00FF, 0xFFE100FF}, {0xFFFF5F6D, 0xFFFFC371},
            {0xFFE0C3FC, 0xFF8EC5FC}, {0xFF2C3E50, 0xFFFD746C}, {0xFF08AEEA, 0xFF2AF598}, {0xFF485563, 0xFF29323C}};
    public static final String[] ENTER_NAMES = {"None", "Fade", "Scale up", "Slide up", "Slide down", "Slide left",
            "Slide right", "Zoom out", "Bounce", "Flip X", "Flip Y", "Rotate in", "Elastic pop", "Drop", "Swing",
            "Soft focus", "Spin zoom", "Slide + tilt", "Pulse in", "Tada"};
    public static final String[] FX_NAMES = {"None", "Gradient spin", "Pulse glow", "Slow zoom", "Drift X", "Drift Y",
            "Sway", "Shimmer", "Aurora", "Bokeh", "Stripes", "Breathe", "Waves", "Sparkle", "Rain", "Vignette pulse",
            "Hue cycle", "Spotlight", "Grid scan", "Snow"};

    // ================================================================ helpers ===================
    public static String S(JSONObject j, String k, String d) { return j.has(k) && !j.isNull(k) ? j.optString(k, d) : d; }
    public static int I(JSONObject j, String k, int d) { return j.optInt(k, d); }
    public static float F(JSONObject j, String k, double d) { return (float) j.optDouble(k, d); }
    public static boolean B(JSONObject j, String k, boolean d) { return j.optBoolean(k, d); }
    public static int col(JSONObject j, String k, String d) {
        try { return Color.parseColor(S(j, k, d)); } catch (Exception e) { return Color.parseColor(d); }
    }
    public static int dp(Context c, float v) { return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f); }

    public static String sha256(String s) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"));
            StringBuilder b = new StringBuilder();
            for (byte x : h) b.append(String.format("%02x", x));
            return b.toString();
        } catch (Exception e) { return s; }
    }

    public static String randKey(String prefix) {
        String cs = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random r = new Random();
        StringBuilder b = new StringBuilder(prefix);
        for (int g = 0; g < 2; g++) {
            b.append('-');
            for (int i = 0; i < 3; i++) b.append(cs.charAt(r.nextInt(cs.length())));
        }
        return b.append("-ST").toString();
    }

    static Typeface[] tfCache = new Typeface[20];
    public static Typeface font(Context c, int i) {
        if (i < 0 || i >= FONT_FILES.length) return null;
        if (tfCache[i] == null) {
            try { tfCache[i] = Typeface.createFromAsset(c.getAssets(), "fonts/" + FONT_FILES[i] + ".ttf"); }
            catch (Exception e) { return null; }
        }
        return tfCache[i];
    }
    static Typeface tf(Context c, int i, boolean bold) {
        Typeface t = font(c, i);
        return t != null ? t : Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL);
    }

    // ================================================================ network ===================
    public interface Res { void done(String r, String err); }
    public interface Bm { void got(Bitmap b); }

    public static String http(String method, String url, String body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(9000);
        c.setReadTimeout(25000);
        c.setRequestMethod(method);
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.getOutputStream().write(body.getBytes("UTF-8"));
        }
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String s = in == null ? "" : new String(readAll(in), "UTF-8");
        if (code >= 400) throw new Exception("HTTP " + code + " " + s);
        return s;
    }

    public static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] b = new byte[16384];
        int n;
        while ((n = in.read(b)) > 0) o.write(b, 0, n);
        in.close();
        return o.toByteArray();
    }

    public static void async(final String m, final String u, final String b, final Res cb) {
        new Thread(() -> {
            String r = null, er = null;
            try { r = http(m, u, b); }
            catch (Exception ex) { er = ex.getMessage() == null ? ex.toString() : ex.getMessage(); }
            final String fr = r, fe = er;
            main.post(() -> cb.done(fr, fe));
        }).start();
    }

    /** Firebase returns a JSON string with quotes; unwrap it. */
    public static String unq(String s) {
        try {
            if (s == null || s.equals("null")) return "";
            if (s.startsWith("\"")) return String.valueOf(new JSONTokener(s).nextValue());
        } catch (Exception e) { /* ignore */ }
        return s;
    }

    /** src = http(s) url | data:...;base64,... | db:name.version (node levi_media/<ck>/<name>) */
    public static byte[] bytes(String src) throws Exception {
        if (src.startsWith("data:")) return Base64.decode(src.substring(src.indexOf(',') + 1), Base64.DEFAULT);
        if (src.startsWith("db:")) {
            String name = src.substring(3).split("\\.")[0];
            return bytes(unq(http("GET", dbBase + "/levi_media/" + dbCk + "/" + name + ".json", null)));
        }
        HttpURLConnection c = (HttpURLConnection) new URL(src).openConnection();
        c.setConnectTimeout(9000);
        c.setReadTimeout(25000);
        c.setRequestProperty("User-Agent", "Mozilla/5.0");
        return readAll(c.getInputStream());
    }

    static final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(24 * 1024 * 1024) {
        @Override protected int sizeOf(String k, Bitmap b) { return b.getByteCount(); }
    };

    public static Bitmap decode(byte[] b, int maxPx) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(b, 0, b.length, o);
        int s = 1;
        while (o.outWidth / s > maxPx * 2 || o.outHeight / s > maxPx * 2) s *= 2;
        o = new BitmapFactory.Options();
        o.inSampleSize = s;
        return BitmapFactory.decodeByteArray(b, 0, b.length, o);
    }

    public static void loadBitmap(final String src, final int maxPx, final Bm cb) {
        final String key = src + "@" + maxPx;
        Bitmap c = cache.get(key);
        if (c != null) { cb.got(c); return; }
        new Thread(() -> {
            Bitmap bm = null;
            try { bm = decode(bytes(src), maxPx); } catch (Exception ex) { /* ignore */ }
            final Bitmap fb = bm;
            if (fb != null) cache.put(key, fb);
            main.post(() -> cb.got(fb));
        }).start();
    }

    // ================================================================ entrance animation ========
    public static void enter(final View v, int type, long dur) {
        v.animate().cancel();
        v.setAlpha(1); v.setScaleX(1); v.setScaleY(1); v.setTranslationX(0); v.setTranslationY(0);
        v.setRotation(0); v.setRotationX(0); v.setRotationY(0);
        if (type <= 0) return;
        float w = v.getWidth() > 0 ? v.getWidth() : 600, h = v.getHeight() > 0 ? v.getHeight() : 600;
        float a = 0, s = 1, tx = 0, ty = 0, r = 0, rx = 0, ry = 0;
        Interpolator ip = new DecelerateInterpolator(1.6f);
        v.setPivotX(w / 2); v.setPivotY(h / 2);
        v.setCameraDistance(9000 * v.getResources().getDisplayMetrics().density);
        switch (type) {
            case 1: break;
            case 2: s = .7f; break;
            case 3: ty = h * .45f; break;
            case 4: ty = -h * .45f; break;
            case 5: tx = w * .6f; break;
            case 6: tx = -w * .6f; break;
            case 7: s = 1.5f; break;
            case 8: s = .3f; ip = new OvershootInterpolator(2.2f); break;
            case 9: rx = 90; break;
            case 10: ry = 90; break;
            case 11: r = -25; s = .7f; break;
            case 12: s = 0; ip = new OvershootInterpolator(4f); break;
            case 13: ty = -h * 2f; ip = new BounceInterpolator(); a = 1; break;
            case 14: r = -22; v.setPivotY(0); ip = new OvershootInterpolator(3f); break;
            case 15: s = 1.15f; break;
            case 16: r = -180; s = 0; break;
            case 17: tx = -w; r = -15; break;
            case 18: s = .8f; ip = new OvershootInterpolator(6f); break;
            case 19: s = .5f; r = 10; ip = new OvershootInterpolator(5f); break;
            default: break;
        }
        v.setAlpha(a); v.setScaleX(s); v.setScaleY(s); v.setTranslationX(tx); v.setTranslationY(ty);
        v.setRotation(r); v.setRotationX(rx); v.setRotationY(ry);
        v.animate().alpha(1).scaleX(1).scaleY(1).translationX(0).translationY(0).rotation(0).rotationX(0).rotationY(0)
                .setDuration(dur).setInterpolator(ip).start();
    }

    // ================================================================ Media (image / video) =====
    public static class Media extends FrameLayout {
        final ImageView iv;
        TextureView tv;
        MediaPlayer mp;
        Surface surf;
        String cur = "", vpath;
        int vw, vh;
        float radius;

        public Media(Context c) {
            super(c);
            iv = new ImageView(c);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            addView(iv, new LayoutParams(-1, -1));
            setClipToOutline(true);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius); }
            });
        }

        public void setRadius(float r) { radius = r; invalidateOutline(); }

        GradientDrawable placeholder() {
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFFB7C9FF, 0xFFFFD6E8});
            return g;
        }

        public void clear() { cur = ""; stopVideo(); vpath = null; if (tv != null) tv.setVisibility(GONE); }

        public void set(String src, boolean video, int maxPx) {
            final String key = (video ? "v:" : "i:") + src;
            if (key.equals(cur)) return;
            cur = key;
            stopVideo();
            vpath = null;
            if (src == null || src.isEmpty()) {
                if (tv != null) tv.setVisibility(GONE);
                iv.setVisibility(VISIBLE);
                iv.setImageDrawable(placeholder());
                return;
            }
            if (video) {
                iv.setVisibility(GONE);
                ensureTv();
                tv.setVisibility(VISIBLE);
                prepareVideo(src, key);
            } else {
                if (tv != null) tv.setVisibility(GONE);
                iv.setVisibility(VISIBLE);
                iv.setImageDrawable(placeholder());
                loadBitmap(src, maxPx, b -> { if (key.equals(cur) && b != null) iv.setImageBitmap(b); });
            }
        }

        void ensureTv() {
            if (tv != null) return;
            tv = new TextureView(getContext());
            addView(tv, 0, new LayoutParams(-1, -1));
            tv.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) { surf = new Surface(st); startVideo(); }
                @Override public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) { fit(); }
                @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture st) { stopVideo(); surf = null; return true; }
                @Override public void onSurfaceTextureUpdated(SurfaceTexture st) { }
            });
        }

        void prepareVideo(final String src, final String key) {
            new Thread(() -> {
                String path = src;
                try {
                    if (!src.startsWith("http")) {
                        File f = new File(getContext().getCacheDir(), "lv_" + Math.abs(src.hashCode()) + ".mp4");
                        if (!f.exists() || f.length() == 0) {
                            FileOutputStream o = new FileOutputStream(f);
                            o.write(bytes(src));
                            o.close();
                        }
                        path = f.getAbsolutePath();
                    }
                } catch (Exception ex) { return; }
                final String fp = path;
                main.post(() -> { if (key.equals(cur)) { vpath = fp; startVideo(); } });
            }).start();
        }

        void startVideo() {
            if (surf == null || vpath == null || tv == null || tv.getVisibility() != VISIBLE) return;
            stopVideo();
            try {
                mp = new MediaPlayer();
                mp.setDataSource(vpath);
                mp.setSurface(surf);
                mp.setLooping(true);
                mp.setVolume(0, 0);
                mp.setOnVideoSizeChangedListener((m, w, h) -> { vw = w; vh = h; fit(); });
                mp.setOnPreparedListener(m -> m.start());
                mp.prepareAsync();
            } catch (Exception ex) { mp = null; }
        }

        void stopVideo() {
            if (mp != null) {
                try { mp.stop(); } catch (Exception ex) { /* ignore */ }
                try { mp.release(); } catch (Exception ex) { /* ignore */ }
                mp = null;
            }
        }

        void fit() {
            if (tv == null || vw == 0 || vh == 0 || getWidth() == 0) return;
            float va = (float) vw / vh, ba = (float) getWidth() / getHeight();
            Matrix m = new Matrix();
            m.setScale(va > ba ? va / ba : 1f, va > ba ? 1f : ba / va, getWidth() / 2f, getHeight() / 2f);
            tv.setTransform(m);
        }

        @Override protected void onSizeChanged(int w, int h, int ow, int oh) { super.onSizeChanged(w, h, ow, oh); invalidateOutline(); fit(); }
        @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); startVideo(); }
        @Override protected void onDetachedFromWindow() { super.onDetachedFromWindow(); stopVideo(); }
    }

    // ================================================================ Eye icon ==================
    public static class Eye extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path a = new Path(), b = new Path(), c = new Path();
        public boolean shown;
        public int color = Color.WHITE;

        public Eye(Context x) {
            super(x);
            a.moveTo(2, 24); a.lineTo(24, 6); a.lineTo(46, 24); a.lineTo(24, 42); a.close();
            b.moveTo(9, 24); b.lineTo(24, 12); b.lineTo(39, 24); b.lineTo(24, 36); b.close();
            c.moveTo(16, 24); c.lineTo(24, 18); c.lineTo(32, 24); c.lineTo(24, 30); c.close();
        }

        @Override protected void onDraw(Canvas cv) {
            float s = getWidth() / 48f;
            cv.scale(s, s);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.6f);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(color);
            cv.drawPath(a, p); cv.drawPath(b, p); cv.drawPath(c, p);
            if (shown) cv.drawLine(6, 42, 42, 6, p);
        }
    }

    // ================================================================ Background layer ==========
    public static class Bg extends View {
        JSONObject cfg = new JSONObject();
        Bitmap img, small;
        String imgKey = "";
        float phase;
        ValueAnimator va;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final float[] rnd = new float[96];
        final Path path = new Path();
        static Bitmap noise;
        boolean video;

        public Bg(Context c) {
            super(c);
            Random r = new Random(7);
            for (int i = 0; i < rnd.length; i++) rnd[i] = r.nextFloat();
        }

        public void set(JSONObject c) {
            cfg = c;
            String t = S(c, "bgType", "gradient");
            video = t.equals("video");
            if (t.equals("image")) {
                final String src = S(c, "bgSrc", "");
                final String k = src + "|" + I(c, "blur", 0);
                if (!k.equals(imgKey)) {
                    imgKey = k; img = null; small = null;
                    if (!src.isEmpty()) loadBitmap(src, 900, b -> {
                        if (k.equals(imgKey) && b != null) { img = b; small = blurred(b, I(cfg, "blur", 0)); invalidate(); }
                    });
                }
            } else { imgKey = ""; img = null; small = null; }
            restartAnim();
            invalidate();
        }

        Bitmap blurred(Bitmap b, int blur) {
            if (blur <= 0) return b;
            float f = 1 + blur * 0.4f;
            return Bitmap.createScaledBitmap(b, Math.max(4, (int) (b.getWidth() / f)), Math.max(4, (int) (b.getHeight() / f)), true);
        }

        int fx() { return B(cfg, "animOn", true) ? I(cfg, "bgFx", 0) : 0; }

        void restartAnim() {
            if (va != null) { va.cancel(); va = null; }
            if (fx() > 0 && isAttachedToWindow()) {
                va = ValueAnimator.ofFloat(0, 1);
                va.setDuration((long) (24000 / Math.max(1, I(cfg, "bgSpd", 5))));
                va.setRepeatCount(ValueAnimator.INFINITE);
                va.setInterpolator(new LinearInterpolator());
                va.addUpdateListener(an -> { phase = (Float) an.getAnimatedValue(); invalidate(); });
                va.start();
            }
        }

        @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); restartAnim(); }
        @Override protected void onDetachedFromWindow() { super.onDetachedFromWindow(); if (va != null) { va.cancel(); va = null; } }

        int[] gcols() {
            int c1, c2;
            if (B(cfg, "gcustom", false)) { c1 = col(cfg, "gc1", "#FF9A8B"); c2 = col(cfg, "gc2", "#FF6A88"); }
            else { int[] g = GRADS[Math.max(0, Math.min(GRADS.length - 1, I(cfg, "grad", 16)))]; c1 = g[0]; c2 = g[1]; }
            if (fx() == 16) {
                float[] h = new float[3];
                Color.colorToHSV(c1, h); h[0] = (h[0] + phase * 360) % 360; c1 = Color.HSVToColor(h);
                Color.colorToHSV(c2, h); h[0] = (h[0] + phase * 360) % 360; c2 = Color.HSVToColor(h);
            }
            return new int[]{c1, c2};
        }

        void drawBase(Canvas cv, int w, int h) {
            if (video) return;
            Bitmap bm = small != null ? small : img;
            if (bm != null && S(cfg, "bgType", "gradient").equals("image")) {
                float vr = (float) w / h, br = (float) bm.getWidth() / bm.getHeight();
                Rect s;
                if (br > vr) { int nw = (int) (bm.getHeight() * vr); s = new Rect((bm.getWidth() - nw) / 2, 0, (bm.getWidth() + nw) / 2, bm.getHeight()); }
                else { int nh = (int) (bm.getWidth() / vr); s = new Rect(0, (bm.getHeight() - nh) / 2, bm.getWidth(), (bm.getHeight() + nh) / 2); }
                p.setFilterBitmap(true);
                cv.drawBitmap(bm, s, new Rect(0, 0, w, h), p);
                return;
            }
            int[] g = gcols();
            double ang = Math.toRadians(I(cfg, "gangle", 135) + (fx() == 1 ? phase * 360 : 0));
            float r = (float) Math.hypot(w, h) / 2, cx = w / 2f, cy = h / 2f;
            float dx = (float) Math.cos(ang) * r, dy = (float) Math.sin(ang) * r;
            p.setShader(new LinearGradient(cx - dx, cy - dy, cx + dx, cy + dy, g[0], g[1], Shader.TileMode.CLAMP));
            cv.drawRect(0, 0, w, h, p);
            p.setShader(null);
        }

        @Override protected void onDraw(Canvas cv) {
            int w = getWidth(), h = getHeight();
            if (w == 0) return;
            int fx = fx();
            double tt = phase * Math.PI * 2;
            float sn = (float) Math.sin(tt), cs = (float) Math.cos(tt), t = phase;
            cv.save();
            if (fx == 3) { float s = 1.08f + 0.06f * sn; cv.scale(s, s, w / 2f, h / 2f); }
            else if (fx == 4) { cv.translate(sn * w * .05f, 0); cv.scale(1.12f, 1.12f, w / 2f, h / 2f); }
            else if (fx == 5) { cv.translate(0, sn * h * .05f); cv.scale(1.12f, 1.12f, w / 2f, h / 2f); }
            else if (fx == 6) { cv.rotate(sn * 4f, w / 2f, h / 2f); cv.scale(1.2f, 1.2f, w / 2f, h / 2f); }
            else if (fx == 11) { float s = 1.05f + 0.04f * sn; cv.scale(s, s, w / 2f, h / 2f); }
            drawBase(cv, w, h);
            cv.restore();

            p.setStyle(Paint.Style.FILL);
            switch (fx) {
                case 2:
                    p.setColor(Color.argb((int) ((0.5 + 0.5 * sn) * 55), 255, 255, 255));
                    cv.drawRect(0, 0, w, h, p);
                    break;
                case 7: {
                    float x = (t * 2.4f - 0.7f) * w;
                    p.setShader(new LinearGradient(x - w * .25f, 0, x + w * .25f, 0, new int[]{0, 0x66FFFFFF, 0}, null, Shader.TileMode.CLAMP));
                    cv.drawRect(0, 0, w, h, p);
                    p.setShader(null);
                    break;
                }
                case 8:
                    for (int i = 0; i < 2; i++) {
                        float cx = w * (0.3f + 0.4f * (float) Math.sin(tt + i * 2)), cy = h * (0.35f + 0.3f * (float) Math.cos(tt + i * 3));
                        p.setShader(new RadialGradient(cx, cy, w * .6f, i == 0 ? 0x88FF66CC : 0x8866CCFF, 0, Shader.TileMode.CLAMP));
                        cv.drawRect(0, 0, w, h, p);
                    }
                    p.setShader(null);
                    break;
                case 9:
                    p.setColor(0x33FFFFFF);
                    for (int i = 0; i < 10; i++) {
                        float y = ((rnd[i * 3 + 1] - t * (0.3f + rnd[i * 3 + 2] * 0.4f)) % 1f + 1f) % 1f * h;
                        cv.drawCircle(rnd[i * 3] * w, y, w * (0.03f + rnd[i * 3 + 2] * 0.06f), p);
                    }
                    break;
                case 10: {
                    p.setColor(0x22FFFFFF);
                    cv.save();
                    cv.rotate(-30, w / 2f, h / 2f);
                    float sw = w / 8f;
                    for (float x = -w; x < w * 2; x += sw * 2) cv.drawRect(x + t * sw * 2, -h, x + t * sw * 2 + sw, h * 2, p);
                    cv.restore();
                    break;
                }
                case 12:
                    for (int k = 0; k < 3; k++) {
                        path.reset();
                        path.moveTo(0, h);
                        for (int i = 0; i <= 20; i++) {
                            float x = w * i / 20f;
                            path.lineTo(x, h * (0.7f + 0.07f * k) + (float) Math.sin(x / w * 6.28f * (1 + k * 0.5f) + tt + k) * h * 0.03f);
                        }
                        path.lineTo(w, h);
                        path.close();
                        p.setColor(0x22FFFFFF);
                        cv.drawPath(path, p);
                    }
                    break;
                case 13:
                    for (int i = 0; i < 14; i++) {
                        float al = (float) (Math.sin(tt * 2 + rnd[i * 2] * 6.28) + 1) / 2f;
                        p.setColor(Color.argb((int) (al * 230), 255, 255, 255));
                        cv.drawCircle(rnd[i * 2] * w, rnd[i * 2 + 1] * h, w * (0.008f + 0.01f * al), p);
                    }
                    break;
                case 14:
                    p.setColor(0x55FFFFFF);
                    p.setStrokeWidth(Math.max(1.5f, w * 0.004f));
                    for (int i = 0; i < 18; i++) {
                        float x = rnd[i * 2] * w, y = ((rnd[i * 2 + 1] + t * (1 + rnd[i] )) % 1f) * h * 1.2f - h * .1f;
                        cv.drawLine(x, y, x - w * .01f, y + h * .07f, p);
                    }
                    break;
                case 15:
                    p.setShader(new RadialGradient(w / 2f, h / 2f, Math.max(w, h) * .75f, new int[]{0, Color.argb((int) (60 + 60 * (0.5 + 0.5 * sn)), 0, 0, 0)},
                            new float[]{0.5f, 1f}, Shader.TileMode.CLAMP));
                    cv.drawRect(0, 0, w, h, p);
                    p.setShader(null);
                    break;
                case 17:
                    p.setShader(new RadialGradient(w * (0.5f + 0.35f * cs), h * (0.5f + 0.35f * sn), w * .55f, 0x66FFFFFF, 0, Shader.TileMode.CLAMP));
                    cv.drawRect(0, 0, w, h, p);
                    p.setShader(null);
                    break;
                case 18: {
                    p.setColor(0x22FFFFFF);
                    p.setStrokeWidth(Math.max(1f, w * 0.002f));
                    for (int i = 1; i < 10; i++) { cv.drawLine(w * i / 10f, 0, w * i / 10f, h, p); cv.drawLine(0, h * i / 10f, w, h * i / 10f, p); }
                    float y = t * h;
                    p.setShader(new LinearGradient(0, y - h * .12f, 0, y, 0, 0x66FFFFFF, Shader.TileMode.CLAMP));
                    cv.drawRect(0, y - h * .12f, w, y, p);
                    p.setShader(null);
                    break;
                }
                case 19:
                    p.setColor(0xCCFFFFFF);
                    for (int i = 0; i < 30; i++) {
                        float x = (rnd[i * 3] + (float) Math.sin(tt + rnd[i * 3 + 1] * 6) * 0.02f) * w;
                        float y = ((rnd[i * 3 + 1] + t * (0.3f + rnd[i * 3 + 2] * 0.7f)) % 1f) * h;
                        cv.drawCircle(x, y, w * (0.004f + rnd[i * 3 + 2] * 0.008f), p);
                    }
                    break;
                default: break;
            }

            int n = I(cfg, "noise", 0);
            if (n > 0) {
                if (noise == null) {
                    int[] px = new int[96 * 96];
                    Random r = new Random(3);
                    for (int i = 0; i < px.length; i++) { int g = r.nextInt(256); px[i] = Color.rgb(g, g, g); }
                    noise = Bitmap.createBitmap(px, 96, 96, Bitmap.Config.ARGB_8888);
                }
                p.setShader(new BitmapShader(noise, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
                p.setAlpha(Math.min(255, n * 2));
                cv.drawRect(0, 0, w, h, p);
                p.setShader(null);
                p.setAlpha(255);
            }
        }
    }

    // ================================================================ The dialog card ===========
    public static class Card extends ViewGroup {
        public JSONObject cfg = new JSONObject();
        public float scale = 1f;          // admin preview uses < 1
        public final Bg bg;
        public final Media bgVid, banner;
        public final TextView title, desc, getBtn, verifyBtn, exitBtn;
        public final EditText input;
        public final Eye eye;
        public final LinearLayout field;
        float u = 3f;
        int cw, ch, lastW = -1;
        boolean dirty = true;
        final Rect[] rc = new Rect[7];
        final Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG);
        static final int R_BAN = 0, R_TIT = 1, R_DES = 2, R_FLD = 3, R_GET = 4, R_VER = 5, R_EXT = 6;

        public Card(Context c) {
            super(c);
            bgVid = new Media(c);
            bg = new Bg(c);
            banner = new Media(c);
            desc = new TextView(c);
            title = new TextView(c);
            field = new LinearLayout(c);
            input = new EditText(c);
            eye = new Eye(c);
            getBtn = new TextView(c);
            verifyBtn = new TextView(c);
            exitBtn = new TextView(c);

            title.setGravity(Gravity.CENTER);
            title.setSingleLine(true);
            title.setIncludeFontPadding(false);
            title.setLetterSpacing(0.02f);
            desc.setGravity(Gravity.CENTER);
            desc.setMaxLines(2);
            field.setOrientation(LinearLayout.HORIZONTAL);
            field.setGravity(Gravity.CENTER_VERTICAL);
            input.setBackground(null);
            input.setSingleLine(true);
            input.setPadding(0, 0, 0, 0);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            input.setTransformationMethod(PasswordTransformationMethod.getInstance());
            input.setIncludeFontPadding(false);
            field.addView(input, new LinearLayout.LayoutParams(0, -2, 1f));
            field.addView(eye, new LinearLayout.LayoutParams(10, 10));
            eye.setOnClickListener(v -> {
                eye.shown = !eye.shown;
                int sel = input.getSelectionEnd();
                input.setTransformationMethod(eye.shown ? null : PasswordTransformationMethod.getInstance());
                input.setSelection(Math.max(0, Math.min(sel, input.length())));
                eye.invalidate();
            });
            for (TextView b : new TextView[]{getBtn, verifyBtn, exitBtn}) {
                b.setGravity(Gravity.CENTER);
                b.setIncludeFontPadding(false);
                b.setClickable(true);
                b.setOnTouchListener((v, ev) -> {
                    int a = ev.getActionMasked();
                    if (a == MotionEvent.ACTION_DOWN) v.animate().scaleX(.96f).scaleY(.96f).setDuration(80).start();
                    else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    return false;
                });
            }
            exitBtn.setText("\u2715");
            addView(bgVid); addView(bg); addView(banner); addView(desc); addView(title);
            addView(field); addView(getBtn); addView(verifyBtn); addView(exitBtn);
            setClipToOutline(true);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View v, Outline o) {
                    o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), Math.min(F(cfg, "radius", 10.4) * u, Math.min(v.getWidth(), v.getHeight()) / 2f));
                }
            });
            apply(new JSONObject());
        }

        public void apply(JSONObject c) {
            cfg = c;
            dirty = true;
            boolean vid = S(c, "bgType", "gradient").equals("video");
            setBackgroundColor(vid ? 0xFF101820 : 0);
            bg.set(c);
            if (vid) { bgVid.setVisibility(VISIBLE); bgVid.set(S(c, "bgSrc", ""), true, 720); }
            else { bgVid.clear(); bgVid.setVisibility(GONE); }
            banner.set(S(c, "bnSrc", ""), S(c, "bnType", "image").equals("video"), 1100);
            requestLayout();
            invalidate();
        }

        Rect r(float x, float y, float w, float h) {
            return new Rect((int) (x * u), (int) (y * u), (int) ((x + w) * u), (int) ((y + h) * u));
        }

        @Override protected void onMeasure(int ws, int hs) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int avail = MeasureSpec.getSize(ws);
            if (avail <= 0) avail = dm.widthPixels;
            float real = Math.min(dm.widthPixels * F(cfg, "cardW", 76) / 100f, dp(getContext(), 420));
            int w = (int) Math.min(avail, real * scale);
            float d = F(cfg, "cardH", 100) - 100;
            int h = (int) (w * (100 + d) / 100f);
            u = w / 100f; cw = w; ch = h;
            float s = F(cfg, "btnS", 100) / 100f, bw = 34.7f * s, bh = 12.7f * s, cy = 82.6f + d + 6.35f;
            rc[R_BAN] = r(2.7f, 13.9f, 94.6f, 46f + d);
            rc[R_TIT] = r(0, 0, 100, 13.9f);
            rc[R_DES] = r(4.7f, 13.9f + 46f + d - 11f, 90.6f, 9f);
            rc[R_FLD] = r(5.2f, 63.7f + d, 89.7f, 13.7f);
            rc[R_GET] = r(28.55f - bw / 2, cy - bh / 2, bw, bh);
            rc[R_VER] = r(70.85f - bw / 2, cy - bh / 2, bw, bh);
            rc[R_EXT] = r(100 - 11f, 2.2f, 8.5f, 8.5f);
            if (dirty || lastW != w) { style(); dirty = false; lastW = w; }
            setMeasuredDimension(w, h);
            int ex = MeasureSpec.EXACTLY;
            bgVid.measure(MeasureSpec.makeMeasureSpec(w, ex), MeasureSpec.makeMeasureSpec(h, ex));
            bg.measure(MeasureSpec.makeMeasureSpec(w, ex), MeasureSpec.makeMeasureSpec(h, ex));
            View[] vs = {banner, title, desc, field, getBtn, verifyBtn, exitBtn};
            int[] ix = {R_BAN, R_TIT, R_DES, R_FLD, R_GET, R_VER, R_EXT};
            for (int i = 0; i < vs.length; i++)
                vs[i].measure(MeasureSpec.makeMeasureSpec(rc[ix[i]].width(), ex), MeasureSpec.makeMeasureSpec(rc[ix[i]].height(), ex));
        }

        @Override protected void onLayout(boolean ch2, int l, int t, int rr, int b) {
            bgVid.layout(0, 0, cw, ch);
            bg.layout(0, 0, cw, ch);
            View[] vs = {banner, title, desc, field, getBtn, verifyBtn, exitBtn};
            int[] ix = {R_BAN, R_TIT, R_DES, R_FLD, R_GET, R_VER, R_EXT};
            for (int i = 0; i < vs.length; i++) { Rect q = rc[ix[i]]; vs[i].layout(q.left, q.top, q.right, q.bottom); }
        }

        @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
            super.onSizeChanged(w, h, ow, oh);
            setPivotX(w / 2f); setPivotY(h / 2f);
            invalidateOutline();
        }

        void style() {
            Context x = getContext();
            JSONObject c = cfg;
            // title (shrinks to fit one line, like the html)
            String tx = S(c, "title", "To access this you need access key");
            if (B(c, "caps", true)) tx = tx.toUpperCase();
            title.setText(tx);
            title.setTypeface(font(x, I(c, "fTitle", 0)) != null ? font(x, I(c, "fTitle", 0)) : Typeface.create("sans-serif-condensed", Typeface.BOLD));
            title.setTextColor(col(c, "cTitle", "#000000"));
            float tsz = 9.2f * u * F(c, "sTitle", 100) / 100f;
            Paint tp = new Paint(title.getPaint());
            tp.setTextSize(tsz);
            tp.setTypeface(title.getTypeface());
            float tw = tp.measureText(tx);
            if (tw > 96 * u) tsz *= 96 * u / tw;
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, tsz);

            // field
            GradientDrawable fg = new GradientDrawable();
            fg.setColor(col(c, "cField", "#5214283C"));
            fg.setCornerRadius(13.7f * u / 2f * F(c, "fieldR", 100) / 100f);
            fg.setStroke(Math.max(1, (int) (F(c, "fieldBW", 0.75) * u)), col(c, "cFieldB", "#EBFFFFFF"));
            field.setBackground(fg);
            field.setPadding((int) (3.8f * u), 0, (int) (3.5f * u), 0);
            int ic = col(c, "cInput", "#FFFFFF");
            input.setTextColor(ic);
            input.setHintTextColor((ic & 0x00FFFFFF) | 0xB8000000);
            input.setHint(S(c, "hint", "enter your key here. . . . ."));
            input.setTypeface(tf(x, I(c, "fInput", -1), false));
            input.setTextSize(TypedValue.COMPLEX_UNIT_PX, 3.6f * u * F(c, "sInput", 100) / 100f);
            input.setLetterSpacing(0.02f);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) eye.getLayoutParams();
            lp.width = lp.height = (int) (5.6f * u);
            eye.setLayoutParams(lp);
            eye.color = ic;
            eye.invalidate();

            // buttons
            float s = F(c, "btnS", 100) / 100f;
            styleBtn(getBtn, S(c, "getTxt", "Get Key"), s);
            styleBtn(verifyBtn, S(c, "verTxt", "Verify"), s);

            // banner
            banner.setRadius(F(c, "bnR", 5.8) * u);

            // description overlay
            String ds = S(c, "desc", "");
            desc.setVisibility(ds.isEmpty() ? GONE : VISIBLE);
            desc.setText(ds);
            desc.setTextColor(col(c, "cDesc", "#FFFFFF"));
            desc.setTypeface(tf(x, I(c, "fDesc", -1), false));
            desc.setTextSize(TypedValue.COMPLEX_UNIT_PX, 3.3f * u * F(c, "sDesc", 100) / 100f);
            GradientDrawable dg = new GradientDrawable();
            dg.setColor(0x66000000);
            dg.setCornerRadius(3 * u);
            desc.setBackground(dg);
            desc.setPadding((int) (2 * u), 0, (int) (2 * u), 0);

            // exit chip
            exitBtn.setVisibility(B(c, "showExit", false) ? VISIBLE : GONE);
            exitBtn.setTextColor(0xFFFFFFFF);
            exitBtn.setTextSize(TypedValue.COMPLEX_UNIT_PX, 4f * u);
            GradientDrawable eg = new GradientDrawable();
            eg.setShape(GradientDrawable.OVAL);
            eg.setColor(0x66000000);
            exitBtn.setBackground(eg);

            invalidateOutline();
        }

        void styleBtn(TextView b, String txt, float s) {
            JSONObject c = cfg;
            GradientDrawable g = new GradientDrawable();
            g.setColor(col(c, "cBtn", "#61FFFFFF"));
            g.setCornerRadius(12.7f * u * s / 2f * F(c, "btnR", 100) / 100f);
            int bw = (int) (F(c, "btnBW", 0) * u);
            if (bw > 0) g.setStroke(bw, col(c, "cBtnB", "#FFFFFFFF"));
            b.setBackground(g);
            b.setText(txt);
            b.setTextColor(col(c, "cBtnT", "#000000"));
            b.setTypeface(tf(getContext(), I(c, "fBtn", -1), true));
            b.setTextSize(TypedValue.COMPLEX_UNIT_PX, 4.6f * u * s);
        }

        @Override protected void dispatchDraw(Canvas cv) {
            super.dispatchDraw(cv);
            float bw = F(cfg, "cardBW", 0) * u;
            if (bw > 0.5f) {
                bp.setStyle(Paint.Style.STROKE);
                bp.setStrokeWidth(bw);
                bp.setColor(col(cfg, "cCardB", "#FFFFFFFF"));
                float rad = Math.min(F(cfg, "radius", 10.4) * u, Math.min(cw, ch) / 2f);
                cv.drawRoundRect(new RectF(bw / 2, bw / 2, cw - bw / 2, ch - bw / 2), rad, rad, bp);
            }
        }

        public void shake() {
            ObjectAnimator.ofFloat(field, "translationX", 0, -14, 14, -10, 10, -5, 5, 0).setDuration(380).start();
        }

        public void playEnter() {
            if (B(cfg, "animOn", true)) enter(this, I(cfg, "enter", 8), I(cfg, "enterMs", 450));
        }
    }

    // ================================================================ Host (dialog flow) ========
    public static void show(final Activity a) {
        if (showing) return;
        dbBase = dbUrl().trim().replaceAll("/+$", "");
        dbCk = connectKey().trim();
        new Host(a).start();
    }

    static class Host {
        final Activity a;
        final SharedPreferences sp;
        Dialog dlg, loading;
        Card card;
        String loginKey = "", raw = "";
        boolean enabled = false, found = false, closed = false, busy = false;
        JSONObject cfg = new JSONObject();

        Host(Activity a) { this.a = a; sp = a.getSharedPreferences("levi_auth", 0); }

        String url() { return dbBase + "/levi_apps/" + dbCk + ".json"; }

        void start() {
            final String saved = sp.getString("ok_" + dbCk, "");
            if (!saved.isEmpty()) {
                // already verified: silently re-check (key rotated by admin => ask again)
                async("GET", url(), null, (r, er) -> {
                    if (er != null || !parse(r)) return;
                    if (enabled && !loginKey.isEmpty() && !loginKey.equals(saved)) {
                        sp.edit().remove("ok_" + dbCk).apply();
                        present();
                    }
                });
                return;
            }
            showing = true;
            showLoading();
            async("GET", url(), null, (r, er) -> {
                dismissLoading();
                if (a.isFinishing()) { showing = false; return; }
                if (er == null) parse(r);
                if (er == null && found && !enabled) { showing = false; return; }   // dialog switched off by admin
                present();
                if (er != null) toast("No connection to server");
                else if (!found) toast("Invalid app connect key / database");
            });
        }

        boolean parse(String r) {
            try {
                if (r == null || r.trim().equals("null")) { found = false; return true; }
                JSONObject o = new JSONObject(r);
                found = true;
                loginKey = o.optString("loginKey", "");
                enabled = o.optBoolean("enabled", false);
                JSONObject cf = o.optJSONObject("cfg");
                cfg = cf == null ? new JSONObject() : cf;
                String sig = String.valueOf(cfg) + enabled + loginKey;
                boolean changed = !sig.equals(raw);
                raw = sig;
                return changed || true;
            } catch (Exception ex) { return false; }
        }

        void toast(String m) { Toast.makeText(a, m, Toast.LENGTH_SHORT).show(); }

        void showLoading() {
            loading = base(true);
            FrameLayout root = new FrameLayout(a);
            ProgressBar pb = new ProgressBar(a);
            root.addView(pb, new FrameLayout.LayoutParams(dp(a, 44), dp(a, 44), Gravity.CENTER));
            loading.setContentView(root);
            try { loading.show(); } catch (Exception ex) { /* ignore */ }
        }

        void dismissLoading() { try { if (loading != null) loading.dismiss(); } catch (Exception ex) { /* ignore */ } loading = null; }

        Dialog base(boolean exitOnBack) {
            Dialog d = new Dialog(a);
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
            d.setCancelable(false);
            d.setCanceledOnTouchOutside(false);
            d.setOnKeyListener((di, kc, ev) -> {
                if (kc == KeyEvent.KEYCODE_BACK) { if (ev.getAction() == KeyEvent.ACTION_UP) exitApp(); return true; }
                return false;
            });
            Window w = d.getWindow();
            w.setBackgroundDrawable(new ColorDrawable(0));
            w.setLayout(-1, -1);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setDimAmount(0.6f);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            return d;
        }

        void exitApp() {
            closed = true; showing = false;
            try { if (dlg != null) dlg.dismiss(); } catch (Exception ex) { /* ignore */ }
            dismissLoading();
            a.finishAffinity();
            main.postDelayed(() -> android.os.Process.killProcess(android.os.Process.myPid()), 200);
        }

        void present() {
            if (a.isFinishing() || closed) { showing = false; return; }
            showing = true;
            dlg = base(true);
            dlg.getWindow().setDimAmount(F(cfg, "dim", 60) / 100f);
            FrameLayout root = new FrameLayout(a);
            card = new Card(a);
            card.apply(cfg);
            root.addView(card, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
            dlg.setContentView(root);
            card.getBtn.setOnClickListener(v -> openGetKey());
            card.verifyBtn.setOnClickListener(v -> verify());
            card.exitBtn.setOnClickListener(v -> exitApp());
            dlg.show();
            card.post(() -> card.playEnter());
            poll();
        }

        void poll() {
            main.postDelayed(() -> {
                if (closed || dlg == null || !dlg.isShowing()) return;
                async("GET", url(), null, (r, er) -> {
                    if (er == null && !closed) {
                        String before = raw;
                        parse(r);
                        if (found && !enabled) { closeOk(false); return; }
                        if (!before.equals(raw) && card != null) card.apply(cfg);
                    }
                    poll();
                });
            }, 4000);
        }

        void closeOk(boolean verified) {
            closed = true; showing = false;
            if (verified) sp.edit().putString("ok_" + dbCk, loginKey).apply();
            final Card c = card;
            c.animate().alpha(0f).scaleX(.9f).scaleY(.9f).setDuration(180).withEndAction(() -> {
                try { dlg.dismiss(); } catch (Exception ex) { /* ignore */ }
            }).start();
        }

        void verify() {
            if (busy) return;
            final String v = card.input.getText().toString().trim();
            if (v.isEmpty()) { card.shake(); toast("Please enter your key."); return; }
            busy = true;
            final CharSequence old = card.verifyBtn.getText();
            card.verifyBtn.setText("...");
            async("GET", url(), null, (r, er) -> {
                busy = false;
                card.verifyBtn.setText(old);
                if (er != null) { toast("No connection to server"); return; }
                parse(r);
                if (!found) { card.shake(); toast("Invalid app connect key / database"); return; }
                if (!enabled) { closeOk(false); return; }
                if (!loginKey.isEmpty() && v.equals(loginKey)) { toast("Verified"); closeOk(true); }
                else { card.shake(); toast("Invalid key"); }
            });
        }

        void openGetKey() {
            String u = S(cfg, "getUrl", "").trim();
            if (u.isEmpty()) { toast("Get Key link not set"); return; }
            if (!u.startsWith("http")) u = "https://" + u;
            try { a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); } catch (Exception ex) { toast("Can't open link"); }
        }
    }
}
