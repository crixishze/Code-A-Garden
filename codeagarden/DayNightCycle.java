package codeagarden;

import java.awt.Color;
import javax.swing.Timer;

/**
 * Produces a smoothly changing translucent color for the farm's day/night effect.
 * This class uses Swing's timer, so updates happen on the Event Dispatch Thread.
 */
public final class DayNightCycle {
    private static final int UPDATE_INTERVAL_MS = 100;

    private final long cycleDurationMs;
    private final long startTimeMs;
    private Timer timer;

    // Time points are fractions of one full cycle. Alpha 0 means no tint.
    private static final Keyframe[] KEYFRAMES = {
        new Keyframe(0.00, new Color(255, 174, 92, 95)),  // sunrise
        new Keyframe(0.18, new Color(255, 221, 137, 35)), // morning
        new Keyframe(0.35, new Color(255, 255, 255, 0)),  // bright noon
        new Keyframe(0.53, new Color(255, 151, 72, 100)), // sunset
        new Keyframe(0.68, new Color(20, 31, 100, 145)),  // night
        new Keyframe(0.86, new Color(15, 24, 78, 125)),  // late night
        new Keyframe(1.00, new Color(255, 174, 92, 95))  // next sunrise
    };

    public DayNightCycle(int secondsPerCycle) {
        if (secondsPerCycle < 4) {
            throw new IllegalArgumentException("secondsPerCycle must be at least 4");
        }
        cycleDurationMs = secondsPerCycle * 1000L;
        startTimeMs = System.currentTimeMillis();
    }

    /** Starts periodic repaint requests. Safe to call more than once. */
    public void start(Runnable repaintAction) {
        if (timer != null) {
            timer.stop();
        }
        timer = new Timer(UPDATE_INTERVAL_MS, e -> repaintAction.run());
        timer.start();
    }

    /** Returns the current translucent lighting tint. */
    public Color getTint() {
        double progress = (System.currentTimeMillis() - startTimeMs) % cycleDurationMs;
        progress /= cycleDurationMs;

        for (int i = 0; i < KEYFRAMES.length - 1; i++) {
            Keyframe a = KEYFRAMES[i];
            Keyframe b = KEYFRAMES[i + 1];
            if (progress >= a.time && progress <= b.time) {
                double t = (progress - a.time) / (b.time - a.time);
                return blend(a.color, b.color, t);
            }
        }
        return KEYFRAMES[KEYFRAMES.length - 1].color;
    }

    private static Color blend(Color a, Color b, double t) {
        int r = interpolate(a.getRed(), b.getRed(), t);
        int g = interpolate(a.getGreen(), b.getGreen(), t);
        int blue = interpolate(a.getBlue(), b.getBlue(), t);
        int alpha = interpolate(a.getAlpha(), b.getAlpha(), t);
        return new Color(r, g, blue, alpha);
    }

    private static int interpolate(int a, int b, double t) {
        return (int) Math.round(a + (b - a) * t);
    }

    private static final class Keyframe {
        final double time;
        final Color color;

        Keyframe(double time, Color color) {
            this.time = time;
            this.color = color;
        }
    }
}
