package examples;

import annotations.Controller;
import annotations.GetMapping;
import annotations.PostMapping;
import annotations.RequestParam;
import model.View;

/**
 * Exemple d'utilisation des redirections dans les contrôleurs
 * Montre les différentes façons d'utiliser la nouvelle syntaxe View("redirect:...")
 */
@Controller(name = "ExampleController", description = "Exemples d'utilisation des redirections")
public class ExampleController {
    
    // =====================================================
    // EXEMPLES DE REDIRECTIONS
    // =====================================================
    
    /**
     * Exemple 1: Redirection simple avec la syntaxe directe
     * Utilisation : new View("redirect:users")
     */
    @GetMapping(value = "/go-to-users", auteur = "Exemple")
    public View redirectToUsers() {
        // Cette méthode redirige vers l'URL "/users"
        return new View("redirect:users");
    }
    
    /**
     * Exemple 2: Redirection avec la méthode utilitaire statique
     * Utilisation : View.redirect("users")
     */
    @GetMapping(value = "/redirect-users", auteur = "Exemple")  
    public View redirectToUsersWithHelper() {
        // Méthode plus élégante avec la méthode statique
        return View.redirect("users");
    }
    
    /**
     * Exemple 3: Redirection après traitement POST
     * Pattern classique POST-REDIRECT-GET
     */
    @PostMapping(value = "/create-user", auteur = "Exemple")
    public View createUser(@RequestParam String name, @RequestParam String email) {
        // Logique de création d'utilisateur
        System.out.println("Création utilisateur: " + name + " - " + email);
        
        // Redirection vers la liste des utilisateurs après création
        return View.redirect("users");
    }
    
    /**
     * Exemple 4: Redirection conditionnelle
     */
    @PostMapping(value = "/login", auteur = "Exemple")
    public View login(@RequestParam String username, @RequestParam String password) {
        // Logique de validation (exemple simplifié)
        if ("admin".equals(username) && "password".equals(password)) {
            // Succès : rediriger vers le dashboard
            return View.redirect("dashboard");
        } else {
            // Échec : rediriger vers login avec paramètre d'erreur
            return View.redirect("login?error=invalid");
        }
    }
    
    /**
     * Exemple 5: Redirection externe (URL complète)
     */
    @GetMapping(value = "/go-external", auteur = "Exemple")
    public View redirectToExternalSite() {
        // Redirection vers un site externe
        return new View("redirect:https://www.google.com");
    }
    
    // =====================================================
    // EXEMPLES DE VUES JSP NORMALES (pour comparaison)
    // =====================================================
    
    /**
     * Exemple de vue JSP normale (pas de redirection)
     */
    @GetMapping(value = "/users", auteur = "Exemple")
    public View showUsers() {
        // Simuler une liste d'utilisateurs
        String[] users = {"Alice", "Bob", "Charlie"};
        
        // Affichage de la JSP users.jsp avec les données
        return View.page("users", users);
    }
    
    /**
     * Exemple de vue JSP simple sans données
     */
    @GetMapping(value = "/dashboard", auteur = "Exemple")
    public View showDashboard() {
        // Affichage de la JSP dashboard.jsp
        return View.page("dashboard");
    }
    
    /**
     * Exemple de vue JSP avec le constructeur traditionnel
     */
    @GetMapping(value = "/profile", auteur = "Exemple")
    public View showProfile(@RequestParam String userId) {
        // Simuler la récupération d'un profil utilisateur
        String userProfile = "Profil de l'utilisateur " + userId;
        
        // Utilisation du constructeur traditionnel
        return new View("userProfile", "profile", userProfile);
    }
}