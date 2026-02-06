package model;

import java.util.HashMap;
import java.util.Map;

public class View {
    private String name;
    private String template;
    private Object data;
    private Map<String, Object> additionalData; // Nouvelle map pour les données supplémentaires

    public void setData(Object data) {
        this.data = data;
    }
    
    public Object getData() {
        return data;
    }

    public void setTemplate(String template){
        this.template = template;
    }
    
    public String getTemplate() {
        return template;
    }

    public void setName(String name){
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Ajoute une donnée supplémentaire à la View
     * Ces données seront disponibles dans la JSP sous forme d'attributs de requête
     * 
     * @param key Le nom de l'attribut
     * @param value La valeur de l'attribut
     */
    public void addData(String key, Object value) {
        if (additionalData == null) {
            additionalData = new HashMap<>();
        }
        additionalData.put(key, value);
    }
    
    /**
     * Récupère toutes les données supplémentaires
     * 
     * @return Map contenant toutes les données ajoutées via addData()
     */
    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    public View(String template) {
        setTemplate(template);
    }

    public View(String name, String template) {
        setName(name);
        setTemplate(template);
    }
    
    public View(String name, String template, Object data) {
        setName(name);
        setTemplate(template);
        setData(data);
    }

    public View() {
    }
    
    /**
     * Vérifie si cette vue est une redirection
     * Une redirection est identifiée par un template qui commence par "redirect:"
     * 
     * @return true si c'est une redirection, false sinon
     */
    public boolean isRedirect() {
        return template != null && template.startsWith("redirect:");
    }
    
    /**
     * Extrait l'URL de redirection si c'est une redirection
     * 
     * @return l'URL de redirection sans le préfixe "redirect:", ou null si ce n'est pas une redirection
     */
    public String getRedirectUrl() {
        if (isRedirect()) {
            String url = template.substring("redirect:".length());
            // Assure que l'URL commence par "/" pour être absolue dans le contexte de l'application
            return url.startsWith("/") ? url : "/" + url;
        }
        return null;
    }
    
    // =====================================================
    // MÉTHODES UTILITAIRES STATIQUES
    // =====================================================
    
    /**
     * Méthode utilitaire pour créer une redirection facilement
     * Utilisation : return View.redirect("users");
     * 
     * @param url L'URL vers laquelle rediriger (sans le préfixe "redirect:")
     * @return Une nouvelle instance de View configurée pour la redirection
     */
    public static View redirect(String url) {
        return new View("redirect:" + url);
    }
    
    /**
     * Méthode utilitaire pour créer une vue JSP normale
     * Utilisation : return View.page("users");
     * 
     * @param templateName Le nom du template JSP
     * @return Une nouvelle instance de View pour l'affichage JSP
     */
    public static View page(String templateName) {
        return new View(templateName);
    }
    
    /**
     * Méthode utilitaire pour créer une vue JSP avec des données
     * Utilisation : return View.page("users", userList);
     * 
     * @param templateName Le nom du template JSP
     * @param data Les données à passer à la JSP
     * @return Une nouvelle instance de View avec les données
     */
    public static View page(String templateName, Object data) {
        return new View("model", templateName, data);
    }
}