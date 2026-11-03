package com.aula.delta_mobile_app.model;

import com.aula.delta_mobile_app.model.ApiModels.AddressResponse;
import com.aula.delta_mobile_app.model.ApiModels.PropertyResponse;

public final class ProfileSettingsData {
    public final PropertyResponse property;
    public final AddressResponse address;

    public ProfileSettingsData(PropertyResponse property, AddressResponse address) {
        this.property = property;
        this.address = address;
    }
}
