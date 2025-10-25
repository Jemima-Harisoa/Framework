// Import des annotations et classes nécessaires
import annotations.Controller;
import lookup.ClassScanner;
import lookup.AnnotationAnalyzer;
import lookup.AnnotationAnalyzer.AnnotationAnalysisResult;
import java.util.List;

/**
 * Classe principale pour scanner et analyser les classes annotées dans un package
 * Utilise les services du package lookup pour la découverte et l'analyse
 */
public class Main {
    
    /**
     * Point d'entrée de l'application
     * @param args arguments de la ligne de commande (non utilisés)
     * @throws Exception si une erreur survient lors du scan des classes
     */
    public static void main(String[] args) throws Exception {
        // Nom du package à scanner
        String packageName = "test";
        
        // Initialisation des services
        ClassScanner classScanner = new ClassScanner();
        AnnotationAnalyzer annotationAnalyzer = new AnnotationAnalyzer();
        
        // Étape 1: Récupérer toutes les classes du package
        List<Class<?>> allClasses = classScanner.getClasses(packageName);
        
        // Étape 2: Analyser les annotations @Controller
        AnnotationAnalysisResult analysisResult = 
            annotationAnalyzer.analyzeClasses(allClasses, Controller.class);
        
        // ===== AFFICHAGE DES RÉSULTATS =====
        displayAnnotationResults(analysisResult);
        displayNonAnnotatedClasses(analysisResult);
        displayStatistics(analysisResult);
    }
    
    /**
     * Affiche les classes annotées avec leurs détails
     */
    private static void displayAnnotationResults(AnnotationAnalysisResult result) {
        System.out.println("=== CLASSES ANNOTÉES AVEC @" + result.getAnnotationClass().getSimpleName() + " ===");
        
        for (Class<?> clazz : result.getAnnotatedClasses()) {
            // Récupérer l'instance de l'annotation pour accéder à ses valeurs
            Controller annotation = clazz.getAnnotation(Controller.class);
            System.out.println("Classe: " + clazz.getSimpleName());
            System.out.println("Valeur annotation: " + annotation.name());
            System.out.println("Nom complet: " + clazz.getName());
            System.out.println("-------------------------");
        }
    }
    
    /**
     * Affiche les classes non annotées
     */
    private static void displayNonAnnotatedClasses(AnnotationAnalysisResult result) {
        System.out.println("=== CLASSES NON ANNOTÉES ===");
        for (Class<?> clazz : result.getNonAnnotatedClasses()) {
            System.out.println("❌ " + clazz.getSimpleName());
        }
    }
    
    /**
     * Affiche les statistiques globales de l'analyse
     */
    private static void displayStatistics(AnnotationAnalysisResult result) {
        System.out.println("=== STATISTIQUES ===");
        System.out.println("Total classes: " + result.getTotalClasses());
        System.out.println("Classes annotées: " + result.getAnnotatedCount());
        System.out.println("Classes non annotées: " + result.getNonAnnotatedCount());
        System.out.printf("Taux d'annotation: %.1f%%\n", result.getAnnotationRatio() * 100);
    }
}