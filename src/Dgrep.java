import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

            long startTime = System.nanoTime();
            AtomicInteger totalLinesProcessed = new AtomicInteger(0);
            ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

            if (Files.isDirectory(path)) {
                Files.walk(path)
                        .filter(Files::isRegularFile)
                        .forEach(file -> {
                            try {
                                processFile(keyword, file, executorService, totalLinesProcessed);
                            } catch (IOException e) {
                                System.err.println("Error processing file: " + file);
                            }
                        });
            } else if (Files.isRegularFile(path)) {
                processFile(keyword, path, executorService, totalLinesProcessed);
            } else {
                System.out.println("Provided path is neither a file nor a directory.");
            }

            executorService.shutdown();

            try {
                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long endTime = System.nanoTime();
            long durationInNano = endTime - startTime;
            double durationInSeconds = durationInNano / 1_000_000_000.0;
            System.out.printf("Elapsed time: %.3f seconds%n", durationInSeconds);
            System.out.println("Total lines processed: " + totalLinesProcessed.get());

        }
    }


    private static void processFile(String keyword, Path file, ExecutorService executorService, AtomicInteger totalLinesProcessed) throws IOException {
        List<String> lines = Files.readAllLines(file);
        int chunkSize = Math.max(lines.size() / THREAD_POOL_SIZE, 1);

        for (int i = 0; i < lines.size(); i += chunkSize) {
            int start = i;
            int end = Math.min(i + chunkSize, lines.size());

            executorService.submit(() -> searchLines(keyword, file, lines.subList(start, end), start, totalLinesProcessed));
        }
    }


    private static void searchLines(String keyword, Path file, List<String> lines, int startLineNumber, AtomicInteger totalLinesProcessed) {
        int lineNumber = startLineNumber + 1;
        for (String line : lines) {
            totalLinesProcessed.incrementAndGet();
            if (line.contains(keyword)) {
//                System.out.println(file + ": line " + lineNumber + " -> " + line);
            }
            lineNumber++;
        }
    }
}
