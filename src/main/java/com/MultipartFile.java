package com;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * Interface pour représenter un fichier uploadé via un formulaire multipart
 */
public interface MultipartFile extends Serializable {
    
    /**
     * Retourne le nom du paramètre dans le formulaire
     */
    String getName();
    
    /**
     * Retourne le nom original du fichier sur le système client
     */
    String getOriginalFilename();
    
    /**
     * Retourne le type de contenu (MIME type)
     */
    String getContentType();
    
    /**
     * Vérifie si le fichier est vide
     */
    boolean isEmpty();
    
    /**
     * Retourne la taille du fichier en bytes
     */
    long getSize();
    
    /**
     * Retourne le contenu du fichier sous forme de tableau de bytes
     */
    byte[] getBytes() throws IOException;
    
    /**
     * Retourne un InputStream pour lire le contenu du fichier
     */
    InputStream getInputStream() throws IOException;
    
    /**
     * Transfère le fichier vers une nouvelle localisation
     */
    void transferTo(File dest) throws IOException, IllegalStateException;
    
    /**
     * Transfère le fichier vers une nouvelle localisation (chemin String)
     */
    void transferTo(String destPath) throws IOException, IllegalStateException;
}