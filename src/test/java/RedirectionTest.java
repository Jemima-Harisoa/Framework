package test;

import model.View;

/**
 * Tests simples pour vérifier la fonctionnalité de redirection
 */
public class RedirectionTest {
    
    public static void main(String[] args) {
        System.out.println("=== Tests de la fonctionnalité de redirection ===");
        
        // Test 1: Redirection simple
        testSimpleRedirection();
        
        // Test 2: Méthode utilitaire
        testUtilityMethod();
        
        // Test 3: Vue JSP normale
        testNormalView();
        
        // Test 4: Redirection externe
        testExternalRedirection();
        
        System.out.println("\n✅ Tous les tests sont passés !");
    }
    
    private static void testSimpleRedirection() {
        System.out.println("\n--- Test 1: Redirection simple ---");
        View view = new View("redirect:users");
        
        System.out.println("Template: " + view.getTemplate());
        System.out.println("Est une redirection: " + view.isRedirect());
        System.out.println("URL de redirection: " + view.getRedirectUrl());
        
        assert view.isRedirect() : "Devrait être une redirection";
        assert "/users".equals(view.getRedirectUrl()) : "URL incorrecte";
        
        System.out.println("✅ Test 1 passé");
    }
    
    private static void testUtilityMethod() {
        System.out.println("\n--- Test 2: Méthode utilitaire ---");
        View view = View.redirect("dashboard");
        
        System.out.println("Template: " + view.getTemplate());
        System.out.println("Est une redirection: " + view.isRedirect());
        System.out.println("URL de redirection: " + view.getRedirectUrl());
        
        assert view.isRedirect() : "Devrait être une redirection";
        assert "/dashboard".equals(view.getRedirectUrl()) : "URL incorrecte";
        
        System.out.println("✅ Test 2 passé");
    }
    
    private static void testNormalView() {
        System.out.println("\n--- Test 3: Vue JSP normale ---");
        View view = View.page("users");
        
        System.out.println("Template: " + view.getTemplate());
        System.out.println("Est une redirection: " + view.isRedirect());
        System.out.println("URL de redirection: " + view.getRedirectUrl());
        
        assert !view.isRedirect() : "Ne devrait pas être une redirection";
        assert view.getRedirectUrl() == null : "URL de redirection devrait être null";
        
        System.out.println("✅ Test 3 passé");
    }
    
    private static void testExternalRedirection() {
        System.out.println("\n--- Test 4: Redirection externe ---");
        View view = new View("redirect:https://www.google.com");
        
        System.out.println("Template: " + view.getTemplate());
        System.out.println("Est une redirection: " + view.isRedirect());
        System.out.println("URL de redirection: " + view.getRedirectUrl());
        
        assert view.isRedirect() : "Devrait être une redirection";
        assert "https://www.google.com".equals(view.getRedirectUrl()) : "URL externe incorrecte";
        
        System.out.println("✅ Test 4 passé");
    }
}