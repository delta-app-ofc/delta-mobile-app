package com.aula.delta_mobile_app.model;

import com.aula.delta_mobile_app.model.ApiModels.UpdateProfileRequest;
import com.aula.delta_mobile_app.model.AuthModels.Profile;
import java.time.LocalDate;
import java.util.Objects;

public final class PersonalDraft {
    public String name, email, phone, birthDate;

    public PersonalDraft(Profile profile) {
        name = value(profile.name);
        email = value(profile.email);
        phone = value(profile.phone);
        birthDate = value(profile.birthDate);
    }

    public String validation() {
        for (String field : new String[] { "name", "email", "phone", "birthDate" }) {
            String error = error(field);
            if (error != null) return error;
        }
        return null;
    }

    public String error(String field) {
        if ("name".equals(field)) return name.trim().isEmpty() || name.trim().length() > 100 ? "Informe um nome com até 100 caracteres." : null;
        if ("email".equals(field)) return email.trim().length() > 255 || !email.trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") ? "Informe um e-mail válido." : null;
        if ("phone".equals(field)) return phone.replaceAll("\\D", "").length() > 15 ? "Informe um telefone com até 15 dígitos." : null;
        if (!"birthDate".equals(field)) return null;
        try {
            if (!birthDate.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") || LocalDate.parse(birthDate).isAfter(LocalDate.now())) throw new IllegalArgumentException();
        } catch (RuntimeException invalid) { return "Informe uma data de nascimento válida no formato AAAA-MM-DD."; }
        return null;
    }

    public boolean changed(Profile profile) {
        UpdateProfileRequest request = request();
        return !Objects.equals(request.name, profile.name) || !Objects.equals(request.email, profile.email)
            || !Objects.equals(request.phone, normalizedPhone(profile.phone)) || !Objects.equals(request.birthDate, profile.birthDate);
    }

    public UpdateProfileRequest request() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.name = name.trim();
        request.email = email.trim();
        request.phone = normalizedPhone(phone);
        request.birthDate = birthDate;
        return request;
    }

    private static String value(String value) { return value == null ? "" : value; }
    private static String normalizedPhone(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }
}
