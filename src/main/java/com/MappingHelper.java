package com;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lookup.MappingAnalyzer.MappedMethod;

/**
 * Classe helper qui contient toute la logique métier pour :
 * - La vérification des URLs et méthodes HTTP
 * - La correspondance avec les méthodes mappées (statiques et dynamiques)
 * - La préparation des paramètres pour l'invocation des méthodes
 * - L'extraction des variables de chemin (path variables)
 */
public class MappingHelper {
    
    /**
     * Extrait le nom du paramètre à partir de l'annotation @RequestParam ou utilise le nom du paramètre Java
     * 
     * @param parameter Le paramètre de la méthode
     * @param annotations Les annotations du paramètre
     * @return Le nom du paramètre à utiliser pour le mapping
     */
    public String getParameterName(java.lang.reflect.Parameter parameter, 
                                annotations.RequestParam requestParamAnnotation) {
        if (requestParamAnnotation != null && 
            requestParamAnnotation.value() != null && 
            !requestParamAnnotation.value().isEmpty()) {
            // Utilise le nom spécifié dans l'annotation
            return requestParamAnnotation.value();
        } else {
            // Utilise le nom du paramètre Java
            return parameter.getName();
        }
    }
    
    /**
     * Classe interne pour stocker le résultat de la correspondance d'une méthode dynamique
     * Contient la méthode mappée trouvée et les variables extraites du chemin
     */
    public static class MethodMatchResult {
        private final MappedMethod mappedMethod;
        private final Map<String, String> pathVariables;
        
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
     * Vérifie si l'URL correspond à une méthode mappée (statique ou dynamique)
     * et si la méthode HTTP correspond
     * 
     * @param urlPath Le chemin de l'URL à vérifier
     * @param requestMethod La méthode HTTP de la requête (GET, POST, etc.)
     * @param urlMethodMap La map des URLs statiques vers les méthodes
     * @param dynamicUrlMethods La liste des méthodes avec URLs dynamiques
     * @return true si une correspondance est trouvée, false sinon
     */
    public boolean isUrlMappedToMethod(String urlPath, String requestMethod,
                                       Map<String, MappedMethod> urlMethodMap,
                                       List<MappedMethod> dynamicUrlMethods) {
        if (urlPath == null || urlPath.equals("/") || urlPath.isEmpty()) {
            return false;
        }

        String methodUrl = extractPathWithoutSlash(urlPath);

        // Vérifie d'abord dans les mappings statiques
        if (urlMethodMap.containsKey(methodUrl)) {
            MappedMethod mappedMethod = urlMethodMap.get(methodUrl);
            return isMethodMatching(mappedMethod.getHttpMethod(), requestMethod);
        }

        // Vérifie ensuite dans les mappings dynamiques
        MethodMatchResult matchResult = findMatchingDynamicMethod(urlPath, dynamicUrlMethods);
        if (matchResult != null) {
            return isMethodMatching(matchResult.getMappedMethod().getHttpMethod(), requestMethod);
        }

        return false;
    }

    /**
     * Vérifie si la méthode HTTP du mapping correspond à celle de la requête
     * 
     * @param mappingMethod La méthode HTTP définie dans le mapping (GET, POST, ANY, etc.)
     * @param requestMethod La méthode HTTP de la requête
     * @return true si elles correspondent ou si le mapping accepte toutes les méthodes (ANY)
     */
    public boolean isMethodMatching(String mappingMethod, String requestMethod) {
        return mappingMethod.equalsIgnoreCase(requestMethod) || 
               mappingMethod.equalsIgnoreCase("ANY");
    }

    /**
     * Récupère la méthode mappée correspondant à l'URL
     * 
     * @param urlPath Le chemin de l'URL
     * @param urlMethodMap La map des URLs statiques
     * @return La méthode mappée ou null si non trouvée
     */
    public MappedMethod getMappedMethodForUrl(String urlPath, Map<String, MappedMethod> urlMethodMap) {
        String methodUrl = extractPathWithoutSlash(urlPath);
        return urlMethodMap.get(methodUrl);
    }
    
