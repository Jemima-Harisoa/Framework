package com;

import java.lang.reflect.Method;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Classe utilitaire pour la sérialisation JSON
 */
public class JsonResponseHandler {
    
    private static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create();
    
    /**
     * Vérifie si une méthode doit retourner du JSON basé sur son type de retour
     */
    public static boolean shouldReturnAsJsonBasedOnReturnType(Method method) {
        Class<?> returnType = method.getReturnType();
        return !returnType.equals(String.class) && 
            !returnType.equals(Void.TYPE) &&
            !model.View.class.isAssignableFrom(returnType);
    }
    /**
     * Vérifie si une méthode doit retourner du JSON
     */
    public static boolean isJsonResponse(Method method) {
        // Vérifie si la méthode a l'annotation @JsonMapping
        return method.isAnnotationPresent(annotations.JsonMapping.class) ||
               // Ou si elle retourne un type qui n'est pas String ou View
               (!method.getReturnType().equals(String.class) && 
                !method.getReturnType().equals(Void.TYPE) &&
                !model.View.class.isAssignableFrom(method.getReturnType()));
    }
    
    /**
     * Vérifie si un objet doit être sérialisé en JSON
     */
    public static boolean shouldReturnAsJson(Object result) {
        return result != null && 
               !(result instanceof String) && 
               !(result instanceof model.View);
    }
    
    /**
     * Sérialise un objet en JSON
     */
    public static String toJson(Object object) {
        return gson.toJson(object);
    }
    
    /**
     * Configure la réponse HTTP pour du JSON
     */
    public static void configureJsonResponse(jakarta.servlet.http.HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }
}