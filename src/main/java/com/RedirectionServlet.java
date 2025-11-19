package com;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
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
    private List<Class<?>> allClasses;
    private AnnotationAnalysisResult analysisResult;
    
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
     * Détermine le type de ressource demandée et affiche les informations
     */
    private void doService(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // response.setContentType("text/plain;charset=UTF-8");
        String path = request.getRequestURI().substring(request.getContextPath().length());
        StringBuilder output = new StringBuilder();
        
        output.append("=== FRAMEWORK MVC - INFORMATIONS DE DEBUG ===\n\n");
        output.append("URL demandée: ").append(path).append("\n");
        output.append("Méthode HTTP: ").append(request.getMethod()).append("\n\n");
        
        // Cas de la racine - affiche toutes les informations
        if ("/".equals(path)) {
            displayHomePage(output);
            response.getWriter().println(output.toString());
            return;
        }
        
        // Vérifie si l'URL correspond à une méthode mappée
        if (isUrlMappedToMethod(path)) {
            // Exécute la méthode mappée et affiche les informations
            executeMappedMethod(request, response, path, output);
            return;
        }
        
        // Vérifie si c'est une ressource statique (CSS, JS, images, etc.)
        boolean resourceExists = getServletContext().getResource(path) != null;
        if (resourceExists) {
            output.append("✅ RESSOURCE STATIQUE TROUVÉE\n\n");
            output.append("Type: Ressource statique (CSS, JS, image, etc.)\n");
            output.append("Chemin: ").append(path).append("\n");
            output.append("Statut: La ressource existe et sera servie par le conteneur par défaut\n");
        } else {
            output.append("❌ AUCUNE CORRESPONDANCE TROUVÉE\n\n");
            output.append("URL: ").append(path).append("\n");
            output.append("Statut: Aucun mapping trouvé pour cette URL\n\n");
            
            output.append("=== MAPPINGS DISPONIBLES ===\n");
            if (urlMethodMap.isEmpty()) {
                output.append("Aucune méthode mappée trouvée\n");
            } else {
                for (String url : urlMethodMap.keySet()) {
                    MappedMethod mappedMethod = urlMethodMap.get(url);
                    output.append("- /").append(url).append(" -> ")
                          .append(mappedMethod.getMethod().getDeclaringClass().getSimpleName())
                          .append(".").append(mappedMethod.getMethod().getName())
                          .append(" (").append(mappedMethod.getHttpMethod()).append(")\n");
                }
            }
        }
        
        response.getWriter().println(output.toString());
    }
    
    /**
     * Affiche la page d'accueil avec toutes les informations
     */
    private void displayHomePage(StringBuilder output) {
        output.append("=== PAGE D'ACCUEIL - INFORMATIONS DU FRAMEWORK ===\n\n");
        
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
                output.append("URL: /").append(url).append("\n");
                output.append("  - Méthode HTTP: ").append(mappedMethod.getHttpMethod()).append("\n");
                output.append("  - Classe: ").append(mappedMethod.getMethod().getDeclaringClass().getSimpleName()).append("\n");
                output.append("  - Méthode Java: ").append(mappedMethod.getMethod().getName()).append("\n");
                output.append("  - Type de retour: ").append(mappedMethod.getMethod().getReturnType().getSimpleName()).append("\n");
                output.append("  - Auteur: ").append(mappedMethod.getAuteur()).append("\n");
                output.append("  - Version: ").append(mappedMethod.getVersion()).append("\n");
                output.append("-------------------------\n");
            }
        }
    }
    
    /**
     * Exécute la méthode mappée et affiche les informations détaillées
     */
    private void executeMappedMethod(HttpServletRequest request, HttpServletResponse response, 
                                String path, StringBuilder output) {
        boolean forwarded = false; // indique si on a forwardé vers une JSP (dans ce cas on ne doit pas écrire la réponse)
        try {
            MappedMethod mappedMethod = getMappedMethodForUrl(path);
            String httpMethod = request.getMethod();
            
            output.append("✅ MÉTHODE MAPPÉE TROUVÉE\n\n");
            
            // Informations sur la méthode mappée
            output.append("=== INFORMATIONS DU MAPPING ===\n");
            output.append("URL: ").append(path).append("\n");
            output.append("Méthode HTTP demandée: ").append(httpMethod).append("\n");
            output.append("Méthode HTTP configurée: ").append(mappedMethod.getHttpMethod()).append("\n");
            output.append("Classe: ").append(mappedMethod.getMethod().getDeclaringClass().getSimpleName()).append("\n");
            output.append("Méthode Java: ").append(mappedMethod.getMethod().getName()).append("\n");
            output.append("Type de retour: ").append(mappedMethod.getMethod().getReturnType().getSimpleName()).append("\n");
            output.append("Auteur: ").append(mappedMethod.getAuteur()).append("\n");
            output.append("Version: ").append(mappedMethod.getVersion()).append("\n\n");
            
            // Vérifie la correspondance de la méthode HTTP
            if (!mappedMethod.getHttpMethod().equalsIgnoreCase(httpMethod)) {
                output.append("⚠️ ATTENTION: Méthode HTTP non correspondante!\n");
                output.append("La méthode est configurée pour: ").append(mappedMethod.getHttpMethod()).append("\n\n");
                response.getWriter().println(output.toString());
                return;
            }
            
            // Récupère la méthode et sa classe
            Method method = mappedMethod.getMethod();
            Class<?> controllerClass = method.getDeclaringClass();
            
            // Crée une instance du contrôleur
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
            
            output.append("=== EXÉCUTION DE LA MÉTHODE ===\n");
            output.append("Contrôleur instancié: ").append(controllerClass.getSimpleName()).append("\n");
            
            // Exécute la méthode annotée
            Object result = method.invoke(controllerInstance);
            
            output.append("Méthode exécutée avec succès!\n\n");
            
            // Traitement du résultat selon son type
            output.append("=== RÉSULTAT DE LA MÉTHODE ===\n");
            if (result == null) {
                output.append("Valeur: null\n");
                output.append("Interprétation: Aucune donnée retournée\n");
            } else {
                output.append("Type de retour: ").append(result.getClass().getSimpleName()).append("\n");
            }
            
            if (result instanceof String) {
                // Cas d'un retour String (nom de vue)
                String viewName = (String) result;
                output.append("Valeur: ").append(viewName).append("\n");
                output.append("Interprétation: Nom de vue JSP\n");
                
                String viewPath = buildViewPath(viewName);
                boolean viewExists = getServletContext().getResource(viewPath) != null;
                
                output.append("Chemin de vue construit: ").append(viewPath).append("\n");
                output.append("Vue existe: ").append(viewExists ? "OUI" : "NON").append("\n");
                
                if (viewExists) {
                    // Forward vers la JSP (pas d'attribut data ici)
                    output.append("Action: Forward vers ").append(viewPath).append("\n");
                    try {
                        request.getRequestDispatcher(viewPath).forward(request, response);
                        forwarded = true;
                        return; // on a forwardé: on ne doit pas écrire le debug après
                    } catch (Exception e) {
                        output.append("Erreur lors du forward: ").append(e.getMessage()).append("\n");
                    }
                } else {
                    output.append("Action: Erreur - Vue non trouvée\n");
                }
                
            } else if (result instanceof View) {
                // Cas d'un retour View
                View view = (View) result;
                output.append("=== DÉTAILS DE L'OBJET VIEW ===\n");
                
                // Utilise la réflexion pour afficher tous les champs (debug)
                Field[] fields = View.class.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    Object value = field.get(view);
                    output.append("  - ").append(field.getName()).append(": ").append(value).append("\n");
                }
                
                output.append("\nInterprétation: Objet View avec données\n");
                
                // Envoi de la view vers la JSP en plaçant les données en attributs et en forwardant
                try {
                    sendViewResponse(request, response, view);
                    forwarded = true;
                    return; // forward effectué
                } catch (Exception e) {
                    output.append("Erreur lors du dispatch de la View: ").append(e.getMessage()).append("\n");
                }
                
            } else if (result != null) {
                // Autres types d'objets
                output.append("Valeur: ").append(result.toString()).append("\n");
                output.append("Interprétation: Objet de type ").append(result.getClass().getSimpleName()).append("\n");
                output.append("Action: Affichage du résultat (pas de redirection)\n");
            }
            
        } catch (Exception e) {
            output.append("❌ ERREUR LORS DE L'EXÉCUTION\n\n");
            output.append("Message d'erreur: ").append(e.getMessage()).append("\n");
            output.append("Type d'erreur: ").append(e.getClass().getSimpleName()).append("\n");
            
            // Affiche la stack trace pour le debug
            output.append("\n=== STACK TRACE ===\n");
            for (StackTraceElement element : e.getStackTrace()) {
                if (element.getClassName().contains("test.") || element.getClassName().contains("com.")) {
                    output.append("  at ").append(element.getClassName())
                        .append(".").append(element.getMethodName())
                        .append("(").append(element.getFileName())
                        .append(":").append(element.getLineNumber()).append(")\n");
                }
            }
        }
                // Si on n'a pas forwardé, écrire le debug dans la réponse
        if (!forwarded) {
            try {
                response.getWriter().println(output.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
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