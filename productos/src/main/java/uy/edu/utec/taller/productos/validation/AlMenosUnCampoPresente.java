package uy.edu.utec.taller.productos.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida que en una actualización parcial se envíe al menos un campo
 * (equivalente a {@code minProperties: 1} en el schema {@code ProductoPatch}).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AlMenosUnCampoPresenteValidator.class)
public @interface AlMenosUnCampoPresente {

    String message() default "Debe enviarse al menos un campo para actualizar";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