    /**
     * Extrait le chemin sans le slash initial
     * Exemple : "/users" devient "users"
     * 
     * @param urlPath Le chemin complet
     * @return Le chemin sans le slash initial
     */
    public String extractPathWithoutSlash(String urlPath) {
        return urlPath.startsWith("/") ? urlPath.substring(1) : urlPath;
    }
    
    /**
     * Cherche une méthode dynamique qui correspond à l'URL
     * Les URLs dynamiques contiennent des variables entre accolades, par exemple : /users/{id}
     * 
     * @param urlPath L'URL à analyser
     * @param dynamicUrlMethods La liste des méthodes avec URLs dynamiques
     * @return Un objet MethodMatchResult contenant la méthode et les variables extraites, ou null
     */
    public MethodMatchResult findMatchingDynamicMethod(String urlPath, List<MappedMethod> dynamicUrlMethods) {
        String normalized = extractPathWithoutSlash(urlPath);
        String[] actualSegments = normalized.split("/");

        for (MappedMethod mappedMethod : dynamicUrlMethods) {
            String pattern = mappedMethod.getUrl();
            String normalizedPattern = pattern.startsWith("/") ? pattern.substring(1) : pattern;
            String[] patternSegments = normalizedPattern.split("/");

            // Les deux URLs doivent avoir le même nombre de segments
            if (patternSegments.length != actualSegments.length) {
                continue;
            }

            Map<String, String> extracted = matchPatternAndExtractVariables(patternSegments, actualSegments);
            if (extracted != null) {
                return new MethodMatchResult(mappedMethod, extracted);
            }
        }
        return null;
    }

    /**
     * Compare deux tableaux de segments d'URL et extrait les variables
     * Exemple : pattern ["users", "{id}"] avec actual ["users", "123"] 
     *           extrait la variable id=123
     * 
     * @param patternSegments Les segments du pattern (avec {variables})
     * @param actualSegments Les segments de l'URL réelle
     * @return Une map des variables extraites, ou null si pas de correspondance
     */
    public Map<String, String> matchPatternAndExtractVariables(String[] patternSegments, String[] actualSegments) {
        Map<String, String> vars = new LinkedHashMap<>();
        for (int i = 0; i < patternSegments.length; i++) {
            String p = patternSegments[i];
            String a = actualSegments[i];

            if (p.startsWith("{") && p.endsWith("}")) {
                // C'est une variable : on extrait son nom et sa valeur
                String varName = p.substring(1, p.length() - 1).trim();
                if (varName.isEmpty()) return null;
                vars.put(varName, a);
            } else {
                // C'est un segment fixe : il doit correspondre exactement
                if (!p.equals(a)) {
                    return null;
                }
            }
        }
        return vars;
    }
    
