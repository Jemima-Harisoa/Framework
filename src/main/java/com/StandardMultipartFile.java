package com;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import jakarta.servlet.http.Part;

/**
 * Implémentation de MultipartFile basée sur l'API Servlet Part
 */
public class StandardMultipartFile implements MultipartFile {
    
    private final Part part;
    // cache uniquement si demandé (lazy)
    private byte[] cachedBytes = null;
    // seuil de cache en bytes (ex: 2MB)
    private static final long CACHE_THRESHOLD = 2 * 1024 * 1024;

    public StandardMultipartFile(Part part) {
        this.part = part;
    }
    
    @Override
    public String getName() {
        return part != null ? part.getName() : null;
    }
    
    @Override
    public String getOriginalFilename() {
        if (part == null) return null;
        String filename = part.getSubmittedFileName();
        if (filename == null) return null;
        // sanitize Windows path and normalize
        filename = filename.contains("\\") ? filename.substring(filename.lastIndexOf("\\") + 1) : filename;
        return sanitizeFilename(filename);
    }
    
    @Override
    public String getContentType() {
        return part != null ? part.getContentType() : null;
    }
    
    @Override
    public boolean isEmpty() {
        return part == null || part.getSize() == 0 || getOriginalFilename() == null || getOriginalFilename().isEmpty();
    }
    
    @Override
    public long getSize() {
        return part != null ? part.getSize() : 0;
    }
    
    @Override
    public synchronized byte[] getBytes() throws IOException {
        if (cachedBytes != null) return cachedBytes;
        if (part == null) return new byte[0];
        // si le fichier est petit, on peut mettre en cache
        if (part.getSize() <= CACHE_THRESHOLD) {
            try (InputStream is = part.getInputStream()) {
                cachedBytes = is.readAllBytes();
                return cachedBytes;
            }
        } else {
            // fichier gros -> ne pas mettre en cache, lire et renvoyer
            try (InputStream is = part.getInputStream()) {
                return is.readAllBytes();
            }
        }
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        if (cachedBytes != null) {
            return new ByteArrayInputStream(cachedBytes);
        }
        if (part == null) return InputStream.nullInputStream();
        return part.getInputStream();
    }
    
    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        if (part == null) throw new IllegalStateException("Part is null");
        if (!dest.exists()) {
            File parent = dest.getParentFile();
            if (parent != null) parent.mkdirs();
        }
        // Utiliser un stream pour être sûr du comportement et écraser sécurité
        try (InputStream in = getInputStream()) {
            Files.copy(in, dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    @Override
    public void transferTo(String destPath) throws IOException, IllegalStateException {
        File dest = new File(destPath);
        transferTo(dest);
    }
    
    public File saveToDirectory(String directoryPath) throws IOException {
        if (isEmpty()) {
            throw new IOException("Cannot save empty file");
        }
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String original = getOriginalFilename();
        String safeName = original != null ? original : ("upload-" + System.currentTimeMillis());
        // évite path traversal
        safeName = sanitizeFilename(safeName);
        File dest = new File(directory, safeName);
        transferTo(dest);
        return dest;
    }
    
    public String getFileExtension() {
        String filename = getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Remplace les caractères potentiellement dangereux par underscore
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) return null;
        // enlève les répertoires et caractères non souhaités
        filename = filename.replaceAll("\\\\+", "/"); // unify
        if (filename.contains("/")) {
            filename = filename.substring(filename.lastIndexOf("/") + 1);
        }
        // conserve seulement les caractères sûrs
        return filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
