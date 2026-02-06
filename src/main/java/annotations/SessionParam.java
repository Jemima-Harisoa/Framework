package annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface SessionParam {
    /**
     * Le nom de la clé dans la session
     * Si vide, utilise le nom du paramètre Java
     */
    String value() default "";
    
    /**
     * Le nom de la clé dans la session (alias pour value)
     */
    String name() default "";
    
    /**
     * Si le paramètre est requis
     * Si true et que la valeur n'existe pas en session, une exception est levée
     */
    boolean required() default false;
    
    /**
     * Valeur par défaut si le paramètre n'est pas trouvé en session
     * Seulement utilisé si required = false
     */
    String defaultValue() default "";
}
