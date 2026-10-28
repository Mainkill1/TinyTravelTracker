package com.mapzen.tangram;

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

public final class FontParserRegressionTest {
    private static FontFileParser parse(String xml, boolean legacy) throws Exception {
        FontFileParser fonts = new FontFileParser();
        KXmlParser parser = new KXmlParser();
        parser.setInput(new StringReader(xml));
        Method method = FontFileParser.class.getDeclaredMethod(
                legacy ? "processDocumentPreLollipop" : "processDocument", XmlPullParser.class);
        method.setAccessible(true);
        try { method.invoke(fonts, parser); }
        catch (InvocationTargetException error) { throw (Exception) error.getCause(); }
        if (parser.getEventType() != XmlPullParser.END_DOCUMENT) throw new AssertionError("Incomplete XML parse");
        return fonts;
    }
    private static void expect(FontFileParser fonts, String key, String filename) {
        String expected = "/system/fonts/" + filename;
        if (!expected.equals(fonts.getFontFile(key)))
            throw new AssertionError(key + ": expected " + expected + ", found " + fonts.getFontFile(key));
    }
    public static void main(String[] args) throws Exception {
        FontFileParser variable = parse("<familyset><family name='sans-serif'>"
                + "<font weight='400'>\n Roboto-Regular.ttf\n <axis tag='ital' stylevalue='0'/>"
                + "<axis tag='wdth' stylevalue='100'/><axis tag='wght' stylevalue='400'/></font>"
                + "<font weight='700' style='italic'>Roboto-Italic.ttf</font></family>"
                + "<alias name='sans' to='sans-serif' weight='400'/>"
                + "<family><font weight='400'>Fallback.ttf<axis tag='wght' stylevalue='400'/></font>"
                + "<font weight='700'>Fallback-Bold.ttf</font></family>"
                + "<family name='serif'><font> Serif.ttf <metadata><axis/></metadata></font></family>"
                + "</familyset>", false);
        expect(variable, "sans-serif_400_normal", "Roboto-Regular.ttf");
        expect(variable, "sans-serif_700_italic", "Roboto-Italic.ttf");
        expect(variable, "sans_400_normal", "Roboto-Regular.ttf");
        expect(variable, "serif_400_normal", "Serif.ttf");

        FontFileParser classic = parse("<familyset><family name='classic'>"
                + "<font weight='400' style='normal'>Classic.ttf</font>"
                + "<font weight='700' style='italic'>Classic-BoldItalic.ttf</font>"
                + "</family></familyset>", false);
        expect(classic, "classic_400_normal", "Classic.ttf");
        expect(classic, "classic_700_italic", "Classic-BoldItalic.ttf");

        android.os.Build.VERSION.SDK_INT = 18;
        FontFileParser legacy = parse("<familyset><family><nameset><name>Legacy</name></nameset>"
                + "<fileset><file>Legacy-Regular.ttf</file></fileset></family></familyset>", true);
        expect(legacy, "legacy_400_normal", "Legacy-Regular.ttf");
        System.out.println("PASS: variable axes, filename whitespace, following fonts/families, fallback axes, alias, classic and legacy XML");
    }
}
