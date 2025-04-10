package test.java;

import main.java.PDFConfig;
import org.junit.Test;

import static org.junit.Assert.*;

public class PDFConfigTest {
    @Test
    public void testConfigLoading() {
        // 기본 설정값 로드 테스트
        assertNotNull("입력 폴더 설정은 null이 아니어야 함", PDFConfig.getInputFolder());
        assertNotNull("출력 폴더 설정은 null이 아니어야 함", PDFConfig.getOutputFolder());
        assertFalse("키워드 목록은 비어있지 않아야 함", PDFConfig.getKeywords().isEmpty());
    }

    @Test
    public void testPatterns() {
        // 정규식 패턴 유효성 테스트
        String testText = "총 5년 경력의 남 1990년 (만 33세) 연봉 4000만원";

        // 경력 연차 추출 테스트
        assertTrue("경력 연차 패턴이 매치되어야 함",
                testText.matches(".*" + PDFConfig.getYearsPattern() + ".*"));

        // 성별/나이 추출 테스트
        assertTrue("성별/나이 패턴이 매치되어야 함",
                testText.matches(".*" + PDFConfig.getGenderAgePattern() + ".*"));

        // 연봉 추출 테스트
        assertTrue("연봉 패턴이 매치되어야 함",
                testText.matches(".*" + PDFConfig.getSalaryPattern() + ".*"));
    }
}