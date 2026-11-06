package com.aula.delta_mobile_app.model;

import com.aula.delta_mobile_app.model.ApiModels.CreateAddressRequest;
import com.aula.delta_mobile_app.model.ApiModels.CreatePropertyRequest;
import java.math.BigDecimal;
import java.util.Arrays;

public final class UnitDraft {
    public static final String[] TYPES = { "CASA", "PRÉDIO" };
    public static final String[] CATEGORIES = { "RESIDENCIAL_NORMAL", "RESIDENCIAL_SOCIAL", "RESIDENCIAL_ESPECIAL", "RESIDENCIAL_FAVELA" };
    public String name = "", cep = "", city = "", state = "", type = "", classification = "", confirmedCep = "";
    public BigDecimal latitude, longitude;

    public String validation() {
        if (name.trim().isEmpty() || name.trim().length() > 100) return "Informe um nome de até 100 caracteres.";
        if (!cep.matches("[0-9]{8}") || !cep.equals(confirmedCep) || city.trim().isEmpty() || state.trim().isEmpty()) return "Consulte um CEP válido antes de continuar.";
        if (!hasCoordinates()) return "O CEP consultado não retornou latitude e longitude válidas.";
        if (!Arrays.asList(TYPES).contains(type)) return "Selecione o tipo da residência.";
        if (!Arrays.asList(CATEGORIES).contains(classification)) return "Selecione a categoria residencial.";
        return null;
    }

    public CreateAddressRequest address() {
        CreateAddressRequest request = new CreateAddressRequest();
        request.cep = cep;
        request.city = city.trim();
        request.state = state.trim();
        request.latitude = latitude;
        request.longitude = longitude;
        return request;
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null && latitude.compareTo(BigDecimal.valueOf(-90)) >= 0 && latitude.compareTo(BigDecimal.valueOf(90)) <= 0
            && longitude.compareTo(BigDecimal.valueOf(-180)) >= 0 && longitude.compareTo(BigDecimal.valueOf(180)) <= 0;
    }

    public CreatePropertyRequest property(int userId, int addressId) {
        CreatePropertyRequest request = new CreatePropertyRequest();
        request.userId = userId;
        request.addressId = addressId;
        request.name = name.trim();
        request.type = type;
        request.classification = classification;
        return request;
    }

    public String[] values() { return new String[] { name, cep, city, state, type, classification, confirmedCep, latitude == null ? "" : latitude.toPlainString(), longitude == null ? "" : longitude.toPlainString() }; }

    public static UnitDraft restore(String[] values) {
        UnitDraft draft = new UnitDraft();
        if (values != null && values.length >= 7) {
            draft.name = values[0]; draft.cep = values[1]; draft.city = values[2]; draft.state = values[3];
            draft.type = values[4]; draft.classification = values[5]; draft.confirmedCep = values[6];
            if (values.length >= 9) {
                draft.latitude = coordinate(values[7]);
                draft.longitude = coordinate(values[8]);
            }
        }
        return draft;
    }

    private static BigDecimal coordinate(String value) {
        if (value == null || value.isEmpty()) return null;
        try { return new BigDecimal(value); } catch (NumberFormatException invalid) { return null; }
    }
}
