// Fichier : RedirectionServlet.java (version modifiée)
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
import lookup.MappingAnalyzer;
import lookup.MappingAnalyzer.MappedMethod;

@WebServlet(name = "RedirectionServlet", urlPatterns = { "/*" })
public class RedirectionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private Map<String, Class<?>> controllerMap = new HashMap<>();
    private Map<String, MappedMethod> urlMethodMap = new HashMap<>();
    private List<Class<?>> allClasses;
    private AnnotationAnalysisResult analysisResult;
    
    @Override
    public void init() throws ServletException {
        try {
            
            // Initialisation des services
            ClassScanner classScanner = new ClassScanner();
            AnnotationAnalyzer annotationAnalyzer = new AnnotationAnalyzer();
            MappingAnalyzer mappingAnalyzer = new MappingAnalyzer();
            
            // Étape 1: Scanner TOUTES les classes dans WEB-INF/classes
            allClasses = classScanner.getAllClassesFromWebInfClasses();
                        
            // Étape 2: Analyser les annotations @Controller
            analysisResult = annotationAnalyzer.analyzeClasses(allClasses, Controller.class);
            
            // Étape 3: Construire la map des contrôleurs
            buildControllerMap();
            
            // Étape 4: Analyser les méthodes avec annotation @Mapping
            buildMethodMappings(mappingAnalyzer);
                      
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
    
    /**
     * Construit la map des méthodes mappées par URL
     */
    private void buildMethodMappings(MappingAnalyzer mappingAnalyzer) {
        // Analyser les méthodes annotées avec @Mapping dans toutes les classes
        Map<Class<?>, List<MappedMethod>> allMethodMappings = mappingAnalyzer.analyzeMethodMappings(allClasses);
        
        // Parcourir toutes les classes et leurs méthodes mappées
        for (Map.Entry<Class<?>, List<MappedMethod>> entry : allMethodMappings.entrySet()) {
            Class<?> clazz = entry.getKey();
            List<MappedMethod> methods = entry.getValue();
            
            for (MappedMethod mappedMethod : methods) {
                String url = mappedMethod.getUrl();
                
                // Normaliser l'URL (supprimer le slash de début s'il existe)
                if (url.startsWith("/")) {
                    url = url.substring(1);
                }
                
                // Vérifier les doublons
                if (urlMethodMap.containsKey(url)) {
                    System.err.println("Conflit de mapping pour l'URL: " + url + 
                                     " - Déjà mappé à: " + urlMethodMap.get(url).getMethod());
                }
                
                urlMethodMap.put(url, mappedMethod);
                System.out.println("Méthode mappée: " + url + " -> " + 
                                 clazz.getSimpleName() + "." + mappedMethod.getMethod().getName());
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
            displayHomePage(output);
            
        } else if (isUrlMappedToMethod(path)) {
            // Afficher les informations de la méthode mappée
            displayMethodMappingInfo(path, output, request.getMethod());
            
        } else if (isUrlMappedToController(path)) {
            // Afficher les informations du contrôleur spécifique
            displayControllerInfo(path, output);
            
        } else {
            // Vérifier si c'est une ressource statique
            handleStaticResource(request, response, path, output);
        }
        
        if (!response.isCommitted()) {
            response.getWriter().println(output.toString());
        }
    }
    
    /**
     * Affiche la page d'accueil avec toutes les informations
     */
    private void displayHomePage(StringBuilder output) {
        output.append("=== PAGE D'ACCUEIL ===\n\n");
        
        // Afficher les statistiques
        output.append("=== STATISTIQUES ===\n");
        output.append("Total classes scannées: ").append(analysisResult.getTotalClasses()).append("\n");
        output.append("Classes annotées @Controller: ").append(analysisResult.getAnnotatedCount()).append("\n");
        output.append("Classes non annotées: ").append(analysisResult.getNonAnnotatedCount()).append("\n");
        output.append("Méthodes @Mapping trouvées: ").append(urlMethodMap.size()).append("\n");
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
        
        // Afficher les méthodes mappées
        output.append("\n=== MÉTHODES MAPPÉES ===\n");
        if (urlMethodMap.isEmpty()) {
            output.append("Aucune méthode trouvée avec l'annotation @Mapping\n");
        } else {
            for (String url : urlMethodMap.keySet()) {
                MappedMethod mappedMethod = urlMethodMap.get(url);
                output.append("URL: ").append(url).append("\n");
                output.append("  - Méthode HTTP: ").append(mappedMethod.getHttpMethod()).append("\n");
                output.append("  - Classe: ").append(mappedMethod.getMethod().getDeclaringClass().getSimpleName()).append("\n");
                output.append("  - Méthode Java: ").append(mappedMethod.getMethod().getName()).append("\n");
                output.append("  - Auteur: ").append(mappedMethod.getAuteur()).append("\n");
                output.append("  - Version: ").append(mappedMethod.getVersion()).append("\n");
                output.append("-------------------------\n");
            }
        }
    }
    
    /**
     * Affiche les informations d'une méthode mappée
     */
    private void displayMethodMappingInfo(String path, StringBuilder output, String httpMethod) {
        MappedMethod mappedMethod = getMappedMethodForUrl(path);
        
        output.append("✅ MÉTHODE MAPPÉE TROUVÉE\n\n");
        output.append("URL: ").append(path).append("\n");
        output.append("Méthode HTTP demandée: ").append(httpMethod).append("\n");
        output.append("Méthode HTTP configurée: ").append(mappedMethod.getHttpMethod()).append("\n");
        output.append("Classe: ").append(mappedMethod.getMethod().getDeclaringClass().getSimpleName()).append("\n");
        output.append("Méthode Java: ").append(mappedMethod.getMethod().getName()).append("\n");
        output.append("Type de retour: ").append(mappedMethod.getMethod().getReturnType().getSimpleName()).append("\n");
        output.append("Auteur: ").append(mappedMethod.getAuteur()).append("\n");
        output.append("Version: ").append(mappedMethod.getVersion()).append("\n");
        
        // Vérifier la correspondance de la méthode HTTP
        if (!mappedMethod.getHttpMethod().equalsIgnoreCase(httpMethod)) {
            output.append("\n⚠️ ATTENTION: Méthode HTTP non correspondante!\n");
            output.append("La méthode est configurée pour: ").append(mappedMethod.getHttpMethod()).append("\n");
        }
    }
    
    /**
     * Affiche les informations d'un contrôleur
     */
    private void displayControllerInfo(String path, StringBuilder output) {
        Class<?> controllerClass = getControllerForUrl(path);
        Controller annotation = controllerClass.getAnnotation(Controller.class);
        
        output.append("✅ CONTRÔLEUR MAPPÉ TROUVÉ\n\n");
        output.append("URL: ").append(path).append("\n");
        output.append("Nom du contrôleur: ").append(annotation.name()).append("\n");
        output.append("Classe: ").append(controllerClass.getSimpleName()).append("\n");
        output.append("Nom complet: ").append(controllerClass.getName()).append("\n");
        output.append("Description: ").append(annotation.description()).append("\n");
    }
    
    /**
     * Gère les ressources statiques
     */
    private void handleStaticResource(HttpServletRequest request, HttpServletResponse response, 
                                    String path, StringBuilder output) throws IOException, ServletException {
        boolean resourceExists = getServletContext().getResource(path) != null;

        if (resourceExists) {
            RequestDispatcher defaultDispatcher = getServletContext().getNamedDispatcher("default");
            defaultDispatcher.forward(request, response);
        } else {
            output.append("❌ CHEMIN INCONNU\n\n");
            output.append("URL: ").append(path).append("\n");
            output.append("Statut: Aucun contrôleur ou méthode trouvé pour cette URL\n");
            output.append("Contrôleurs disponibles: ").append(controllerMap.keySet()).append("\n");
            output.append("URLs de méthodes disponibles: ").append(urlMethodMap.keySet()).append("\n");
        }
    }
    
    /**
     * Vérifie si l'URL correspond à un contrôleur
     */
    private boolean isUrlMappedToController(String urlPath) {
        if (urlPath == null || urlPath.equals("/") || urlPath.isEmpty()) {
            return false;
        }
        
        String controllerName = extractPathWithoutSlash(urlPath);
        return controllerMap.containsKey(controllerName);
    }

    /**
     * Vérifie si l'URL correspond à une méthode mappée
     */
    private boolean isUrlMappedToMethod(String urlPath) {
        if (urlPath == null || urlPath.equals("/") || urlPath.isEmpty()) {
            return false;
        }
        
        String methodUrl = extractPathWithoutSlash(urlPath);
        return urlMethodMap.containsKey(methodUrl);
    }

    /**
     * Récupère la classe contrôleur correspondant à l'URL
     */
    private Class<?> getControllerForUrl(String urlPath) {
        String controllerName = extractPathWithoutSlash(urlPath);
        return controllerMap.get(controllerName);
    }
    
    /**
     * Récupère la méthode mappée correspondant à l'URL
     */
    private MappedMethod getMappedMethodForUrl(String urlPath) {
        String methodUrl = extractPathWithoutSlash(urlPath);
        return urlMethodMap.get(methodUrl);
    }
    
    /**
     * Extrait le chemin sans le slash initial
     */
    private String extractPathWithoutSlash(String urlPath) {
        return urlPath.startsWith("/") ? urlPath.substring(1) : urlPath;
    }
}