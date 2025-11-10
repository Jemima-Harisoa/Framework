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

/**
 * Servlet principale qui gère le routage des requêtes HTTP
 * Scanne automatiquement les classes et méthodes annotées pour le mapping des URLs
 */
@WebServlet(name = "RedirectionServlet", urlPatterns = { "/" })
public class RedirectionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    // Map qui associe les URLs aux méthodes annotées avec @Mapping
    private Map<String, MappedMethod> urlMethodMap = new HashMap<>();
    
    /**
     * Initialisation du servlet - scanne toutes les classes et construit les mappings
     */
    @Override
    public void init() throws ServletException {
        try {
            // Initialisation des services de scan
            ClassScanner classScanner = new ClassScanner();
            AnnotationAnalyzer annotationAnalyzer = new AnnotationAnalyzer();
            MappingAnalyzer mappingAnalyzer = new MappingAnalyzer();
            
            // Étape 1: Scanner toutes les classes dans WEB-INF/classes
            List<Class<?>> allClasses = classScanner.getAllClassesFromWebInfClasses();
            
            // Étape 2: Analyser les annotations @Controller (pour information)
            AnnotationAnalysisResult analysisResult = annotationAnalyzer.analyzeClasses(allClasses, Controller.class);
            
            // Étape 3: Construire la map des méthodes mappées
            buildMethodMappings(mappingAnalyzer, allClasses);
                      
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation du scan des contrôleurs", e);
        }
    }
    
    /**
     * Construit la map des URLs vers les méthodes annotées avec @Mapping
     * @param mappingAnalyzer l'analyseur de méthodes
     * @param allClasses la liste de toutes les classes scannées
     */
    private void buildMethodMappings(MappingAnalyzer mappingAnalyzer, List<Class<?>> allClasses) {
        // Analyse toutes les méthodes annotées avec @Mapping dans toutes les classes
        Map<Class<?>, List<MappedMethod>> allMethodMappings = mappingAnalyzer.analyzeMethodMappings(allClasses);
        
        // Parcourt toutes les classes et leurs méthodes mappées
        for (Map.Entry<Class<?>, List<MappedMethod>> entry : allMethodMappings.entrySet()) {
            Class<?> clazz = entry.getKey();
            List<MappedMethod> methods = entry.getValue();
            
            for (MappedMethod mappedMethod : methods) {
                String url = mappedMethod.getUrl();
                
                // Normalise l'URL en supprimant le slash initial
                if (url.startsWith("/")) {
                    url = url.substring(1);
                }
                
                // Vérifie les conflits de mapping
                if (urlMethodMap.containsKey(url)) {
                    System.err.println("Conflit de mapping pour l'URL: " + url);
                }
                
                // Ajoute le mapping à la map
                urlMethodMap.put(url, mappedMethod);
            }
        }
    }
    
    /**
     * Gestion des requêtes GET
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doService(request, response);
    }

    /**
     * Gestion des requêtes POST
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doService(request, response);
    }

    /**
     * Méthode principale de traitement des requêtes
     * Détermine le type de ressource demandée et redirige vers le traitement approprié
     */
    private void doService(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Extrait le chemin de l'URL demandée
        String path = request.getRequestURI().substring(request.getContextPath().length());
        
        // Cas de la racine - redirige vers la page d'accueil
        if ("/".equals(path)) {
            RequestDispatcher dispatcher = request.getRequestDispatcher("/index.jsp");
            dispatcher.forward(request, response);
            return;
        }
        
        // Vérifie si l'URL correspond à une méthode mappée
        if (isUrlMappedToMethod(path)) {
            // Exécute la méthode mappée et gère la redirection
            boolean handled = executeMappedMethod(request, response, path);
            if (handled) {
                return; // La réponse a été gérée
            }
        }
        
        // Vérifie si c'est une ressource statique (CSS, JS, images, etc.)
        boolean resourceExists = getServletContext().getResource(path) != null;
        if (resourceExists) {
            RequestDispatcher defaultDispatcher = getServletContext().getNamedDispatcher("default");
            defaultDispatcher.forward(request, response);
            return;
        }
        
        // Si aucune correspondance n'est trouvée, retourne une erreur 404
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Ressource non trouvée: " + path);
    }
    
    /**
     * Exécute la méthode mappée correspondant à l'URL et redirige vers la vue JSP
     * @param request la requête HTTP
     * @param response la réponse HTTP
     * @param path le chemin de l'URL demandée
     * @return true si la méthode a été exécutée avec succès, false sinon
     */
    private boolean executeMappedMethod(HttpServletRequest request, HttpServletResponse response, String path) {
        try {
            // Récupère la méthode mappée pour l'URL
            MappedMethod mappedMethod = getMappedMethodForUrl(path);
            String httpMethod = request.getMethod();
            
            // Vérifie la correspondance de la méthode HTTP (GET, POST, etc.)
            if (!mappedMethod.getHttpMethod().equalsIgnoreCase(httpMethod)) {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, 
                    "Méthode " + httpMethod + " non autorisée. Méthode attendue: " + mappedMethod.getHttpMethod());
                return true;
            }
            
            // Récupère la méthode et sa classe
            Method method = mappedMethod.getMethod();
            Class<?> controllerClass = method.getDeclaringClass();
            
            // Crée une instance du contrôleur
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
            
            // Exécute la méthode annotée
            Object result = method.invoke(controllerInstance);
            
            // Vérifie que le résultat est une String (nom de la vue)
            if (result instanceof String) {
                String viewName = (String) result;
                String viewPath = buildViewPath(viewName);
                
                // Vérifie si la vue JSP existe
                if (getServletContext().getResource(viewPath) != null) {
                    // Redirige vers la vue JSP
                    RequestDispatcher dispatcher = request.getRequestDispatcher(viewPath);
                    dispatcher.forward(request, response);
                    return true;
                } else {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Vue non trouvée: " + viewPath);
                    return true;
                }
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "La méthode doit retourner un String");
                return true;
            }
            
        } catch (Exception e) {
            // Gestion des erreurs lors de l'exécution
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
     * @param viewName le nom de la vue retourné par la méthode
     * @return le chemin complet vers la JSP
     */
    private String buildViewPath(String viewName) {
        // Si la vue contient déjà l'extension .jsp, on l'utilise directement
        if (viewName.endsWith(".jsp")) {
            return viewName;
        }
        
        // Sinon, on ajoute l'extension .jsp
        return viewName + ".jsp";
    }
    
    /**
     * Vérifie si l'URL correspond à une méthode mappée
     * @param urlPath le chemin de l'URL à vérifier
     * @return true si l'URL est mappée à une méthode
     */
    private boolean isUrlMappedToMethod(String urlPath) {
        if (urlPath == null || urlPath.equals("/") || urlPath.isEmpty()) {
            return false;
        }
        
        String methodUrl = extractPathWithoutSlash(urlPath);
        return urlMethodMap.containsKey(methodUrl);
    }
    
    /**
     * Récupère la méthode mappée correspondant à l'URL
     * @param urlPath le chemin de l'URL
     * @return la méthode mappée
     */
    private MappedMethod getMappedMethodForUrl(String urlPath) {
        String methodUrl = extractPathWithoutSlash(urlPath);
        return urlMethodMap.get(methodUrl);
    }
    
    /**
     * Extrait le chemin sans le slash initial
     * @param urlPath le chemin complet de l'URL
     * @return le chemin sans le slash initial
     */
    private String extractPathWithoutSlash(String urlPath) {
        return urlPath.startsWith("/") ? urlPath.substring(1) : urlPath;
    }
}