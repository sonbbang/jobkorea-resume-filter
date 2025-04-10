package test.java;

import main.java.FileHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class FileHandlerTest {
    private File testOutputDir;

    @Before
    public void setUp() {
        testOutputDir = new File("test_output");
        testOutputDir.mkdir();
    }

    @Test
    public void testCreateOutputFolder() {
        File outputFolder = FileHandler.createOutputFolder("test_output", "pdf_out");
        assertTrue("출력 폴더가 생성되어야 함", outputFolder.exists());
        assertTrue("생성된 폴더가 디렉토리여야 함", outputFolder.isDirectory());
    }

    @Test
    public void testGetPdfFiles() {
        // 테스트용 PDF 파일 생성
        File testFile = new File(testOutputDir, "test.pdf");
        try {
            testFile.createNewFile();

            List<File> pdfFiles = FileHandler.findResumeFiles(testOutputDir.getPath());
            assertFalse("PDF 파일 목록이 비어있지 않아야 함", pdfFiles.isEmpty());
            assertEquals("PDF 파일 수가 1이어야 함", 1, pdfFiles.size());
        } catch (IOException e) {
            fail("테스트 파일 생성 실패: " + e.getMessage());
        }
    }

    @After
    public void tearDown() {
        // 테스트 파일 및 디렉토리 정리
        if (testOutputDir.exists()) {
            File[] files = testOutputDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            testOutputDir.delete();
        }
    }
}