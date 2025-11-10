// Fichier : RedirectionServlet.java
package com;

import java.io.IOException;
import java.lang.reflect.Method;
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

        String path = request.getRequestURI().substring(request.getContextPath().length());
        
        // Cas de la racine - Afficher toutes les informations (TEST - COMMENTÉ POUR LE MOMENT)
        if ("/".equals(path)) {
            // displayHomePage(response); // COMMENTÉ - Activation pour tests seulement
            // return;
            
            // Pour l'instant, rediriger vers une page par défaut
            RequestDispatcher dispatcher = request.getRequestDispatcher("/index.jsp");
            dispatcher.forward(request, response);
            return;
        }
        
        // Vérifier si l'URL correspond à une méthode mappée
        if (isUrlMappedToMethod(path)) {
            // Exécuter la méthode mappée et gérer la redirection
            boolean handled = executeMappedMethod(request, response, path);
            if (handled) {
                return; // La réponse a été gérée
            }
        }
        
        // Vérifier si l'URL correspond à un contrôleur (COMMENTÉ POUR LE MOMENT)
        /* if (isUrlMappedToController(path)) {
            // displayControllerInfo(response, path); // COMMENTÉ - Activation pour tests seulement
            // return;
        } */
        
        // Vérifier si c'est une ressource statique
        boolean resourceExists = getServletContext().getResource(path) != null;
        if (resourceExists) {
            RequestDispatcher defaultDispatcher = getServletContext().getNamedDispatcher("default");
            defaultDispatcher.forward(request, response);
            return;
        }
        
        // Si aucune correspondance n'est trouvée, erreur 404
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Ressource non trouvée: " + path);
    }
    
    /**
     * Exécute la méthode mappée et redirige vers la vue JSP
     * 
     * @param request la requête HTTP
     * @param response la réponse HTTP
     * @param path le chemin de l'URL
     * @return true si la méthode a été exécutée avec succès, false sinon
     */
    private boolean executeMappedMethod(HttpServletRequest request, HttpServletResponse response, String path) {
        try {
            MappedMethod mappedMethod = getMappedMethodForUrl(path);
            String httpMethod = request.getMethod();
            
            // Vérifier la correspondance de la méthode HTTP
            if (!mappedMethod.getHttpMethod().equalsIgnoreCase(httpMethod)) {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, 
                    "Méthode " + httpMethod + " non autorisée. Méthode attendue: " + mappedMethod.getHttpMethod());
                return true;
            }
            
            // Récupérer la méthode et sa classe
            Method method = mappedMethod.getMethod();
            Class<?> controllerClass = method.getDeclaringClass();
            
            // Créer une instance du contrôleur
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
            
            // Exécuter la méthode
            Object result = method.invoke(controllerInstance);
            
            // Vérifier que le résultat est une String (nom de la vue)
            if (result instanceof String) {
                String viewName = (String) result;
                String viewPath = buildViewPath(viewName);
                
                // Vérifier si la vue existe
                if (getServletContext().getResource(viewPath) != null) {
                    // Rediriger vers la vue JSP
                    RequestDispatcher dispatcher = request.getRequestDispatcher(viewPath);
                    dispatcher.forward(request, response);
                    return true;
                } else {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                        "Vue non trouvée: " + viewPath);
                    return true;
                }
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "La méthode doit retourner un String (nom de la vue)");
                return true;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Erreur lors de l'exécution de la méthode: " + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return true;
        }
    }
    
    /**
     * Construit le chemin complet vers la vue JSP
     * 
     * @param viewName le nom de la vue retourné par la méthode
     * @return le chemin complet vers la JSP
     */
    private String buildViewPath(String viewName) {
        // Si la vue contient déjà l'extension .jsp, on l'utilise directement
        if (viewName.endsWith(".jsp")) {
            return viewName;
        }
        
        // Sinon, on ajoute le chemin par défaut et l'extension
        // Vous pouvez modifier ce chemin selon votre structure de projet
        return  viewName + ".jsp";
    }
    
    /**
     * Affiche la page d'accueil avec toutes les informations (TEST - COMMENTÉ)
     */
    /*
    private void displayHomePage(HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        StringBuilder output = new StringBuilder();
        
        output.append("=== PAGE D'ACCUEIL ===\n\n");
        
        // Afficher les statistiques
        output.append("=== STATISTIQUES ===\n");
        output.append("Total classes scannées: ").append(analysisResult.getTotalClasses()).append("\n");
        output.append("Classes annotées @Controller: ").append(analysisResult.getAnnotatedCount()).append("\n");
        output.append("Classes non annotées: ").append(analysisResult.getNonAnnotatedCount()).append("\n");
        output.append("Méthodes @Mapping trouvées: ").append(urlMethodMap.size()).append("\n");
        output.append("Taux d'annotation: ").append(String.format("%.1f", analysisResult.getAnnotationRatio() * 100)).append("%\n\n");
        
        // Afficher les méthodes mappées
        output.append("=== MÉTHODES MAPPÉES ===\n");
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
        
        response.getWriter().println(output.toString());
    }
    */
    
    /**
     * Affiche les informations d'un contrôleur (TEST - COMMENTÉ)
     */
    /*
    private void displayControllerInfo(HttpServletResponse response, String path) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        StringBuilder output = new StringBuilder();
        
        Class<?> controllerClass = getControllerForUrl(path);
        Controller annotation = controllerClass.getAnnotation(Controller.class);
        
        output.append("CONTRÔLEUR MAPPÉ TROUVÉ\n\n");
        output.append("URL: ").append(path).append("\n");
        output.append("Nom du contrôleur: ").append(annotation.name()).append("\n");
        output.append("Classe: ").append(controllerClass.getSimpleName()).append("\n");
        output.append("Nom complet: ").append(controllerClass.getName()).append("\n");
        output.append("Description: ").append(annotation.description()).append("\n");
        
        response.getWriter().println(output.toString());
    }
    */
    
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