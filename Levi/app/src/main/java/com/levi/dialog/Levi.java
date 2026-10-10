package com.levi.dialog;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
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
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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
import java.util.Locale;
import java.util.Random;

/**
 * Levi - login-key dialog.
 * Host usage (in the app's main Activity.onCreate):  Levi.show(this);
 *
 * MT Manager: open classes.dex -> com/levi/dialog/Levi.smali.
 * The two placeholders are the FIRST fields at the top of the smali file:
 *   APP_ACCESS_KEY        = "LV-XXX-XXX-ST"                                       (App Connect Key from Levi Admin)
 *   FIREBASE_DATABASE_URL = "https://YOUR-PROJECT-default-rtdb.firebaseio.com"    (your Firebase RTDB databaseURL)
 * Replace the text between the quotes, save, sign. Each value exists exactly once.
 */
public class Levi {

    // ------------------------------------------------------------ REPLACE ME (top of smali) ----
    public static String APP_ACCESS_KEY = "LV-XXX-XXX-ST";
    public static String FIREBASE_DATABASE_URL = "https://YOUR-PROJECT-default-rtdb.firebaseio.com";
    // -----------------------------------------------------------------------------------------

    static String dbUrl() { return FIREBASE_DATABASE_URL; }
    static String connectKey() { return APP_ACCESS_KEY; }

    public static String dbBase = "";   // used by media loader (set by show() / by the admin app)
    public static String dbCk = "";
    static final Handler main = new Handler(Looper.getMainLooper());
    static Host cur;                 // dialog currently managed
    static boolean passed = false;   // verified (or dialog disabled) -> never show again this run
    static boolean hooked = false, silentStarted = false;

    // ================================================================ catalogues ================
    public static final String[] FONT_NAMES = {"League Gothic", "Bebas Neue", "Anton", "Oswald", "Pacifico", "Lobster",
            "Dancing Script", "Caveat", "Bangers", "Righteous", "Orbitron", "Audiowide", "Press Start 2P", "Cinzel",
            "Abril Fatface", "Playfair Display", "Russo One", "Monoton", "Creepster", "Bungee"};
    public static final String[] FONT_FILES = {"league_gothic", "bebas_neue", "anton", "oswald", "pacifico", "lobster",
            "dancing_script", "caveat", "bangers", "righteous", "orbitron", "audiowide", "press_start_2p", "cinzel",
            "abril_fatface", "playfair_display", "russo_one", "monoton", "creepster", "bungee"};
    public static final String[] GRAD_NAMES = {"Sunset", "Ocean", "Peach", "Mint", "Lavender", "Midnight", "Aurora",
            "Candy", "Ember", "Forest", "Sky", "Rose", "Gold", "Mono", "Cyber", "Coral", "Cotton", "Dusk", "Neon", "Slate",
            "Levi Violet", "Levi Glow", "Emerald", "Lime", "Teal Dream", "Pearl", "Blush", "Ice", "Berry", "Citrus"};
    public static final int[][] GRADS = {
            {0xFFFF9A8B, 0xFFFF6A88}, {0xFF2193B0, 0xFF6DD5ED}, {0xFFFFECD2, 0xFFFCB69F}, {0xFF84FAB0, 0xFF8FD3F4},
            {0xFFA18CD1, 0xFFFBC2EB}, {0xFF141E30, 0xFF243B55}, {0xFF00C9FF, 0xFF92FE9D}, {0xFFFF9EEA, 0xFF8EC5FC},
            {0xFFF12711, 0xFFF5AF19}, {0xFF134E5E, 0xFF71B280}, {0xFF89F7FE, 0xFF66A6FF}, {0xFFFFC3A0, 0xFFFFAFBD},
            {0xFFF7971E, 0xFFFFD200}, {0xFFBDC3C7, 0xFF2C3E50}, {0xFF7F00FF, 0xFFE100FF}, {0xFFFF5F6D, 0xFFFFC371},
            {0xFFE0C3FC, 0xFF8EC5FC}, {0xFF2C3E50, 0xFFFD746C}, {0xFF08AEEA, 0xFF2AF598}, {0xFF485563, 0xFF29323C},
            {0xFF272533, 0xFF8049AC}, {0xFF47345E, 0xFFB57BEE}, {0xFF11998E, 0xFF38EF7D}, {0xFFA8E063, 0xFF56AB2F},
            {0xFF43CEA2, 0xFF185A9D}, {0xFFF5F7FA, 0xFFB8C6DB}, {0xFFFFDDE1, 0xFFEE9CA7}, {0xFFE0EAFC, 0xFFCFDEF3},
            {0xFF8E2DE2, 0xFF4A00E0}, {0xFFFDC830, 0xFF37ECBA}};
    public static final String[] ENTER_NAMES = {"None", "Fade", "Scale up", "Slide up", "Slide down", "Slide left",
            "Slide right", "Zoom out", "Bounce", "Flip X", "Flip Y", "Rotate in", "Elastic pop", "Drop", "Swing",
            "Soft focus", "Spin zoom", "Slide + tilt", "Pulse in", "Tada"};
    public static final String[] FX_NAMES = {"None", "Gradient spin", "Pulse glow", "Slow zoom", "Drift X", "Drift Y",
            "Sway", "Shimmer", "Aurora", "Bokeh", "Stripes", "Breathe", "Waves", "Sparkle", "Rain", "Vignette pulse",
            "Hue cycle", "Spotlight", "Grid scan", "Snow", "Color flow"};

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

