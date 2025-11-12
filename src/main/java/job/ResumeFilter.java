package job;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;

public class ResumeFilter {
    public static void main(String[] args) {
        try {
            File outputFolder = FileHandler.createOutputFolder(
                    PDFConfig.getInputFolder(),
                    PDFConfig.getOutputFolder()
            );

            List<File> pdfFiles = FileHandler.findResumeFiles(PDFConfig.getInputFolder());
            List<File> subPdfFiles = FileHandler.findPdfsUnderChildFolders(PDFConfig.getInputFolder());

            // 가변 리스트로 시작 (pdfFiles가 null이면 빈 리스트)
            List<File> merged = new ArrayList<>(
                pdfFiles != null ? pdfFiles : Collections.emptyList()
            );

            // subPdfFiles가 null이면 addAll에 빈 리스트 전달
            merged.addAll(subPdfFiles != null ? subPdfFiles : Collections.emptyList());

            // 이후 로직에서 merged 사용
            if (merged.isEmpty()) {
                System.out.println("No PDF files found under input and its child folders.");
            }

            // 파일 수에 따라 병렬 처리 여부 결정
            if (merged.size() > 10) {  // 10개 이상일 때만 병렬 처리
                processResumesInParallel(merged, outputFolder);
            } else {
                processResumes(merged, outputFolder);  // 기존 순차 처리
            }

            // === 최종적으로 out 폴더의 파일 목록 읽기 ===
            saveExtractedNamesToTxt(outputFolder);

        } catch (Exception e) {
            System.err.println("Error processing PDFs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 연차 접두어 존재 여부 체크
    private static boolean hasYearsPrefix(String fileName) {
        return Pattern.compile("^(\\d+)년차").matcher(fileName).find();
    }

    /**
     * out 폴더 내 PDF 파일들을 읽어 이름만 출력
     */

    private static void saveExtractedNamesToTxt(File outputFolder) {
        File[] pdfFiles = outputFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
        if (pdfFiles == null || pdfFiles.length == 0) {
            System.out.println("No PDF files found in " + outputFolder.getAbsolutePath());
            return;
        }

        // 결과 파일 경로: outputFolder 내부에 저장
        Path outFile = outputFolder.toPath().resolve("서류전형이름추출.txt");

        try {
            List<String> names = Arrays.stream(pdfFiles)
                .sorted(Comparator.comparingInt(f -> extractYearsFromFileName(f.getName())))
                .map(File::getName)
                .map(ResumeFilter::extractNameFromFileName) // Main 내부면 Main::extractNameFromFileName
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                //.distinct() // 중복 제거 원하면 주석 해제
                .toList();

            Files.write(outFile, names, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println("\n=== Saved Extracted Names ===");
            System.out.println("Count : " + names.size());
            System.out.println("File  : " + outFile.toAbsolutePath());

        } catch (IOException e) {
            System.err.println("Failed to save extracted names: " + e.getMessage());
        }
    }


    /*
    private static void printExtractedNamesFromOutputFolder(File outputFolder) {
        File[] pdfFiles = outputFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
        if (pdfFiles == null || pdfFiles.length == 0) {
            System.out.println("No PDF files found in " + outputFolder.getAbsolutePath());
            return;
        }

        System.out.println("\n=== Extracted Names from Output Folder ===");
        Arrays.stream(pdfFiles)
            .sorted(Comparator.comparingInt(f -> extractYearsFromFileName(f.getName())))
            .map(File::getName)
            .map(ResumeFilter::extractNameFromFileName) // Main 클래스 안에 있을 경우 Main::extractNameFromFileName
            .filter(Objects::nonNull)                  // null 제외
            .filter(s -> !s.isBlank())                 // (선택) 빈 문자열도 제외
            .forEach(System.out::println);
    }*/
    /**
     * 파일명에서 'xx년차' 앞의 숫자만 추출 (없으면 0)
     */
    private static int extractYearsFromFileName(String fileName) {
        Matcher matcher = Pattern.compile("^(\\d+)년차").matcher(fileName);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }
    /**
     * 파일명에서 이름만 추출
     * JOBKOREA : "7년차_남_51세_12,000만원_C,JAVA,PHP,팀장,프리랜서_백엔드 개발자_손형선_20250828_0100015.pdf"
     * 사람인 : "17년차_남_43세_ISMS,IDC,ACL,컴공_손형선_이력서.pdf"
     * → "shinhy"
     */
    /*
    private static String extractNameFromFileName(String fileName) {
        String baseName = fileName.replaceFirst("\\.pdf$", "");
        String[] parts = baseName.split("_");

        if (parts.length >= 3) {
            return parts[parts.length - 3]; // 마지막에서 두 번째 값
        } else {
            return baseName;
        }
    }*/


    private static final Pattern DATE_8 = Pattern.compile("^\\d{8}$");
    private static final Pattern DIGITS = Pattern.compile("^\\d{5,}$"); // 잡코리아 ID 대략 5자리 이상 숫자
    private static final Set<String> TRAILING_SUFFIX = Set.of(
        "이력서","자기소개서","자소서","경력기술서","지원서","포트폴리오",
        "CV","Resume","resume"
    );
    private static final Pattern NAME_LIKE = Pattern.compile("^[\\p{IsHangul}A-Za-z][\\p{IsHangul}A-Za-z\\.\\s]*$");


    private static String extractNameFromFileName(String fileName) {

        // 연차 없으면 제외 신호(null)
        if (!hasYearsPrefix(fileName)) return null;

        // 확장자 제거 (대소문자 무시)
        String baseName = fileName.replaceFirst("(?i)\\.pdf$", "");
        // 연속 언더스코어도 하나로 취급
        String[] parts = baseName.split("_+");
        int n = parts.length;
        if (n == 0) return baseName;

        String last = parts[n - 1].trim();
        String secondLast = n >= 2 ? parts[n - 2].trim() : "";
        String thirdLast  = n >= 3 ? parts[n - 3].trim() : "";

        // 1) 잡코리아: ..._이름_YYYYMMDD_숫자
        if (n >= 3 && DATE_8.matcher(secondLast).matches() && DIGITS.matcher(last).matches()) {
            return thirdLast;
        }

        // 2) 사람인: ..._이름_이력서(또는 유사 꼬리말)
        if (n >= 2 && TRAILING_SUFFIX.contains(last)) {
            return secondLast;
        }

        // 3) 일반 폴백: 뒤에서부터 "이름스러운" 토큰을 찾아 반환
        for (int i = n - 1; i >= 0; i--) {
            String tok = parts[i].trim();
            if (tok.isEmpty()) continue;
            // 날짜/숫자/꼬리말은 건너뛰기
            if (DATE_8.matcher(tok).matches() || DIGITS.matcher(tok).matches() || TRAILING_SUFFIX.contains(tok)) {
                continue;
            }
            // 한글/알파벳 시작의 이름스러운 토큰이면 채택
            if (NAME_LIKE.matcher(tok).matches()) {
                return tok;
            }
        }

        // 4) 최종 폴백: 기존 규칙과 유사하게 뒤에서 3번째 시도 → 없으면 기본 이름 반환
        if (n >= 3) return thirdLast;
        if (n >= 2) return secondLast;
        return baseName;
    }

    public static void processResumes(List<File> pdfFiles, File outputFolder) {
        int total = pdfFiles.size();
        int processed = 0;
        int successful = 0;

        System.out.println("Starting to process " + total + " PDF files...");

        for (File file : pdfFiles) {
            processed++;
            try {
                processResumeFile(file, outputFolder);
                successful++;
                System.out.printf("Progress: %d/%d files processed (%d%%)%n",
                        processed, total, (processed * 100) / total);
            } catch (Exception e) {
                System.err.println("Error processing file " + file.getName() + ": " + e.getMessage());
            }
        }

        System.out.printf("Processing complete. Successfully processed %d out of %d files.%n",
                successful, total);
    }

    private static void processResumeFile(File file, File outputFolder) throws IOException {
        System.out.println("Processing file: " + file.getName());

        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(file))) {
            String fullText = PDFTextExtractor.extractText(document);
            ResumeInfo info = ResumeInfoExtractor.extractResumeInfo(fullText);

            String newFileName = generateFileName(info, file.getName());
            FileHandler.copyResumeWithFormattedName(file, outputFolder, newFileName);
        }
    }

    private static void processResumesInParallel(List<File> pdfFiles, File outputFolder) {
        int total = pdfFiles.size();
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger successful = new AtomicInteger(0);
        System.out.println("Starting parallel processing of " + total + " PDF files...");

        // 스레드 풀 크기 제한 (CPU 코어 수를 기준으로)
        int processors = Runtime.getRuntime().availableProcessors();
        int parallelism = Math.min(processors, total);

        try {
            pdfFiles.parallelStream()
                    .forEach(file -> {
                        try {
                            processResumeFile(file, outputFolder);
                            successful.incrementAndGet();

                            // 진행상황 출력
                            int current = processed.incrementAndGet();
                            System.out.printf("Progress: %d/%d files processed (%d%%)%n",
                                    current, total, (current * 100) / total);

                        } catch (Exception e) {
                            System.err.println("Error processing file " + file.getName() + ": " + e.getMessage());
                            processed.incrementAndGet();
                        }
                    });

        } finally {
            System.out.printf("Processing complete. Successfully processed %d out of %d files.%n",
                    successful.get(), total);

        }
    }


    private static String generateFileName(ResumeInfo info, String originalFileName) {
        // 나이가 없으면 원본 파일명 유지 (권장: sanitize로 한 번 정리)
        if (info.age() == null || String.valueOf(info.age()).trim().isEmpty()) {
            return originalFileName;
        }

        return String.format("%s년차_%s_%s세%s_%s_%s",
                info.experienceYears() != null ? info.experienceYears() : "0",
                info.gender() != null ? info.gender() : "",
                info.age() != null ? info.age() : "",
                info.desiredSalary() != null ? "_" + info.desiredSalary() + "만원" : "",
                String.join(",", info.technicalSkills()),
                originalFileName
        );
    }
}
