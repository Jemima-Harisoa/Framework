package annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour marquer les méthodes qui retournent du JSON
 * Peut être utilisée en complément de @GetMapping/@PostMapping
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonMapping {
    // Peut rester vide ou contenir des attributs comme un nom de mapping spécifique
    String value() default "";
}