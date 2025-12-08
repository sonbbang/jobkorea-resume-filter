package job;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF 문서에서 텍스트를 추출하는 유틸리티 클래스입니다.
 */
public class PDFTextExtractor {

    /**
     * PDDocument에서 모든 텍스트를 추출합니다.
     *
     * @param document 텍스트를 추출할 PDDocument 객체
     * @return 추출된 전체 텍스트
     * @throws IOException 텍스트 추출 중 오류가 발생할 경우
     */
    public static String extractText(PDDocument document) throws IOException {
        PDFTextStripper pdfStripper = new PDFTextStripper();
        return pdfStripper.getText(document);
    }

    /**
     * 주어진 텍스트에서 정규식 패턴과 일치하는 첫 번째 그룹을 추출합니다.
     *
     * @param text    검색할 텍스트
     * @param pattern 정규식 패턴
     * @param group   추출할 그룹 번호
     * @return 일치하는 그룹의 텍스트, 일치하는 항목이 없으면 null
     */
    public static String extractFirstMatch(String text, String pattern, int group) {
        Matcher matcher = Pattern.compile(pattern).matcher(text);
        return matcher.find() ? matcher.group(group) : null;
    }
}