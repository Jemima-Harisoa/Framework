package com;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import lookup.AnnotationAnalyzer.AnnotationAnalysisResult;
import lookup.MappingAnalyzer.MappedMethod;

/**
 * Classe responsable de l'affichage HTML de la page d'accueil
 * Génère une page HTML avec toutes les informations de débogage du framework :
 * - Statistiques sur les classes scannées
 * - Liste de toutes les méthodes mappées (statiques et dynamiques)
 * - Informations détaillées sur chaque mapping
 */
public class HomePageRenderer {
    
    /**
     * Affiche la page d'accueil complète avec toutes les informations du framework
     * 
     * @param response La réponse HTTP où écrire le HTML
     * @param analysisResult Les résultats de l'analyse des annotations
     * @param urlMethodMap La map des URLs statiques vers les méthodes
     * @param dynamicUrlMethods La liste des méthodes avec URLs dynamiques
     * @throws IOException Si une erreur survient lors de l'écriture
     */
    public void displayHomePage(HttpServletResponse response,
                                AnnotationAnalysisResult analysisResult,
                                Map<String, MappedMethod> urlMethodMap,
                                List<MappedMethod> dynamicUrlMethods) throws IOException {
        
        response.setContentType("text/html;charset=UTF-8");
        StringBuilder html = new StringBuilder();
        
        // Construction de la structure HTML
        buildHtmlHeader(html);
        buildStatisticsSection(html, analysisResult, urlMethodMap, dynamicUrlMethods);
        buildMappedMethodsSection(html, urlMethodMap, dynamicUrlMethods);
        buildHtmlFooter(html);
        
        response.getWriter().println(html.toString());
    }
    
    /**
     * Construit l'en-tête HTML avec le titre et les styles CSS
     * 
     * @param html Le StringBuilder où ajouter le HTML
     */
    private void buildHtmlHeader(StringBuilder html) {
        html.append("<!DOCTYPE html>");
        html.append("<html><head><title>Framework MVC - Informations</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 40px; }");
        html.append("h1 { color: #333; }");
        html.append(".section { margin-bottom: 30px; padding: 20px; border: 1px solid #ddd; border-radius: 5px; }");
        html.append(".mapping { background: #f9f9f9; padding: 10px; margin: 5px 0; }");
        html.append(".get-method { border-left: 5px solid #4CAF50; }");
        html.append(".post-method { border-left: 5px solid #2196F3; }");
        html.append(".any-method { border-left: 5px solid #FF9800; }");
        html.append("</style>");
        html.append("</head><body>");
        
        html.append("<h1>Framework MVC - Informations de Débogage</h1>");
    }
    
    /**
     * Construit la section des statistiques
     * Affiche le nombre de classes scannées, annotées, et le taux d'annotation
     * 
     * @param html Le StringBuilder où ajouter le HTML
     * @param analysisResult Les résultats de l'analyse des annotations
     * @param urlMethodMap La map des URLs statiques
     * @param dynamicUrlMethods La liste des URLs dynamiques
     */
    private void buildStatisticsSection(StringBuilder html,
                                       AnnotationAnalysisResult analysisResult,
                                       Map<String, MappedMethod> urlMethodMap,
                                       List<MappedMethod> dynamicUrlMethods) {
        html.append("<div class='section'>");
        html.append("<h2>Statistiques</h2>");
        html.append("<p><strong>Total classes scannées:</strong> ")
            .append(analysisResult.getTotalClasses()).append("</p>");
        html.append("<p><strong>Classes annotées @Controller:</strong> ")
            .append(analysisResult.getAnnotatedCount()).append("</p>");
        html.append("<p><strong>Classes non annotées:</strong> ")
            .append(analysisResult.getNonAnnotatedCount()).append("</p>");
        html.append("<p><strong>Méthodes @Mapping trouvées:</strong> ")
            .append(urlMethodMap.size() + dynamicUrlMethods.size()).append("</p>");
        html.append("<p><strong>Taux d'annotation:</strong> ")
            .append(String.format("%.1f", analysisResult.getAnnotationRatio() * 100))
            .append("%</p>");
        html.append("</div>");
    }
    
