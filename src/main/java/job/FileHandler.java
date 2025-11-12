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

    // 클래스 내부에 추가
    public static List<File> findPdfsUnderChildFolders(String inputFolderPath) {
        File root = new File(inputFolderPath);
        if (!root.exists() || !root.isDirectory()) {
            System.err.println("Input folder does not exist or is not a directory: " + inputFolderPath);
            return List.of();
        }

        File[] childDirs = root.listFiles(File::isDirectory);
        if (childDirs == null || childDirs.length == 0) {
            System.out.println("No child folders found under: " + inputFolderPath);
            return List.of();
        }

        return Arrays.stream(childDirs)
            .flatMap(dir -> {
                File[] pdfs = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
                if (pdfs == null || pdfs.length == 0) {
                    // 이 폴더에는 PDF가 없음
                    return Stream.<File>empty();
                }
                // 여러 개일 경우 '가장 최근 수정된' 파일을 선택 (원하면 용량 기준 등으로 변경 가능)
                File chosen = Arrays.stream(pdfs)
                    .max(Comparator.comparingLong(File::lastModified))
                    .orElse(null);
                return chosen == null ? Stream.<File>empty() : Stream.of(chosen);
            })
            .collect(Collectors.toList());
    }

}