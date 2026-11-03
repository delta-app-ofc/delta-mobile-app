package com.aula.delta_mobile_app.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.aula.delta_mobile_app.view.fragments.QrScanFragment;

import org.junit.Test;

public class QrCodeParserTest {

    @Test
    public void testValidDeviceIdQrCode() {
        assertEquals("123456", QrScanFragment.parseDeviceId("deviceID:123456"));
        assertEquals("9876543210", QrScanFragment.parseDeviceId("deviceID: 9876543210"));
        assertEquals("112233", QrScanFragment.parseDeviceId("DEVICEID:112233"));
        assertEquals("554433", QrScanFragment.parseDeviceId("DeviceID: 554433"));
    }

    @Test
    public void testValidNumericSequenceOnly() {
        assertEquals("123456", QrScanFragment.parseDeviceId("123456"));
        assertEquals("987654", QrScanFragment.parseDeviceId("  987654  "));
    }

    @Test
    public void testInvalidQrCodeFormats() {
        assertNull(QrScanFragment.parseDeviceId(null));
        assertNull(QrScanFragment.parseDeviceId(""));
        assertNull(QrScanFragment.parseDeviceId("   "));
        assertNull(QrScanFragment.parseDeviceId("deviceID:"));
        assertNull(QrScanFragment.parseDeviceId("deviceID:abc"));
        assertNull(QrScanFragment.parseDeviceId("deviceID:123a"));
        assertNull(QrScanFragment.parseDeviceId("https://example.com"));
        assertNull(QrScanFragment.parseDeviceId("random_qr_code_text"));
    }
}
