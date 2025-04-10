package test.java;
import main.java.ResumeInfo;
import main.java.ResumeInfoExtractor;
import org.junit.Test;
import static org.junit.Assert.*;

public class ResumeInfoExtractorTest {
    @Test
    public void testInfoExtraction() {
        String testText = "총 5년 경력의 남 1990년 (만 33세) 연봉 4000만원\n" +
                "JAVA, AWS 개발 경험\n" +
                "컴퓨터공학 전공";

        ResumeInfo info = ResumeInfoExtractor.extractResumeInfo(testText);

        assertEquals("경력 연차가 일치해야 함", "5", info.experienceYears());
        assertEquals("성별이 일치해야 함", "남", info.gender());
        assertEquals("나이가 일치해야 함", "33", info.age());
        assertEquals("연봉이 일치해야 함", "4000", info.desiredSalary());

        assertTrue("JAVA 키워드가 포함되어야 함", info.technicalSkills().contains("JAVA"));
        assertTrue("AWS 키워드가 포함되어야 함", info.technicalSkills().contains("AWS"));
    }
}