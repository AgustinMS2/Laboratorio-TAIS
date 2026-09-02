package uy.edu.utec.taller.productos.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import uy.edu.utec.taller.productos.dto.ProductoPatchDTO;

public class AlMenosUnCampoPresenteValidator
        implements ConstraintValidator<AlMenosUnCampoPresente, ProductoPatchDTO> {

    @Override
    public boolean isValid(ProductoPatchDTO cambios, ConstraintValidatorContext context) {
        return cambios != null && !cambios.sinCambios();
    }
}
