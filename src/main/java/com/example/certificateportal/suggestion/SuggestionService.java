package com.example.certificateportal.suggestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class SuggestionService {

    private final Path storagePath;
    private final ObjectMapper objectMapper;
    private long nextId;
    private final List<SuggestionPost> posts;

    public SuggestionService(
            @Value("${portal.suggestions.file:data/suggestions.json}") String storageFile,
            ObjectMapper objectMapper
    ) {
        this.storagePath = Path.of(storageFile).toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
        SuggestionStore store = loadStore();
        this.nextId = Math.max(1, store.nextId());
        this.posts = new ArrayList<>(store.posts() == null ? List.of() : store.posts());
    }

    public synchronized List<SuggestionPost> findAll() {
        return posts.stream()
                .sorted(Comparator.comparingLong(SuggestionPost::id).reversed())
                .toList();
    }

    public synchronized Optional<SuggestionPost> findById(long id) {
        return posts.stream().filter(post -> post.id() == id).findFirst();
    }

    public synchronized SuggestionPost create(SuggestionCategory category,
                                              String title,
                                              String content,
                                              String authorId,
                                              String authorName) {
        SuggestionPost post = new SuggestionPost(
                nextId++, category, title, content, LocalDateTime.now(), authorId, authorName
        );
        posts.add(post);
        saveStore();
        return post;
    }

    public synchronized SuggestionPost update(long id,
                                              SuggestionCategory category,
                                              String title,
                                              String content) {
        SuggestionPost existing = requirePost(id);
        SuggestionPost updated = new SuggestionPost(
                existing.id(), category, title, content, existing.createdAt(),
                existing.authorId(), existing.authorName()
        );
        posts.set(posts.indexOf(existing), updated);
        saveStore();
        return updated;
    }

    public synchronized void delete(long id) {
        posts.remove(requirePost(id));
        saveStore();
    }

    public boolean canManage(SuggestionPost post, String userId, boolean administrator) {
        return administrator || post.authorId().equals(userId);
    }

    private SuggestionPost requirePost(long id) {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    }

    private SuggestionStore loadStore() {
        if (Files.notExists(storagePath)) {
            return new SuggestionStore(1, List.of());
        }
        try {
            return objectMapper.readValue(storagePath.toFile(), SuggestionStore.class);
        } catch (IOException exception) {
            throw new IllegalStateException("건의게시판 저장 파일을 읽을 수 없습니다.", exception);
        }
    }

    private void saveStore() {
        try {
            Path parent = storagePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temporaryFile = storagePath.resolveSibling(storagePath.getFileName() + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(temporaryFile.toFile(), new SuggestionStore(nextId, List.copyOf(posts)));
            moveAtomically(temporaryFile, storagePath);
        } catch (IOException exception) {
            throw new IllegalStateException("건의게시판 내용을 저장할 수 없습니다.", exception);
        }
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private record SuggestionStore(long nextId, List<SuggestionPost> posts) {
    }
}