    /**
     * Construit la section des méthodes mappées
     * Affiche toutes les URLs mappées avec leurs détails (méthode HTTP, classe, etc.)
     * 
     * @param html Le StringBuilder où ajouter le HTML
     * @param urlMethodMap La map des URLs statiques
     * @param dynamicUrlMethods La liste des URLs dynamiques
     */
    private void buildMappedMethodsSection(StringBuilder html,
                                          Map<String, MappedMethod> urlMethodMap,
                                          List<MappedMethod> dynamicUrlMethods) {
        html.append("<div class='section'>");
        html.append("<h2>Méthodes Mappées</h2>");
        
        if (urlMethodMap.isEmpty() && dynamicUrlMethods.isEmpty()) {
            html.append("<p>Aucune méthode trouvée avec les annotations @Mapping, @GetMapping ou @PostMapping</p>");
        } else {
            // Affiche les mappings statiques
            for (String url : urlMethodMap.keySet()) {
                MappedMethod mappedMethod = urlMethodMap.get(url);
                buildMappingCard(html, "/" + url, mappedMethod, false);
            }
            
            // Affiche les mappings dynamiques
            for (MappedMethod mappedMethod : dynamicUrlMethods) {
                buildMappingCard(html, mappedMethod.getUrl(), mappedMethod, true);
            }
        }
        
        html.append("</div>");
    }
    
    /**
     * Construit une carte (card) HTML pour un mapping spécifique
     * Une carte affiche toutes les informations d'un mapping : URL, méthode HTTP, classe, etc.
     * 
     * @param html Le StringBuilder où ajouter le HTML
     * @param url L'URL du mapping
     * @param mappedMethod Les informations de la méthode mappée
     * @param isDynamic Indique si c'est une URL dynamique (avec variables)
     */
    private void buildMappingCard(StringBuilder html, String url, MappedMethod mappedMethod, boolean isDynamic) {
        String methodClass = getMethodClass(mappedMethod.getHttpMethod());
        
        html.append("<div class='mapping ").append(methodClass).append("'>");
        html.append("<p><strong>URL:</strong> ").append(url);
        if (isDynamic) {
            html.append(" (dynamique)");
        }
        html.append("</p>");
        html.append("<p><strong>Méthode HTTP:</strong> ")
            .append(mappedMethod.getHttpMethod()).append("</p>");
        html.append("<p><strong>Classe:</strong> ")
            .append(mappedMethod.getMethod().getDeclaringClass().getSimpleName()).append("</p>");
        html.append("<p><strong>Méthode Java:</strong> ")
            .append(mappedMethod.getMethod().getName()).append("</p>");
        html.append("<p><strong>Type de retour:</strong> ")
            .append(mappedMethod.getMethod().getReturnType().getSimpleName()).append("</p>");
        html.append("<p><strong>Auteur:</strong> ")
            .append(mappedMethod.getAuteur()).append("</p>");
        html.append("<p><strong>Version:</strong> ")
            .append(mappedMethod.getVersion()).append("</p>");
        html.append("</div>");
    }
    
    /**
     * Ferme les balises HTML
     * 
     * @param html Le StringBuilder où ajouter le HTML
     */
    private void buildHtmlFooter(StringBuilder html) {
        html.append("</body></html>");
    }
    
    /**
     * Retourne la classe CSS appropriée en fonction de la méthode HTTP
     * Cela permet de colorer différemment les cartes selon le type de requête
     * - GET = vert
     * - POST = bleu
     * - Autre = orange
     * 
     * @param httpMethod La méthode HTTP (GET, POST, ANY, etc.)
     * @return Le nom de la classe CSS à appliquer
     */
    private String getMethodClass(String httpMethod) {
        if ("GET".equalsIgnoreCase(httpMethod)) {
            return "get-method";
        } else if ("POST".equalsIgnoreCase(httpMethod)) {
            return "post-method";
        } else {
            return "any-method";
        }
    }
}