package annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Annotation pour mapper les requêtes HTTP POST

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PostMapping {
    String value(); // Le chemin de l'URL à mapper (ex: "/users", "/products/{id}")
    String auteur() default "Inconnu"; // Auteur de la méthode (optionnel => cas authentification)
    String method() default "POST"; // Méthode HTTP (POST)
}