    // ================================================================ dialog designs (v2) =======
    public static final String[] DESIGN_NAMES = {"Classic Glass", "Neon Cyber", "Hero Banner", "Bottom Sheet", "iOS Frost", "Ticket Pass", "Terminal", "Aurora Orb"};
    public static final String[] DESIGN_INFO = {"Soft glass card, pill input", "Cyber neon, scanlines, glow frame", "Photo hero with amber CTA",
            "Slide-up sheet, underline input", "Centered alert, split buttons", "Boarding-pass ticket, barcode", "Hacker terminal window", "Glowing orb, gradient CTA"};
    public static final float[] DESIGN_H0 = {100, 104, 118, 90, 82, 108, 98, 110};
    static final float[] TSZ = {9.2f, 7.2f, 8.6f, 7.4f, 6.4f, 7.6f, 5.8f, 8.4f};
    static final float[] INSZ = {3.6f, 3.4f, 3.6f, 3.8f, 3.6f, 3.4f, 3.4f, 3.7f};
    static final float[] BSZ = {4.6f, 4.0f, 4.4f, 4.4f, 4.6f, 4.0f, 3.8f, 4.6f};
    static final float[] DSZ = {3.3f, 3.0f, 3.4f, 3.4f, 3.3f, 3.1f, 3.0f, 3.4f};
    public static final String[] DESIGN_DEFAULTS = {
            "{\"design\":0,\"bgType\":\"gradient\",\"grad\":16,\"gcustom\":false,\"gc1\":\"#FF9A8B\",\"gc2\":\"#FF6A88\",\"gangle\":135,\"bgFx\":20,\"noise\":0,\"blur\":0,\"radius\":10.4,\"cardW\":76,\"cardH\":100,\"cardBW\":0,\"tAlign\":0,\"fieldStyle\":0,\"btnStyle\":0,\"btnStyleV\":0,\"fieldR\":100,\"fieldBW\":0.75,\"btnR\":100,\"btnBW\":0,\"btnS\":100,\"sTitle\":100,\"sDesc\":100,\"sInput\":100,\"sTag\":100,\"cTitle\":\"#000000\",\"cDesc\":\"#FFFFFF\",\"cAcc\":\"#FFFFFF\",\"cTag\":\"#12B76A\",\"cField\":\"#5214283C\",\"cFieldB\":\"#EBFFFFFF\",\"cInput\":\"#FFFFFF\",\"cGetBg\":\"#61FFFFFF\",\"cGetT\":\"#000000\",\"cGetB\":\"#FFFFFFFF\",\"cVerBg\":\"#61FFFFFF\",\"cVerBg2\":\"#61FFFFFF\",\"cVerT\":\"#000000\",\"cVerB\":\"#FFFFFFFF\",\"tag\":\"\",\"tagChip\":false,\"bnR\":5.8,\"descChip\":true,\"fTitle\":0,\"fTag\":-1,\"fBtn\":-1,\"fInput\":-1,\"fDesc\":-1,\"deco\":true,\"enter\":8,\"showExit\":false,\"caps\":true}",
            "{\"design\":1,\"bgType\":\"gradient\",\"grad\":16,\"gcustom\":true,\"gc1\":\"#07031A\",\"gc2\":\"#1B0B3B\",\"gangle\":160,\"bgFx\":18,\"noise\":6,\"blur\":0,\"radius\":4,\"cardW\":88,\"cardH\":100,\"cardBW\":0,\"tAlign\":1,\"fieldStyle\":4,\"btnStyle\":2,\"btnStyleV\":0,\"fieldR\":25,\"fieldBW\":0.5,\"btnR\":22,\"btnBW\":0,\"btnS\":100,\"sTitle\":100,\"sDesc\":100,\"sInput\":100,\"sTag\":100,\"cTitle\":\"#E6FDFF\",\"cDesc\":\"#CFFAFE\",\"cAcc\":\"#22E4FF\",\"cTag\":\"#22E4FF\",\"cField\":\"#33000000\",\"cFieldB\":\"#FF22E4FF\",\"cInput\":\"#E6FDFF\",\"cGetBg\":\"#00000000\",\"cGetT\":\"#22E4FF\",\"cGetB\":\"#FF22E4FF\",\"cVerBg\":\"#FF22E4FF\",\"cVerBg2\":\"#FFB026FF\",\"cVerT\":\"#FF05030F\",\"cVerB\":\"#00000000\",\"tag\":\"// ACCESS REQUIRED\",\"tagChip\":false,\"bnR\":2,\"descChip\":true,\"fTitle\":10,\"fTag\":-2,\"fBtn\":10,\"fInput\":-2,\"fDesc\":-2,\"deco\":true,\"enter\":10,\"showExit\":false,\"caps\":true}",
            "{\"design\":2,\"bgType\":\"gradient\",\"grad\":16,\"gcustom\":true,\"gc1\":\"#0F172A\",\"gc2\":\"#1E293B\",\"gangle\":180,\"bgFx\":0,\"noise\":0,\"blur\":0,\"radius\":8,\"cardW\":82,\"cardH\":100,\"cardBW\":0,\"tAlign\":1,\"fieldStyle\":0,\"btnStyle\":4,\"btnStyleV\":0,\"fieldR\":40,\"fieldBW\":0.5,\"btnR\":45,\"btnBW\":0,\"btnS\":100,\"sTitle\":100,\"sDesc\":100,\"sInput\":100,\"sTag\":100,\"cTitle\":\"#FFFFFF\",\"cDesc\":\"#CBD5E1\",\"cAcc\":\"#F59E0B\",\"cTag\":\"#0F172A\",\"cField\":\"#1FFFFFFF\",\"cFieldB\":\"#33FFFFFF\",\"cInput\":\"#FFFFFF\",\"cGetBg\":\"#00000000\",\"cGetT\":\"#FBBF24\",\"cGetB\":\"#00000000\",\"cVerBg\":\"#FFF59E0B\",\"cVerBg2\":\"#FFEF4444\",\"cVerT\":\"#FFFFFFFF\",\"cVerB\":\"#00000000\",\"tag\":\"PREMIUM\",\"tagChip\":true,\"bnR\":0,\"descChip\":false,\"fTitle\":15,\"fTag\":-1,\"fBtn\":-1,\"fInput\":-1,\"fDesc\":-1,\"deco\":true,\"enter\":2,\"showExit\":false,\"caps\":true}",
            "{\"design\":3,\"bgType\":\"gradient\",\"grad\":16,\"gcustom\":true,\"gc1\":\"#FFFFFF\",\"gc2\":\"#F1F5F9\",\"gangle\":180,\"bgFx\":0,\"noise\":0,\"blur\":0,\"radius\":8,\"cardW\":100,\"cardH\":100,\"cardBW\":0,\"tAlign\":1,\"fieldStyle\":1,\"btnStyle\":2,\"btnStyleV\":0,\"fieldR\":30,\"fieldBW\":0.45,\"btnR\":40,\"btnBW\":0,\"btnS\":100,\"sTitle\":100,\"sDesc\":100,\"sInput\":100,\"sTag\":100,\"cTitle\":\"#0F172A\",\"cDesc\":\"#64748B\",\"cAcc\":\"#CBD5E1\",\"cTag\":\"#12B76A\",\"cField\":\"#00000000\",\"cFieldB\":\"#FF0F172A\",\"cInput\":\"#0F172A\",\"cGetBg\":\"#00000000\",\"cGetT\":\"#0F172A\",\"cGetB\":\"#FFCBD5E1\",\"cVerBg\":\"#FF0F172A\",\"cVerBg2\":\"#FF334155\",\"cVerT\":\"#FFFFFFFF\",\"cVerB\":\"#00000000\",\"tag\":\"SECURE\",\"tagChip\":false,\"bnR\":50,\"descChip\":false,\"fTitle\":3,\"fTag\":-1,\"fBtn\":-1,\"fInput\":-1,\"fDesc\":-1,\"deco\":true,\"enter\":3,\"showExit\":false,\"caps\":true}",
            "{\"design\":4,\"bgType\":\"gradient\",\"grad\":16,\"gcustom\":true,\"gc1\":\"#F2F8FAFF\",\"gc2\":\"#EDEFF3FA\",\"gangle\":180,\"bgFx\":0,\"noise\":0,\"blur\":0,\"radius\":7,\"cardW\":70,\"cardH\":100,\"cardBW\":0,\"tAlign\":0,\"fieldStyle\":0,\"btnStyle\":1,\"btnStyleV\":1,\"fieldR\":28,\"fieldBW\":0,\"btnR\":0,\"btnBW\":0,\"btnS\":100,\"sTitle\":100,\"sDesc\":100,\"sInput\":100,\"sTag\":100,\"cTitle\":\"#111827\",\"cDesc\":\"#6B7280\",\"cAcc\":\"#C7CBD6\",\"cTag\":\"#6B7280\",\"cField\":\"#12000000\",\"cFieldB\":\"#00000000\",\"cInput\":\"#111827\",\"cGetBg\":\"#00000000\",\"cGetT\":\"#007AFF\",\"cGetB\":\"#00000000\",\"cVerBg\":\"#00000000\",\"cVerBg2\":\"#00000000\",\"cVerT\":\"#007AFF\",\"cVerB\":\"#00000000\",\"tag\":\"\",\"tagChip\":false,\"bnR\":50,\"descChip\":false,\"fTitle\":-1,\"fTag\":-1,\"fBtn\":-1,\"fInput\":-1,\"fDesc\":-1,\"deco\":true,\"enter\":18,\"showExit\":false,\"caps\":true}",
            "{\"design\":5,\"bgType\":\"gradient\",\"grad\":16,\"gcustom\":true,\"gc1\":\"#FFFBF0\",\"gc2\":\"#FFEFD0\",\"gangle\":160,\"bgFx\":0,\"noise\":0,\"blur\":0,\"radius\":5,\"cardW\":80,\"cardH\":100,\"cardBW\":0,\"tAlign\":1,\"fieldStyle\":2,\"btnStyle\":2,\"btnStyleV\":0,\"fieldR\":10,\"fieldBW\":0.4,\"btnR\":30,\"btnBW\":0,\"btnS\":100,\"sTitle\":100,\"sDesc\":100,\"sInput\":100,\"sTag\":100,\"cTitle\":\"#3B2A1A\",\"cDesc\":\"#7A5C3E\",\"cAcc\":\"#B45309\",\"cTag\":\"#B45309\",\"cField\":\"#00000000\",\"cFieldB\":\"#FFB45309\",\"cInput\":\"#3B2A1A\",\"cGetBg\":\"#00000000\",\"cGetT\":\"#B45309\",\"cGetB\":\"#FFB45309\",\"cVerBg\":\"#FFB45309\",\"cVerBg2\":\"#FFD97706\",\"cVerT\":\"#FFFFFFFF\",\"cVerB\":\"#00000000\",\"tag\":\"ACCESS PASS\",\"tagChip\":false,\"bnR\":3,\"descChip\":false,\"fTitle\":14,\"fTag\":-1,\"fBtn\":-1,\"fInput\":-1,\"fDesc\":-1,\"deco\":true,\"enter\":14,\"showExit\":false,\"caps\":true}",
            "{\"design\":6,\"bgType\":\"gradient\",\"grad\":16,\"gcustom\":true,\"gc1\":\"#04100A\",\"gc2\":\"#071D12\",\"gangle\":180,\"bgFx\":0,\"noise\":0,\"blur\":0,\"radius\":3,\"cardW\":86,\"cardH\":100,\"cardBW\":0,\"tAlign\":1,\"fieldStyle\":3,\"btnStyle\":3,\"btnStyleV\":3,\"fieldR\":10,\"fieldBW\":0.4,\"btnR\":10,\"btnBW\":0,\"btnS\":100,\"sTitle\":100,\"sDesc\":100,\"sInput\":100,\"sTag\":100,\"cTitle\":\"#9CFFB0\",\"cDesc\":\"#4ADE80\",\"cAcc\":\"#22C55E\",\"cTag\":\"#86EFAC\",\"cField\":\"#1122C55E\",\"cFieldB\":\"#9922C55E\",\"cInput\":\"#BBF7D0\",\"cGetBg\":\"#00000000\",\"cGetT\":\"#86EFAC\",\"cGetB\":\"#00000000\",\"cVerBg\":\"#00000000\",\"cVerBg2\":\"#00000000\",\"cVerT\":\"#FFD1FAE5\",\"cVerB\":\"#00000000\",\"tag\":\"levi@access:~\",\"tagChip\":false,\"bnR\":1,\"descChip\":false,\"fTitle\":-2,\"fTag\":-2,\"fBtn\":-2,\"fInput\":-2,\"fDesc\":-2,\"deco\":true,\"enter\":1,\"showExit\":false,\"caps\":false}",
            "{\"design\":7,\"bgType\":\"gradient\",\"grad\":16,\"gcustom\":true,\"gc1\":\"#0C1B2E\",\"gc2\":\"#3B1D6E\",\"gangle\":150,\"bgFx\":8,\"noise\":0,\"blur\":0,\"radius\":12,\"cardW\":78,\"cardH\":100,\"cardBW\":0,\"tAlign\":0,\"fieldStyle\":0,\"btnStyle\":4,\"btnStyleV\":5,\"fieldR\":100,\"fieldBW\":0.5,\"btnR\":100,\"btnBW\":0,\"btnS\":100,\"sTitle\":100,\"sDesc\":100,\"sInput\":100,\"sTag\":100,\"cTitle\":\"#FFFFFF\",\"cDesc\":\"#C4B5FD\",\"cAcc\":\"#A78BFA\",\"cTag\":\"#A78BFA\",\"cField\":\"#26FFFFFF\",\"cFieldB\":\"#55FFFFFF\",\"cInput\":\"#FFFFFF\",\"cGetBg\":\"#00000000\",\"cGetT\":\"#C4B5FD\",\"cGetB\":\"#00000000\",\"cVerBg\":\"#FF7C3AED\",\"cVerBg2\":\"#FF22D3EE\",\"cVerT\":\"#FFFFFFFF\",\"cVerB\":\"#00000000\",\"tag\":\"SECURE ACCESS\",\"tagChip\":false,\"bnR\":50,\"descChip\":false,\"fTitle\":9,\"fTag\":-1,\"fBtn\":-1,\"fInput\":-1,\"fDesc\":-1,\"deco\":true,\"enter\":8,\"showExit\":false,\"caps\":true}"};

