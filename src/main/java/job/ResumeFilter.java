package job;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import job.PDFConfig.ResumeFormatType;

public class ResumeFilter {

    private static ChatGPTService chatGPTService;
    private static boolean chatgptEnabled;
    private static ResumeFormatType resumeFormatType;

    public static void main(String[] args) {
        try {
            chatgptEnabled = PDFConfig.getChatgptEnabled();
            resumeFormatType = PDFConfig.getResumeFormatType();

            if (chatgptEnabled) {
                String apiKey = PDFConfig.getOpenApiKey();
                if (apiKey == null || apiKey.equals("YOUR_API_KEY_HERE") || apiKey.trim().isEmpty()) {
                    System.err.println("OpenAI API key is not configured. ChatGPT integration will be disabled.");
                    chatgptEnabled = false;
                } else {
                    chatGPTService = new ChatGPTService(apiKey);
                }
            } else {
                System.out.println("ChatGPT integration is disabled in config.properties.");
            }

            String inputFolder = PDFConfig.getInputFolder();
            if (inputFolder == null || inputFolder.trim().isEmpty()) {
                System.err.println("No input folder configured. Please set 'input.folder' in config.properties.");
                return;
            }

            Path outputPath = Paths.get(inputFolder, PDFConfig.getOutputFolder());
            File outputFolder = outputPath.toFile();
            if (outputFolder.exists()) {
                FileHandler.deleteDirectory(outputFolder);
            }
            FileHandler.createOutputFolder(outputPath.toString());

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

        } catch (Exception e) {
            System.err.println("Error processing PDFs: " + e.getMessage());
            e.printStackTrace();
        }
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
        List<String> lines = new ArrayList<>();
        lines.add("이름,나이,성별,최종학력,경력기간(년),주요경력,희망연봉(만원),점수,점수코멘트");

        for (ResumeInfo info : resumeInfos) {
            if (info != null) {
                String name = info.name() != null ? info.name() : "";
                String age = info.age() != null ? info.age() : "";
                String gender = info.gender() != null ? info.gender() : "";
                String education = info.education() != null ? info.education().replace("\"", "\"\"") : "";
                String experienceYears = info.experienceYears() != null ? info.experienceYears() : "0";
                String mainCareer = info.mainCareer() != null ? info.mainCareer().replace("\"", "\"\"") : "";
                String desiredSalary = info.desiredSalary() != null ? info.desiredSalary().replace("\"", "\"\"") : "";
                String score = info.score() != null ? info.score() : "0";
                String scoreReason = info.scoreReason() != null ? info.scoreReason().replace("\"", "\"\"") : "N/A";

                lines.add(String.format("%s,%s,%s,\"%s\",%s,\"%s\",\"%s\",%s,\"%s\"",
                        name, age, gender, education, experienceYears, mainCareer, desiredSalary, score, scoreReason
                ));
            }
        }

        try {
            Files.write(outFile, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("\n=== Saved Extracted Resume Infos ===");
            System.out.println("Count : " + (lines.size() - 1));
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
            
            // 1. 기본 정보 추출 (점수와 사유는 아직 null)
            ResumeInfo initialInfo = ResumeInfoExtractor.extractResumeInfo(fullText, name, resumeFormatType);
            
            String maskedText = ResumeInfoExtractor.maskPersonalInfo(fullText, name);

            String score = "0";
            String reason = "N/A";

            // 2. ChatGPT 호출 (활성화된 경우)
            if (chatgptEnabled && chatGPTService != null) {
                try {
                    Map<String, String> result = chatGPTService.getScoreAndReason(maskedText, PDFConfig.getStructuredJd());
                    score = result.get("score");
                    reason = result.get("reason");
                } catch (IOException e) {
                    System.err.println("Failed to get score from ChatGPT for file " + file.getName() + ": " + e.getMessage());
                }
            }

            // 3. 최종 정보 객체 생성 (점수와 사유 포함)
            ResumeInfo finalInfo = new ResumeInfo(
                initialInfo.name(), initialInfo.experienceYears(), initialInfo.gender(), initialInfo.age(), 
                initialInfo.desiredSalary(), initialInfo.education(), initialInfo.mainCareer(), 
                score, reason, // 점수와 사유를 여기서 최종적으로 할당
                initialInfo.technicalSkills()
            );

            // 4. 파일명 생성 및 복사
            String newFileName = generateFileName(finalInfo, file.getName(), score);
            FileHandler.copyResumeWithFormattedName(file, outputFolder, newFileName);
            
            // 5. 최종 정보 반환
            return finalInfo;
        }
    }

    private static String generateFileName(ResumeInfo info, String originalFileName, String score) {
        String baseNameWithoutExt = originalFileName.replaceFirst("(?i)\\.pdf$", "");
        
        String scorePart = "";
        if (!"N/A".equals(score)) {
            scorePart = String.format("_%s점", score);
        }

        if (info.age() == null || String.valueOf(info.age()).trim().isEmpty()) {
            return String.format("%s%s.pdf", baseNameWithoutExt, scorePart);
        }

        return String.format("%s년차_%s_%s세%s_%s_%s%s.pdf",
                info.experienceYears() != null ? info.experienceYears() : "0",
                info.gender() != null ? info.gender() : "",
                info.age() != null ? info.age() : "",
                info.desiredSalary() != null ? "_" + info.desiredSalary() + "만원" : "",
                String.join(",", info.technicalSkills()),
                baseNameWithoutExt,
                scorePart
        );
    }
}
