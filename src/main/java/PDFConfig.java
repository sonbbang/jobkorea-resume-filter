package main.java;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class PDFConfig {

    // 정규식 패턴을 상수로 정의
    private static final String EXPERIENCE_YEARS_PATTERN = "총\\s*(\\d+)\\s*년";
    private static final String GENDER_AND_AGE_PATTERN = "(남|여)\\s*(\\d{4})년\\s*\\(만\\s*(\\d+)세\\)";
    private static final String DESIRED_SALARY_PATTERN = "연봉\\s*(.*?)\\s*만원";
    private static final String BEFORE_SALARY_PATTERN = "(.*?)\\s*만원연봉";

    // 정규식 패턴 관련 메소드
    public static String getYearsPattern() {
        return EXPERIENCE_YEARS_PATTERN;
    }

    public static String getGenderAgePattern() {
        return GENDER_AND_AGE_PATTERN;
    }

    public static String getSalaryPattern() {
        return DESIRED_SALARY_PATTERN;
    }

    public static String getBeforeSalaryPattern() {
        return BEFORE_SALARY_PATTERN;
    }

    private static final Properties properties = new Properties();

    static {
        try (InputStreamReader reader = new InputStreamReader(
                PDFConfig.class.getClassLoader()
                        .getResourceAsStream("resources/config.properties"),
                StandardCharsets.UTF_8)) {

            properties.load(reader);  // UTF-8로 properties 파일 로드
        } catch (IOException e) {
            System.err.println("설정 파일을 불러오는 중 오류 발생: " + e.getMessage());
        }
    }

    // 폴더 설정 관련
    public static String getInputFolder() {
        return properties.getProperty("input.folder");
    }

    public static String getOutputFolder() {
        return properties.getProperty("output.folder", "out");
    }

    // 키워드 관련
    public static List<String> getKeywords() {
        String keywordsStr = properties.getProperty("keywords", "");
        return Arrays.asList(keywordsStr.split(","));
    }

    public static List<String> getComputerScienceTerms() {
        String termsStr = properties.getProperty("computer.science.terms", "");
        return Arrays.asList(termsStr.split(","));
    }

}