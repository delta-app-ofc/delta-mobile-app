package com.aula.delta_mobile_app.viewmodel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.function.Predicate;

public final class SignupValidation {
    private SignupValidation() {}

    public static LocalDate birthDate(String value) {
        return LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT));
    }

    public static String personal(String name, String phone, String date, LocalDate today) {
        if (name.trim().isEmpty() || name.trim().length() > 100)
            return "Informe um nome com até 100 caracteres.";
        if (phone.replaceAll("\\D", "").length() > 15) return "O telefone deve ter até 15 dígitos.";
        try {
            if (birthDate(date).isAfter(today.minusYears(18)))
                return "É necessário ter pelo menos 18 anos para criar uma conta.";
        } catch (RuntimeException invalid) {
            return "Informe o nascimento como dd/MM/aaaa.";
        }
        return null;
    }

    public static String email(String email, Predicate<String> validEmail) {
        if (email.trim().length() > 255 || !validEmail.test(email.trim()))
            return "Digite um e-mail válido.";
        return null;
    }

    public static String password(String password) {
        if (password.trim().isEmpty() || password.length() < 8 || password.length() > 256)
            return "A senha deve ter entre 8 e 256 caracteres.";
        return null;
    }

    public static String credentials(String email, String password, String confirmation, Predicate<String> validEmail) {
        String error = email(email, validEmail);
        if (error != null) return error;
        error = password(password);
        if (error != null) return error;
        if (!password.equals(confirmation)) return "As senhas não coincidem.";
        return null;
    }

    public static boolean isResidential(String classification) {
        if (classification == null) return false;
        switch (classification) {
            case "RESIDENCIAL_NORMAL":
            case "RESIDENCIAL_ESPECIAL":
            case "RESIDENCIAL_FAVELA":
            case "RESIDENCIAL_SOCIAL":
                return true;
            default:
                return false;
        }
    }

    public static String property(String name, int type, int classification, int typeCount, int classificationCount, String area) {
        if (name.trim().isEmpty() || name.trim().length() > 100)
            return "Informe o nome do imóvel com até 100 caracteres.";
        if (type <= 0 || type >= typeCount || classification <= 0 || classification >= classificationCount)
            return "Selecione o tipo e a classificação.";
        try {
            if (!area.trim().isEmpty() && new BigDecimal(area.trim().replace(',', '.')).signum() <= 0)
                return "Informe uma área positiva.";
        } catch (NumberFormatException invalid) {
            return "Informe uma área válida.";
        }
        return null;
    }
}
