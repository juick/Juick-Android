/*
 * Copyright (C) 2008-2020, Juick
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.juick.android.testing;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;

import com.juick.App;
import com.juick.R;
import com.juick.android.MainActivity;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UITest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule
            = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void isDisplayed_MainActivity() {
        onView(withId(R.id.app_bar_layout))
                .check(matches(isDisplayed()));
    }
    @Test
    public void isCorrectNotification_NotificationSender() throws IOException {
        InputStream notificationData = getClass().getResourceAsStream("/test_notification.json");
        String notification = IOUtils.toString(notificationData, "UTF-8");
        App.getInstance().getNotificationSender().showNotification(notification);
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.openNotification();
        device.waitForIdle();
        List<UiObject2> popups = device.findObjects(By.text("Hello, world!"));
        assertThat(popups.size(), is(1));
    }
}
