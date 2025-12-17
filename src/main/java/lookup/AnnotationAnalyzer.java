package lookup;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service d'analyse des annotations sur les classes
 * Fournit des méthodes pour analyser et trier les classes selon leurs annotations
 */
public class AnnotationAnalyzer {

    /**
     * Analyse un ensemble de classes et les sépare selon la présence de plusieurs annotations
     * 
     * @param classes liste des classes à analyser
     * @param annotationClasses types d'annotations à rechercher
     * @return map contenant les résultats pour chaque annotation
     */
    public Map<Class<? extends Annotation>, AnnotationAnalysisResult> analyzeMultipleAnnotations(
            List<Class<?>> classes, 
            Class<? extends Annotation>... annotationClasses) {
        
        Map<Class<? extends Annotation>, AnnotationAnalysisResult> results = new HashMap<>();
        
        for (Class<? extends Annotation> annotationClass : annotationClasses) {
            results.put(annotationClass, analyzeClasses(classes, annotationClass));
        }
        
        return results;
    }
    
    /**
     * Analyse un ensemble de classes et les sépare selon la présence d'une annotation
     * 
     * @param classes liste des classes à analyser
     * @param annotationClass type d'annotation à rechercher
     * @return résultat contenant les classes annotées et non annotées
     */
    public <T extends Annotation> AnnotationAnalysisResult analyzeClasses(
            List<Class<?>> classes, 
            Class<T> annotationClass) {
        
        List<Class<?>> annotatedClasses = new ArrayList<>();
        List<Class<?>> nonAnnotatedClasses = new ArrayList<>();
        
        // Parcourir toutes les classes et les trier selon la présence de l'annotation
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(annotationClass)) {
                annotatedClasses.add(clazz);
            } else {
                nonAnnotatedClasses.add(clazz);
            }
        }
        
        return new AnnotationAnalysisResult(annotatedClasses, nonAnnotatedClasses, annotationClass);
    }
    
    /**
     * Récupère les valeurs d'une annotation spécifique pour une classe
     * 
     * @param clazz la classe à analyser
     * @param annotationClass le type d'annotation
     * @return l'instance de l'annotation ou null si non présente
     */
    public <T extends Annotation> T getAnnotationValue(Class<?> clazz, Class<T> annotationClass) {
        return clazz.getAnnotation(annotationClass);
    }
    
    /**
     * Vérifie si une classe possède une annotation spécifique
     * 
     * @param clazz la classe à vérifier
     * @param annotationClass le type d'annotation
     * @return true si la classe possède l'annotation
     */
    public <T extends Annotation> boolean hasAnnotation(Class<?> clazz, Class<T> annotationClass) {
        return clazz.isAnnotationPresent(annotationClass);
    }
    
    /**
     * Classe contenant le résultat de l'analyse des annotations
     */
    public static class AnnotationAnalysisResult {
        private final List<Class<?>> annotatedClasses;
        private final List<Class<?>> nonAnnotatedClasses;
        private final Class<? extends Annotation> annotationClass;
        
        public AnnotationAnalysisResult(List<Class<?>> annotatedClasses, 
                                      List<Class<?>> nonAnnotatedClasses, 
                                      Class<? extends Annotation> annotationClass) {
            this.annotatedClasses = annotatedClasses;
            this.nonAnnotatedClasses = nonAnnotatedClasses;
            this.annotationClass = annotationClass;
        }
        
        // Getters
        public List<Class<?>> getAnnotatedClasses() { return annotatedClasses; }
        public List<Class<?>> getNonAnnotatedClasses() { return nonAnnotatedClasses; }
        public Class<? extends Annotation> getAnnotationClass() { return annotationClass; }
        
        // Méthodes utilitaires
        public int getTotalClasses() { 
            return annotatedClasses.size() + nonAnnotatedClasses.size(); 
        }
        
        public int getAnnotatedCount() { 
            return annotatedClasses.size(); 
        }
        
        public int getNonAnnotatedCount() { 
            return nonAnnotatedClasses.size(); 
        }
        
        public double getAnnotationRatio() {
            int total = getTotalClasses();
            return total > 0 ? (double) getAnnotatedCount() / total : 0.0;
        }
    }
}