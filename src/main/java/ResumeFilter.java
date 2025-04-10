package main.java;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ResumeFilter {
    public static void main(String[] args) {
        try {
            File outputFolder = FileHandler.createOutputFolder(
                    PDFConfig.getInputFolder(),
                    PDFConfig.getOutputFolder()
            );

            List<File> pdfFiles = FileHandler.findResumeFiles(PDFConfig.getInputFolder());
            if (pdfFiles.isEmpty()) {
                System.out.println("No PDF files found in the specified directory.");
                return;
            }

            // 파일 수에 따라 병렬 처리 여부 결정
            if (pdfFiles.size() > 10) {  // 10개 이상일 때만 병렬 처리
                processResumesInParallel(pdfFiles, outputFolder);
            } else {
                processResumes(pdfFiles, outputFolder);  // 기존 순차 처리
            }

        } catch (Exception e) {
            System.err.println("Error processing PDFs: " + e.getMessage());
            e.printStackTrace();
        }
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
