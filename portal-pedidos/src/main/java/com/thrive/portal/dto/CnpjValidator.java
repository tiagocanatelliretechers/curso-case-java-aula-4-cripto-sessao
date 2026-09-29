package com.thrive.portal.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CnpjValidator implements ConstraintValidator<CNPJ, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null) return false;
        String cnpj = value.replaceAll("\\D", "");
        if (cnpj.length() != 14 || cnpj.chars().distinct().count() == 1) return false;
        int[] p1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] p2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        return dig(cnpj, 12, p1) == (cnpj.charAt(12) - '0')
            && dig(cnpj, 13, p2) == (cnpj.charAt(13) - '0');
    }

    private int dig(String c, int len, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < len; i++) {
            soma += (c.charAt(i) - '0') * pesos[i];
        }
        int r = soma % 11;
        return (r < 2) ? 0 : 11 - r;
    }
}
