package com.dexdrip.stephenblack.nightwatch.model;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import android.content.Context;

import com.dexdrip.stephenblack.nightwatch.BuildConfig;
import com.dexdrip.stephenblack.nightwatch.NightWatchTest;

import org.junit.Test;
import static org.junit.Assert.*;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@RunWith(RobolectricTestRunner.class)
@Config(
        sdk = 28,
        // Run with TestApplication instead of actual
        application = NightWatchTest.class
        // Test against Lollipop
)
public class AlertTypeTest extends NightWatchTest {
    @Mock
    Context mockContext;

    private AlertType mAt;

    @Before
    public void setUp() throws Exception {
        mAt = new AlertType();

    }
    @Test
    public void testHigh() {

        mAt.remove_all();
        mAt.add_alert("001", "high alert 1", AlertType.alertType.high, 180, true, 10, null, 0, 0, true, 20, true);
        mAt.add_alert("002", "high alert 2", AlertType.alertType.high, 200, true, 10, null, 0, 0, true, 20, true);
        mAt.add_alert("003", "high alert 3", AlertType.alertType.high, 220, true, 10, null, 0, 0, true, 20, true);
        mAt.print_all();

        // check to see if we get the alerts we expect
        AlertType ah1 = mAt.get_highest_active_alert(mockContext, 190);
        assertTrue(ah1.uuid.equals("001"));
        AlertType ah2 = mAt.get_highest_active_alert(mockContext, 210);
        assertTrue(ah2.uuid.equals("002"));
        AlertType ah3 = mAt.get_highest_active_alert(mockContext, 225);
        assertTrue(ah3.uuid.equals("003"));


    }
    @Test
    public void testAll() {
        //UserError.Log.d(TAG, "ah1 = " + ah1.toString());
        //UserError.Log.d(TAG, "ah2 = " + ah2.toString());


        // check to make sure we retrieve the correct record
        //UserError.Log.d(TAG, "ah1 == a3 ? need to see true " + (ah1==a3) + " " + ah1 + " " + a3);

        AlertType.add_alert(null, "low alert 1", AlertType.alertType.low, 80, true, 10, null, 0, 0, true, 20, true);
        AlertType.add_alert(null, "low alert 2", AlertType.alertType.low, 60, true, 10, null, 0, 0, true, 20, true);
        AlertType.print_all();

        // a bs of 90 should not trigger an Alarm
        // 90 > 80 && 90 > 60 && 90 < 180, 200, 220
        AlertType al1 = AlertType.get_highest_active_alert(mockContext, 90);
        //UserError.Log.d(TAG, "al1 should be null  " + al1);

        // a bs of 80 should trigger a low
        al1 = AlertType.get_highest_active_alert(mockContext, 80);
        //UserError.Log.d(TAG, "al1 = " + al1.toString());

        // now we should see the 60 alarm instead of 80
        AlertType al2 = AlertType.get_highest_active_alert(mockContext, 50);
        //UserError.Log.d(TAG, "al2 = " + al2.toString());

        AlertType.add_alert(null, "missed data 1", AlertType.alertType.missed, 15, true, 10, null, 0, 0, true, 20, true);
        AlertType.add_alert(null, "missed data 2", AlertType.alertType.missed, 30, true, 10, null, 0, 0, true, 20, true);

        AlertType md1 = AlertType.get_highest_active_alert(mockContext, 10);
        if ( md1 != null ) {
            //UserError.Log.d(TAG, "md1 = " + md1.toString());
        }

        AlertType md2 = AlertType.get_highest_active_alert(mockContext, 35);
        if ( md2 != null ) {
            //UserError.Log.d(TAG, "md2 = " + md2.toString());
        }
        AlertType.print_all();

        //UserError.Log.d(TAG, "HigherAlert(ah1, ah2) = ah2? " +  (at.HigherAlert(ah1,ah2) == ah2));
        //UserError.Log.d(TAG, "HigherAlert(al1, al2) = al2? " +  (at.HigherAlert(al1,al2) == al2));
        //UserError.Log.d(TAG, "HigherAlert(ah1, al1) = al1? " +  (at.HigherAlert(ah1,al1) == al1));
        //UserError.Log.d(TAG, "HigherAlert(al1, ah2) = al1? " +  (at.HigherAlert(al1,ah2) == al1));

        // Make sure we do not influence on real data...
        AlertType.remove_all();
    }
}
