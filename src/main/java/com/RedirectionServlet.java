package com;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import annotations.Controller;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lookup.AnnotationAnalyzer;
import lookup.AnnotationAnalyzer.AnnotationAnalysisResult;
import lookup.ClassScanner;

@WebServlet(name = "RedirectionServlet", urlPatterns = { "/" })
public class RedirectionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private Map<String, Class<?>> controllerMap = new HashMap<>();

    @Override
    public void init() throws ServletException {
        try {
            // Nom du package à scanner
            String classPath = "WEBINF"; 
            
            // Initialisation des services
            ClassScanner classScanner = new ClassScanner();
            AnnotationAnalyzer annotationAnalyzer = new AnnotationAnalyzer();
            
            // Étape 1: Récupérer toutes les classes du package
            List<Class<?>> allClasses = classScanner.getClasses(classPath);
            
            // Étape 2: Analyser les annotations @Controller
            AnnotationAnalysisResult analysisResult = 
                annotationAnalyzer.analyzeClasses(allClasses, Controller.class);
            
            // Construire la map des contrôleurs
            buildControllerMap(analysisResult);
            
            // ===== AFFICHAGE DES RÉSULTATS =====
            displayAnnotationResults(analysisResult);
            displayNonAnnotatedClasses(analysisResult);
            displayStatistics(analysisResult);
            
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation du scan des contrôleurs", e);
        }
    }

    /**
     * Construit une map des contrôleurs avec leur nom d'annotation comme clé
     */
    private void buildControllerMap(AnnotationAnalysisResult result) {
        for (Class<?> clazz : result.getAnnotatedClasses()) {
            Controller annotation = clazz.getAnnotation(Controller.class);
            String controllerName = annotation.name();
            if (!controllerName.isEmpty()) {
                controllerMap.put(controllerName, clazz);
                System.out.println("Contrôleur mappé: " + controllerName + " -> " + clazz.getSimpleName());
            }
        }
        System.out.println("Total des contrôleurs mappés: " + controllerMap.size());
    }

    /**
     * Vérifie si l'URL correspond à un contrôleur
     */
    private boolean isUrlMappedToController(String urlPath) {
        // Extraire le nom du contrôleur de l'URL
        // Exemple: "/productController" -> "productController"
        if (urlPath == null || urlPath.equals("/") || urlPath.isEmpty()) {
            return false;
        }
        
        String controllerName = urlPath.startsWith("/") ? 
            urlPath.substring(1) : urlPath;
        
        return controllerMap.containsKey(controllerName);
    }

    /**
     * Récupère la classe contrôleur correspondant à l'URL
     */
    private Class<?> getControllerForUrl(String urlPath) {
        if (urlPath == null || urlPath.equals("/") || urlPath.isEmpty()) {
            return null;
        }
        
        String controllerName = urlPath.startsWith("/") ? 
            urlPath.substring(1) : urlPath;
        
        return controllerMap.get(controllerName);
    }

    private void doService(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain;charset=UTF-8");
        String path = request.getRequestURI().substring(request.getContextPath().length());
        
        System.out.println("URL demandée: " + path);
        
        // Cas de la racine
        if ("/".equals(path)) {
            response.getWriter().println("Page d'accueil - Aucun contrôleur spécifié");
            return;
        }

        // Vérifier si l'URL correspond à un contrôleur
        if (isUrlMappedToController(path)) {
            Class<?> controllerClass = getControllerForUrl(path);
            Controller annotation = controllerClass.getAnnotation(Controller.class);
            
            String message = String.format(
                "✅ URL MAPPÉE AVEC SUCCÈS\n" +
                "URL: %s\n" +
                "Contrôleur: %s\n" +
                "Classe: %s\n" +
                "Description: %s",
                path,
                annotation.name(),
                controllerClass.getSimpleName(),
                annotation.description()
            );
            
            response.getWriter().println(message);
        } else {
            // Vérifier si c'est une ressource statique
            boolean resourceExists = getServletContext().getResource(path) != null;

            if (resourceExists) {
                RequestDispatcher defaultDispatcher = getServletContext().getNamedDispatcher("default");
                defaultDispatcher.forward(request, response);
            } else {
                String message = String.format(
                    "❌ CHEMIN INCONNU\n" +
                    "URL: %s\n" +
                    "Statut: Aucun contrôleur trouvé pour cette URL",
                    path
                );
                response.getWriter().println(message);
            }
        }
    }

    // Les méthodes d'affichage restent inchangées
    private static void displayAnnotationResults(AnnotationAnalysisResult result) {
        System.out.println("=== CLASSES ANNOTÉES AVEC @" + result.getAnnotationClass().getSimpleName() + " ===");
        
        for (Class<?> clazz : result.getAnnotatedClasses()) {
            Controller annotation = clazz.getAnnotation(Controller.class);
            System.out.println("Classe: " + clazz.getSimpleName());
            System.out.println("Valeur annotation: " + annotation.name());
            System.out.println("Nom complet: " + clazz.getName());
            System.out.println("-------------------------");
        }
    }
    
    private static void displayNonAnnotatedClasses(AnnotationAnalysisResult result) {
        System.out.println("=== CLASSES NON ANNOTÉES ===");
        for (Class<?> clazz : result.getNonAnnotatedClasses()) {
            System.out.println("❌ " + clazz.getSimpleName());
        }
    }
    
    private static void displayStatistics(AnnotationAnalysisResult result) {
        System.out.println("=== STATISTIQUES ===");
        System.out.println("Total classes: " + result.getTotalClasses());
        System.out.println("Classes annotées: " + result.getAnnotatedCount());
        System.out.println("Classes non annotées: " + result.getNonAnnotatedCount());
        System.out.printf("Taux d'annotation: %.1f%%\n", result.getAnnotationRatio() * 100);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doService(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doService(request, response);
    }
}