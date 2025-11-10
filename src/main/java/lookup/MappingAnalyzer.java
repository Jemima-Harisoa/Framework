// Fichier : MappingAnalyzer.java
package lookup;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import annotations.Mapping;

/**
 * Service d'analyse des méthodes annotées avec @Mapping
 */
public class MappingAnalyzer {
    
    /**
     * Analyse toutes les méthodes annotées avec @Mapping dans une liste de classes
     * 
     * @param classes liste des classes à analyser
     * @return map des méthodes annotées organisées par classe
     */
    public Map<Class<?>, List<MappedMethod>> analyzeMethodMappings(List<Class<?>> classes) {
        Map<Class<?>, List<MappedMethod>> methodMappings = new HashMap<>();
        
        for (Class<?> clazz : classes) {
            List<MappedMethod> mappedMethods = analyzeClassMethods(clazz);
            if (!mappedMethods.isEmpty()) {
                methodMappings.put(clazz, mappedMethods);
            }
        }
        
        return methodMappings;
    }
    
    /**
     * Analyse les méthodes d'une classe spécifique pour trouver les annotations @Mapping
     * 
     * @param clazz la classe à analyser
     * @return liste des méthodes mappées trouvées
     */
    private List<MappedMethod> analyzeClassMethods(Class<?> clazz) {
        List<MappedMethod> mappedMethods = new ArrayList<>();
        
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Mapping.class)) {
                Mapping mapping = method.getAnnotation(Mapping.class);
                MappedMethod mappedMethod = new MappedMethod(method, mapping);
                mappedMethods.add(mappedMethod);
            }
        }
        
        return mappedMethods;
    }
    
    /**
     * Classe représentant une méthode mappée avec son annotation
     */
    public static class MappedMethod {
        private final Method method;
        private final Mapping mapping;
        
        public MappedMethod(Method method, Mapping mapping) {
            this.method = method;
            this.mapping = mapping;
        }
        
        // Getters
        public Method getMethod() { return method; }
        public Mapping getMapping() { return mapping; }
        public String getUrl() { return mapping.value(); }
        public String getHttpMethod() { return mapping.method(); }
        public String getAuteur() { return mapping.auteur(); }
        public int getVersion() { return mapping.version(); }
        
        @Override
        public String toString() {
            return String.format("MappedMethod{url='%s', method='%s', javaMethod=%s}", 
                getUrl(), getHttpMethod(), method.getName());
        }
    }
}