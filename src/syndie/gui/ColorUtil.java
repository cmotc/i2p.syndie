package syndie.gui;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class ColorUtil {
    private static final Map<String, String> _colorNameToRGB = new HashMap();

    private static final Map<String, Color> _colorNameToSystem = new HashMap();

    private static final Map<String, Color> _colorRGBToSystem = new HashMap();

    private static final ArrayList<String> _systemColorNames = new ArrayList();

    private static void buildColorNameToSystemColor() {
        Device dev = Display.getDefault();
        _colorNameToSystem.put("darkblue", dev.getSystemColor(SWT.COLOR_DARK_BLUE));
        _colorNameToSystem.put("darkcyan", dev.getSystemColor(SWT.COLOR_DARK_CYAN));
        _colorNameToSystem.put("darkgray", dev.getSystemColor(SWT.COLOR_DARK_GRAY));
        _colorNameToSystem.put("darkgreen", dev.getSystemColor(SWT.COLOR_DARK_GREEN));
        _colorNameToSystem.put("darkmagenta", dev.getSystemColor(SWT.COLOR_DARK_MAGENTA));
        _colorNameToSystem.put("darkred", dev.getSystemColor(SWT.COLOR_DARK_RED));
        _colorNameToSystem.put("darkyellow", dev.getSystemColor(SWT.COLOR_DARK_YELLOW));
        _colorNameToSystem.put("blue", dev.getSystemColor(SWT.COLOR_BLUE));
        _colorNameToSystem.put("cyan", dev.getSystemColor(SWT.COLOR_CYAN));
        _colorNameToSystem.put("gray", dev.getSystemColor(SWT.COLOR_GRAY));
        _colorNameToSystem.put("green", dev.getSystemColor(SWT.COLOR_GREEN));
        _colorNameToSystem.put("magenta", dev.getSystemColor(SWT.COLOR_MAGENTA));
        _colorNameToSystem.put("red", dev.getSystemColor(SWT.COLOR_RED));
        _colorNameToSystem.put("yellow", dev.getSystemColor(SWT.COLOR_YELLOW));
        _colorNameToSystem.put("white", dev.getSystemColor(SWT.COLOR_WHITE));
        _colorNameToSystem.put("black", dev.getSystemColor(SWT.COLOR_BLACK));
        _systemColorNames.clear();
        _systemColorNames.addAll(new TreeSet(_colorNameToSystem.keySet()));
        for (Map.Entry<String, Color> e :_colorNameToSystem.entrySet()) {
            String name = e.getKey();
            Color color = e.getValue();
            String rgb = "#" + toHex(color.getRed()) + toHex(color.getGreen()) + toHex(color.getBlue());
            _colorNameToRGB.put(name, rgb);
            _colorRGBToSystem.put(rgb, color);
        }
    }

    private static String toHex(int color) {
        String rv = Integer.toHexString(color);
        if (color <= 0x0F)
            return "0" + rv;
        else
            return rv;
    }

    private static Map<Color, Image> _systemColorSwatches = new HashMap();

    private static void buildSystemColorSwatches() {
        for (Color color : _colorNameToSystem.values()) {
            _systemColorSwatches.put(color, ImageUtil.createImage(16, 16, color, true));
        }
    }

    private static boolean _initialized = false;

    public static void init() {
        synchronized (ColorUtil.class) {
            buildColorNameToSystemColor();
            _initialized = true;
        }
        // lazyload 'em
        //buildSystemColorSwatches();
    }
    
    /** alphabetically ordered list of system colors */
    public static ArrayList<String> getSystemColorNames() { return _systemColorNames; }
    
    private static class ColorQuery {
        Color rv;
        String color;
        final Map<String, Color> cache;
        public ColorQuery(String col, Map<String, Color> cacheVal) { color = col; cache = cacheVal; }
    }
    
    public static Color getSystemColor(String rgb) { return _colorRGBToSystem.get(rgb); }
    
    public static Color getColor(String color) { return getColor(color, null); }

    /**
     * get the given color, pulling it from the set of system colors or the cache,
     * if possible.  can be called from any thread
     */
    public static Color getColor(String color, Map<String, Color> cache) {
        if (color == null) return null;
        color = color.trim();
        if (color.length() <= 0) return null;
        final ColorQuery q = new ColorQuery(color, cache);
        String rgb = _colorNameToRGB.get(q.color);
        if (rgb != null)
            q.color = rgb;
        
        int r = -1;
        int g = -1;
        int b = -1;

        //System.out.println("color: " + color);
        if (q.color.startsWith("#") && (q.color.length() == 7)) {
            Color cached = _colorRGBToSystem.get(q.color);
            if ( (q.cache != null) && (cached == null) )
                cached = q.cache.get(q.color);
            if (cached == null) {
                try {
                    r = Integer.parseInt(q.color.substring(1, 3), 16);
                    g = Integer.parseInt(q.color.substring(3, 5), 16);
                    b = Integer.parseInt(q.color.substring(5, 7), 16);
                    //System.out.println("rgb: " + cached + " [" + r + "/" + g + "/" + b + "]");
                } catch (NumberFormatException nfe) {
                    // invalid rgb
                    //System.out.println("invalid rgb");
                    //nfe.printStackTrace();
                }
            }
            if (cached != null)
                return cached;
        } else {
            // invalid rgb, and not one of the system colors
            return null;
        }
        
        if (r != -1) {
            final int qr = r;
            final int qg = g;
            final int qb = b;
            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    Color cached = new Color(Display.getDefault(), qr, qg, qb);
                    if (q.cache != null)
                        q.cache.put(q.color, cached);
                    q.rv = cached;
                }
            });
            return q.rv;
        } else {
            return null;
        }
    }

    /**
     * 16x16 pixel image of the given system color (do not dispose!)
     */
    public static Image getSystemColorSwatch(Color color) { 
        synchronized (_systemColorSwatches) {
            if (_systemColorSwatches.size() == 0)
                buildSystemColorSwatches();
            return _systemColorSwatches.get(color);
        }
    }

    static String getSystemColorName(Color color) {
        for (Map.Entry<String, Color> e : _colorNameToSystem.entrySet()) {
            Color cur = e.getValue();
            if ( (cur == color) || (cur.equals(color)) )
                return e.getKey();
        }
        return null;
    }

    static boolean isSystemColor(Color color) { return _colorNameToSystem.containsValue(color); }
}
