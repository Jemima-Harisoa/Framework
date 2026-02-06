# 🔐 Système de Gestion de Sessions - Documentation

## 📖 Vue d'ensemble

Le framework intègre un système de sessions robuste qui permet de maintenir des données utilisateur entre les requêtes HTTP. Le système est basé sur des cookies sécurisés et utilise une architecture thread-safe.

## ⭐ Fonctionnalités Principales

### 🔧 Injection Automatique
- **`@SessionParam`** : Injecte automatiquement les valeurs depuis la session
- **Gestion des types** : Support des primitives, objets et wrappers
- **Valeurs par défaut** : Configuration de valeurs par défaut si la clé n'existe pas
- **Paramètres requis** : Exception automatique si une valeur requise est absente

### 🔒 Sécurité & Isolation
- **Isolation complète** : Chaque utilisateur a sa propre Map de session
- **Thread-safe** : Utilisation de ConcurrentHashMap
- **Cookies sécurisés** : HttpOnly, gestion automatique des chemins
- **Expiration automatique** : Sessions expirées nettoyées automatiquement

### 🎯 Facilité d'utilisation
- **Création automatique** : Sessions créées transparentemente
- **Gestion des cookies** : Automatique, aucune intervention requise
- **API simple** : Méthodes statiques pour accès direct

## 📝 Annotations

### @SessionParam

```java
@SessionParam(
    value = "keyName",           // Nom de la clé en session
    name = "keyName",            // Alias pour value
    required = false,            // Si le paramètre est requis
    defaultValue = "defaultVal"  // Valeur par défaut si absent
)
```

**Priorités pour le nom de la clé :**
1. `value()` dans l'annotation
2. `name()` dans l'annotation  
3. Nom du paramètre Java

## 💻 Utilisation dans les Contrôleurs

### 1. **Lecture Automatique depuis la Session**

```java
@GetMapping("/dashboard")
public View dashboard(@SessionParam("username") String username,
                     @SessionParam("loginTime") Long loginTime) {
    // Les valeurs sont automatiquement injectées depuis la session
    return View.page("dashboard", username);
}
```

### 2. **Paramètres Optionnels avec Valeur par Défaut**

```java
@GetMapping("/settings")
public View settings(@SessionParam(value = "theme", defaultValue = "light") String theme,
                    @SessionParam(value = "language", defaultValue = "fr") String language) {
    // Si 'theme' n'existe pas en session, utilise "light"
    return View.page("settings");
}
```

### 3. **Paramètres Requis**

```java
@GetMapping("/admin")
public View adminPanel(@SessionParam(value = "userRole", required = true) String role) {
    // Exception levée si 'userRole' n'existe pas en session
    if (!"admin".equals(role)) {
        return View.redirect("unauthorized");
    }
    return View.page("admin");
}
```

### 4. **Stockage Manuel en Session**

```java
@PostMapping("/login")
public View login(@RequestParam String username,
                 HttpServletRequest request,
                 HttpServletResponse response) {
    
    String sessionId = SessionManager.getOrCreateSession(request, response);
    
    // Stocker des données
    SessionManager.setSessionValue(sessionId, "username", username);
    SessionManager.setSessionValue(sessionId, "loginTime", System.currentTimeMillis());
    SessionManager.setSessionValue(sessionId, "isLoggedIn", true);
    
    return View.redirect("dashboard");
}
```

## 🛠️ API SessionManager

### Méthodes Principales

```java
// Récupérer/Créer une session
String sessionId = SessionManager.getOrCreateSession(request, response);

// Stocker une valeur
SessionManager.setSessionValue(sessionId, "key", value);

// Récupérer une valeur
Object value = SessionManager.getSessionValue(sessionId, "key");

// Supprimer une valeur
SessionManager.removeSessionValue(sessionId, "key");

// Détruire une session
SessionManager.destroySession(sessionId);

// Vérifier l'existence
boolean exists = SessionManager.sessionExists(sessionId);

// Récupérer toutes les données
Map<String, Object> data = SessionManager.getSessionData(sessionId);
```

### Nettoyage Automatique

```java
// Nettoyer les sessions expirées (à appeler périodiquement)
SessionManager.cleanupExpiredSessions();
```

## 📋 Exemples Pratiques

### 1. **Système de Connexion Complet**

