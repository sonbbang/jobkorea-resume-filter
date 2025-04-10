package main.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileHandler {
    public static List<File> findResumeFiles(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
        return files != null ? Arrays.asList(files) : new ArrayList<>();
    }

    public static File createOutputFolder(String basePath, String outputFolderName) {
        File outputFolder = new File(basePath, outputFolderName);
        if (!outputFolder.exists() && !outputFolder.mkdir()) {
            System.out.println("basePath = " + basePath);
            System.out.println("basePath = " + outputFolderName);
            throw new RuntimeException("Failed to create output directory.");
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
}