package com;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.MappingHelper.MethodMatchResult;

import annotations.Controller;
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
import model.View;

/**
 * Servlet principale qui gère le routage des requêtes HTTP
 * Scanne automatiquement les classes et méthodes annotées pour le mapping des URLs
 * 
 * Cette version refactorisée délègue :
 * - La logique métier à MappingHelper
 * - L'affichage HTML à HomePageRenderer
 */
@WebServlet(name = "RedirectionServlet", urlPatterns = { "/" })
public class RedirectionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    // Maps et listes pour stocker les mappings
    private Map<String, MappedMethod> urlMethodMap = new HashMap<>();
    private List<MappedMethod> dynamicUrlMethods = new ArrayList<>();
    private List<Class<?>> allClasses;
    private AnnotationAnalysisResult analysisResult;
    
    // Classes helper pour déléguer les opérations
    private MappingHelper mappingHelper;
    private HomePageRenderer homePageRenderer;    
    private Map<Class<?>, Object> entityCache = new HashMap<>();
    
    /**
     * Initialisation du servlet - scanne toutes les classes et construit les mappings
     * Cette méthode est appelée une seule fois au démarrage de l'application
     */

    @Override
    public void init() throws ServletException {
        try {
            // Initialisation des helpers
            mappingHelper = new MappingHelper();
            homePageRenderer = new HomePageRenderer();
            
            // Scanner et analyseur pour trouver les classes et annotations
            ClassScanner classScanner = new ClassScanner();
            AnnotationAnalyzer annotationAnalyzer = new AnnotationAnalyzer();
            MappingAnalyzer mappingAnalyzer = new MappingAnalyzer();
            
            // Scan de toutes les classes dans WEB-INF/classes
            allClasses = classScanner.getAllClassesFromWebInfClasses();
            
            // Analyse des classes avec l'annotation @Controller
            analysisResult = annotationAnalyzer.analyzeClasses(allClasses, Controller.class);
            
            // Analyse des classes avec l'annotation @Entity
            AnnotationAnalyzer.AnnotationAnalysisResult entityResult = 
                annotationAnalyzer.analyzeClasses(allClasses, annotations.Entity.class);
            
            // Cache les entités pour référence rapide
            cacheEntities(entityResult.getAnnotatedClasses());
            
            // Construction des mappings URL -> Méthode
            buildMethodMappings(mappingAnalyzer, allClasses);
                        
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation du scan des contrôleurs", e);
        }
    }

    /**
     * Met en cache les classes d'entité pour référence rapide
     */
    private void cacheEntities(List<Class<?>> entityClasses) {
        for (Class<?> entityClass : entityClasses) {
            entityCache.put(entityClass, null); // On peut mettre null ou une instance par défaut
            System.out.println("Entité trouvée: " + entityClass.getName());
        }
    }
    /**
     * Construit la map des URLs vers les méthodes annotées avec @Mapping, @GetMapping et @PostMapping
     * Sépare les URLs statiques (ex: /users) des URLs dynamiques (ex: /users/{id})
     * 
     * @param mappingAnalyzer L'analyseur de mappings
     * @param allClasses La liste de toutes les classes scannées
     */
    private void buildMethodMappings(MappingAnalyzer mappingAnalyzer, List<Class<?>> allClasses) {
        Map<Class<?>, List<MappedMethod>> allMethodMappings = mappingAnalyzer.analyzeMethodMappings(allClasses);

        for (Map.Entry<Class<?>, List<MappedMethod>> entry : allMethodMappings.entrySet()) {
            List<MappedMethod> methods = entry.getValue();

            for (MappedMethod mappedMethod : methods) {
                String url = mappedMethod.getUrl();

                if (url == null) continue;

                String normalizedUrl = url.startsWith("/") ? url.substring(1) : url;

                // Si l'URL contient des accolades {}, c'est une URL dynamique
                if (url.contains("{")) {
                    dynamicUrlMethods.add(mappedMethod);
                } else {
                    // URL statique : on l'ajoute dans la map
                    String key = normalizedUrl;
                    if (urlMethodMap.containsKey(key)) {
                        System.err.println("Conflit de mapping pour l'URL: " + key);
                    }
                    urlMethodMap.put(key, mappedMethod);
                }
            }
        }
    }
    
    /**
     * Gère les requêtes HTTP GET
     * Délègue le traitement à doService()
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doService(request, response);
    }

    /**
     * Gère les requêtes HTTP POST
     * Délègue le traitement à doService()
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doService(request, response);
    }

    /**
     * Méthode principale de traitement des requêtes
     * Détermine quelle action entreprendre en fonction de l'URL demandée :
     * - Affiche la page d'accueil si l'URL est "/"
     * - Exécute la méthode mappée si une correspondance est trouvée
     * - Retourne une erreur 404 si aucune correspondance n'est trouvée
     * 
     * @param request La requête HTTP entrante
     * @param response La réponse HTTP à envoyer
     */
    private void doService(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Extrait le chemin de l'URL (sans le contexte de l'application)
        String path = request.getRequestURI().substring(request.getContextPath().length());
        
        // Si c'est la racine "/", affiche la page d'accueil
        if ("/".equals(path)) {
            displayHomePage(request, response);
            return;
        }
        
        // Vérifie si l'URL correspond à une méthode mappée
        if (mappingHelper.isUrlMappedToMethod(path, request.getMethod(), urlMethodMap, dynamicUrlMethods)) {
            executeMappedMethod(request, response, path);
            return;
        }
        
        // Vérifie si c'est une ressource statique (fichier CSS, JS, image, etc.)
        boolean resourceExists = getServletContext().getResource(path) != null;
        if (resourceExists) {
            // Laisse le conteneur servir la ressource statique
            return;
        } else {
            // Aucune correspondance trouvée : erreur 404
            response.setStatus(404);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().println("<h1>404 - Page non trouvée</h1>");
            response.getWriter().println("<p>Aucun mapping trouvé pour l'URL: " + path + "</p>");
        }
    }
    
    /**
     * Affiche la page d'accueil avec toutes les informations de débogage
     * Délègue l'affichage à HomePageRenderer
     * 
     * @param request La requête HTTP
     * @param response La réponse HTTP
     */
    private void displayHomePage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        homePageRenderer.displayHomePage(response, analysisResult, urlMethodMap, dynamicUrlMethods);
    }
    
    /**
     * Exécute la méthode mappée correspondant à l'URL
     * Gère :
     * - La recherche de la méthode (statique ou dynamique)
     * - La vérification de la méthode HTTP (GET/POST)
     * - L'injection des paramètres
     * - L'invocation de la méthode
     * - Le traitement du résultat (View, String, ou autre)
     * 
     * @param request La requête HTTP
     * @param response La réponse HTTP
     * @param path Le chemin de l'URL
     */
    private void executeMappedMethod(HttpServletRequest request, HttpServletResponse response, 
                                   String path) throws ServletException, IOException {
        try {
            // Recherche la méthode mappée (statique ou dynamique)
            MappedMethod mappedMethod = mappingHelper.getMappedMethodForUrl(path, urlMethodMap);
            Map<String, String> pathVariables = null;
            
            if (mappedMethod == null) {
                // Pas trouvé dans les statiques, cherche dans les dynamiques
                MethodMatchResult matchResult = mappingHelper.findMatchingDynamicMethod(path, dynamicUrlMethods);
                if (matchResult != null) {
                    mappedMethod = matchResult.getMappedMethod();
                    pathVariables = matchResult.getPathVariables();
                }
            } else {
                pathVariables = new HashMap<>();
            }
            
            // Si aucune méthode trouvée, erreur 404
            if (mappedMethod == null) {
                response.setStatus(404);
                response.getWriter().println("Méthode mappée non trouvée pour l'URL: " + path);
                return;
            }
            
            // Vérifie que la méthode HTTP correspond
            String requestMethod = request.getMethod();
            String mappingMethod = mappedMethod.getHttpMethod();
            if (!mappingHelper.isMethodMatching(mappingMethod, requestMethod)) {
                response.setStatus(405);
                response.getWriter().println("Méthode HTTP non autorisée. Attendu: " + mappingMethod);
                return;
            }
            
            // Récupère la méthode Java et crée une instance du contrôleur
            Method method = mappedMethod.getMethod();
            Object controllerInstance = method.getDeclaringClass().getDeclaredConstructor().newInstance();
            
            // Prépare les paramètres (injection automatique)
            Object[] parameters = mappingHelper.prepareMethodParameters(method, pathVariables, request, response);
            
            // Invoque la méthode du contrôleur
            Object result = method.invoke(controllerInstance, parameters);

            // Traite le résultat selon son type
            if (result instanceof View) {
                // Si c'est un objet View, envoie vers la JSP
                sendViewResponse(request, response, (View) result);
            } else if (result instanceof String) {
                // Si c'est une String, crée une View avec ce nom
                View view = new View((String) result);
                sendViewResponse(request, response, view);
            } else {
                // Pour tout autre type, affiche en texte brut
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().println(result != null ? result.toString() : "null");
            }
            
        }
        catch (IllegalArgumentException e) {
            // Gestion spécifique des erreurs de conversion de paramètres
            response.setStatus(400); // Bad Request
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().println("<h1>400 - Requête incorrecte</h1>");
            response.getWriter().println("<p>Erreur de paramètre: " + e.getMessage() + "</p>");
            e.printStackTrace(response.getWriter());
        }  
        catch (Exception e) {
            throw new ServletException("Erreur lors de l'exécution de la méthode mappée: " + e.getMessage(), e);
        }
    }
    
    /**
     * Envoie la View vers la JSP : place les données en attribut(s) et forward vers le template
     * 
     * @param request La requête HTTP
     * @param response La réponse HTTP
     * @param view L'objet View contenant les données et le nom du template
     */
    private void sendViewResponse(HttpServletRequest request, HttpServletResponse response, View view)
            throws ServletException, IOException {
        if (view == null) {
            throw new IllegalArgumentException("View ne peut pas être null");
        }
        
        String template = view.getTemplate();
        String name = view.getName();
        Object data = view.getData();
        
        // Si pas de nom spécifié, utilise "model" par défaut
        if (name == null || name.isBlank()) {
            name = "model";
        }
        
        // Construction du chemin vers la JSP
        String viewPath = template;
        if (viewPath == null || viewPath.isBlank()) {
            viewPath = "/" + name;
        }
        if (!viewPath.startsWith("/")) {
            viewPath = "/" + viewPath;
        }
        if (!viewPath.endsWith(".jsp")) {
            viewPath = viewPath + ".jsp";
        }
        
        // Vérifie que le template existe
        if (getServletContext().getResource(viewPath) == null) {
            throw new ServletException("Template JSP introuvable: " + viewPath);
        }
        
        // Place les données principales en attribut
        if (data != null) {
            request.setAttribute(name, data);
        }
        
        // Place les données supplémentaires en attributs
        Map<String, Object> additionalData = view.getAdditionalData();
        if (additionalData != null) {
            for (Map.Entry<String, Object> entry : additionalData.entrySet()) {
                request.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        
        // Forward vers la JSP
        request.getRequestDispatcher(viewPath).forward(request, response);
    }
}