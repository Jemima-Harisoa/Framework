package examples;

import com.SessionManager;

import annotations.Controller;
import annotations.GetMapping;
import annotations.PostMapping;
import annotations.RequestParam;
import annotations.SessionParam;
import model.View;

/**
 * Exemple d'utilisation du système de sessions
 * Montre comment utiliser @SessionParam pour maintenir des données entre les requêtes
 */
@Controller(name = "SessionController", description = "Exemples d'utilisation des sessions")
public class SessionExampleController {
    
    // =====================================================
    // EXEMPLES BASIQUES DE SESSION
    // =====================================================
    
    /**
     * Exemple 1: Stockage d'une valeur en session
     * Cette méthode stocke le nom d'utilisateur en session
     */
    @PostMapping(value = "/login", auteur = "Session Example")
    public View login(@RequestParam String username, 
                     jakarta.servlet.http.HttpServletRequest request,
                     jakarta.servlet.http.HttpServletResponse response) {
        
        // Récupérer l'ID de session
        String sessionId = SessionManager.getOrCreateSession(request, response);
        
        // Stocker des données en session
        SessionManager.setSessionValue(sessionId, "username", username);
        SessionManager.setSessionValue(sessionId, "loginTime", System.currentTimeMillis());
        SessionManager.setSessionValue(sessionId, "isLoggedIn", true);
        
        return View.redirect("dashboard");
    }
    
    /**
     * Exemple 2: Lecture automatique depuis la session avec @SessionParam
     * Le framework injecte automatiquement les valeurs depuis la session
     */
    @GetMapping(value = "/dashboard", auteur = "Session Example")
    public View dashboard(@SessionParam("username") String username,
                         @SessionParam("loginTime") Long loginTime,
                         @SessionParam(value = "isLoggedIn", defaultValue = "false") Boolean isLoggedIn) {
        
        if (!isLoggedIn) {
            return View.redirect("login?error=not_logged_in");
        }
        
        // Créer une vue avec les données de session
        View view = View.page("dashboard");
        view.addData("username", username);
        view.addData("loginTime", new java.util.Date(loginTime));
        
        return view;
    }
    
    /**
     * Exemple 3: Paramètre de session optionnel avec valeur par défaut
     */
    @GetMapping(value = "/profile", auteur = "Session Example")
    public View profile(@SessionParam("username") String username,
                       @SessionParam(value = "theme", defaultValue = "light") String theme,
                       @SessionParam(value = "language", defaultValue = "fr") String language) {
        
        View view = View.page("profile");
        view.addData("username", username);
        view.addData("theme", theme);
        view.addData("language", language);
        
        return view;
    }
    
    /**
     * Exemple 4: Mise à jour d'une valeur en session
     */
    @PostMapping(value = "/update-theme", auteur = "Session Example")
    public View updateTheme(@RequestParam String newTheme,
                           jakarta.servlet.http.HttpServletRequest request,
                           jakarta.servlet.http.HttpServletResponse response) {
        
        String sessionId = SessionManager.getOrCreateSession(request, response);
        SessionManager.setSessionValue(sessionId, "theme", newTheme);
        
        return View.redirect("profile?updated=theme");
    }
    
    // =====================================================
    // EXEMPLES AVANCÉS
    // =====================================================
    
    /**
     * Exemple 5: Compteur de visites en session
     */
    @GetMapping(value = "/visit-counter", auteur = "Session Example") 
    public View visitCounter(@SessionParam(value = "visitCount", defaultValue = "0") Integer visitCount,
                            jakarta.servlet.http.HttpServletRequest request,
                            jakarta.servlet.http.HttpServletResponse response) {
        
        // Incrémenter le compteur
        visitCount++;
        
        // Sauvegarder en session
        String sessionId = SessionManager.getOrCreateSession(request, response);
        SessionManager.setSessionValue(sessionId, "visitCount", visitCount);
        
        View view = View.page("visit-counter");
        view.addData("visitCount", visitCount);
        
        return view;
    }
    
    /**
     * Exemple 6: Panier d'achat en session
     */
    @PostMapping(value = "/add-to-cart", auteur = "Session Example")
    public View addToCart(@RequestParam String productId,
                         @RequestParam Integer quantity,
                         @SessionParam(value = "cartItems", defaultValue = "0") Integer cartItems,
                         jakarta.servlet.http.HttpServletRequest request,
                         jakarta.servlet.http.HttpServletResponse response) {
        
        String sessionId = SessionManager.getOrCreateSession(request, response);
        
        // Récupérer le panier actuel ou créer un nouveau
        @SuppressWarnings("unchecked")
        java.util.Map<String, Integer> cart = 
            (java.util.Map<String, Integer>) SessionManager.getSessionValue(sessionId, "cart");
        
        if (cart == null) {
            cart = new java.util.HashMap<>();
        }
        
        // Ajouter/Mettre à jour l'item
        cart.put(productId, cart.getOrDefault(productId, 0) + quantity);
        
        // Sauvegarder en session
        SessionManager.setSessionValue(sessionId, "cart", cart);
        SessionManager.setSessionValue(sessionId, "cartItems", cart.values().stream().mapToInt(Integer::intValue).sum());
        
        return View.redirect("cart");
    }
    
    /**
     * Exemple 7: Affichage du panier
     */
    @GetMapping(value = "/cart", auteur = "Session Example")
    public View showCart(@SessionParam(value = "cartItems", defaultValue = "0") Integer cartItems,
                        jakarta.servlet.http.HttpServletRequest request,
                        jakarta.servlet.http.HttpServletResponse response) {
        
        String sessionId = SessionManager.getOrCreateSession(request, response);
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Integer> cart = 
            (java.util.Map<String, Integer>) SessionManager.getSessionValue(sessionId, "cart");
        
        if (cart == null) {
            cart = new java.util.HashMap<>();
        }
        
        View view = View.page("cart");
        view.addData("cart", cart);
        view.addData("cartItems", cartItems);
        
        return view;
    }
    
    /**
     * Exemple 8: Déconnexion - destruction de session
     */
    @PostMapping(value = "/logout", auteur = "Session Example")
    public View logout(jakarta.servlet.http.HttpServletRequest request,
                      jakarta.servlet.http.HttpServletResponse response) {
        
        String sessionId = SessionManager.getOrCreateSession(request, response);
        
        // Détruire la session
        SessionManager.destroySession(sessionId);
        
        return View.redirect("login?message=logged_out");
    }
    
    /**
     * Exemple 9: Paramètre de session requis (lève une exception si absent)
     */
    @GetMapping(value = "/admin", auteur = "Session Example")
    public View adminPanel(@SessionParam(value = "userRole", required = true) String userRole) {
        
        if (!"admin".equals(userRole)) {
            return View.redirect("unauthorized");
        }
        
        View view = View.page("admin-panel");
        view.addData("userRole", userRole);
        
        return view;
    }
}