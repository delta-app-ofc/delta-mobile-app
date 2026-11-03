package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.AuthModels.Completed;
import com.aula.delta_mobile_app.model.AuthModels.PropertyLink;

import java.util.List;

public final class SignupCheck {
    private SignupCheck() {}

    public static Completed confirmed(Integer userId, List<PropertyLink> links) {
        if (userId == null || userId <= 0 || links == null) return null;
        if (links.stream().noneMatch(link -> link != null && link.propertyId != null && link.propertyId > 0))
            return null;
        Completed value = new Completed();
        value.userId = userId;
        value.completed = true;
        value.recoveredExisting = true;
        return value;
    }
}
