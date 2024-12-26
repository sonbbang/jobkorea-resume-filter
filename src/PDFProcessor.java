import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDFProcessor {

    private static final String PATTERN_YEARS = "총\\s*(\\d+)\\s*년";
    private static final String PATTERN_GENDER_AGE = "(남|여)\\s*(\\d{4})년\\s*\\(만\\s*(\\d+)세\\)";

    // 희망 연봉
    private static final String PATTERN_SALARY = "연봉\\s*(.*?)\\s*만원";

    // 이전 연봉
    private static final String PATTERN_BEFORE_SALARY = "(.*?)\\s*만원연봉";

    // 핵심키워드
    private static final String[] KEYWORDS = {"JPA", "JAVA", "PHP", "AWS", "채팅", "팀장"};
    private static final String[] COMPUTER_SCIENCE_TERMS = {"정보통신학", "전산계산학", "전산학", "소프트웨어학", "소프트웨어공학", "소프트웨어전공", "컴퓨터공학", "컴퓨터과학"};

    public static void main(String[] args) {
        String folderPath = "C:\\Users\\n3299\\Desktop\\NateOn개발 지원서";
        File folder = new File(folderPath);
        File[] pdfFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));

        if (pdfFiles == null || pdfFiles.length == 0) {
            System.out.println("No PDF files found in the specified directory.");
            return;
        }

        for (File file : pdfFiles) {
            processPDF(file);
        }
    }

    private static void processPDF(File file) {
        System.out.println("Processing file: " + file.getName());

        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(file))) {
            String fullText = extractText(document);

            String years = extractFirstMatch(fullText, PATTERN_YEARS, 1);
            String[] genderAndAge = extractGenderAndAge(fullText);
            String salary = extractSalary(fullText);
            ArrayList<String> keywords = extractKeywords(fullText);

            renameFile(file, years, genderAndAge, salary, keywords);
        } catch (IOException e) {
            System.err.println("Error processing file: " + file.getName());
            e.printStackTrace();
        }
    }

    private static String extractText(PDDocument document) throws IOException {
        PDFTextStripper pdfStripper = new PDFTextStripper();
        StringBuilder fullText = new StringBuilder();

        for (int i = 1; i <= document.getNumberOfPages(); i++) {
            pdfStripper.setStartPage(i);
            pdfStripper.setEndPage(i);
            fullText.append(pdfStripper.getText(document));
        }

        return fullText.toString();
    }

    private static String extractFirstMatch(String text, String pattern, int group) {
        Matcher matcher = Pattern.compile(pattern).matcher(text);
        return matcher.find() ? matcher.group(group) : null;
    }

    private static String[] extractGenderAndAge(String text) {
        Matcher matcher = Pattern.compile(PATTERN_GENDER_AGE).matcher(text);
        return matcher.find() ? new String[]{matcher.group(1), matcher.group(3)} : new String[]{null, null};
    }

    private static String extractSalary(String text) {
        String salary = extractFirstMatch(text, PATTERN_SALARY, 1);
        return salary != null ? salary : extractFirstMatch(text, PATTERN_BEFORE_SALARY, 1);
    }

    private static ArrayList<String> extractKeywords(String text) {
        ArrayList<String> keywords = new ArrayList<>();
        String lowerCaseText = text.toLowerCase();

        for (String keyword : KEYWORDS) {
            if (lowerCaseText.contains(keyword.toLowerCase())) {
                keywords.add(keyword);
            }
        }

        for (String term : COMPUTER_SCIENCE_TERMS) {
            if (text.contains(term)) {
                keywords.add("컴공");
                break;
            }
        }

        if (countOccurrences(text, "프리랜서") >= 2) {
            keywords.add("프리랜서");
        }

        return keywords;
    }

    private static int countOccurrences(String text, String substring) {
        int count = 0;
        int index = text.indexOf(substring);

        while (index != -1) {
            count++;
            index = text.indexOf(substring, index + 1);
        }

        return count;
    }

    private static void renameFile(File file, String years, String[] genderAndAge, String salary, ArrayList<String> keywords) {
        String newName = String.format("%s년차_%s_%s세%s_%s_%s",
                years != null ? years : "0",
                genderAndAge[0] != null ? genderAndAge[0] : "",
                genderAndAge[1] != null ? genderAndAge[1] : "",
                salary != null ? "_" + salary + "만원" : "",
                String.join(",", keywords),
                file.getName());

        File newFile = new File(file.getParent(), newName);

        if (file.renameTo(newFile)) {
            System.out.println("File renamed to: " + newFile.getName());
        } else {
            System.out.println("Failed to rename file: " + file.getName());
        }
    }
}
