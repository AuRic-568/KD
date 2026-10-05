import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Task {
    public int id;
    public String description;
    public String status;
    public String createdAt;
    public String updatedAt;

    public Task() {}

    public Task(int id, String description) {
        this.id = id;
        this.status = "todo";
        this.description = description;
        String now = now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = now();
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = now();
    }

    public static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
