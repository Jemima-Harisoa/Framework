package annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GetMapping {
    String value(); // Le chemin de l'URL à mapper (ex: "/users", "/products/{id}")
    String auteur() default "Inconnu"; // Auteur de la méthode (optionnel => cas authentification)
    String method() default "GET"; // Méthode HTTP (GET)

}