    public static int designOf(JSONObject c) { return Math.max(0, Math.min(DESIGN_NAMES.length - 1, c.optInt("design", 0))); }

    public static JSONObject designDefaults(int i) {
        try { return new JSONObject(DESIGN_DEFAULTS[Math.max(0, Math.min(DESIGN_DEFAULTS.length - 1, i))]); }
        catch (Exception e) { return new JSONObject(); }
    }

    public static JSONObject merge(JSONObject base, JSONObject over) {
        try {
            java.util.Iterator<String> it = over.keys();
            while (it.hasNext()) { String k = it.next(); base.put(k, over.get(k)); }
        } catch (Exception e) { /* ignore */ }
        return base;
    }

    static Typeface tfx(Context c, int i, boolean bold, String fam) {
        if (i == -2) return Typeface.create(Typeface.MONOSPACE, bold ? Typeface.BOLD : Typeface.NORMAL);
        Typeface t = font(c, i);
        return t != null ? t : Typeface.create(fam, bold ? Typeface.BOLD : Typeface.NORMAL);
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

    public static class Resp { public int code; public String body = ""; public String etag; }

    /** HTTP that does not throw on 4xx (needed for ETag conditional writes -> 412). */
    public static Resp httpR(String method, String url, String body, String ifMatch, boolean wantEtag) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(9000);
        c.setReadTimeout(25000);
        c.setRequestMethod(method);
        if (wantEtag) c.setRequestProperty("X-Firebase-ETag", "true");
        if (ifMatch != null) c.setRequestProperty("if-match", ifMatch);
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.getOutputStream().write(body.getBytes("UTF-8"));
        }
        Resp r = new Resp();
        r.code = c.getResponseCode();
        InputStream in = r.code >= 400 ? c.getErrorStream() : c.getInputStream();
        r.body = in == null ? "" : new String(readAll(in), "UTF-8");
        r.etag = c.getHeaderField("ETag");
        return r;
    }

    static String devId;

    /** Stable per-install device id (hashed ANDROID_ID). */
    public static String deviceId(Context c) {
        if (devId != null) return devId;
        String a = null;
        try { a = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID); } catch (Exception e) { /* ignore */ }
        SharedPreferences sp = c.getSharedPreferences("levi_auth", 0);
        if (a == null || a.isEmpty() || a.equals("9774d56d682e549c")) {
            a = sp.getString("did", "");
            if (a.isEmpty()) { a = Long.toHexString(new Random().nextLong()) + Long.toHexString(System.nanoTime()); sp.edit().putString("did", a).apply(); }
        }
        devId = sha256(a + "|levi").substring(0, 20);
        return devId;
    }

    /** Firebase server time (tamper-proof expiry checks). */
    static long serverNow(String ck, String dev) {
        try {
            Resp r = httpR("PUT", dbBase + "/levi_ping/" + ck + "_" + dev + ".json", "{\".sv\":\"timestamp\"}", null, false);
            if (r.code < 400) return Long.parseLong(r.body.trim());
        } catch (Exception e) { /* fall back to device clock */ }
        return System.currentTimeMillis();
    }

    public static class VR { public boolean ok; public String msg = ""; public long exp; }

    static VR fail(String m) { VR v = new VR(); v.msg = m; return v; }

    /**
     * Checks a login key against levi_keys/<connectKey>/<key>:
     * status, server-time expiry, and device binding (one key = one device by default).
     * silent=true: re-check of an already verified key (never binds a new device).
     */
    public static VR verifyKey(Context ctx, String raw, boolean silent, String legacyKey) throws Exception {
        String key = raw.trim().toUpperCase(Locale.US);
        if (!key.matches("[A-Z0-9_-]{4,48}")) return fail("Invalid key");
        String dev = deviceId(ctx), model = (Build.MANUFACTURER + " " + Build.MODEL).trim();
        long now = serverNow(dbCk, dev);
        String url = dbBase + "/levi_keys/" + dbCk + "/" + key + ".json";
        for (int attempt = 0; attempt < 3; attempt++) {
            Resp r = httpR("GET", url, null, null, true);
            if (r.code >= 400) throw new Exception("HTTP " + r.code + " " + r.body);
            if (r.body.trim().equals("null")) {
                if (legacyKey != null && !legacyKey.isEmpty() && legacyKey.equalsIgnoreCase(key)) { VR v = new VR(); v.ok = true; return v; }
                return fail("Invalid key");
            }
            JSONObject o = new JSONObject(r.body);
            String st = o.optString("status", "active");
            if (st.equals("deleted")) return fail("Invalid key");
            if (st.equals("inactive")) return fail("This key is disabled");
            if (st.equals("draft")) return fail("This key is not activated yet");
            long exp = o.optLong("expiry", 0);
            if (exp > 0 && now > exp) return fail("This key has expired");
            JSONObject devs = o.optJSONObject("devices");
            if (devs == null) devs = new JSONObject();
            boolean has = devs.has(dev);
            int max = Math.max(1, o.optInt("maxDevices", 1));
            if (!has) {
                if (silent) return fail("Please verify your key again");
                if (devs.length() >= max) return fail(max == 1 ? "This key is already used on another device" : "Device limit reached for this key");
                devs.put(dev, new JSONObject().put("m", model).put("t", now));
                o.put("devices", devs);
                if (!o.has("firstUse")) o.put("firstUse", now);
                long dur = o.optLong("dur", 0);
                if (dur > 0 && exp == 0) { exp = now + dur; o.put("expiry", exp); }   // countdown starts on first login
            }
            if (silent && has) {
                httpR("PUT", dbBase + "/levi_keys/" + dbCk + "/" + key + "/lastSeen.json", String.valueOf(now), null, false);
                VR v = new VR(); v.ok = true; v.exp = exp; return v;
            }
            o.put("lastSeen", now).put("lastDev", model);
            if (!silent) o.put("uses", o.optInt("uses", 0) + 1);
            Resp w = httpR("PUT", url, o.toString(), r.etag, false);
            if (w.code == 412) continue;                       // changed by someone else meanwhile -> retry
            if (w.code >= 400) throw new Exception("HTTP " + w.code + " " + w.body);
            VR v = new VR(); v.ok = true; v.exp = exp; return v;
        }
        return fail("Server busy, try again");
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
                @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), Math.min(radius, Math.min(v.getWidth(), v.getHeight()) / 2f)); }
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
            if (fx() == 20) {
                float k = (float) (0.5 + 0.5 * Math.sin(phase * Math.PI * 2));
                int a1 = lerp(c1, c2, k), a2 = lerp(c2, c1, k);
                c1 = a1; c2 = a2;
            }
            if (fx() == 16) {
                float[] h = new float[3];
                Color.colorToHSV(c1, h); h[0] = (h[0] + phase * 360) % 360; c1 = Color.HSVToColor(h);
                Color.colorToHSV(c2, h); h[0] = (h[0] + phase * 360) % 360; c2 = Color.HSVToColor(h);
            }
            return new int[]{c1, c2};
        }

        int lerp(int a, int b, float t) {
            return Color.argb((int) (Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * t), (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                    (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t), (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
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

    // ================================================================ Deco (per-design ornaments) ===
    static int al(int c, float f) { return (Math.max(0, Math.min(255, (int) (255 * f))) << 24) | (c & 0xFFFFFF); }

    public static class Deco extends View {
        final Card k;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float ph;
        ValueAnimator va;

        public Deco(Card k) { super(k.getContext()); this.k = k; }

        void restart() {
            if (va != null) { va.cancel(); va = null; }
            int dz = k.design;
            if (isAttachedToWindow() && B(k.cfg, "animOn", true) && (dz == 1 || dz == 6 || dz == 7)) {
                va = ValueAnimator.ofFloat(0, 1);
                va.setDuration(3200);
                va.setRepeatCount(ValueAnimator.INFINITE);
                va.setInterpolator(new LinearInterpolator());
                va.addUpdateListener(an -> { ph = (Float) an.getAnimatedValue(); invalidate(); });
                va.start();
            }
        }

        @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); restart(); }
        @Override protected void onDetachedFromWindow() { super.onDetachedFromWindow(); if (va != null) { va.cancel(); va = null; } }

        static void drawLock(Canvas cv, Paint p, float cx, float cy, float s, int color) {
            p.setShader(null);
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            cv.drawRoundRect(new RectF(cx - s * .5f, cy - s * .08f, cx + s * .5f, cy + s * .5f), s * .12f, s * .12f, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(s * .13f);
            p.setStrokeCap(Paint.Cap.ROUND);
            cv.drawArc(new RectF(cx - s * .3f, cy - s * .55f, cx + s * .3f, cy + s * .1f), 180, 180, false, p);
            cv.drawLine(cx - s * .3f, cy - s * .22f, cx - s * .3f, cy - s * .06f, p);
            cv.drawLine(cx + s * .3f, cy - s * .22f, cx + s * .3f, cy - s * .06f, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x88000000);
            cv.drawCircle(cx, cy + s * .2f, s * .075f, p);
        }

        @Override protected void onDraw(Canvas cv) {
            JSONObject c = k.cfg;
            int w = k.cw, h = k.ch, dz = k.design;
            if (w == 0) return;
            float u = k.u;
            int acc = col(c, "cAcc", "#22E4FF");
            float rad = Math.min(F(c, "radius", 10.4) * u, Math.min(w, h) / 2f);
            float sn = (float) Math.sin(ph * Math.PI * 2);
            Rect ban = k.rc[Card.R_BAN];
            p.setShader(null);
            p.setPathEffect(null);
            if (B(c, "deco", true)) {
                switch (dz) {
                    case 1: {
                        float glow = 0.6f + 0.4f * sn;
                        p.setStyle(Paint.Style.STROKE);
                        RectF rr = new RectF(0.6f * u, 0.6f * u, w - 0.6f * u, h - 0.6f * u);
                        for (int i = 4; i >= 1; i--) { p.setStrokeWidth(i * 0.8f * u); p.setColor(al(acc, 0.06f * glow)); cv.drawRoundRect(rr, rad, rad, p); }
                        p.setStrokeWidth(0.45f * u); p.setColor(al(acc, 0.95f)); cv.drawRoundRect(rr, rad, rad, p);
                        p.setStrokeWidth(0.7f * u); p.setStrokeCap(Paint.Cap.SQUARE);
                        float L = 7 * u, o = 2.2f * u;
                        cv.drawLine(o, o, o + L, o, p); cv.drawLine(o, o, o, o + L, p);
                        cv.drawLine(w - o, o, w - o - L, o, p); cv.drawLine(w - o, o, w - o, o + L, p);
                        cv.drawLine(o, h - o, o + L, h - o, p); cv.drawLine(o, h - o, o, h - o - L, p);
                        cv.drawLine(w - o, h - o, w - o - L, h - o, p); cv.drawLine(w - o, h - o, w - o, h - o - L, p);
                        p.setStrokeWidth(0.35f * u); p.setColor(al(acc, 0.8f));
                        cv.drawRoundRect(new RectF(ban.left - 0.5f * u, ban.top - 0.5f * u, ban.right + 0.5f * u, ban.bottom + 0.5f * u), 2 * u, 2 * u, p);
                        p.setStrokeWidth(Math.max(1f, 0.35f * u)); p.setColor(0x0DFFFFFF);
                        for (float y = 0; y < h; y += 1.7f * u) cv.drawLine(0, y, w, y, p);
                        break;
                    }
                    case 2: {
                        float top = ban.bottom - 34 * u;
                        p.setStyle(Paint.Style.FILL);
                        p.setShader(new LinearGradient(0, top, 0, ban.bottom, 0x00000000, 0xD9000000, Shader.TileMode.CLAMP));
                        cv.drawRect(0, top, w, ban.bottom, p);
                        p.setShader(null);
                        break;
                    }
                    case 3: {
                        p.setStyle(Paint.Style.FILL);
                        p.setColor(al(acc, 0.9f));
                        cv.drawRoundRect(new RectF(w / 2f - 7 * u, 2f * u, w / 2f + 7 * u, 3.2f * u), u, u, p);
                        break;
                    }
                    case 4: {
                        p.setStyle(Paint.Style.STROKE);
                        p.setStrokeWidth(Math.max(1f, 0.3f * u));
                        p.setColor(al(acc, 0.9f));
                        Rect g = k.rc[Card.R_GET];
                        cv.drawLine(0, g.top, w, g.top, p);
                        cv.drawLine(w / 2f, g.top, w / 2f, h, p);
                        if (S(c, "bnSrc", "").isEmpty()) drawLock(cv, p, ban.centerX(), ban.centerY(), ban.width() * 0.46f, 0xFFFFFFFF);
                        break;
                    }
                    case 5: {
                        float ny = k.notchY;
                        p.setStyle(Paint.Style.STROKE);
                        p.setStrokeWidth(Math.max(1f, 0.32f * u));
                        p.setColor(al(acc, 0.65f));
                        p.setPathEffect(new DashPathEffect(new float[]{1.7f * u, 1.2f * u}, 0));
                        cv.drawLine(6 * u, ny, w - 6 * u, ny, p);
                        p.setPathEffect(null);
                        p.setStyle(Paint.Style.FILL);
                        p.setColor(al(acc, 0.85f));
                        Random r = new Random(5);
                        float x = 6 * u, top = h - 9 * u;
                        while (x < w - 6 * u) {
                            float bw = (0.35f + r.nextFloat() * 1.1f) * u;
                            if (r.nextInt(3) > 0) cv.drawRect(x, top, Math.min(x + bw, w - 6 * u), top + 5.5f * u, p);
                            x += bw + (0.3f + r.nextFloat() * 0.7f) * u;
                        }
                        break;
                    }
                    case 6: {
                        p.setStyle(Paint.Style.FILL);
                        p.setColor(al(acc, 0.16f));
                        cv.drawRect(0, 0, w, 9 * u, p);
                        p.setColor(al(acc, 0.5f));
                        cv.drawRect(0, 9 * u, w, 9 * u + Math.max(1f, 0.25f * u), p);
                        int[] dc = {0xFFFF5F57, 0xFFFFBD2E, 0xFF28C840};
                        for (int i = 0; i < 3; i++) { p.setColor(dc[i]); cv.drawCircle((4 + i * 3.6f) * u, 4.5f * u, 1.1f * u, p); }
                        p.setStyle(Paint.Style.STROKE);
                        p.setStrokeWidth(0.4f * u);
                        p.setColor(al(acc, 0.75f));
                        cv.drawRoundRect(new RectF(0.2f * u, 0.2f * u, w - 0.2f * u, h - 0.2f * u), rad, rad, p);
                        p.setStrokeWidth(Math.max(1f, 0.3f * u));
                        p.setColor(0x0AFFFFFF);
                        for (float y = 9 * u; y < h; y += 1.8f * u) cv.drawLine(0, y, w, y, p);
                        Rect f = k.rc[Card.R_FLD];
                        p.setStyle(Paint.Style.FILL);
                        p.setTypeface(Typeface.MONOSPACE);
                        p.setTextSize(4.4f * u);
                        p.setColor(acc);
                        cv.drawText("$", f.left + 3 * u, f.centerY() + 1.5f * u, p);
                        if (sn > 0) { p.setColor(al(acc, 0.9f)); cv.drawRect(w - 7 * u, 3 * u, w - 5.6f * u, 6 * u, p); }
                        break;
                    }
                    case 7: {
                        float cx = ban.centerX(), cy = ban.centerY(), r0 = ban.width() / 2f;
                        p.setStyle(Paint.Style.FILL);
                        p.setShader(new RadialGradient(cx, cy, r0 * 2.0f, al(acc, 0.50f + 0.1f * sn), al(acc, 0f), Shader.TileMode.CLAMP));
                        cv.drawCircle(cx, cy, r0 * 2.0f, p);
                        p.setShader(null);
                        p.setStyle(Paint.Style.STROKE);
                        p.setStrokeWidth(0.7f * u);
                        p.setColor(al(acc, 0.9f));
                        cv.drawCircle(cx, cy, r0 + 1.2f * u * (1f + 0.05f * sn), p);
                        p.setStrokeWidth(0.4f * u);
                        p.setColor(al(acc, 0.35f));
                        cv.drawCircle(cx, cy, r0 + 3f * u * (1f - 0.05f * sn), p);
                        if (S(c, "bnSrc", "").isEmpty()) drawLock(cv, p, cx, cy, r0 * 0.9f, 0xFFFFFFFF);
                        break;
                    }
                    default: break;
                }
            }
            if (I(c, "btnStyleV", 0) == 5) {
                Rect v = k.rc[Card.R_VER];
                int g1 = col(c, "cVerBg", "#7C3AED");
                float vr = v.height() / 2f * F(c, "btnR", 100) / 100f;
                p.setStyle(Paint.Style.FILL);
                p.setShader(null);
                p.setColor(al(g1, 0.12f));
                for (int i = 3; i >= 1; i--)
                    cv.drawRoundRect(new RectF(v.left - i * 0.9f * u, v.top - i * 0.5f * u, v.right + i * 0.9f * u, v.bottom + i * 1.1f * u), vr + i * u, vr + i * u, p);
            }
        }
    }

    // ================================================================ The dialog card ===========
    public static class Card extends ViewGroup {
        public JSONObject cfg = new JSONObject();   // effective config = design defaults + user config
        public float scale = 1f;                    // admin preview uses < 1
        public int design = 0;
        public final Bg bg;
        public final Media bgVid, banner;
        public final Deco deco;
        public final TextView title, desc, tag, getBtn, verifyBtn, exitBtn;
        public final EditText input;
        public final Eye eye;
        public final LinearLayout field;
        float u = 3f, notchY = 0;
        int cw, ch, lastW = -1;
        boolean dirty = true;
        final Rect[] rc = new Rect[8];
        final Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint clr = new Paint(Paint.ANTI_ALIAS_FLAG);
        static final int R_BAN = 0, R_TIT = 1, R_DES = 2, R_FLD = 3, R_GET = 4, R_VER = 5, R_EXT = 6, R_TAG = 7;

        public Card(Context c) {
            super(c);
            bgVid = new Media(c);
            bg = new Bg(c);
            banner = new Media(c);
            deco = new Deco(this);
            desc = new TextView(c);
            title = new TextView(c);
            tag = new TextView(c);
            field = new LinearLayout(c);
            input = new EditText(c);
            eye = new Eye(c);
            getBtn = new TextView(c);
            verifyBtn = new TextView(c);
            exitBtn = new TextView(c);
            clr.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

            title.setSingleLine(true);
            title.setIncludeFontPadding(false);
            title.setLetterSpacing(0.02f);
            tag.setSingleLine(true);
            tag.setIncludeFontPadding(false);
            desc.setMaxLines(2);
            desc.setIncludeFontPadding(false);
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
            addView(bgVid); addView(bg); addView(banner); addView(deco); addView(desc); addView(title); addView(tag);
            addView(field); addView(getBtn); addView(verifyBtn); addView(exitBtn);
            setClipToOutline(true);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View v, Outline o) {
                    float r = Math.min(F(cfg, "radius", 10.4) * u, Math.min(v.getWidth(), v.getHeight()) / 2f);
                    if (design == 3) o.setRoundRect(0, 0, v.getWidth(), (int) (v.getHeight() + r + 2), r);   // sheet: only top corners round
                    else o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r);
                }
            });
            apply(new JSONObject());
        }

        public void apply(JSONObject raw) {
            JSONObject eff = merge(designDefaults(designOf(raw)), raw);
            if (!raw.has("design") && raw.has("cBtn")) {   // v1 configs
                try {
                    eff.put("cGetBg", raw.get("cBtn")); eff.put("cVerBg", raw.get("cBtn")); eff.put("cVerBg2", raw.get("cBtn"));
                    if (raw.has("cBtnT")) { eff.put("cGetT", raw.get("cBtnT")); eff.put("cVerT", raw.get("cBtnT")); }
                    if (raw.has("cBtnB")) { eff.put("cGetB", raw.get("cBtnB")); eff.put("cVerB", raw.get("cBtnB")); }
                } catch (Exception ex) { /* ignore */ }
            }
            cfg = eff;
            design = designOf(eff);
            dirty = true;
            boolean vid = S(eff, "bgType", "gradient").equals("video");
            setBackgroundColor(vid ? 0xFF101820 : 0);
            setLayerType(design == 5 ? LAYER_TYPE_HARDWARE : LAYER_TYPE_NONE, null);
            bg.set(eff);
            if (vid) { bgVid.setVisibility(VISIBLE); bgVid.set(S(eff, "bgSrc", ""), true, 720); }
            else { bgVid.clear(); bgVid.setVisibility(GONE); }
            banner.set(S(eff, "bnSrc", ""), S(eff, "bnType", "image").equals("video"), 1100);
            deco.restart();
            requestLayout();
            invalidate();
        }

        Rect r(float x, float y, float w, float h) {
            return new Rect((int) (x * u), (int) (y * u), (int) ((x + w) * u), (int) ((y + h) * u));
        }

        Rect btn(float cx, float cy, float w, float h, float s) {
            float ww = Math.min(w * s, 96f), hh = h * s;
            return r(cx - ww / 2, cy - hh / 2, ww, hh);
        }

        void layoutRects(float H, float d) {
            float s = F(cfg, "btnS", 100) / 100f;
            rc[R_TAG] = r(0, 0, 1, 1);
            switch (design) {
                case 1:
                    rc[R_TAG] = r(6, 5, 88, 5); rc[R_TIT] = r(6, 10.5f, 88, 11); rc[R_BAN] = r(6, 25, 88, 30 + d);
                    rc[R_DES] = r(8, 45 + d, 84, 8); rc[R_FLD] = r(6, 60 + d, 88, 12.5f);
                    rc[R_GET] = btn(27.5f, 84.5f + d, 42, 11.5f, s); rc[R_VER] = btn(72.5f, 84.5f + d, 42, 11.5f, s);
                    rc[R_EXT] = r(87, 3, 8, 8);
                    break;
                case 2:
                    rc[R_BAN] = r(0, 0, 100, 58 + d); rc[R_TAG] = r(5, 5, 26, 5.5f); rc[R_TIT] = r(5, 44 + d, 90, 12);
                    rc[R_DES] = r(6, 60 + d, 88, 9); rc[R_FLD] = r(6, 71 + d, 88, 12.5f);
                    rc[R_VER] = btn(50, 92.5f + d, 88, 12.5f, s); rc[R_GET] = btn(50, 107 + d, 40, 6, s);
                    rc[R_EXT] = r(88, 3, 8.5f, 8.5f);
                    break;
                case 3:
                    rc[R_TAG] = r(6, 5, 60, 4); rc[R_TIT] = r(6, 9, 70, 10); rc[R_DES] = r(6, 19, 88, 8.5f);
                    rc[R_BAN] = r(78, 5, 16, 16); rc[R_FLD] = r(6, 30, 88, 11.5f);
                    rc[R_VER] = btn(50, 53, 88, 12.5f, s); rc[R_GET] = btn(50, 69.5f, 88, 12.5f, s);
                    rc[R_EXT] = r(86.5f, 22.5f, 7, 7);
                    break;
                case 4:
                    rc[R_BAN] = r(36, 5, 28, 28); rc[R_TIT] = r(4, 35, 92, 8.5f); rc[R_TAG] = r(4, 43.5f, 92, 4);
                    rc[R_DES] = r(8, 44, 84, 9); rc[R_FLD] = r(8, 55, 84, 10.5f);
                    rc[R_GET] = r(0, 69.5f, 50, H - 69.5f); rc[R_VER] = r(50, 69.5f, 50, H - 69.5f);
                    rc[R_EXT] = r(88, 2, 8, 8);
                    break;
                case 5:
                    rc[R_BAN] = r(5, 5, 90, 32 + d); rc[R_TAG] = r(5, 39 + d, 60, 4.5f); rc[R_TIT] = r(5, 43.5f + d, 90, 10);
                    rc[R_DES] = r(5, 54 + d, 90, 8); rc[R_FLD] = r(6, 68 + d, 88, 12);
                    rc[R_GET] = btn(27.5f, 90 + d, 41, 11, s); rc[R_VER] = btn(72.5f, 90 + d, 41, 11, s);
                    rc[R_EXT] = r(87, 6, 7, 7);
                    notchY = (64.5f + d) * u;
                    break;
                case 6:
                    rc[R_TAG] = r(14, 0, 72, 9); rc[R_BAN] = r(6, 12.5f, 88, 24 + d); rc[R_TIT] = r(6, 39 + d, 88, 8);
                    rc[R_DES] = r(6, 47.5f + d, 88, 8.5f); rc[R_FLD] = r(6, 57.5f + d, 88, 11);
                    rc[R_GET] = btn(27.5f, 78 + d, 42, 10, s); rc[R_VER] = btn(72.5f, 78 + d, 42, 10, s);
                    rc[R_EXT] = r(88, 1.2f, 7, 7);
                    break;
                case 7:
                    rc[R_BAN] = r(35, 6, 30, 30); rc[R_TIT] = r(4, 38.5f, 92, 11); rc[R_TAG] = r(4, 50, 92, 5);
                    rc[R_DES] = r(8, 55.5f, 84, 8.5f); rc[R_FLD] = r(7, 66, 86, 12.5f);
                    rc[R_VER] = btn(50, 88, 86, 13, s); rc[R_GET] = btn(50, 101, 40, 6, s);
                    rc[R_EXT] = r(88, 3, 8.5f, 8.5f);
                    break;
                default:
                    rc[R_BAN] = r(2.7f, 13.9f, 94.6f, 46 + d); rc[R_TIT] = r(0, 0, 100, 13.9f);
                    rc[R_DES] = r(4.7f, 48.9f + d, 90.6f, 9); rc[R_FLD] = r(5.2f, 63.7f + d, 89.7f, 13.7f);
                    rc[R_GET] = btn(28.55f, 88.95f + d, 34.7f, 12.7f, s); rc[R_VER] = btn(70.85f, 88.95f + d, 34.7f, 12.7f, s);
                    rc[R_EXT] = r(89, 2.2f, 8.5f, 8.5f);
                    break;
            }
        }

        @Override protected void onMeasure(int ws, int hs) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int avail = MeasureSpec.getSize(ws);
            if (avail <= 0) avail = dm.widthPixels;
            float pct = F(cfg, "cardW", 76) / 100f;
            float real = design == 3 ? dm.widthPixels * pct : Math.min(dm.widthPixels * pct, dp(getContext(), 420));
            int w = (int) Math.min(avail, real * scale);
            float d = F(cfg, "cardH", 100) - 100;
            float H = DESIGN_H0[design] + d;
            int h = (int) (w * H / 100f);
            u = w / 100f; cw = w; ch = h;
            layoutRects(H, d);
            if (dirty || lastW != w) { style(); dirty = false; lastW = w; }
            setMeasuredDimension(w, h);
            int ex = MeasureSpec.EXACTLY;
            View[] full = {bgVid, bg, deco};
            for (View v : full) v.measure(MeasureSpec.makeMeasureSpec(w, ex), MeasureSpec.makeMeasureSpec(h, ex));
            View[] vs = {banner, title, desc, tag, field, getBtn, verifyBtn, exitBtn};
            int[] ix = {R_BAN, R_TIT, R_DES, R_TAG, R_FLD, R_GET, R_VER, R_EXT};
            for (int i = 0; i < vs.length; i++)
                vs[i].measure(MeasureSpec.makeMeasureSpec(Math.max(1, rc[ix[i]].width()), ex), MeasureSpec.makeMeasureSpec(Math.max(1, rc[ix[i]].height()), ex));
        }

        @Override protected void onLayout(boolean changed, int l, int t, int rr, int b) {
            bgVid.layout(0, 0, cw, ch);
            bg.layout(0, 0, cw, ch);
            deco.layout(0, 0, cw, ch);
            View[] vs = {banner, title, desc, tag, field, getBtn, verifyBtn, exitBtn};
            int[] ix = {R_BAN, R_TIT, R_DES, R_TAG, R_FLD, R_GET, R_VER, R_EXT};
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
            int dz = design;
            boolean left = I(c, "tAlign", 0) == 1;
            int hg = left ? (Gravity.CENTER_VERTICAL | Gravity.START) : Gravity.CENTER;

            // ---- title (shrinks to fit one line)
            String tx = S(c, "title", "To access this you need access key");
            if (B(c, "caps", true)) tx = tx.toUpperCase();
            title.setText(tx);
            title.setTypeface(tfx(x, I(c, "fTitle", 0), true, "sans-serif-condensed"));
            title.setTextColor(col(c, "cTitle", "#000000"));
            title.setGravity(hg);
            float tsz = TSZ[dz] * u * F(c, "sTitle", 100) / 100f;
            Paint tp = new Paint(title.getPaint());
            tp.setTextSize(tsz);
            tp.setTypeface(title.getTypeface());
            float aw = rc[R_TIT].width() * 0.96f, tw = tp.measureText(tx);
            if (tw > aw && tw > 0) tsz *= aw / tw;
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, tsz);

            // ---- tag line
            String tg = S(c, "tag", "");
            tag.setVisibility(tg.isEmpty() ? GONE : VISIBLE);
            tag.setText(tg);
            tag.setTypeface(tfx(x, I(c, "fTag", -1), true, "sans-serif"));
            tag.setTextSize(TypedValue.COMPLEX_UNIT_PX, 3.0f * u * F(c, "sTag", 100) / 100f);
            tag.setLetterSpacing(0.14f);
            if (B(c, "tagChip", false)) {
                GradientDrawable tgd = new GradientDrawable();
                tgd.setColor(col(c, "cAcc", "#F59E0B"));
                tgd.setCornerRadius(rc[R_TAG].height() / 2f);
                tag.setBackground(tgd);
                tag.setGravity(Gravity.CENTER);
                tag.setTextColor(col(c, "cTag", "#0F172A"));
            } else {
                tag.setBackground(null);
                tag.setGravity(hg);
                tag.setTextColor(col(c, "cTag", "#12B76A"));
            }

            // ---- input field
            int fs = I(c, "fieldStyle", 0), fh = Math.max(1, rc[R_FLD].height());
            int fill = col(c, "cField", "#5214283C"), bord = col(c, "cFieldB", "#EBFFFFFF");
            int sw = Math.max(1, (int) (F(c, "fieldBW", 0.75) * u));
            float frad = fh / 2f * F(c, "fieldR", 100) / 100f;
            Drawable fd;
            if (fs == 1) {
                GradientDrawable base = new GradientDrawable();
                base.setColor(fill);
                GradientDrawable ln = new GradientDrawable();
                ln.setColor(bord);
                LayerDrawable ld = new LayerDrawable(new Drawable[]{base, ln});
                ld.setLayerInset(1, 0, Math.max(0, fh - Math.max(2, sw * 2)), 0, 0);
                fd = ld;
            } else {
                GradientDrawable g = new GradientDrawable();
                g.setColor(fill);
                g.setCornerRadius(fs == 3 ? Math.min(frad, 2 * u) : frad);
                if (fs == 2) g.setStroke(sw, bord, 3 * u, 2 * u);
                else if (fs == 4) g.setStroke(Math.max(sw, (int) (0.5f * u)), bord);
                else g.setStroke(sw, bord);
                fd = g;
            }
            field.setBackground(fd);
            field.setPadding((int) (fs == 3 ? 9f * u : 3.8f * u), 0, (int) (3.5f * u), 0);
            int ic = col(c, "cInput", "#FFFFFF");
            input.setTextColor(ic);
            input.setHintTextColor((ic & 0x00FFFFFF) | 0xB8000000);
            input.setHint(S(c, "hint", "enter your key here. . . . ."));
            input.setTypeface(tfx(x, I(c, "fInput", -1), false, "sans-serif"));
            input.setTextSize(TypedValue.COMPLEX_UNIT_PX, INSZ[dz] * u * F(c, "sInput", 100) / 100f);
            input.setLetterSpacing(0.02f);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) eye.getLayoutParams();
            lp.width = lp.height = (int) (5.6f * u);
            eye.setLayoutParams(lp);
            eye.color = ic;
            eye.invalidate();

            // ---- buttons
            float bs = F(c, "btnS", 100) / 100f;
            int vb = col(c, "cVerBg", "#61FFFFFF");
            int vb2 = c.has("cVerBg2") ? col(c, "cVerBg2", "#61FFFFFF") : vb;
            styleBtn(getBtn, S(c, "getTxt", "Get Key"), I(c, "btnStyle", 0), col(c, "cGetBg", "#61FFFFFF"), col(c, "cGetBg", "#61FFFFFF"),
                    col(c, "cGetT", "#000000"), col(c, "cGetB", "#FFFFFFFF"), rc[R_GET].height(), false, bs);
            styleBtn(verifyBtn, S(c, "verTxt", "Verify"), I(c, "btnStyleV", 0), vb, vb2,
                    col(c, "cVerT", "#000000"), col(c, "cVerB", "#FFFFFFFF"), rc[R_VER].height(), true, bs);

            // ---- banner / description / exit
            banner.setRadius(F(c, "bnR", 5.8) * u);
            String ds = S(c, "desc", "");
            desc.setVisibility(ds.isEmpty() ? GONE : VISIBLE);
            desc.setText(ds);
            desc.setTextColor(col(c, "cDesc", "#FFFFFF"));
            desc.setTypeface(tfx(x, I(c, "fDesc", -1), false, "sans-serif"));
            desc.setTextSize(TypedValue.COMPLEX_UNIT_PX, DSZ[dz] * u * F(c, "sDesc", 100) / 100f);
            desc.setGravity(hg);
            if (B(c, "descChip", false)) {
                GradientDrawable dg = new GradientDrawable();
                dg.setColor(0x66000000);
                dg.setCornerRadius(3 * u);
                desc.setBackground(dg);
                desc.setPadding((int) (2 * u), 0, (int) (2 * u), 0);
                desc.setGravity(Gravity.CENTER);
            } else { desc.setBackground(null); desc.setPadding(0, 0, 0, 0); }

            exitBtn.setVisibility(B(c, "showExit", false) ? VISIBLE : GONE);
            exitBtn.setTextColor(0xFFFFFFFF);
            exitBtn.setTextSize(TypedValue.COMPLEX_UNIT_PX, 4f * u);
            GradientDrawable eg = new GradientDrawable();
            eg.setShape(GradientDrawable.OVAL);
            eg.setColor(0x66000000);
            exitBtn.setBackground(eg);
            deco.restart();
            invalidateOutline();
        }

        void styleBtn(TextView b, String txt, int st, int bg1, int bg2, int tc, int bd, int hp, boolean bold, float bs) {
            Context x = getContext();
            JSONObject c = cfg;
            GradientDrawable g = null;
            float rad = hp / 2f * F(c, "btnR", 100) / 100f;
            int bw = (int) (F(c, "btnBW", 0) * u);
            String t = txt;
            b.getPaint().setUnderlineText(false);
            if (st == 0 || st == 5) {
                g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{bg1, bg2});
                if (bw > 0) g.setStroke(bw, bd);
            } else if (st == 2) {
                g = new GradientDrawable();
                g.setColor(0);
                g.setStroke(Math.max(1, (int) (0.45f * u)), bd);
            } else if (st == 3) {
                t = "[ " + txt.toLowerCase() + " ]";
            }
            if (g != null) g.setCornerRadius(rad);
            b.setBackground(g);
            b.setText(t);
            if (st == 4) b.getPaint().setUnderlineText(true);
            b.setTextColor(tc);
            b.setTypeface(tfx(x, I(c, "fBtn", -1), bold || st == 0 || st == 5, "sans-serif"));
            b.setTextSize(TypedValue.COMPLEX_UNIT_PX, BSZ[design] * u * bs * (st == 4 ? 0.85f : 1f));
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
            if (design == 5 && B(cfg, "deco", true)) {   // ticket notches (punch real holes)
                cv.drawCircle(0, notchY, 4 * u, clr);
                cv.drawCircle(cw, notchY, 4 * u, clr);
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
    /** Call from the main Activity.onCreate. Covers every activity of the app until the key is verified. */
    public static void show(final Activity a) {
        dbBase = dbUrl().trim().replaceAll("/+$", "");
        dbCk = connectKey().trim();
        if (!hooked) {
            hooked = true;
            a.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override public void onActivityCreated(Activity x, Bundle b) { }
                @Override public void onActivityStarted(Activity x) { }
                @Override public void onActivityResumed(Activity x) { ensure(x); recheck(x); }
                @Override public void onActivityPaused(Activity x) { }
                @Override public void onActivityStopped(Activity x) { }
                @Override public void onActivitySaveInstanceState(Activity x, Bundle b) { }
                @Override public void onActivityDestroyed(Activity x) {
                    if (cur != null && cur.a == x) { cur.dispose(); cur = null; }
                }
            });
        }
        ensure(a);
    }

    static long lastCheck = 0;

    /** every few minutes (on resume) re-validate the saved key: disabled / expired / device removed => dialog again */
    static void recheck(Activity a) {
        if (!passed || System.currentTimeMillis() - lastCheck < 180000) return;
        if (a.getSharedPreferences("levi_auth", 0).getString("ok_" + dbCk, "").isEmpty()) return;
        lastCheck = System.currentTimeMillis();
        new Host(a).start();
    }

    static void ensure(Activity a) {
        if (passed || a.isFinishing()) return;
        if (cur != null && cur.a == a && !cur.closed) return;
        if (cur != null) { cur.dispose(); cur = null; }
        SharedPreferences sp = a.getSharedPreferences("levi_auth", 0);
        if (!sp.getString("ok_" + dbCk, "").isEmpty()) {
            // verified before: let the app run, silently re-check once (key rotated => ask again)
            if (!silentStarted) { silentStarted = true; passed = true; lastCheck = System.currentTimeMillis(); new Host(a).start(); }
            return;
        }
        cur = new Host(a);
        cur.start();
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

        void dispose() {
            closed = true;
            dismissLoading();
            try { if (dlg != null) dlg.dismiss(); } catch (Exception ex) { /* ignore */ }
            dlg = null;
        }

        void start() {
            final String saved = sp.getString("ok_" + dbCk, "");
            if (!saved.isEmpty()) { silent(saved); return; }
            showLoading();
            async("GET", url(), null, (r, er) -> {
                dismissLoading();
                if (closed || a.isFinishing()) return;
                if (er == null) parse(r);
                if (er == null && found && !enabled) { passed = true; closed = true; return; }   // dialog switched off by admin
                present();
                if (er != null) toast("No connection to server");
                else if (!found) toast("Invalid app connect key / database");
            });
        }

        /** background re-check of an already verified key (offline = allowed) */
        void silent(final String saved) {
            new Thread(() -> {
                String msg = null;
                try {
                    parse(http("GET", url(), null));
                    if (!(found && !enabled)) {
                        VR vr = verifyKey(a, saved, true, loginKey);
                        if (!vr.ok) msg = vr.msg;
                    }
                } catch (Exception ex) { /* offline: keep access */ }
                final String fm = msg;
                main.post(() -> {
                    if (fm == null || closed || a.isFinishing()) return;
                    sp.edit().remove("ok_" + dbCk).apply();
                    passed = false;
                    cur = this;
                    present();
                    toast(fm);
                });
            }).start();
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
            dispose();
            a.finishAffinity();
            main.postDelayed(() -> android.os.Process.killProcess(android.os.Process.myPid()), 200);
        }

        void present() {
            if (a.isFinishing() || closed) return;
            dlg = base(true);
            dlg.getWindow().setDimAmount(F(cfg, "dim", 60) / 100f);
            FrameLayout root = new FrameLayout(a);
            card = new Card(a);
            card.apply(cfg);
            root.addView(card, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
            place();
            dlg.setContentView(root);
            card.getBtn.setOnClickListener(v -> openGetKey());
            card.verifyBtn.setOnClickListener(v -> verify());
            card.exitBtn.setOnClickListener(v -> exitApp());
            dlg.show();
            card.post(() -> card.playEnter());
            poll();
        }

        void place() {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) card.getLayoutParams();
            int g = card.design == 3 ? (Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL) : Gravity.CENTER;
            if (lp.gravity != g) { lp.gravity = g; card.setLayoutParams(lp); }
        }

        void poll() {
            main.postDelayed(() -> {
                if (closed || dlg == null || !dlg.isShowing()) return;
                async("GET", url(), null, (r, er) -> {
                    if (er == null && !closed) {
                        String before = raw;
                        parse(r);
                        if (found && !enabled) { closeOk(false, ""); return; }
                        if (!before.equals(raw) && card != null) { card.apply(cfg); place(); }
                    }
                    poll();
                });
            }, 4000);
        }

        void closeOk(boolean verified, String key) {
            closed = true; passed = true;
            if (verified) sp.edit().putString("ok_" + dbCk, key).apply();
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
            new Thread(() -> {
                VR vr = null;
                String er = null;
                try {
                    parse(http("GET", url(), null));
                    if (!found) vr = fail("Invalid app connect key / database");
                    else if (!enabled) { vr = new VR(); vr.ok = true; }
                    else vr = verifyKey(a, v, false, loginKey);
                } catch (Exception ex) { er = "No connection to server"; }
                final VR fv = vr;
                final String fe = er;
                main.post(() -> {
                    busy = false;
                    card.verifyBtn.setText(old);
                    if (fe != null || fv == null) { toast(fe == null ? "Error" : fe); return; }
                    if (fv.ok) {
                        toast(fv.exp > 0 ? "Verified - valid until " + new java.text.SimpleDateFormat("dd MMM yyyy", Locale.US).format(new java.util.Date(fv.exp)) : "Verified");
                        closeOk(enabled, v.toUpperCase(Locale.US));
                    } else { card.shake(); toast(fv.msg); }
                });
            }).start();
        }

        void openGetKey() {
            String u = S(cfg, "getUrl", "").trim();
            if (u.isEmpty()) { toast("Get Key link not set"); return; }
            if (!u.startsWith("http")) u = "https://" + u;
            try { a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); } catch (Exception ex) { toast("Can't open link"); }
        }
    }
}
