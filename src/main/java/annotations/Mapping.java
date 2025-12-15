package annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Mapping {
    // Le chemin de l'URL à mapper (ex: "/users", "/products/{id}")
    String value();
    
    // Méthode HTTP (GET, POST, PUT, DELETE, etc.)
    String method() default "GET";
    
    // Auteur de la méthode (optionnel)
    String auteur() default "Inconnu";
    
    // Version (optionnel)
    int version() default 1;
}


