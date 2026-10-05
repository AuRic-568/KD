import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TaskStore {
    private final ObjectMapper mapper;
    private final Path dataFile;

    public TaskStore(ObjectMapper mapper) throws IOException {
        this.mapper = mapper;
        Path appDir = Path.of(System.getProperty("user.home"), ".tasktracker");
        Files.createDirectories(appDir);
        this.dataFile = appDir.resolve("tasks.jsonl");
        migrateLegacyDataIfNeeded();
        if (Files.notExists(dataFile)) {
            Files.createFile(dataFile);
        }
    }

    private void migrateLegacyDataIfNeeded() {
        Path legacy = Path.of("src", "tasks.json");
        if (Files.notExists(dataFile) && Files.exists(legacy)) {
            try {
                Files.copy(legacy, dataFile);
            } catch (IOException ignored) {
                // Fresh installs simply start with an empty task file.
            }
        }
    }

    public synchronized List<Task> load() {
        List<Task> tasks = new ArrayList<>();
        if (Files.notExists(dataFile)) return tasks;

        try (BufferedReader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                tasks.add(mapper.readValue(line, Task.class));
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not read task data", e);
        }
        return tasks;
    }

    public synchronized void save(List<Task> tasks) {
        try (BufferedWriter writer = Files.newBufferedWriter(
                dataFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            for (Task task : tasks) {
                writer.write(mapper.writeValueAsString(task));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not save task data", e);
        }
    }

    public synchronized Task add(String description) {
        List<Task> tasks = load();
        int nextId = tasks.stream().map(t -> t.id).max(Comparator.naturalOrder()).orElse(-1) + 1;
        Task task = new Task(nextId, description);
        tasks.add(task);
        save(tasks);
        return task;
    }

    public synchronized Task update(int id, String description, String status) {
        List<Task> tasks = load();
        for (Task task : tasks) {
            if (task.id == id) {
                if (description != null) task.setDescription(description);
                if (status != null) task.setStatus(status);
                save(tasks);
                return task;
            }
        }
        return null;
    }

    public synchronized boolean delete(int id) {
        List<Task> tasks = load();
        boolean removed = tasks.removeIf(task -> task.id == id);
        if (removed) save(tasks);
        return removed;
    }

    public Path getDataFile() {
        return dataFile;
    }
}