```java
@Controller
public class AuthController {
    
    @PostMapping("/login")
    public View login(@RequestParam String username, 
                     @RequestParam String password,
                     HttpServletRequest request, 
                     HttpServletResponse response) {
        
        if (authenticate(username, password)) {
            String sessionId = SessionManager.getOrCreateSession(request, response);
            SessionManager.setSessionValue(sessionId, "username", username);
            SessionManager.setSessionValue(sessionId, "isLoggedIn", true);
            return View.redirect("dashboard");
        } else {
            return View.redirect("login?error=invalid");
        }
    }
    
    @GetMapping("/dashboard")
    public View dashboard(@SessionParam("username") String username,
                         @SessionParam(value = "isLoggedIn", defaultValue = "false") Boolean isLoggedIn) {
        
        if (!isLoggedIn) {
            return View.redirect("login");
        }
        
        View view = View.page("dashboard");
        view.addData("username", username);
        return view;
    }
    
    @PostMapping("/logout")
    public View logout(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = SessionManager.getOrCreateSession(request, response);
        SessionManager.destroySession(sessionId);
        return View.redirect("login?message=logged_out");
    }
}
```

### 2. **Compteur de Visites**

```java
@GetMapping("/counter")
public View visitCounter(@SessionParam(value = "visits", defaultValue = "0") Integer visits,
                        HttpServletRequest request, 
                        HttpServletResponse response) {
    
    visits++; // Incrémenter
    
    // Sauvegarder la nouvelle valeur
    String sessionId = SessionManager.getOrCreateSession(request, response);
    SessionManager.setSessionValue(sessionId, "visits", visits);
    
    View view = View.page("counter");
    view.addData("visits", visits);
    return view;
}
```

### 3. **Panier d'Achat**

```java
@PostMapping("/add-to-cart")
public View addToCart(@RequestParam String productId,
                     @RequestParam Integer quantity,
                     HttpServletRequest request,
                     HttpServletResponse response) {
    
    String sessionId = SessionManager.getOrCreateSession(request, response);
    
    // Récupérer le panier
    @SuppressWarnings("unchecked")
    Map<String, Integer> cart = (Map<String, Integer>) 
        SessionManager.getSessionValue(sessionId, "cart");
    
    if (cart == null) {
        cart = new HashMap<>();
    }
    
    // Ajouter l'item
    cart.put(productId, cart.getOrDefault(productId, 0) + quantity);
    
    // Sauvegarder
    SessionManager.setSessionValue(sessionId, "cart", cart);
    
    return View.redirect("cart");
}

@GetMapping("/cart")
public View showCart(HttpServletRequest request, HttpServletResponse response) {
    String sessionId = SessionManager.getOrCreateSession(request, response);
    
    @SuppressWarnings("unchecked")
    Map<String, Integer> cart = (Map<String, Integer>) 
        SessionManager.getSessionValue(sessionId, "cart");
    
    View view = View.page("cart");
    view.addData("cart", cart != null ? cart : new HashMap<>());
    return view;
}
```

## ⚙️ Configuration

### Paramètres par Défaut

```java
// Nom du cookie de session
private static final String SESSION_COOKIE_NAME = "FRAMEWORK_SESSIONID";

// Durée de vie (30 minutes)
private static final long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000;
```

### Cookies de Session

