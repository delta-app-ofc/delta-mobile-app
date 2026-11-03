package com.aula.delta_mobile_app.viewmodel;

import static org.junit.Assert.*;

import org.junit.Test;

import java.time.LocalDate;

public class SignupValidationTest {
    private final LocalDate today = LocalDate.of(2026, 10, 14);

    @Test
    public void personalFieldsRequireNameAndStrictAdultBirthDate() {
        assertNotNull(SignupValidation.personal("", "", "01/01/2000", today));
        assertNotNull(SignupValidation.personal("Nome", "", "", today));
        assertNotNull(SignupValidation.personal("Nome", "", "29/02/2001", today));
        assertNotNull(SignupValidation.personal("Nome", "", "15/10/2008", today));
        assertNull(SignupValidation.personal("Nome", "", "14/10/2008", today));
        assertNull(SignupValidation.personal("Nome", "", "29/02/2000", today));
    }

    @Test
    public void phoneIsOptionalAndLimitedToFifteenDigits() {
        assertNull(SignupValidation.personal("Nome", "", "01/01/2000", today));
        assertNull(SignupValidation.personal("Nome", "(11) 99999-9999", "01/01/2000", today));
        assertNotNull(SignupValidation.personal("Nome", "1".repeat(16), "01/01/2000", today));
    }

    @Test
    public void passwordsMustMatchAndCannotContainOnlySpaces() {
        java.util.function.Predicate<String> email = value -> value.equals("user@example.com");
        assertNotNull(SignupValidation.credentials("invalid", "password", "password", email));
        assertNotNull(SignupValidation.credentials("user@example.com", "short", "short", email));
        assertNotNull(SignupValidation.credentials("user@example.com", "        ", "        ", email));
        assertNotNull(SignupValidation.credentials("user@example.com", "password", "different", email));
        assertNotNull(SignupValidation.credentials("user@example.com", "x".repeat(257), "x".repeat(257), email));
        assertNull(SignupValidation.credentials(" user@example.com ", "password", "password", email));
    }

    @Test
    public void passwordLimitsMatchTheAccountScreen() {
        assertNotNull(SignupValidation.password("x".repeat(7)));
        assertNull(SignupValidation.password("x".repeat(8)));
        assertNull(SignupValidation.password("x".repeat(256)));
        assertNotNull(SignupValidation.password("x".repeat(257)));
        assertNotNull(SignupValidation.password(" ".repeat(8)));
    }

    @Test
    public void passwordConfirmationKeepsSignificantSpaces() {
        assertNotNull(SignupValidation.credentials("user@example.com", "password ", "password", value -> true));
        assertNull(SignupValidation.credentials("user@example.com", "password ", "password ", value -> true));
    }

    @Test
    public void emailLengthIsCheckedBeforeSendingTheAccount() {
        assertNull(SignupValidation.email("x".repeat(255), value -> true));
        assertNotNull(SignupValidation.email("x".repeat(256), value -> true));
        assertNotNull(SignupValidation.email("invalid", value -> false));
    }

    @Test
    public void mobileSignupOnlyAllowsResidentialClassifications() {
        assertTrue(SignupValidation.isResidential("RESIDENCIAL_NORMAL"));
        assertTrue(SignupValidation.isResidential("RESIDENCIAL_ESPECIAL"));
        assertTrue(SignupValidation.isResidential("RESIDENCIAL_FAVELA"));
        assertTrue(SignupValidation.isResidential("RESIDENCIAL_SOCIAL"));
        assertFalse(SignupValidation.isResidential("COMERCIAL_ENTIDADE_ASSISTENCIA_SOCIAL"));
        assertFalse(SignupValidation.isResidential("COMERCIAL_NORMAL_INDUSTRIAL"));
        assertFalse(SignupValidation.isResidential("COMERCIAL_ESPECIAL"));
        assertFalse(SignupValidation.isResidential("PUBLICA_COM_CONTRATO"));
        assertFalse(SignupValidation.isResidential("RESIDENCIAL_DESCONHECIDA"));
        assertFalse(SignupValidation.isResidential(null));
    }

    @Test
    public void oldCommercialSelectionsAreNotAcceptedAsResidential() {
        for (int index : new int[] {1, 4, 6, 8}) assertTrue(SignupViewModel.isResidentialClassification(index));
        for (int index : new int[] {-1, 0, 2, 3, 5, 7, 9}) assertFalse(SignupViewModel.isResidentialClassification(index));
    }

    @Test
    public void propertyRequiresNameTypeAndClassification() {
        assertNotNull(SignupValidation.property("", 1, 1, 3, 9, ""));
        assertNotNull(SignupValidation.property("Casa", 0, 1, 3, 9, ""));
        assertNotNull(SignupValidation.property("Casa", 1, 9, 3, 9, ""));
        assertNull(SignupValidation.property("Casa", 1, 1, 3, 9, ""));
    }

    @Test
    public void optionalAreaMustBePositiveWhenProvided() {
        assertNotNull(SignupValidation.property("Casa", 1, 1, 3, 9, "0"));
        assertNotNull(SignupValidation.property("Casa", 1, 1, 3, 9, "-10"));
        assertNotNull(SignupValidation.property("Casa", 1, 1, 3, 9, "invalid"));
        assertNull(SignupValidation.property("Casa", 1, 1, 3, 9, " 45,5 "));
    }
}
