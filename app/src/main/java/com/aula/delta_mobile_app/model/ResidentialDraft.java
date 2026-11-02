package com.aula.delta_mobile_app.model;

import com.aula.delta_mobile_app.model.ApiModels.AddressResponse;
import com.aula.delta_mobile_app.model.ApiModels.UpdateAddressRequest;
import java.util.Objects;

public final class ResidentialDraft {
    public String cep, city, state;
    public String name;

    public ResidentialDraft(ProfileSettingsData data) {
        this(data.address);
        name = value(data.property.name);
    }

    public boolean changed(ProfileSettingsData data) {
        return changed(data.address) || !Objects.equals(name.trim(), value(data.property.name));
    }

    public ResidentialDraft(AddressResponse address) {
        cep = value(address.cep);
        city = value(address.city);
        state = value(address.state);
    }

    public boolean changed(AddressResponse address) {
        return !Objects.equals(cep, value(address.cep)) || !Objects.equals(city, value(address.city)) || !Objects.equals(state, value(address.state));
    }

    public String validation() {
        if (name != null && name.trim().isEmpty()) return "Informe o nome da residência.";
        if (!cep.matches("[0-9]{8}")) return "Informe um CEP com oito dígitos.";
        if (city.trim().isEmpty() || city.length() > 60) return "Informe uma cidade válida, com até 60 caracteres.";
        if (state.trim().isEmpty() || state.length() > 30) return "Informe um estado válido.";
        return null;
    }

    public UpdateAddressRequest request(AddressResponse original) {
        UpdateAddressRequest request = new UpdateAddressRequest();
        request.cep = cep;
        request.city = city.trim();
        request.state = state.trim();
        if (Objects.equals(cep, original.cep)) {
            request.latitude = original.latitude;
            request.longitude = original.longitude;
        }
        return request;
    }

    private static String value(String value) { return value == null ? "" : value; }
}
