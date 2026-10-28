package com.mapzen.tangram;

import android.util.Xml;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;
import static org.junit.Assert.*;

/** Fail on a partially parsed real system font configuration, including nested axes. */
@RunWith(AndroidJUnit4.class)
public class FontFileParserTest {
    @Test public void actualSystemFontsParseCompletelyAndResolveFiles() throws Exception {
        FontFileParser fonts = new FontFileParser();
        XmlPullParser parser = Xml.newPullParser();
        Method parse = FontFileParser.class.getDeclaredMethod("processDocument", XmlPullParser.class);
        parse.setAccessible(true);
        try (FileInputStream input = new FileInputStream("/system/etc/fonts.xml")) {
            parser.setInput(input, null);
            // Call the reader directly so its public parse() error logging cannot hide failure.
            parse.invoke(fonts, parser);
            assertEquals(XmlPullParser.END_DOCUMENT, parser.getEventType());
        }
        String regular = fonts.getFontFile("sans-serif_400_normal");
        assertNotNull(regular);
        assertFalse("No default sans-serif font parsed", regular.isEmpty());
        assertTrue("Default font path must be usable: " + regular, new File(regular).isFile());
        String italic = fonts.getFontFile("sans-serif_400_italic");
        assertNotNull(italic);
        assertFalse("No italic font parsed", italic.isEmpty());
        assertTrue("Italic font path must be usable: " + italic, new File(italic).isFile());
        String fallback = fonts.getFontFallback(0, 400);
        assertFalse("No fallback font parsed", fallback.isEmpty());
        assertTrue("Fallback font path must be usable: " + fallback, new File(fallback).isFile());
    }
}
