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
    private List<Class<?>> allClasses;
    private AnnotationAnalysisResult analysisResult;
    
    @Override
    public void init() throws ServletException {
        try {
            
            // Initialisation des services
            ClassScanner classScanner = new ClassScanner();
            AnnotationAnalyzer annotationAnalyzer = new AnnotationAnalyzer();
            
            // Étape 1: Scanner TOUTES les classes dans WEB-INF/classes
            allClasses = classScanner.getAllClassesFromWebInfClasses();
                        
            // Étape 2: Analyser les annotations @Controller
            analysisResult = annotationAnalyzer.analyzeClasses(allClasses, Controller.class);
            
            // Étape 3: Construire la map des contrôleurs
            buildControllerMap();
                      
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Erreur lors de l'initialisation du scan des contrôleurs", e);
        }
    }
    
    /**
     * Construit la map des contrôleurs avec leur nom d'annotation comme clé
     */
    private void buildControllerMap() {
        for (Class<?> clazz : analysisResult.getAnnotatedClasses()) {
            Controller annotation = clazz.getAnnotation(Controller.class);
            String controllerName = annotation.name();
            if (!controllerName.isEmpty()) {
                controllerMap.put(controllerName, clazz);
            }
        }
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

    private void doService(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain;charset=UTF-8");
        String path = request.getRequestURI().substring(request.getContextPath().length());
        
        StringBuilder output = new StringBuilder();
        
        // Afficher l'URL demandée
        output.append("URL demandée: ").append(path).append("\n\n");
        
        // Cas de la racine - Afficher toutes les informations
        if ("/".equals(path)) {
            output.append("=== PAGE D'ACCUEIL ===\n\n");
            
            // Afficher les statistiques
            output.append("=== STATISTIQUES ===\n");
            output.append("Total classes scannées: ").append(analysisResult.getTotalClasses()).append("\n");
            output.append("Classes annotées @Controller: ").append(analysisResult.getAnnotatedCount()).append("\n");
            output.append("Classes non annotées: ").append(analysisResult.getNonAnnotatedCount()).append("\n");
            output.append("Taux d'annotation: ").append(String.format("%.1f", analysisResult.getAnnotationRatio() * 100)).append("%\n\n");
            
            // Afficher toutes les classes avec leurs annotations
            output.append("=== TOUTES LES CLASSES SCANNÉES ===\n");
            for (Class<?> clazz : allClasses) {
                output.append("Classe: ").append(clazz.getSimpleName()).append("\n");
                output.append("Nom complet: ").append(clazz.getName()).append("\n");
                
                if (clazz.isAnnotationPresent(Controller.class)) {
                    Controller annotation = clazz.getAnnotation(Controller.class);
                    output.append("Annotation @Controller: OUI\n");
                    output.append("  - Nom: ").append(annotation.name()).append("\n");
                    output.append("  - Description: ").append(annotation.description()).append("\n");
                } else {
                    output.append("Annotation @Controller: NON\n");
                }
                output.append("-------------------------\n");
            }
            
            // Afficher les contrôleurs mappés
            output.append("\n=== CONTRÔLEURS MAPPÉS ===\n");
            if (controllerMap.isEmpty()) {
                output.append("Aucun contrôleur trouvé avec l'annotation @Controller\n");
            } else {
                for (String controllerName : controllerMap.keySet()) {
                    Class<?> controllerClass = controllerMap.get(controllerName);
                    output.append("Nom: ").append(controllerName).append(" -> Classe: ").append(controllerClass.getSimpleName()).append("\n");
                }
            }
            
        } else if (isUrlMappedToController(path)) {
            // Afficher les informations du contrôleur spécifique
            Class<?> controllerClass = getControllerForUrl(path);
            Controller annotation = controllerClass.getAnnotation(Controller.class);
            
            output.append("✅ URL MAPPÉE AVEC SUCCÈS\n\n");
            output.append("URL: ").append(path).append("\n");
            output.append("Nom du contrôleur: ").append(annotation.name()).append("\n");
            output.append("Classe: ").append(controllerClass.getSimpleName()).append("\n");
            output.append("Nom complet: ").append(controllerClass.getName()).append("\n");
            output.append("Description: ").append(annotation.description()).append("\n");
            
        } else {
            // Vérifier si c'est une ressource statique
            boolean resourceExists = getServletContext().getResource(path) != null;

            if (resourceExists) {
                RequestDispatcher defaultDispatcher = getServletContext().getNamedDispatcher("default");
                defaultDispatcher.forward(request, response);
                return;
            } else {
                output.append("❌ CHEMIN INCONNU\n\n");
                output.append("URL: ").append(path).append("\n");
                output.append("Statut: Aucun contrôleur trouvé pour cette URL\n");
            }
        }
        
        response.getWriter().println(output.toString());
    }
    /**
     * Vérifie si l'URL correspond à un contrôleur
     */
    private boolean isUrlMappedToController(String urlPath) {
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
}