- **Nom** : `FRAMEWORK_SESSIONID`
- **HttpOnly** : `true` (sécurité XSS)
- **Path** : `/` (disponible pour toute l'application)
- **Expiration** : 30 minutes d'inactivité

## 🔍 Types Supportés

### Injection @SessionParam

✅ **Supportés :**
- `String`, `Integer`, `Long`, `Double`, `Float`, `Boolean`
- Objets complexes (sérialisés automatiquement)
- Collections (`Map`, `List`, etc.)
- Wrappers et types nullable

❌ **Non supportés :**
- Types primitifs (`int`, `boolean`, etc.) - utiliser les wrappers

### Stockage SessionManager

✅ **Tous les objets Java** peuvent être stockés en session

## 🚨 Gestion d'Erreurs

### Paramètre Requis Absent

```java
@GetMapping("/secure")
public View secure(@SessionParam(value = "token", required = true) String token) {
    // Exception: "Paramètre de session requis non trouvé: token"
}
```

### Conversion de Type Impossible

```java
// Si "count" contient "abc" et qu'on demande un Integer
@GetMapping("/count")
public View count(@SessionParam("count") Integer count) {
    // Exception: "Impossible de convertir la valeur 'abc' en type Integer"
}
```

## 🎯 Bonnes Pratiques

### 1. **Sécurité**
```java
// ✅ Bon : Vérifier les permissions
@GetMapping("/admin")
public View admin(@SessionParam("role") String role) {
    if (!"admin".equals(role)) {
        return View.redirect("unauthorized");
    }
    // ...
}

// ❌ Mauvais : Faire confiance aveuglément aux données de session
```

### 2. **Performance**
```java
// ✅ Bon : Stocker des identifiants, pas des objets lourds
SessionManager.setSessionValue(sessionId, "userId", userId);

// ❌ Mauvais : Stocker de gros objets en session
SessionManager.setSessionValue(sessionId, "allUsers", heavyUserList);
```

### 3. **Nettoyage**
```java
// ✅ Bon : Détruire la session à la déconnexion
@PostMapping("/logout")
public View logout(HttpServletRequest request, HttpServletResponse response) {
    String sessionId = SessionManager.getOrCreateSession(request, response);
    SessionManager.destroySession(sessionId); // Important !
    return View.redirect("login");
}
```

## 🔧 Dépannage

### Session Non Créée
**Problème** : Les valeurs ne sont pas sauvegardées
**Solution** : Vérifier que `HttpServletResponse` est bien passé à `getOrCreateSession()`

### Valeur Null Inattendue
**Problème** : `@SessionParam` retourne null
**Solution** : Utiliser `defaultValue` ou vérifier que la clé a été stockée

### Exception de Conversion
**Problème** : Erreur lors de la conversion de type
**Solution** : Vérifier que le type stocké correspond au type demandé

## 📊 Exemple Complet : Application E-commerce

```java
@Controller
public class EcommerceController {
    
    // Page d'accueil avec compteur de visites
    @GetMapping("/")
    public View home(@SessionParam(value = "visits", defaultValue = "1") Integer visits,
                    HttpServletRequest request, HttpServletResponse response) {
        
        String sessionId = SessionManager.getOrCreateSession(request, response);
        SessionManager.setSessionValue(sessionId, "visits", visits + 1);
        
        return View.page("home");
    }
    
    // Connexion utilisateur
    @PostMapping("/login")
    public View login(@RequestParam String email, @RequestParam String password,
                     HttpServletRequest request, HttpServletResponse response) {
        
        User user = userService.authenticate(email, password);
        if (user != null) {
            String sessionId = SessionManager.getOrCreateSession(request, response);
            SessionManager.setSessionValue(sessionId, "userId", user.getId());
            SessionManager.setSessionValue(sessionId, "userRole", user.getRole());
            SessionManager.setSessionValue(sessionId, "isLoggedIn", true);
            
            return View.redirect("dashboard");
        }
        
        return View.redirect("login?error=invalid");
    }
    
    // Tableau de bord utilisateur
    @GetMapping("/dashboard")
    public View dashboard(@SessionParam("userId") Integer userId,
                         @SessionParam(value = "isLoggedIn", defaultValue = "false") Boolean isLoggedIn) {
        
        if (!isLoggedIn) {
            return View.redirect("login");
        }
        
        User user = userService.findById(userId);
        return View.page("dashboard", user);
    }
    
    // Ajouter au panier
    @PostMapping("/cart/add")
    public View addToCart(@RequestParam Integer productId, @RequestParam Integer quantity,
                         HttpServletRequest request, HttpServletResponse response) {
        
        String sessionId = SessionManager.getOrCreateSession(request, response);
        
        @SuppressWarnings("unchecked")
        Map<Integer, Integer> cart = (Map<Integer, Integer>) 
            SessionManager.getSessionValue(sessionId, "cart");
        
        if (cart == null) {
            cart = new HashMap<>();
        }
        
        cart.put(productId, cart.getOrDefault(productId, 0) + quantity);
        SessionManager.setSessionValue(sessionId, "cart", cart);
        
        return View.redirect("cart");
    }
    
    // Voir le panier
    @GetMapping("/cart")
    public View cart(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = SessionManager.getOrCreateSession(request, response);
        
        @SuppressWarnings("unchecked")
        Map<Integer, Integer> cart = (Map<Integer, Integer>) 
            SessionManager.getSessionValue(sessionId, "cart");
        
        if (cart == null) {
            cart = new HashMap<>();
        }
        
        List<CartItem> cartItems = cart.entrySet().stream()
            .map(entry -> new CartItem(
                productService.findById(entry.getKey()),
                entry.getValue()
            ))
            .collect(Collectors.toList());
        
        View view = View.page("cart");
        view.addData("cartItems", cartItems);
        view.addData("totalItems", cart.values().stream().mapToInt(Integer::intValue).sum());
        
        return view;
    }
    
    // Déconnexion
    @PostMapping("/logout")
    public View logout(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = SessionManager.getOrCreateSession(request, response);
        SessionManager.destroySession(sessionId);
        return View.redirect("home?message=logged_out");
    }
}
```

## ✅ Résumé

Le système de sessions du framework offre :

- 🎯 **Simplicité** : Injection automatique avec `@SessionParam`
- 🔒 **Sécurité** : Isolation complète entre utilisateurs
- ⚡ **Performance** : Thread-safe avec ConcurrentHashMap
- 🛠️ **Flexibilité** : API complète pour gestion manuelle
- 🔧 **Robustesse** : Gestion d'erreurs et nettoyage automatique

**Votre framework supporte maintenant un système de sessions professionnel et prêt pour la production !**