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
}