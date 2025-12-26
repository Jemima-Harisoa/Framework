package com;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Utilitaire pour gérer l'upload de fichiers
 */
public class FileUploadUtil {
    
    private static final String UPLOAD_DIR = "uploads";
    
    /**
     * Sauvegarde un fichier uploadé
     */
    public static File saveUploadedFile(MultipartFile file, String baseDirectory) 
            throws IOException {
        return saveUploadedFile(file, baseDirectory, UPLOAD_DIR);
    }
    
    /**
     * Sauvegarde un fichier uploadé avec répertoire d'upload configurable
     */
    public static File saveUploadedFile(MultipartFile file, String baseDirectory, String uploadDirName) 
            throws IOException {
        
        if (file == null || file.isEmpty()) {
            throw new IOException("File is empty or null");
        }
        
        // Créer le répertoire d'upload s'il n'existe pas
        File uploadDir = new File(baseDirectory, uploadDirName);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        // Générer un nom de fichier unique pour éviter les collisions
        String originalFilename = file.getOriginalFilename();
        String safeOriginal = originalFilename != null ? originalFilename.replaceAll("[^A-Za-z0-9._-]", "_") : "";
        String fileExtension = "";
        if (safeOriginal.contains(".")) {
            fileExtension = safeOriginal.substring(safeOriginal.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        File destination = new File(uploadDir, uniqueFilename);
        
        // Sauvegarder le fichier
        file.transferTo(destination);
        
        return destination;
    }
    
    /**
     * Supprime un fichier uploadé
     */
    public static boolean deleteUploadedFile(String filePath) {
        File file = new File(filePath);
        return file.delete();
    }
    
    /**
     * Vérifie si un fichier est une image
     */
    public static boolean isImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }
    
    /**
     * Vérifie la taille d'un fichier
     */
    public static boolean isFileSizeValid(MultipartFile file, long maxSizeInBytes) {
        return file != null && file.getSize() <= maxSizeInBytes;
    }
    
    /**
     * Récupère le chemin absolu du répertoire d'upload
     */
    public static String getUploadDirectory(String baseDirectory) {
        return getUploadDirectory(baseDirectory, UPLOAD_DIR);
    }
    
    /**
     * Récupère le chemin absolu du répertoire d'upload avec nom configurable
     */
    public static String getUploadDirectory(String baseDirectory, String uploadDirName) {
        return Paths.get(baseDirectory, uploadDirName).toString();
    }
}