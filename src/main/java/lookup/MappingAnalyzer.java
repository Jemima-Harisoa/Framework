// Fichier : MappingAnalyzer.java
package lookup;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import annotations.GetMapping;
import annotations.Mapping;
import annotations.PostMapping;

/**
 * Service d'analyse des méthodes annotées avec @Mapping, @GetMapping et @PostMapping
 */
public class MappingAnalyzer {
    
    /**
     * Analyse toutes les méthodes annotées avec @Mapping, @GetMapping ou @PostMapping
     * dans une liste de classes
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
     * Analyse les méthodes d'une classe spécifique pour trouver les annotations
     * @Mapping, @GetMapping et @PostMapping
     * 
     * @param clazz la classe à analyser
     * @return liste des méthodes mappées trouvées
     */
    private List<MappedMethod> analyzeClassMethods(Class<?> clazz) {
        List<MappedMethod> mappedMethods = new ArrayList<>();
        
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            // Vérifier @Mapping
            if (method.isAnnotationPresent(Mapping.class)) {
                Mapping mapping = method.getAnnotation(Mapping.class);
                MappedMethod mappedMethod = new MappedMethod(
                    method, 
                    mapping.value(), 
                    mapping.method(), 
                    mapping.auteur(), 
                    mapping.version()
                );
                mappedMethods.add(mappedMethod);
            }
            // Vérifier @GetMapping
            else if (method.isAnnotationPresent(GetMapping.class)) {
                GetMapping mapping = method.getAnnotation(GetMapping.class);
                MappedMethod mappedMethod = new MappedMethod(
                    method, 
                    mapping.value(), 
                    mapping.method(), 
                    mapping.auteur(), 
                    1  // Version par défaut pour GetMapping
                );
                mappedMethods.add(mappedMethod);
            }
            // Vérifier @PostMapping
            else if (method.isAnnotationPresent(PostMapping.class)) {
                PostMapping mapping = method.getAnnotation(PostMapping.class);
                MappedMethod mappedMethod = new MappedMethod(
                    method, 
                    mapping.value(), 
                    mapping.method(), 
                    mapping.auteur(), 
                    1  // Version par défaut pour PostMapping
                );
                mappedMethods.add(mappedMethod);
            }
        }
        
        return mappedMethods;
    }
    
    /**
     * Classe représentant une méthode mappée avec ses informations
     */
    public static class MappedMethod {
        private final Method method;
        private final String url;
        private final String httpMethod;
        private final String auteur;
        private final int version;
        /* 
         * TODO: ajouter de quoi stocker les paramètres attendus par la méthode 
         * => Les méthodes seront annotées 
         */       
        public MappedMethod(Method method, String url, String httpMethod, String auteur, int version) {
            this.method = method;
            this.url = url;
            this.httpMethod = httpMethod;
            this.auteur = auteur;
            this.version = version;
        }
        
        // Getters
        public Method getMethod() { return method; }
        public String getUrl() { return url; }
        public String getHttpMethod() { return httpMethod; }
        public String getAuteur() { return auteur; }
        public int getVersion() { return version; }
        
        @Override
        public String toString() {
            return String.format("MappedMethod{url='%s', httpMethod='%s', javaMethod=%s, auteur='%s', version=%d}", 
                url, httpMethod, method.getName(), auteur, version);
        }
    }
}