package com.example.expense.service;

import com.example.expense.exception.BadRequestException;
import com.example.expense.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {
    public static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> TYPE_TO_EXT = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "application/pdf", "pdf");

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create upload directory", e);
        }
    }

    public record StoredFile(String storedName, String originalName, String contentType) {}

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Please choose a file to upload");
        if (file.getSize() > MAX_SIZE) throw new BadRequestException("File is too large (max 5 MB)");

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String ext = TYPE_TO_EXT.get(contentType);
        if (ext == null) throw new BadRequestException("Only JPEG, PNG or PDF files are allowed");

        String original = file.getOriginalFilename() == null ? "receipt" : Path.of(file.getOriginalFilename()).getFileName().toString();
        String lower = original.toLowerCase(Locale.ROOT);
        boolean extOk = switch (ext) {
            case "jpg" -> lower.endsWith(".jpg") || lower.endsWith(".jpeg");
            default -> lower.endsWith("." + ext);
        };
        if (!extOk) throw new BadRequestException("File extension does not match its type");

        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(8);
            if (!matchesSignature(ext, head)) throw new BadRequestException("File content does not match its type");
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }

        String storedName = UUID.randomUUID() + "." + ext; // never trust the client's file name
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file", e);
        }
        return new StoredFile(storedName, original, contentType);
    }

    public Resource load(String storedName) {
        try {
            Path p = root.resolve(storedName).normalize();
            if (!p.startsWith(root) || !Files.exists(p)) throw new ResourceNotFoundException("Receipt file not found");
            return new UrlResource(p.toUri());
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Receipt file not found");
        }
    }

    public void delete(String storedName) {
        if (storedName == null) return;
        try {
            Path p = root.resolve(storedName).normalize();
            if (p.startsWith(root)) Files.deleteIfExists(p);
        } catch (IOException ignored) {
            // best effort
        }
    }

    private static boolean matchesSignature(String ext, byte[] h) {
        if (h.length < 4) return false;
        return switch (ext) {
            case "pdf" -> h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F';
            case "png" -> (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
            case "jpg" -> (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
            default -> false;
        };
    }
}
