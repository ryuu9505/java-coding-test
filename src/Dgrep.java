import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Dgrep {

    private static int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) throws IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            String[] cmd = br.readLine().split(" ");

            if (cmd.length != 3 || !cmd[0].equals("dgrep")) {
                System.out.println("Usage: dgrep {keyword} {relative path}");
                continue;
            }

            String keyword = cmd[1];
            Path path = Paths.get(cmd[2]);

            ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

            if (Files.isDirectory(path)) {
                Files.walk(path)
                        .filter(Files::isRegularFile)
                        .forEach(file -> executorService.submit(() -> searchFile(keyword, file)));
            } else if (Files.isRegularFile(path)) {
                executorService.submit(() -> searchFile(keyword, path));
            } else {
                System.out.println("Provided path is neither a file nor a directory.");
            }

            executorService.shutdown();

            try {
                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void searchFile(String keyword, Path file) {
        System.out.println("Searching in file: " + file);

        try (Stream<String> lines = Files.lines(file)) {
            final int[] lineNumber = {1};

            lines.forEach(line -> {
                if (line.contains(keyword)) {
                    synchronizedPrint(file + ": line " + lineNumber[0] + " -> " + line);
                }
                lineNumber[0]++;
            });
        } catch (IOException e) {
            System.err.println("Error reading file: " + file);
        }
    }

    public static synchronized void synchronizedPrint(String message) {
        System.out.println(message);
    }
}
