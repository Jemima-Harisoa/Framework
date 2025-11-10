package lookup;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Service de scan de classes dans un package
 * Fournit des méthodes pour découvrir et charger des classes Java
 */
public class ClassScanner {
    /**
     * Scanne toutes les classes présentes dans WEB-INF/classes
     * Utilisé pour scanner les classes d'une application web déployée
     * 
     * @return liste de toutes les classes trouvées dans WEB-INF/classes
     * @throws Exception si une erreur survient lors du scan
     */
    public List<Class<?>> getAllClassesFromWebInfClasses() throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        
        // Récupère le ClassLoader pour accéder aux ressources
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        // Récupère l'URL du répertoire racine des classes (WEB-INF/classes)
        URL resource = classLoader.getResource("");
        
        if (resource == null) {
            System.err.println("Impossible de trouver le répertoire des classes");
            return classes;
        }
        
//        System.out.println("Scan du répertoire: " + resource.getPath());
        
        File classesDir = new File(resource.getFile());
        
        if (classesDir.exists() && classesDir.isDirectory()) {
            // Scanner récursivement depuis la racine avec un package vide
            classes.addAll(scanDirectory(classesDir, ""));
        }
        
        return classes;
    }

    /**
     * Scanne récursivement un répertoire pour trouver toutes les classes
     * 
     * @param directory le répertoire à scanner
     * @param packagePrefix le préfixe de package actuel
     * @return liste des classes trouvées
     */
    private List<Class<?>> scanDirectory(File directory, String packagePrefix) {
        List<Class<?>> classes = new ArrayList<>();
        
        File[] files = directory.listFiles();
        if (files == null) return classes;
        
        for (File file : files) {
            if (file.isDirectory()) {
                // Construire le nouveau préfixe de package
                String newPrefix = packagePrefix.isEmpty() 
                    ? file.getName() 
                    : packagePrefix + "." + file.getName();
                
                // Scanner récursivement le sous-répertoire
                classes.addAll(scanDirectory(file, newPrefix));
                
            } else if (file.getName().endsWith(".class")) {
                // Construire le nom complet de la classe
                String className = packagePrefix.isEmpty()
                    ? file.getName().substring(0, file.getName().length() - 6)
                    : packagePrefix + "." + file.getName().substring(0, file.getName().length() - 6);
                
                try {
                    // Charger la classe
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                //   System.out.println("Classe trouvée: " + className);
                } catch (Exception e) {
                    System.err.println("Erreur lors du chargement de: " + className);
                }
            }
        }
        
        return classes;
    }
    
    /**
     * Scanne récursivement un package et retourne toutes les classes qu'il contient
     * Utilise le ClassLoader pour trouver les ressources dans le classpath
     * 
     * @param packageName le nom du package à scanner (ex: "com.monapp.test")
     * @return liste de toutes les classes trouvées dans le package
     * @throws Exception si une erreur de chargement ou d'accès aux ressources survient
     */
    public List<Class<?>> getClasses(String packageName) throws Exception {
        // Récupère le ClassLoader du thread courant pour localiser les ressources
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        // Convertit le nom du package en chemin de répertoire (ex: "test" → "test/")
        String path = packageName.replace('.', '/');
        
        // Récupère toutes les URLs des ressources correspondant au chemin du package
        Enumeration<URL> resources = classLoader.getResources(path);
        
        List<Class<?>> classes = new ArrayList<>();
        
        // Parcourt toutes les ressources trouvées (fichiers, JARs, etc.)
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            
            // Ne traite que les ressources de type fichier (ignore les JARs, etc.)
            if (resource.getProtocol().equals("file")) {
                // Ajoute toutes les classes trouvées dans le répertoire
                classes.addAll(findClasses(new File(resource.getFile()), packageName));
            }
            // Note: Pour scanner les JARs, il faudrait ajouter un traitement supplémentaire ici
        }
        
        return classes;
    }
    
    /**
     * Parcourt récursivement un répertoire pour trouver tous les fichiers .class
     * et les charge en tant que classes Java
     * 
     * @param directory le répertoire à scanner
     * @param packageName le nom du package correspondant au répertoire courant
     * @return liste des classes chargées depuis ce répertoire
     * @throws ClassNotFoundException si une classe ne peut pas être chargée
     */
    private List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        
        // Vérifie que le répertoire existe
        if (!directory.exists()) {
            return classes;  // Retourne une liste vide si le répertoire n'existe pas
        }
        
        // Liste tous les fichiers et sous-répertoires
        File[] files = directory.listFiles();
        if (files == null) return classes;  // Sécurité si listFiles() retourne null
        
        // Parcourt chaque élément du répertoire
        for (File file : files) {
            if (file.isDirectory()) {
                // Si c'est un sous-répertoire, appelle récursivement findClasses
                // avec le nouveau nom de package (ajoute le nom du sous-répertoire)
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                // Si c'est un fichier .class, construit le nom complet de la classe
                // Enlève l'extension ".class" (6 caractères) du nom de fichier
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                
                try {
                    // Charge la classe en mémoire et l'ajoute à la liste
                    classes.add(Class.forName(className));
                } catch (Exception e) {
                    // Gère les erreurs de chargement (classes abstraites, interfaces, etc.)
                    System.err.println("Impossible de charger la classe: " + className);
                    // On ignore la classe et continue avec les suivantes
                }
            }
            // Note: Les fichiers non .class sont ignorés
        }
        return classes;
    }
    
    /**
     * Récupère toutes les classes d'un package et les filtre par un critère
     * 
     * @param packageName le package à scanner
     * @param filter filtre personnalisé pour les classes
     * @return liste des classes correspondant au filtre
     * @throws Exception si une erreur survient lors du scan
     */
    public List<Class<?>> getClassesWithFilter(String packageName, ClassFilter filter) throws Exception {
        List<Class<?>> allClasses = getClasses(packageName);
        List<Class<?>> filteredClasses = new ArrayList<>();
        
        for (Class<?> clazz : allClasses) {
            if (filter.accept(clazz)) {
                filteredClasses.add(clazz);
            }
        }
        
        return filteredClasses;
    }
    
    /**
     * Interface pour filtrer les classes pendant le scan
     */
    public interface ClassFilter {
        boolean accept(Class<?> clazz);
    }
}