package com.prank;

/** Accessibility + render settings, read from the GUI tab. */
public class Access {
    private static boolean on() { return Modules.ACCESS != null && Modules.ACCESS.enabled; }
    private static double opacity() { return on() ? Modules.ACCESS.num("Panel Opacity") : 0.56; }

    public static boolean motion() { return !(on() && Modules.ACCESS.on("Reduce Motion")); }
    public static boolean contrast() { return on() && Modules.ACCESS.on("High Contrast"); }
    public static boolean colorblind() { return on() && Modules.ACCESS.on("Colorblind Health"); }
    public static float hudScale() { return on() ? (float) Modules.ACCESS.num("HUD Scale") : 1f; }

    public static int bg() {
        if (contrast()) return 0xF0000000;
        return ((int) (255 * opacity()) << 24) | 0x101018;
    }

    public static int bgStrong() {
        if (contrast()) return 0xFF000000;
        return (Math.min(255, (int) (255 * opacity()) + 32) << 24) | 0x101018;
    }

    private static boolean render() { return Modules.RENDER != null && Modules.RENDER.enabled; }
    public static float lineWidth() { return render() ? (float) Modules.RENDER.num("Line Width") : 1.5f; }
    public static int fillAlpha() { return (int) (255 * (render() ? Modules.RENDER.num("Fill Opacity") : 0.33)); }
    public static double rainbowSpeed() { return render() ? Modules.RENDER.num("Rainbow Speed") : 1.0; }
}
