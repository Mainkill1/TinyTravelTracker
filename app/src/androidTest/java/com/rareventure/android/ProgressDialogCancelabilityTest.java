package com.rareventure.android;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** AndroidX must retain the task's requested Back/outside-touch cancellation policy. */
@RunWith(AndroidJUnit4.class)
public class ProgressDialogCancelabilityTest {
    @Test public void constructorPassesCancellationPolicyToAndroidX() {
        assertFalse(new ProgressDialogActivity.MyProgressDialogFragment(null, false, true, 0, 0).isCancelable());
        assertTrue(new ProgressDialogActivity.MyProgressDialogFragment(null, true, true, 0, 0).isCancelable());
    }
}
