package com;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 */
@WebServlet(name = "RedirectionServlet", urlPatterns = { "/" })
public class RedirectionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    // Map qui associe les URLs aux méthodes annotées avec @Mapping
    private Map<String, MappedMethod> urlMethodMap = new HashMap<>();
    private List<MappedMethod> dynamicUrlMethods = new ArrayList<>(); // pour les méthodes avec URL dynamique
    private List<Class<?>> allClasses;
    private AnnotationAnalysisResult analysisResult;
    
    /**
     * Classe interne pour stocker le résultat de la correspondance d'une méthode dynamique
     */
    private static class MethodMatchResult {
        private MappedMethod mappedMethod;
        private Map<String, String> pathVariables;
        
        public MethodMatchResult(MappedMethod mappedMethod, Map<String, String> pathVariables) {
            this.mappedMethod = mappedMethod;
            this.pathVariables = pathVariables;
        }
        
        public MappedMethod getMappedMethod() {
            return mappedMethod;
        }
        
        public Map<String, String> getPathVariables() {
            return pathVariables;
        }
    }
    
    /**
     * Envoie la View vers la JSP : place les données en attribut(s) et forward vers le template.
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param view objet View contenant template, name et data
     * @throws ServletException
     * @throws IOException
     */
    private void sendViewResponse(HttpServletRequest request, HttpServletResponse response, View view)
            throws ServletException, IOException {
        if (view == null) {
            throw new IllegalArgumentException("View ne peut pas être null");
        }
        
        String template = view.getTemplate();
        String name = view.getName();
        Object data = view.getData();
        
        // Nom par défaut si absent
        if (name == null || name.isBlank()) {
            name = "model";
        }
        
        // Si getTemplate fournit déjà un chemin (commence par '/'), on l'utilise,
        // sinon on construit un chemin dans /WEB-INF/views/... et ajoute .jsp si nécessaire.
        String viewPath = template;
        if (viewPath == null || viewPath.isBlank()) {
            // si template absent, utiliser name comme template
            viewPath = "/" + name;
        }
        if (!viewPath.startsWith("/")) {
            viewPath = "/" + viewPath;
        }
        if (!viewPath.endsWith(".jsp")) {
            viewPath = viewPath + ".jsp";
        }
        
        // Vérifie l'existence de la ressource
        if (getServletContext().getResource(viewPath) == null) {
            throw new ServletException("Template JSP introuvable: " + viewPath);
        }
        
        // Place la data dans la requête sous le nom donné
        request.setAttribute(name, data);
        
        // Forward vers la JSP
        request.getRequestDispatcher(viewPath).forward(request, response);
    }
    
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
            allClasses = classScanner.getAllClassesFromWebInfClasses();
            
            // Étape 2: Analyser les annotations @Controller (pour information)
            analysisResult = annotationAnalyzer.analyzeClasses(allClasses, Controller.class);
            
            // Étape 3: Construire la map des méthodes mappées
            buildMethodMappings(mappingAnalyzer, allClasses);
                      
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation du scan des contrôleurs", e);
        }
    }
    
    /**
     * Construit la map des URLs vers les méthodes annotées avec @Mapping
     * et prépare la liste des méthodes dynamiques
     */
    private void buildMethodMappings(MappingAnalyzer mappingAnalyzer, List<Class<?>> allClasses) {
        Map<Class<?>, List<MappedMethod>> allMethodMappings = mappingAnalyzer.analyzeMethodMappings(allClasses);

        for (Map.Entry<Class<?>, List<MappedMethod>> entry : allMethodMappings.entrySet()) {
            List<MappedMethod> methods = entry.getValue();

            for (MappedMethod mappedMethod : methods) {
                String url = mappedMethod.getUrl();

                if (url == null) continue;

                // Normalisation : retire le slash initial si présent pour les clés statiques
                String normalizedUrl = url.startsWith("/") ? url.substring(1) : url;

                // Si pattern contient accolade -> dynamique
                if (url.contains("{")) {
                    dynamicUrlMethods.add(mappedMethod);
                } else {
                    // mapping statique (stocké sans slash initial)
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
     * Détermine le type de ressource demandée et affiche les informations
     */
    private void doService(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getRequestURI().substring(request.getContextPath().length());
        
        // Cas de la racine - affiche toutes les informations
        if ("/".equals(path)) {
            displayHomePage(request, response);
            return;
        }
        
        // Vérifie si l'URL correspond à une méthode mappée
        if (isUrlMappedToMethod(path)) {
            // Exécute la méthode mappée
            executeMappedMethod(request, response, path);
            return;
        }
        
        // Vérifie si c'est une ressource statique (CSS, JS, images, etc.)
        boolean resourceExists = getServletContext().getResource(path) != null;
        if (resourceExists) {
            // La ressource statique sera servie par le conteneur par défaut
            return;
        } else {
            // Aucune correspondance trouvée
            response.setStatus(404);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().println("<h1>404 - Page non trouvée</h1>");
            response.getWriter().println("<p>Aucun mapping trouvé pour l'URL: " + path + "</p>");
        }
    }
    
    /**
     * Affiche la page d'accueil avec toutes les informations
     */
    private void displayHomePage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("text/html;charset=UTF-8");
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html><head><title>Framework MVC - Informations</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 40px; }");
        html.append("h1 { color: #333; }");
        html.append(".section { margin-bottom: 30px; padding: 20px; border: 1px solid #ddd; border-radius: 5px; }");
        html.append(".mapping { background: #f9f9f9; padding: 10px; margin: 5px 0; }");
        html.append("</style>");
        html.append("</head><body>");
        
        html.append("<h1>Framework MVC - Informations de Débogage</h1>");
        
        // Afficher les statistiques
        html.append("<div class='section'>");
        html.append("<h2>Statistiques</h2>");
        html.append("<p><strong>Total classes scannées:</strong> ").append(analysisResult.getTotalClasses()).append("</p>");
        html.append("<p><strong>Classes annotées @Controller:</strong> ").append(analysisResult.getAnnotatedCount()).append("</p>");
        html.append("<p><strong>Classes non annotées:</strong> ").append(analysisResult.getNonAnnotatedCount()).append("</p>");
        html.append("<p><strong>Méthodes @Mapping trouvées:</strong> ").append(urlMethodMap.size()).append("</p>");
        html.append("<p><strong>Taux d'annotation:</strong> ").append(String.format("%.1f", analysisResult.getAnnotationRatio() * 100)).append("%</p>");
        html.append("</div>");
        
        // Afficher les méthodes mappées
        html.append("<div class='section'>");
        html.append("<h2>Méthodes Mappées</h2>");
        if (urlMethodMap.isEmpty()) {
            html.append("<p>Aucune méthode trouvée avec l'annotation @Mapping</p>");
        } else {
            for (String url : urlMethodMap.keySet()) {
                MappedMethod mappedMethod = urlMethodMap.get(url);
                html.append("<div class='mapping'>");
                html.append("<p><strong>URL:</strong> /").append(url).append("</p>");
                html.append("<p><strong>Méthode HTTP:</strong> ").append(mappedMethod.getHttpMethod()).append("</p>");
                html.append("<p><strong>Classe:</strong> ").append(mappedMethod.getMethod().getDeclaringClass().getSimpleName()).append("</p>");
                html.append("<p><strong>Méthode Java:</strong> ").append(mappedMethod.getMethod().getName()).append("</p>");
                html.append("<p><strong>Type de retour:</strong> ").append(mappedMethod.getMethod().getReturnType().getSimpleName()).append("</p>");
                html.append("<p><strong>Auteur:</strong> ").append(mappedMethod.getAuteur()).append("</p>");
                html.append("<p><strong>Version:</strong> ").append(mappedMethod.getVersion()).append("</p>");
                html.append("</div>");
            }
        }
        html.append("</div>");
        
        html.append("</body></html>");
        
        response.getWriter().println(html.toString());
    }
    
    /**
     * Vérifie si l'URL correspond à une méthode mappée (statique ou dynamique)
     */
    private boolean isUrlMappedToMethod(String urlPath) {
        if (urlPath == null || urlPath.equals("/") || urlPath.isEmpty()) {
            return false;
        }

        String methodUrl = extractPathWithoutSlash(urlPath);

        // URLs statiques
        if (urlMethodMap.containsKey(methodUrl)) {
            return true;
        }

        // URLs dynamiques
        return findMatchingDynamicMethod(urlPath) != null;
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
    
    /**
     * Cherche une méthode dynamique qui correspond à l'URL
     * Retourne MethodMatchResult si trouvé (mappedMethod + map variables ordonnée)
     */
    private MethodMatchResult findMatchingDynamicMethod(String urlPath) {
        // Normalise le path (supprime slash initial)
        String normalized = extractPathWithoutSlash(urlPath);
        String[] actualSegments = normalized.split("/");

        for (MappedMethod mappedMethod : dynamicUrlMethods) {
            String pattern = mappedMethod.getUrl();
            // pattern peut avoir été enregistré sans '/' initial -> normaliser
            String normalizedPattern = pattern.startsWith("/") ? pattern.substring(1) : pattern;
            String[] patternSegments = normalizedPattern.split("/");

            // lengths must match to map one-to-one segments (simpler and predictable)
            if (patternSegments.length != actualSegments.length) {
                continue;
            }

            // try to match and extract variables in pattern order
            Map<String, String> extracted = matchPatternAndExtractVariables(patternSegments, actualSegments);
            if (extracted != null) {
                return new MethodMatchResult(mappedMethod, extracted);
            }
        }
        return null;
    }

    /**
     * Compare deux tableaux de segments (pattern + actual) et retourne map des variables (LinkedHashMap pour l'ordre)
     * patternSegment examples: "products", "{id}", "categorie", "{idcategorie}"
     * actualSegments: "products", "12", "categorie", "34"
     */
    private Map<String, String> matchPatternAndExtractVariables(String[] patternSegments, String[] actualSegments) {
        Map<String, String> vars = new LinkedHashMap<>();
        for (int i = 0; i < patternSegments.length; i++) {
            String p = patternSegments[i];
            String a = actualSegments[i];

            if (p.startsWith("{") && p.endsWith("}")) {
                String varName = p.substring(1, p.length() - 1).trim();
                if (varName.isEmpty()) return null;
                vars.put(varName, a);
            } else {
                // literal must match exactly
                if (!p.equals(a)) {
                    return null;
                }
            }
        }
        return vars;
    }
    
    /**
     * Prépare les paramètres pour l'appel de la méthode en injectant :
     * - HttpServletRequest / HttpServletResponse si présents
     * - Map<String,String> des path variables si param de type Map
     * - pour les types String/int/Integer/long/Long/double/Double/boolean injecte les variables de chemin
     *   en respectant l'ordre des paramètres : d'abord on essaye par nom (si compilé avec -parameters),
     *   sinon on consomme les valeurs dans l'ordre d'apparition dans l'URL pattern.
     */
    private Object[] prepareMethodParameters(Method method, Map<String, String> pathVariables,
                                           HttpServletRequest request, HttpServletResponse response) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        Object[] parametersValues = new Object[parameterTypes.length];

        // ordered list of path variable values (pattern order)
        java.util.List<String> orderedValues = new ArrayList<>();
        if (pathVariables != null) {
            orderedValues.addAll(pathVariables.values());
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> pType = parameterTypes[i];
            String pName = parameters[i].getName(); // peut être "arg0" si pas -parameters
            Object value = null;

            if (pType == HttpServletRequest.class) {
                value = request;
            } else if (pType == HttpServletResponse.class) {
                value = response;
            } else if (Map.class.isAssignableFrom(pType)) {
                value = pathVariables;
            } else {
                // try by name first
                String byName = pathVariables != null ? pathVariables.get(pName) : null;
                if (byName != null) {
                    value = convertParameterValue(byName, pType);
                } else {
                    // fallback by order
                    if (!orderedValues.isEmpty()) {
                        String val = orderedValues.remove(0);
                        value = convertParameterValue(val, pType);
                    } else {
                        // none available -> defaults for primitives / null for objects
                        value = getDefaultForType(pType);
                    }
                }
            }

            parametersValues[i] = value;
        }

        return parametersValues;
    }
    
    private Object getDefaultForType(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        if (type == char.class) return '\0';
        return null;
    }

    private Object convertParameterValue(String value, Class<?> targetType) {
        if (value == null) return getDefaultForType(targetType);
        try {
            if (targetType == String.class) return value;
            if (targetType == Integer.class || targetType == int.class) return Integer.parseInt(value);
            if (targetType == Long.class || targetType == long.class) return Long.parseLong(value);
            if (targetType == Double.class || targetType == double.class) return Double.parseDouble(value);
            if (targetType == Boolean.class || targetType == boolean.class) return Boolean.parseBoolean(value);
        } catch (Exception ex) {
            // conversion failed -> return default/null
            return getDefaultForType(targetType);
        }
        // fallback: if method expects Object or non-primitive unknown type, pass the raw string
        return value;
    }
    
    /**
     * Exécute la méthode mappée correspondant à l'URL
     */
    private void executeMappedMethod(HttpServletRequest request, HttpServletResponse response, 
                                   String path) throws ServletException, IOException {
        try {
            MappedMethod mappedMethod = getMappedMethodForUrl(path);
            Map<String, String> pathVariables = null;
            
            // Si pas de méthode statique, chercher une méthode dynamique
            if (mappedMethod == null) {
                MethodMatchResult matchResult = findMatchingDynamicMethod(path);
                if (matchResult != null) {
                    mappedMethod = matchResult.getMappedMethod();
                    pathVariables = matchResult.getPathVariables();
                }
            }
            
            if (mappedMethod == null) {
                response.setStatus(404);
                response.getWriter().println("Méthode mappée non trouvée pour l'URL: " + path);
                return;
            }
            
            // Vérifier la méthode HTTP
            String requestMethod = request.getMethod();
            String mappingMethod = mappedMethod.getHttpMethod();
            if (!mappingMethod.equalsIgnoreCase(requestMethod) && !mappingMethod.equalsIgnoreCase("ANY")) {
                response.setStatus(405);
                response.getWriter().println("Méthode HTTP non autorisée. Attendu: " + mappingMethod);
                return;
            }
            
            Method method = mappedMethod.getMethod();
            Object controllerInstance = method.getDeclaringClass().getDeclaredConstructor().newInstance();
            
            // Préparer les paramètres
            Object[] parameters = prepareMethodParameters(method, pathVariables, request, response);
            
            // Invoquer la méthode
            Object result = method.invoke(controllerInstance, parameters);
            
            // Traiter le résultat
            if (result instanceof View) {
                sendViewResponse(request, response, (View) result);
            } else if (result instanceof String) {
                // Si c'est une String, on suppose que c'est le nom d'une vue
                View view = new View((String) result);
                sendViewResponse(request, response, view);
            } else {
                // Autres types : afficher dans la réponse
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().println(result != null ? result.toString() : "null");
            }
            
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'exécution de la méthode mappée: " + e.getMessage(), e);
        }
    }
}