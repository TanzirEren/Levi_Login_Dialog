package com.levi.admin;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.media3.common.Effect;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.effect.Presentation;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.DefaultEncoderFactory;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.Effects;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.ProgressHolder;
import androidx.media3.transformer.Transformer;
import androidx.media3.transformer.VideoEncoderSettings;

import com.google.common.collect.ImmutableList;

import java.io.File;

/** Re-encodes a gallery video (H.264, no audio, lower resolution + bitrate) so it fits the size budget. Call on the UI thread. */
public final class Compressor {
    public interface Cb {
        void progress(int pct);
        void done(File out, String err);
    }

    public static void run(final Context c, final Uri src, final long targetBytes, final Cb cb) {
        long durMs = 0;
        int w = 0, h = 0;
        try {
            MediaMetadataRetriever m = new MediaMetadataRetriever();
            m.setDataSource(c, src);
            durMs = Long.parseLong(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            w = Integer.parseInt(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            h = Integer.parseInt(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            String rot = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            if (rot != null && (rot.equals("90") || rot.equals("270"))) { int t = w; w = h; h = t; }
            m.release();
        } catch (Exception e) {
            cb.done(null, "Can't read video info");
            return;
        }
        if (durMs <= 0 || w <= 0 || h <= 0) { cb.done(null, "Can't read video info"); return; }

        // budget: bits per second for the whole file, minus 12% for container overhead
        long bps = (long) (targetBytes * 8L * 1000L / durMs * 0.88);
        bps = Math.max(120000L, Math.min(3000000L, bps));
        int maxSide = bps >= 1800000L ? 960 : bps >= 1000000L ? 720 : bps >= 600000L ? 540 : 426;
        final int big = Math.max(w, h);
        ImmutableList<Effect> fx;
        if (big > maxSide) {
            int outH = h >= w ? maxSide : Math.round(maxSide * (float) h / w);
            fx = ImmutableList.<Effect>of(Presentation.createForHeight(Math.max(144, outH)));
        } else fx = ImmutableList.<Effect>of();

        final File out = new File(c.getCacheDir(), "lv_cmp_" + System.currentTimeMillis() + ".mp4");
        final Handler hd = new Handler(Looper.getMainLooper());
        try {
            DefaultEncoderFactory enc = new DefaultEncoderFactory.Builder(c)
                    .setRequestedVideoEncoderSettings(new VideoEncoderSettings.Builder().setBitrate((int) bps).build()).build();
            final Transformer[] holder = new Transformer[1];
            final boolean[] fin = {false};
            Transformer t = new Transformer.Builder(c)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setEncoderFactory(enc)
                    .addListener(new Transformer.Listener() {
                        @Override public void onCompleted(Composition comp, ExportResult res) { fin[0] = true; cb.done(out, null); }
                        @Override public void onError(Composition comp, ExportResult res, ExportException ex) { fin[0] = true; out.delete(); cb.done(null, "Compression failed: " + ex.getMessage()); }
                    }).build();
            holder[0] = t;
            EditedMediaItem item = new EditedMediaItem.Builder(MediaItem.fromUri(src))
                    .setRemoveAudio(true)
                    .setEffects(new Effects(ImmutableList.<AudioProcessor>of(), fx)).build();
            t.start(item, out.getAbsolutePath());
            final ProgressHolder ph = new ProgressHolder();
            hd.post(new Runnable() {
                @Override public void run() {
                    if (fin[0]) return;
                    if (holder[0].getProgress(ph) == Transformer.PROGRESS_STATE_AVAILABLE) cb.progress(ph.progress);
                    hd.postDelayed(this, 350);
                }
            });
        } catch (Exception e) {
            cb.done(null, "Compression failed: " + e.getMessage());
        }
    }
}
