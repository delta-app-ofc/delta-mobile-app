package com.aula.delta_mobile_app.model;

import com.aula.delta_mobile_app.model.ApiModels.CreateAddressRequest;
import com.aula.delta_mobile_app.model.ApiModels.CreatePropertyRequest;
import java.util.Arrays;

public final class UnitDraft {
    public static final String[] TYPES = { "CASA", "PRÉDIO" };
    public static final String[] CATEGORIES = { "RESIDENCIAL_NORMAL", "RESIDENCIAL_SOCIAL", "RESIDENCIAL_ESPECIAL", "RESIDENCIAL_FAVELA" };
    public String name = "", cep = "", city = "", state = "", type = "", classification = "", confirmedCep = "";

    public String validation() {
        if (name.trim().isEmpty() || name.trim().length() > 100) return "Informe um nome de até 100 caracteres.";
        if (!cep.matches("[0-9]{8}") || !cep.equals(confirmedCep) || city.trim().isEmpty() || state.trim().isEmpty()) return "Consulte um CEP válido antes de continuar.";
        if (!Arrays.asList(TYPES).contains(type)) return "Selecione o tipo da unidade.";
        if (!Arrays.asList(CATEGORIES).contains(classification)) return "Selecione a categoria residencial.";
        return null;
    }

    public CreateAddressRequest address() {
        CreateAddressRequest request = new CreateAddressRequest();
        request.cep = cep;
        request.city = city.trim();
        request.state = state.trim();
        return request;
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

    public String[] values() { return new String[] { name, cep, city, state, type, classification, confirmedCep }; }

    public static UnitDraft restore(String[] values) {
        UnitDraft draft = new UnitDraft();
        if (values != null && values.length == 7) {
            draft.name = values[0]; draft.cep = values[1]; draft.city = values[2]; draft.state = values[3];
            draft.type = values[4]; draft.classification = values[5]; draft.confirmedCep = values[6];
        }
        return draft;
    }
}
