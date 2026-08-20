package com.aprendia.app.safety;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SafetyFilterTest {
    private final SafetyFilter filter = new SafetyFilter();

    @Test
    public void isUnsafe_returnsTrueForBlockedTerm() {
        assertTrue(filter.isUnsafe("Como hago trampa en un examen?"));
        assertTrue(filter.isUnsafe("Quiero copiar la tarea"));
    }

    @Test
    public void isUnsafe_returnsFalseForSafeQuestion() {
        assertFalse(filter.isUnsafe("Que es la fotosintesis?"));
        assertFalse(filter.isUnsafe("Como cuidar el agua?"));
    }

    @Test
    public void isUnsafe_normalizesAccentsAndCase() {
        assertTrue(filter.isUnsafe("DAME UNA ARMA"));
        assertTrue(filter.isUnsafe("¿Cómo robar?"));
    }

    @Test
    public void isUnsafe_treatsPartialMatchesAsBlocked() {
        assertTrue(filter.isUnsafe("violencia escolar"));
        assertTrue(filter.isUnsafe("droga que estudiar"));
    }

    @Test
    public void isUnsafe_emptyAndShortInputSafe() {
        assertFalse(filter.isUnsafe(""));
        assertFalse(filter.isUnsafe("hola"));
    }
}