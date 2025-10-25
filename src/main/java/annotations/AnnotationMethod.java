package annotations;
// Import nécessaires
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

// Déclaration de l'annotation
@Retention(RetentionPolicy.RUNTIME) // accessible à l'exécution
@Target(ElementType.METHOD)           // applicable uniquement aux méthodes
public @interface AnnotationMethod {
    // On peut définir des "paramètres" pour l'annotation
    String auteur() default "Inconnu";
    int version() default 1;
}
