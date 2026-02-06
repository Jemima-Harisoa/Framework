package com;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Gestionnaire de sessions pour le framework
 * Gère les sessions utilisateur avec isolation complète entre les utilisateurs
 * 
 * Fonctionnalités :
 * - Création automatique de sessions
 * - Isolation par sessionId (Map<String, Object> par session)
 * - Gestion des cookies de session
 * - Nettoyage automatique des sessions expirées
 * - Thread-safe avec ConcurrentHashMap
 */
public class SessionManager {
    
    // Stockage des sessions : sessionId -> Map<key, value>
    private static final Map<String, Map<String, Object>> sessions = new ConcurrentHashMap<>();
    
    // Stockage des métadonnées de session : sessionId -> SessionMetadata
    private static final Map<String, SessionMetadata> sessionMetadata = new ConcurrentHashMap<>();
    
    // Nom du cookie de session
    private static final String SESSION_COOKIE_NAME = "FRAMEWORK_SESSIONID";
    
    // Durée de vie par défaut d'une session (30 minutes)
    private static final long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes en ms
    
    /**
     * Récupère ou crée une session pour la requête actuelle
     * 
     * @param request La requête HTTP
     * @param response La réponse HTTP (pour créer le cookie si nécessaire)
     * @return L'ID de session
     */
    public static String getOrCreateSession(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = getSessionIdFromRequest(request);
        
        if (sessionId == null || !sessions.containsKey(sessionId) || isSessionExpired(sessionId)) {
            // Créer une nouvelle session
            sessionId = createNewSession(response);
        } else {
            // Rafraîchir l'expiration de la session existante
            refreshSession(sessionId);
        }
        
        return sessionId;
    }
    
    /**
     * Récupère l'ID de session depuis les cookies de la requête
     */
    private static String getSessionIdFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    /**
     * Crée une nouvelle session
     */
    private static String createNewSession(HttpServletResponse response) {
        String sessionId = UUID.randomUUID().toString();
        
        // Créer la map de données de session
        sessions.put(sessionId, new ConcurrentHashMap<>());
        
        // Créer les métadonnées de session
        sessionMetadata.put(sessionId, new SessionMetadata(
            System.currentTimeMillis(),
            System.currentTimeMillis() + DEFAULT_SESSION_TIMEOUT
        ));
        
        // Créer le cookie de session
        Cookie sessionCookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
        sessionCookie.setHttpOnly(true);
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge((int) (DEFAULT_SESSION_TIMEOUT / 1000)); // En secondes
        response.addCookie(sessionCookie);
        
        System.out.println("Nouvelle session créée: " + sessionId);
        return sessionId;
    }
    
    /**
     * Rafraîchit l'expiration d'une session
     */
    private static void refreshSession(String sessionId) {
        SessionMetadata metadata = sessionMetadata.get(sessionId);
        if (metadata != null) {
            metadata.lastAccessed = System.currentTimeMillis();
            metadata.expiresAt = System.currentTimeMillis() + DEFAULT_SESSION_TIMEOUT;
        }
    }
    
    /**
     * Vérifie si une session a expiré
     */
    private static boolean isSessionExpired(String sessionId) {
        SessionMetadata metadata = sessionMetadata.get(sessionId);
        return metadata == null || System.currentTimeMillis() > metadata.expiresAt;
    }
    
    /**
     * Récupère une valeur depuis la session
     * 
     * @param sessionId L'ID de session
     * @param key La clé
     * @return La valeur ou null si non trouvée
     */
    public static Object getSessionValue(String sessionId, String key) {
        Map<String, Object> sessionData = sessions.get(sessionId);
        return sessionData != null ? sessionData.get(key) : null;
    }
    
    /**
     * Stocke une valeur en session
     * 
     * @param sessionId L'ID de session
     * @param key La clé
     * @param value La valeur
     */
    public static void setSessionValue(String sessionId, String key, Object value) {
        Map<String, Object> sessionData = sessions.get(sessionId);
        if (sessionData != null) {
            sessionData.put(key, value);
            refreshSession(sessionId);
        }
    }
    
    /**
     * Supprime une valeur de la session
     * 
     * @param sessionId L'ID de session
     * @param key La clé à supprimer
     */
    public static void removeSessionValue(String sessionId, String key) {
        Map<String, Object> sessionData = sessions.get(sessionId);
        if (sessionData != null) {
            sessionData.remove(key);
        }
    }
    
    /**
     * Détruit complètement une session
     * 
     * @param sessionId L'ID de session
     */
    public static void destroySession(String sessionId) {
        sessions.remove(sessionId);
        sessionMetadata.remove(sessionId);
        System.out.println("Session détruite: " + sessionId);
    }
    
    /**
     * Nettoie les sessions expirées (à appeler périodiquement)
     */
    public static void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        sessionMetadata.entrySet().removeIf(entry -> {
            if (now > entry.getValue().expiresAt) {
                sessions.remove(entry.getKey());
                System.out.println("Session expirée nettoyée: " + entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Récupère toutes les données d'une session
     * 
     * @param sessionId L'ID de session
     * @return Map contenant toutes les données de session
     */
    public static Map<String, Object> getSessionData(String sessionId) {
        return sessions.get(sessionId);
    }
    
    /**
     * Vérifie si une session existe
     * 
     * @param sessionId L'ID de session
     * @return true si la session existe et n'est pas expirée
     */
    public static boolean sessionExists(String sessionId) {
        return sessionId != null && sessions.containsKey(sessionId) && !isSessionExpired(sessionId);
    }
    
    /**
     * Classe interne pour stocker les métadonnées de session
     */
    private static class SessionMetadata {
        long createdAt;
        long lastAccessed;
        long expiresAt;
        
        SessionMetadata(long createdAt, long expiresAt) {
            this.createdAt = createdAt;
            this.lastAccessed = createdAt;
            this.expiresAt = expiresAt;
        }
    }
}
