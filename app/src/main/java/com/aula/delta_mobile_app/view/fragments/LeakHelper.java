package com.aula.delta_mobile_app.view.fragments;

import android.view.*;
import android.widget.*;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.LeakMonitoringData;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

final class LeakHelper {
    private LeakHelper() {}

    static View eventCard(LayoutInflater inflater, ViewGroup parent, LeakMonitoringData.Event event, LeakMonitoringData.Availability availability) {
        View card = inflater.inflate(R.layout.item_leak_event, parent, false);
        android.content.res.Resources r = card.getResources();
        TextView flow = card.findViewById(R.id.leak_flow), normal = card.findViewById(R.id.leak_normal);
        TextView status = card.findViewById(R.id.leak_event_status);
        flow.setText(r.getString(R.string.leak_flow_value, number(event == null ? null : event.currentFlow, 1)));
        normal.setText(r.getString(R.string.leak_normal, number(event == null ? null : event.normalFlow, 0),
            event == null || event.normalWindow == null ? "" : event.normalWindow));
        int label = R.string.leak_unavailable, color = 0xFF4A617D;
        if (event != null) {
            if (event.status == LeakMonitoringData.Status.ACTIVE) {
                label = R.string.leak_active;
                color = 0xFFBD0000;
            } else if (event.status == LeakMonitoringData.Status.INVESTIGATING) {
                label = R.string.leak_investigating;
                color = 0xFF9E8600;
            } else {
                label = R.string.leak_resolved;
                color = 0xFF26754C;
            }
        } else if (availability == LeakMonitoringData.Availability.AVAILABLE)
        label = R.string.leak_no_events;
        status.setText(label);
        status.setTextColor(color);
        card.setContentDescription(r.getString(R.string.leak_event_a11y, r.getString(label), flow.getText()));
        return card;
    }

    static int notice(LeakMonitoringData data) {
        if (data.availability == LeakMonitoringData.Availability.NO_PROPERTY)
            return R.string.leak_missing_property;
        if (data.availability == LeakMonitoringData.Availability.NO_METER)
            return R.string.leak_no_meter;
        if (data.availability == LeakMonitoringData.Availability.AVAILABLE)
            return R.string.leak_no_events_description;
        return R.string.leak_no_telemetry_description;
    }

    static String number(BigDecimal value, int decimals) {
        if (value == null) return "—";
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        format.setMinimumFractionDigits(decimals);
        format.setMaximumFractionDigits(Math.max(1, decimals));
        return format.format(value);
    }
}