    /**
     * Prépare les paramètres pour l'appel de la méthode du contrôleur
     * Cette méthode injecte automatiquement :
     * - HttpServletRequest / HttpServletResponse si la méthode les demande
     * - Les variables de chemin (path parameters) extraites de l'URL
     * - Les données du formulaire (request parameters) GET ou POST
     * 
     * @param method La méthode à invoquer
     * @param pathVariables Les variables extraites du chemin
     * @param request La requête HTTP
     * @param response La réponse HTTP
     * @return Un tableau d'objets contenant les valeurs des paramètres dans le bon ordre
     * @throws IllegalArgumentException si un paramètre requis n'est pas trouvé
     */
    public Object[] prepareMethodParameters(Method method, Map<String, String> pathVariables,
                                        HttpServletRequest request, HttpServletResponse response) 
            throws IllegalArgumentException {
        Class<?>[] parameterTypes = method.getParameterTypes();
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        Object[] parametersValues = new Object[parameterTypes.length];

        // Récupère les données du formulaire (GET ou POST)
        Map<String, String> formData = getFormData(request);

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> pType = parameterTypes[i];
            java.lang.reflect.Parameter parameter = parameters[i];
            Object value = null;

            // Injection spéciale pour les types servlets
            if (pType == HttpServletRequest.class) {
                value = request;
            } else if (pType == HttpServletResponse.class) {
                value = response;
            } else if (Map.class.isAssignableFrom(pType)) {
                // Injection de la Map de variables de chemin
                value = pathVariables != null ? pathVariables : new HashMap<>();
            } else {
                // Vérifie si le paramètre a l'annotation @RequestParam
                annotations.RequestParam requestParamAnnotation = 
                    parameter.getAnnotation(annotations.RequestParam.class);
                
                // Détermine le nom du paramètre à utiliser
                String paramName = getParameterName(parameter, requestParamAnnotation);
                
                boolean valueFound = false;
                
                // Recherche dans les path variables
                if (pathVariables != null && pathVariables.containsKey(paramName)) {
                    value = convertParameterValue(pathVariables.get(paramName), pType);
                    valueFound = true;
                } 
                // Recherche dans les données du formulaire
                else if (formData.containsKey(paramName)) {
                    value = convertParameterValue(formData.get(paramName), pType);
                    valueFound = true;
                }
                
                // Si aucune valeur n'a été trouvée
                if (!valueFound) {
                    if (requestParamAnnotation != null) {
                        // Avec @RequestParam mais valeur non trouvée
                        if (pType.isPrimitive()) {
                            throw new IllegalArgumentException(
                                String.format("Paramètre requis non trouvé: '%s'. Le paramètre annoté @RequestParam '%s' (type: %s) est requis mais n'a pas été fourni.",
                                            paramName, parameter.getName(), pType.getSimpleName()));
                        } else {
                            // Pour les objets, on peut utiliser null
                            value = null;
                        }
                    } else {
                        // SANS annotation @RequestParam et valeur non trouvée = ERREUR
                        throw new IllegalArgumentException(
                            String.format("Paramètre non mappé: '%s'. Le paramètre '%s' (type: %s) n'est pas annoté avec @RequestParam et ne correspond à aucun paramètre de la requête.",
                                        paramName, parameter.getName(), pType.getSimpleName()));
                    }
                }
            }

            parametersValues[i] = value;
        }

        return parametersValues;
    }     
    
    /**
     * Récupère les données du formulaire (GET ou POST)
     * 
     * @param request La requête HTTP
     * @return Une map contenant tous les paramètres de la requête
     */
    public Map<String, String> getFormData(HttpServletRequest request) {
        Map<String, String> data = new HashMap<>();
        
        // Récupère tous les paramètres (GET ou POST)
        java.util.Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            data.put(paramName, paramValue);
        }
        
        return data;
    }
    
    /**
     * Retourne la valeur par défaut pour un type donné
     * Pour les types primitifs, retourne leur valeur par défaut (0, false, etc.)
     * Pour les objets, retourne null
     * 
     * @param type Le type de classe
     * @return La valeur par défaut
     */
    public Object getDefaultForType(Class<?> type) {
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

    /**
     * Convertit une valeur String en type cible
     * Supporte les types : String, Integer, Long, Double, Boolean
     * 
     * @param value La valeur à convertir (String)
     * @param targetType Le type cible souhaité
     * @return La valeur convertie
     * @throws IllegalArgumentException si la conversion échoue pour un type non-primitif
     */
    public Object convertParameterValue(String value, Class<?> targetType) {
        if (value == null) {
            return getDefaultForType(targetType);
        }
        
        try {
            if (targetType == String.class) return value;
            if (targetType == Integer.class || targetType == int.class) return Integer.parseInt(value);
            if (targetType == Long.class || targetType == long.class) return Long.parseLong(value);
            if (targetType == Double.class || targetType == double.class) return Double.parseDouble(value);
            if (targetType == Boolean.class || targetType == boolean.class) return Boolean.parseBoolean(value);
            if (targetType == Float.class || targetType == float.class) return Float.parseFloat(value);
            if (targetType == Short.class || targetType == short.class) return Short.parseShort(value);
            if (targetType == Byte.class || targetType == byte.class) return Byte.parseByte(value);
        } catch (NumberFormatException | NullPointerException ex) {
            // Pour les types primitifs, retourne la valeur par défaut
            if (targetType.isPrimitive()) {
                return getDefaultForType(targetType);
            }
            // Pour les types objets, lance une exception
            throw new IllegalArgumentException(
                String.format("Impossible de convertir la valeur '%s' en type %s", 
                            value, targetType.getSimpleName()), ex);
        }
        
        // Si le type n'est pas supporté, retourne la valeur telle quelle
        return value;
    }
}