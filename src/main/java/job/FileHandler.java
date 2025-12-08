package job;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileHandler {
    public static List<File> findResumeFiles(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
        return files != null ? Arrays.asList(files) : new ArrayList<>();
    }

    public static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    public static File createOutputFolder(String outputFolderName) {
        File outputFolder = new File(outputFolderName);
        if (!outputFolder.exists()) {
            if (!outputFolder.mkdirs()) {
                throw new RuntimeException("Failed to create output directory: " + outputFolderName);
            }
        }
        return outputFolder;
    }

    public static void copyResumeWithFormattedName(File sourceFile, File outputFolder, String newFileName) {
        try {
            Path destinationPath = new File(outputFolder, newFileName).toPath();
            Files.copy(sourceFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File copied to: " + destinationPath.getFileName());
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy file: " + sourceFile.getName(), e);
        }
    }

    public static List<File> findPdfsUnderChildFolders(String inputFolderPath) {
        File root = new File(inputFolderPath);
        if (!root.exists() || !root.isDirectory()) {
            System.err.println("Input folder does not exist or is not a directory: " + inputFolderPath);
            return List.of();
        }

        File[] childDirs = root.listFiles(File::isDirectory);
        if (childDirs == null || childDirs.length == 0) {
            return List.of();
        }

        String outputFolderName = PDFConfig.getOutputFolder();

        return Arrays.stream(childDirs)
            .filter(dir -> !dir.getName().equals(outputFolderName)) // 'out' 폴더 제외
            .flatMap(dir -> {
                File[] pdfs = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
                if (pdfs == null || pdfs.length == 0) {
                    return Stream.empty();
                }
                File chosen = Arrays.stream(pdfs)
                    .max(Comparator.comparingLong(File::lastModified))
                    .orElse(null);
                return chosen == null ? Stream.empty() : Stream.of(chosen);
            })
            .collect(Collectors.toList());
    }
}
