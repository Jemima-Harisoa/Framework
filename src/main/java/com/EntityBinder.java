// EntityBinder.java dans le package com
package com;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class EntityBinder {
    
    /**
     * Crée et remplit une entité à partir des données de la requête
     * Utilise le cache d'entités pour vérifier si la classe est une entité
     * 
     * @param clazz Le type d'entité à instancier
     * @param formData Les données du formulaire (Map<String, Object>)
     * @param pathVariables Les variables de chemin
     * @return Une instance de l'entité remplie avec les données
     */
    public static <T> T bindEntity(Class<T> clazz, 
                                  Map<String, Object> formData,
                                  Map<String, String> pathVariables) 
            throws InstantiationException, IllegalAccessException, 
                   IllegalArgumentException, InvocationTargetException, 
                   NoSuchMethodException, SecurityException {
        
        // Vérifie si c'est une entité
        if (!isEntity(clazz)) {
            throw new IllegalArgumentException("La classe " + clazz.getName() + " n'est pas une entité");
        }
        
        // Crée une instance de l'entité
        T instance = clazz.getDeclaredConstructor().newInstance();
        
        // Parcourt tous les champs de la classe
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();
            
            // Cherche d'abord dans les path variables
            Object value = null;
            if (pathVariables != null && pathVariables.containsKey(fieldName)) {
                value = convertToType(pathVariables.get(fieldName), fieldType);
            }
            
            // Cherche dans les données du formulaire avec notation directe
            if (value == null && formData != null && formData.containsKey(fieldName)) {
                value = convertToType(formData.get(fieldName), fieldType);
            }
            
            // Cherche dans les données du formulaire avec notation pointée (classe.champ)
            if (value == null && formData != null) {
                String className = clazz.getSimpleName();
                String dottedName = className + "." + fieldName;
                if (formData.containsKey(dottedName)) {
                    value = convertToType(formData.get(dottedName), fieldType);
                }
            }
            
            // Cherche avec notation pointée (nom paramètre.champ)
            if (value == null && formData != null) {
                String paramName = clazz.getSimpleName().toLowerCase();
                String paramDottedName = paramName + "." + fieldName;
                if (formData.containsKey(paramDottedName)) {
                    value = convertToType(formData.get(paramDottedName), fieldType);
                }
            }
            
            // Si une valeur a été trouvée, l'assigne au champ
            if (value != null) {
                field.set(instance, value);
            }
        }
        
        return instance;
    }
    
    /**
     * Vérifie si une classe est une entité
     */
    private static boolean isEntity(Class<?> clazz) {
        return clazz.isAnnotationPresent(annotations.Entity.class);
    }
    
    /**
     * Convertit une valeur en un type cible
     */
    private static Object convertToType(Object value, Class<?> targetType) {
        if (value == null) return null;
        
        // Si la valeur est déjà du bon type
        if (targetType.isInstance(value)) {
            return value;
        }
        
        // Pour les types simples, utilise la conversion de MappingHelper
        MappingHelper helper = new MappingHelper();
        return helper.convertParameterValue(value, targetType);
    }
}