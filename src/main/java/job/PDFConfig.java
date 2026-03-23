package job;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class PDFConfig {

    public enum ResumeFormatType {
        JOBKOREA,
        SARAMIN
    }

	// 정규식 패턴을 상수로 정의
	private static final String EXPERIENCE_YEARS_PATTERN = "(총\\s*\\d+\\s*년(?:\\s*\\d+\\s*개월)?|신입)";
	private static final String GENDER_AND_AGE_PATTERN = "(남|여)\\s*[,/]?\\s*(\\d{4})\\s*(?:년(?:생)?)?\\s*\\(\\s*(?:만\\s*)?(\\d+)\\s*세\\s*\\)";
	private static final String JOBKOREA_SALARY_PATTERN = "희망연봉\\s*(.*?)\\s*만원";
    private static final String SARAMIN_SALARY_PATTERN = "연봉\\s*(\\d+,?\\d+)\\s*만원";
	private static final String BEFORE_SALARY_PATTERN = "(.*?)\\s*만원연봉";

	// 정규식 패턴 관련 메소드
	public static String getYearsPattern() {
		return EXPERIENCE_YEARS_PATTERN;
	}

	public static String getGenderAgePattern() {
		return GENDER_AND_AGE_PATTERN;
	}

	public static String getJobkoreaSalaryPattern() {
		return JOBKOREA_SALARY_PATTERN;
	}

    public static String getSaraminSalaryPattern() {
        return SARAMIN_SALARY_PATTERN;
    }

	public static String getBeforeSalaryPattern() {
		return BEFORE_SALARY_PATTERN;
	}

	private static final Properties properties = new Properties();

	public static String getOutputFolder() {
		return properties.getProperty("output.folder", "out");
	}

	public static void setOutputFolder(String folder) {
		properties.setProperty("output.folder", folder);
	}

	public static void setKeywords(String keywords) {
		properties.setProperty("keywords", keywords);
	}


    public static ResumeFormatType getResumeFormatType() {
        String type = properties.getProperty("resume.format.type", "jobkorea");
        try {
            return ResumeFormatType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResumeFormatType.JOBKOREA;
        }
    }

    public static String getOpenApiKey() {
        return properties.getProperty("openai.api.key");
    }

    public static boolean getChatgptEnabled() {
        return "Y".equalsIgnoreCase(properties.getProperty("chatgpt.enabled", "N"));
    }

    public static String getStructuredJd() {
        StringBuilder jdBuilder = new StringBuilder();
        jdBuilder.append("직무: ").append(properties.getProperty("job.position", "")).append("\n\n");
        jdBuilder.append("담당 예정 업무:\n").append(properties.getProperty("job.responsibilities", "")).append("\n\n");
        jdBuilder.append("지원 자격 요건:\n").append(properties.getProperty("job.requirements", "")).append("\n\n");
        jdBuilder.append("우대사항:\n").append(properties.getProperty("job.preferred", ""));

        String custom = properties.getProperty("job.custom");
        if (custom != null && !custom.trim().isEmpty()) {
            jdBuilder.append("\n\n적합도(score) 산정 공식:\n").append(custom);
        }

        return jdBuilder.toString();
    }

	// 키워드 관련
	public static List<String> getKeywords() {
		String keywordsStr = properties.getProperty("keywords", "");
		return Arrays.asList(keywordsStr.split(","));
	}

}
