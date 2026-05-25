package job;

import java.io.File;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import job.PDFConfig.ResumeFormatType;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DebugCareer {
    public static void main(String[] args) throws Exception {
        String pdfPath = args[0];
        ResumeFormatType fmt = args.length > 1 && "saramin".equalsIgnoreCase(args[1])
            ? ResumeFormatType.SARAMIN : ResumeFormatType.JOBKOREA;

        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBufferedFile(new File(pdfPath)))) {
            String fullText = PDFTextExtractor.extractText(doc);

            // 경력 섹션 텍스트 추출
            String careerSection;
            if (fmt == ResumeFormatType.SARAMIN) {
                int end = fullText.indexOf("경력기술서");
                careerSection = end == -1 ? fullText : fullText.substring(0, end);
                System.out.println("=== 경력기술서 위치: " + end + " ===");
            } else {
                careerSection = fullText;
            }

            System.out.println("\n=== 경력 섹션 텍스트 (마지막 500자) ===");
            int from = Math.max(0, careerSection.length() - 500);
            System.out.println("[..." + careerSection.substring(from) + "]");

            System.out.println("\n=== DATE_RANGE 매칭 라인 ===");
            Pattern DATE_RANGE = Pattern.compile(
                "(\\d{4})[.년\\-]\\s*(\\d{1,2})[월.]?(?:\\.\\d{1,2})?" +
                "(?:\\s*[~～\\-–—]\\s*|[\\s\\x00]{1,})" +
                "(?:(\\d{4})[.년\\-]\\s*(\\d{1,2})[월.]?(?:\\.\\d{1,2})?" +
                "|(현재|재직\\s*중|재직중|이직가능|퇴직))"
            );
            for (String line : careerSection.split("\\R")) {
                Matcher m = DATE_RANGE.matcher(line);
                if (m.find()) {
                    System.out.println("  MATCH: [" + line.trim() + "]");
                }
            }

            System.out.println("\n=== NON_CAREER 필터로 제외된 라인 (날짜 포함) ===");
            for (String line : careerSection.split("\\R")) {
                Matcher m = DATE_RANGE.matcher(line);
                if (m.find()) {
                    // 날짜가 있는데 isNonCareerLine에 걸리는지
                    String lower = line.toLowerCase();
                    for (String kw : new String[]{"졸업","재학","수료","대학교","대학원","고등학교","중학교","자격증","토익","토플","ielts","toeic","학점","gpa","수상","어학","병역","보충역"}) {
                        if (lower.contains(kw)) {
                            System.out.println("  FILTERED by [" + kw + "]: " + line.trim());
                            break;
                        }
                    }
                }
            }

            System.out.println("\n=== parseCareerPeriods 결과 ===");
            List<int[]> periods = CareerAnalyzer.parseCareerPeriods(careerSection);
            System.out.println("periods.size() = " + periods.size());
            for (int[] p : periods) {
                int sy = p[0] / 12, sm = p[0] % 12;
                int ey = p[1] / 12, em = p[1] % 12;
                System.out.printf("  %d.%02d ~ %d.%02d%n", sy, sm, ey, em);
            }
        }
    }
}
