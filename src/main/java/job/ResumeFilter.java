package job;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import job.PDFConfig.ResumeFormatType;

public class ResumeFilter {

    private static ResumeFormatType resumeFormatType;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new ResumeFilterGui().setVisible(true));
    }

    public static void run(String inputFolder, ResumeFormatType type) throws Exception {
        resumeFormatType = type;

        if (inputFolder == null || inputFolder.trim().isEmpty()) {
            System.err.println("No input folder configured.");
            return;
        }

        Path outputPath = Paths.get(inputFolder, PDFConfig.getOutputFolder());
        File outputFolder = outputPath.toFile();
        if (outputFolder.exists()) {
            System.out.println("기존 out 폴더 삭제 중: " + outputFolder.getAbsolutePath());
            FileHandler.deleteDirectory(outputFolder);
            if (outputFolder.exists()) {
                throw new IOException("out 폴더를 삭제할 수 없습니다. '서류전형요약.csv' 파일이 열려 있으면 닫고 다시 실행해주세요.\n경로: " + outputFolder.getAbsolutePath());
            }
            System.out.println("기존 out 폴더 삭제 완료");
        }
        FileHandler.createOutputFolder(outputPath.toString());

        List<File> nonPdfFiles = FileHandler.findNonPdfFiles(inputFolder);
        for (File nonPdf : nonPdfFiles) {
            FileHandler.copyFile(nonPdf, outputFolder);
        }

        List<File> pdfFiles = FileHandler.findResumeFiles(inputFolder);
        List<File> subPdfFiles = FileHandler.findPdfsUnderChildFolders(inputFolder);

        List<File> allPdfFiles = new ArrayList<>(pdfFiles != null ? pdfFiles : Collections.emptyList());
        if (subPdfFiles != null) {
            allPdfFiles.addAll(subPdfFiles);
        }

        if (allPdfFiles.isEmpty()) {
            System.out.println("No PDF files found in the specified input folder and its subfolders.");
            return;
        }

        List<ResumeInfo> processedResumes;
        if (allPdfFiles.size() > 10) {
            processedResumes = processResumesInParallel(allPdfFiles, outputFolder);
        } else {
            processedResumes = processResumes(allPdfFiles, outputFolder);
        }

        saveResumeInfosToTxt(processedResumes, outputFolder);
    }

    private static double getYearsAsDouble(ResumeInfo info) {
        if (info == null || info.experienceYears() == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(info.experienceYears());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static void saveResumeInfosToTxt(List<ResumeInfo> resumeInfos, File outputFolder) {
        resumeInfos.sort(Comparator.comparingDouble(ResumeFilter::getYearsAsDouble));

        Path outFile = outputFolder.toPath().resolve("서류전형요약.csv");
        
        try (FileOutputStream fos = new FileOutputStream(outFile.toFile());
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(osw)) {

            // Write UTF-8 BOM
            fos.write(0xEF);
            fos.write(0xBB);
            fos.write(0xBF);

            // Write header
            writer.write("이름,나이,성별,최종학력,경력기간(년),주요경력,희망연봉(만원)");
            writer.newLine();

            // Write data
            for (ResumeInfo info : resumeInfos) {
                if (info != null) {
                    String name = info.name() != null ? info.name() : "";
                    String age = info.age() != null ? info.age() : "";
                    String gender = info.gender() != null ? info.gender() : "";
                    String education = info.education() != null ? info.education().replace("\"", "\"\"") : "";
                    String experienceYears = info.experienceYears() != null ? info.experienceYears() : "0";
                    String mainCareer = info.mainCareer() != null ? info.mainCareer().replace("\"", "\"\"") : "";
                    String desiredSalary = info.desiredSalary() != null ? info.desiredSalary().replace("\"", "\"\"") : "";

                    String line = String.format("%s,%s,%s,\"%s\",%s,\"%s\",\"%s\"",
                            name, age, gender, education, experienceYears, mainCareer, desiredSalary
                    );
                    writer.write(line);
                    writer.newLine();
                }
            }
            
            System.out.println("\n=== Saved Extracted Resume Infos ===");
            System.out.println("Count : " + resumeInfos.size());
            System.out.println("File  : " + outFile.toAbsolutePath());

        } catch (IOException e) {
            System.err.println("Failed to save extracted resume infos: " + e.getMessage());
        }
    }

    private static final Pattern NAME_LIKE = Pattern.compile("^[\\p{IsHangul}A-Za-z][\\p{IsHangul}A-Za-z\\.\\s]*$");
    private static final Set<String> TRAILING_SUFFIX = Set.of("이력서", "자기소개서", "자소서", "경력기술서", "지원서", "포트폴리오", "CV", "Resume", "resume");
    private static final Pattern DATE_8 = Pattern.compile("^\\d{8}$");
    private static final Pattern DIGITS = Pattern.compile("^\\d{5,}$");

    private static String extractNameFromFileName(String fileName) {
        String baseName = fileName.replaceFirst("(?i)\\.pdf$", "");
        String[] parts = baseName.split("_+");
        int n = parts.length;
        if (n == 0) return baseName;

        String last = parts[n - 1].trim();
        String secondLast = n >= 2 ? parts[n - 2].trim() : "";
        String thirdLast = n >= 3 ? parts[n - 3].trim() : "";

        if (n >= 3 && DATE_8.matcher(secondLast).matches() && DIGITS.matcher(last).matches()) return thirdLast;
        if (n >= 2 && TRAILING_SUFFIX.contains(last)) return secondLast;

        for (int i = n - 1; i >= 0; i--) {
            String tok = parts[i].trim();
            if (tok.isEmpty() || DATE_8.matcher(tok).matches() || DIGITS.matcher(tok).matches() || TRAILING_SUFFIX.contains(tok)) continue;
            if (NAME_LIKE.matcher(tok).matches()) return tok;
        }

        if (n >= 3) return thirdLast;
        if (n >= 2) return secondLast;
        return baseName;
    }

    public static List<ResumeInfo> processResumes(List<File> pdfFiles, File outputFolder) {
        System.out.println("Starting to process " + pdfFiles.size() + " PDF files...");
        List<ResumeInfo> processedInfos = new ArrayList<>();
        for (int i = 0; i < pdfFiles.size(); i++) {
            File file = pdfFiles.get(i);
            try {
                processedInfos.add(processResumeFile(file, outputFolder));
                System.out.printf("Progress: %d/%d files processed (%d%%)%n", i + 1, pdfFiles.size(), ((i + 1) * 100) / pdfFiles.size());
            } catch (Exception e) {
                System.err.println("Error processing file " + file.getName() + ": " + e.getMessage());
            }
        }
        return processedInfos;
    }

    private static List<ResumeInfo> processResumesInParallel(List<File> pdfFiles, File outputFolder) {
        System.out.println("Starting parallel processing of " + pdfFiles.size() + " PDF files...");
        AtomicInteger processedCount = new AtomicInteger(0);
        List<ResumeInfo> processedInfos = pdfFiles.parallelStream()
                .map(file -> {
                    try {
                        ResumeInfo info = processResumeFile(file, outputFolder);
                        int current = processedCount.incrementAndGet();
                        System.out.printf("Progress: %d/%d files processed (%d%%)%n", current, pdfFiles.size(), (current * 100) / pdfFiles.size());
                        return info;
                    } catch (Exception e) {
                        System.err.println("Error processing file " + file.getName() + ": " + e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return processedInfos;
    }

    private static ResumeInfo processResumeFile(File file, File outputFolder) throws IOException {
        System.out.println("Processing file: " + file.getName());
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(file))) {
            String fullText = PDFTextExtractor.extractText(document);
            String name = extractNameFromFileName(file.getName());
            
            ResumeInfo info = ResumeInfoExtractor.extractResumeInfo(fullText, name, resumeFormatType);

            String newFileName = generateFileName(info, file.getName());
            FileHandler.copyResumeWithFormattedName(file, outputFolder, newFileName);
            return info;
        }
    }

    private static String generateFileName(ResumeInfo info, String originalFileName) {
        String baseNameWithoutExt = originalFileName.replaceFirst("(?i)\\.pdf$", "");

        if (info.age() == null || info.age().trim().isEmpty()) {
            return String.format("%s.pdf", baseNameWithoutExt);
        }

        return String.format("%s년차_%s_%s세%s_%s_%s.pdf",
                info.experienceYears() != null ? info.experienceYears() : "0",
                info.gender() != null ? info.gender() : "",
                info.age() != null ? info.age() : "",
                info.desiredSalary() != null ? "_" + info.desiredSalary() + "만원" : "",
                String.join(",", info.technicalSkills()),
                baseNameWithoutExt
        );
    }
}
