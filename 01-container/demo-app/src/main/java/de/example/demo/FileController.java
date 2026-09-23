package de.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class FileController {

    @Value("${demo.html-dir:/var/demo/html}")
    private String htmlDir;

    @GetMapping("/")
    public ResponseEntity<String> index() throws IOException {
        return serveFile("index.html");
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<String> file(@PathVariable String filename) throws IOException {
        return serveFile(filename);
    }

    private ResponseEntity<String> serveFile(String filename) throws IOException {
        Path filePath = Paths.get(htmlDir, filename).normalize();
        Path baseDir = Paths.get(htmlDir).toRealPath();
        if (!filePath.toRealPath().startsWith(baseDir)) {
            return ResponseEntity.badRequest().build();
        }
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            return ResponseEntity.notFound().build();
        }
        String content = Files.readString(filePath);
        MediaType mediaType = filename.endsWith(".html") ? MediaType.TEXT_HTML : MediaType.TEXT_PLAIN;
        return ResponseEntity.ok().contentType(mediaType).body(content);
    }
}
