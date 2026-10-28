package com.rareventure.gps2;

import android.content.Context;
import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class WindowInsetsTest {
    @Test public void systemBarsKeepContentVisibleWithoutAccumulatingPadding() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {

            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            View content = new View(context);
            content.setPadding(1, 2, 3, 4);
            LegacyWindowInsets.install(content);
            WindowInsetsCompat insets = new WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 30, 0, 24))
                    .build();
            ViewCompat.dispatchApplyWindowInsets(content, insets);
            ViewCompat.dispatchApplyWindowInsets(content, insets);
            assertEquals(1, content.getPaddingLeft());
            assertEquals(32, content.getPaddingTop());
            assertEquals(3, content.getPaddingRight());
            assertEquals(28, content.getPaddingBottom());
            WindowInsetsCompat keyboard = new WindowInsetsCompat.Builder(insets)
                    .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, 300))
                    .build();
            ViewCompat.dispatchApplyWindowInsets(content, keyboard);
            assertEquals(304, content.getPaddingBottom());
            ViewCompat.dispatchApplyWindowInsets(content, insets);
            assertEquals(28, content.getPaddingBottom());
        });
    }
}
