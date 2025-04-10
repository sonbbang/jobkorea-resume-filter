package test.java;
import main.java.ResumeFilter;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

public class ResumeFilterTest {
    private File testInputDir;
    private File testOutputDir;

    @Before
    public void setUp() throws IOException {
        // 테스트 디렉토리 설정
        testInputDir = new File("test_input");
        testOutputDir = new File("test_output");
        testInputDir.mkdir();
        testOutputDir.mkdir();

        // 실제 PDF 파일을 테스트 입력 폴더로 복사
        File samplePdf = new File("C:/Users/n3299/Desktop/resume/sample.pdf");  // 실제 존재하는 PDF 파일
        File testPdf = new File(testInputDir, "test_resume.pdf");
        Files.copy(samplePdf.toPath(), testPdf.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    public void testFullProcess() {
        // 테스트할 메소드 호출
        List<File> pdfFiles = Arrays.asList(testInputDir.listFiles());
        ResumeFilter.processResumes(pdfFiles, testOutputDir);

        // 결과 검증
        File[] outputFiles = testOutputDir.listFiles();
        assertNotNull("출력 파일이 존재해야 함", outputFiles);
        assertTrue("출력 파일이 1개 이상이어야 함", outputFiles.length > 0);
    }

    @After
    public void tearDown() {
        // 테스트 파일 및 디렉토리 정리
        deleteDirectory(testInputDir);
        deleteDirectory(testOutputDir);
    }

    private void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }
}