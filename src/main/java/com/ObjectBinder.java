package com;

import java.lang.reflect.Field;
import java.util.Map;

public class ObjectBinder {
    
    /**
     * Crée et remplit un objet complexe à partir des données de la requête
     * Délègue aux entités si l'objet est une entité
     * 
     * @param clazz Le type d'objet à instancier
     * @param formData Les données du formulaire (Map<String, Object>)
     * @param pathVariables Les variables de chemin
     * @return Une instance de l'objet remplie avec les données
     */
    public static <T> T bindObject(Class<T> clazz, 
                                  Map<String, Object> formData,
                                  Map<String, String> pathVariables) 
            throws Exception {
        
        // Si c'est une entité, utilise EntityBinder
        if (isEntity(clazz)) {
            return EntityBinder.bindEntity(clazz, formData, pathVariables);
        }
        
        // Sinon, utilise la logique normale
        return bindSimpleObject(clazz, formData, pathVariables);
    }
    
    /**
     * Vérifie si une classe est une entité
     */
    private static boolean isEntity(Class<?> clazz) {
        try {
            return clazz.isAnnotationPresent(annotations.Entity.class);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Crée et remplit un objet simple (non entité)
     */
    private static <T> T bindSimpleObject(Class<T> clazz, 
                                         Map<String, Object> formData,
                                         Map<String, String> pathVariables) 
            throws Exception {
        
        T instance = clazz.getDeclaredConstructor().newInstance();
        
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();
            
            Object value = findValueForField(fieldName, fieldType, formData, pathVariables, clazz);
            
            if (value != null) {
                field.set(instance, value);
            }
        }
        
        return instance;
    }
    
    /**
     * Cherche une valeur pour un champ donné
     */
    private static Object findValueForField(String fieldName, Class<?> fieldType,
                                          Map<String, Object> formData,
                                          Map<String, String> pathVariables,
                                          Class<?> parentClass) 
            throws Exception {
        
        // 1. Cherche dans les path variables
        if (pathVariables != null && pathVariables.containsKey(fieldName)) {
            return convertToType(pathVariables.get(fieldName), fieldType);
        }
        
        // 2. Cherche dans formData avec notation directe
        if (formData != null && formData.containsKey(fieldName)) {
            return convertToType(formData.get(fieldName), fieldType);
        }
        
        // 3. Cherche avec notation pointée (ClassName.fieldName)
        if (formData != null) {
            String className = parentClass.getSimpleName();
            String dottedName = className + "." + fieldName;
            if (formData.containsKey(dottedName)) {
                return convertToType(formData.get(dottedName), fieldType);
            }
        }
        
        // 4. Cherche avec notation pointée (nom paramètre.champ)
        if (formData != null) {
            String paramName = parentClass.getSimpleName().toLowerCase();
            String paramDottedName = paramName + "." + fieldName;
            if (formData.containsKey(paramDottedName)) {
                return convertToType(formData.get(paramDottedName), fieldType);
            }
        }
        
        return null;
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