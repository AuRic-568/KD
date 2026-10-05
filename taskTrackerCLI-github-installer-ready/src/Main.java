import tools.jackson.databind.ObjectMapper;

import java.util.List;

public class Main {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        try {
            TaskStore store = new TaskStore(MAPPER);

            if (args.length == 0 || "gui".equalsIgnoreCase(args[0]) || "serve".equalsIgnoreCase(args[0])) {
                int port = 8765;
                if (args.length > 1) {
                    port = Integer.parseInt(args[1]);
                }
                new WebServer(MAPPER, store, port).start(true);
                return;
            }

            runCli(args, store);
        } catch (Exception e) {
            System.err.println("TaskTracker error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void runCli(String[] args, TaskStore store) {
        String command = args[0];
        switch (command) {
            case "add" -> {
                requireArgs(args, 2, "add \"Task description\"");
                Task task = store.add(args[1]);
                System.out.printf("Task Saved (ID: %d)%n", task.id);
            }
            case "delete" -> {
                requireArgs(args, 2, "delete <id>");
                int id = parseId(args[1]);
                System.out.println(store.delete(id) ? "Task deleted." : "Task not found.");
            }
            case "update" -> {
                requireArgs(args, 3, "update <id> \"New description\"");
                Task task = store.update(parseId(args[1]), args[2], null);
                System.out.println(task != null ? "Task updated." : "Task not found.");
            }
            case "mark-in-progress" -> updateStatus(args, store, "in-progress");
            case "mark-done" -> updateStatus(args, store, "done");
            case "list" -> list(args, store);
            default -> printUsage();
        }
    }

    private static void updateStatus(String[] args, TaskStore store, String status) {
        requireArgs(args, 2, args[0] + " <id>");
        Task task = store.update(parseId(args[1]), null, status);
        System.out.println(task != null ? "Task updated." : "Task not found.");
    }

    private static void list(String[] args, TaskStore store) {
        List<Task> tasks = store.load();
        String filter = args.length > 1 ? args[1] : null;

        for (Task task : tasks) {
            if (filter != null && !filter.equals(task.status)) continue;
            System.out.printf(
                    "ID: %d%nDESCRIPTION: %s%nSTATUS: %s%nCREATED AT: %s%nLAST UPDATED: %s%n%n",
                    task.id, task.description, task.status, task.createdAt, task.updatedAt
            );
        }
    }

    private static int parseId(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Task id must be a number.");
        }
    }

    private static void requireArgs(String[] args, int minimum, String usage) {
        if (args.length < minimum) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
    }

    private static void printUsage() {
        System.out.println("""
                TaskTracker

                GUI:
                  java -jar TaskTracker.jar

                CLI:
                  add \"Buy groceries\"
                  update <id> \"New description\"
                  delete <id>
                  mark-in-progress <id>
                  mark-done <id>
                  list [todo|in-progress|done]
                """);
    }
}
