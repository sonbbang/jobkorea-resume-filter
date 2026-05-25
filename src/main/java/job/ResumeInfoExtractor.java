package job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import job.PDFConfig.ResumeFormatType;

public class ResumeInfoExtractor {

    public static ResumeInfo extractResumeInfo(String fullText, String name, ResumeFormatType formatType,
                                               List<String> jdRequiredKw, List<String> jdPreferredKw) {
        String rawYears = PDFTextExtractor.extractFirstMatch(fullText, PDFConfig.getYearsPattern(), 1);

        String formattedYears;
        String mainCareer;

        if ("신입".equals(rawYears)) {
            formattedYears = "0";
            mainCareer = "신입";
        } else {
            formattedYears = formatExperienceYears(rawYears);
            mainCareer = extractMainCareer(fullText, formatType);
        }

        String[] genderAndAge = extractGenderAndAge(fullText);
        String currentSalary = extractCurrentSalary(fullText, formatType);
        String desiredSalary = extractDesiredSalary(fullText, formatType);
        String education = extractEducation(fullText, formatType);
        List<String> keywords = extractKeywords(fullText);
        String isEmployed = extractIsEmployed(fullText);
        String applicationPath = formatType == ResumeFormatType.SARAMIN ? "사람인" : "잡코리아";
        String jdMatchScore = JdScorer.score(fullText, jdRequiredKw, jdPreferredKw);

        List<int[]> periods = CareerAnalyzer.parseCareerPeriods(extractCareerSectionText(fullText, formatType));
        double totalYears;
        try { totalYears = Double.parseDouble(formattedYears); } catch (NumberFormatException e) { totalYears = 0; }

        String jobChangeCount     = CareerAnalyzer.jobChangeCount(periods);
        String maxGapMonths       = CareerAnalyzer.maxGapMonths(periods);
        String avgTenureMonths    = CareerAnalyzer.avgTenureMonths(periods);
        String jobChangeFrequency = CareerAnalyzer.jobChangeFrequency(periods, totalYears);

        return new ResumeInfo(name, formattedYears, genderAndAge[0], genderAndAge[1], desiredSalary, education, mainCareer, keywords, currentSalary, isEmployed, applicationPath, jdMatchScore, jobChangeCount, maxGapMonths, avgTenureMonths, jobChangeFrequency);
    }

    private static String formatExperienceYears(String rawExperience) {
        if (rawExperience == null) {
            return "0";
        }

        Pattern yearPattern = Pattern.compile("(\\d+)\\s*년");
        Pattern monthPattern = Pattern.compile("(\\d+)\\s*개월");

        Matcher yearMatcher = yearPattern.matcher(rawExperience);
        Matcher monthMatcher = monthPattern.matcher(rawExperience);

        int years = 0;
        int months = 0;

        if (yearMatcher.find()) {
            years = Integer.parseInt(yearMatcher.group(1));
        }

        if (monthMatcher.find()) {
            months = Integer.parseInt(monthMatcher.group(1));
        }

        if (years == 0 && months == 0) {
            return "0";
        }

        double totalYears = years + ((double) months / 10.0);
        return String.format("%.1f", totalYears);
    }

    private static String extractFirstMatch(String text, String pattern, int group) {
        Matcher matcher = Pattern.compile(pattern).matcher(text);
        return matcher.find() ? matcher.group(group) : null;
    }

    private static String[] extractGenderAndAge(String text) {
        Matcher matcher = Pattern.compile(PDFConfig.getGenderAgePattern()).matcher(text);
        return matcher.find() ? new String[]{matcher.group(1), matcher.group(3)} : new String[]{null, null};
    }


private static String tailTokenOrOriginal(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return trimmed;
        String[] parts = trimmed.split("\\s+");
        return parts.length > 1 ? parts[parts.length - 1] : parts[0];
    }

    private static String extractEducation(String text, ResumeFormatType formatType) {
        if (formatType == ResumeFormatType.SARAMIN) {
            String[] lines = text.split("\\R");
            for (int i = 1; i < lines.length; i++) {
                String currentLine = lines[i].trim();
                if (currentLine.contains("재학중") || currentLine.contains("졸업")) {
                    return lines[i-1].trim()
                            .replaceAll("\\s*\\d{4}[./]\\d{1,2}(~\\d{4}[./]\\d{1,2})?\\s*$", "")
                            .replaceAll("\\s+", " ")
                            .trim();
                }
            }
            return null; // 못 찾은 경우
        }

        // JOBKOREA 또는 기본 로직
        int eduHeaderIndex = text.indexOf("학력");
        if (eduHeaderIndex == -1) return null;

        String eduSection = text.substring(eduHeaderIndex + "학력".length());
        String[] lines = eduSection.split("\\R");
        
        if (formatType == ResumeFormatType.SARAMIN) {
            System.out.println("[DEBUG] Saramin Education - All lines in section: " + Arrays.toString(lines));
        }

        List<String> nonEmptyLines = new ArrayList<>();
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                nonEmptyLines.add(trimmedLine);
            }
        }

        if (nonEmptyLines.isEmpty()) {
            return null;
        }

        String firstEducationLine = nonEmptyLines.get(0);

        if (formatType == ResumeFormatType.SARAMIN) {
            String result = firstEducationLine.replaceAll("\\s+", " ").trim();
            return result;
        }

        // JOBKOREA 또는 기본 로직
        String cleanedLine = firstEducationLine;
        Pattern datePrefixPattern = Pattern.compile(
            "^(?:\\d{4}[.\\s년]*\\d{0,2}[월\\s]*\\s*(?:~|부터|까지)?\\s*(?:재학중|졸업|수료|휴학|중퇴|졸업예정|\\d{4}[.\\s년]*\\d{0,2}[월\\s]*)?)?\\s*[-~]?\\s*",
            Pattern.CASE_INSENSITIVE
        );
        cleanedLine = datePrefixPattern.matcher(cleanedLine).replaceAll("").trim();
        cleanedLine = cleanedLine.replaceAll("^~\\s*|\\s*~$", "").trim();
        cleanedLine = cleanedLine.replaceAll("^-\\s*|\\s*-$", "").trim();
        cleanedLine = cleanedLine.replaceAll("\\s+", " ").trim();

        return cleanedLine.isEmpty() ? null : cleanedLine;
    }

    private static String extractMainCareer(String text, ResumeFormatType formatType) {
        if (formatType == ResumeFormatType.SARAMIN) {
            String[] lines = text.split("\\R");
            for (int i = 1; i < lines.length; i++) {
                String currentLine = lines[i].trim();
                if (currentLine.matches("총\\s*\\d+\\s*년(\\s*\\d+\\s*개월)?")) {
                    return lines[i-1].trim()
                            .replaceAll("\\s*(재직\\s*중|이직가능|퇴사)\\s*$", "")
                            .replaceAll("\\s+", " ")
                            .trim();
                }
            }
            return null;
        }

        // JOBKOREA 또는 기본 로직
        int careerHeaderIndex = text.indexOf("경력", 20);
        if (careerHeaderIndex == -1) {
            careerHeaderIndex = text.indexOf("경력사항", 20);
        }
        if (careerHeaderIndex == -1) return null;

        int endOfCareerSection = text.length();
        String[] nextHeaders = {"학력", "자격증", "수상", "교육", "프로젝트", "인턴·대외활동 / 해외경험"};
        for (String header : nextHeaders) {
            int nextHeaderIndex = text.indexOf(header, careerHeaderIndex + 5);
            if (nextHeaderIndex != -1 && nextHeaderIndex < endOfCareerSection) {
                endOfCareerSection = nextHeaderIndex;
            }
        }

        String careerSection = text.substring(careerHeaderIndex, endOfCareerSection);
        String[] lines = careerSection.split("\\R");
        List<String> careerLines = new ArrayList<>();
        int linesCollected = 0;
        
        Pattern skipLine = Pattern.compile("재직\\s*중|이직가능|퇴사|총\\s*\\d+\\s*년|인턴|대외활동|해외경험|접수번호|포지션\\s*:");
        for (int i = 1; i < lines.length && linesCollected < 2; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty() && !skipLine.matcher(line).find()) {
                careerLines.add(line.replaceAll("\\s+", " "));
                linesCollected++;
            }
        }
        
        return careerLines.isEmpty() ? null : String.join(" ", careerLines);
    }

    private static String extractCurrentSalary(String text, ResumeFormatType formatType) {
        if (formatType == ResumeFormatType.SARAMIN) {
            // 1차: "직전 연봉 : N,NNN 만원"
            String val = extractFirstMatch(text, PDFConfig.getSaraminCurrentSalaryPattern(), 1);
            if (val != null) return val.replace(",", "");
            // 2차 fallback: "연봉 N,NNN만원" (경력 내 이전 연봉)
            val = extractFirstMatch(text, PDFConfig.getSaraminSalaryPattern(), 1);
            return val != null ? val.replace(",", "") : null;
        }
        // JOBKOREA: 기존 salary 추출 로직 → 최종연봉으로 사용
        String salary = extractFirstMatch(text, PDFConfig.getJobkoreaSalaryPattern(), 1);
        salary = tailTokenOrOriginal(salary);
        return salary != null ? salary : extractFirstMatch(text, PDFConfig.getBeforeSalaryPattern(), 1);
    }

    private static String extractDesiredSalary(String text, ResumeFormatType formatType) {
        if (formatType == ResumeFormatType.JOBKOREA) {
            int idx = text.indexOf("희망근무조건");
            if (idx == -1) return "";
            String section = text.substring(idx, Math.min(idx + 500, text.length()));
            Matcher m = Pattern.compile("희망연봉\\s+([^\n\r]+)").matcher(section);
            return m.find() ? m.group(1).trim() : "";
        }
        // SARAMIN: "6,000~7,000만원" 범위 형태, 없으면 "회사내규에 따름"
        Matcher m = Pattern.compile(PDFConfig.getSaraminDesiredSalaryPattern()).matcher(text);
        return m.find() ? m.group(1).trim() + "만원" : "회사내규에 따름";
    }

    private static String extractIsEmployed(String text) {
        Matcher m = Pattern.compile(PDFConfig.getIsEmployedPattern()).matcher(text);
        return m.find() ? m.group(1).replaceAll("\\s+", "") : "퇴사";
    }

    private static ArrayList<String> extractKeywords(String text) {
        ArrayList<String> keywords = new ArrayList<>();
        String lowerCaseText = text.toLowerCase();

        for (String keyword : PDFConfig.getKeywords()) {
            if (lowerCaseText.contains(keyword.toLowerCase())) {
                keywords.add(keyword);
            }
        }

        if (countOccurrences(text, "프리랜서") >= 2) {
            keywords.add("프리랜서");
        }

        return keywords;
    }

    private static String extractCareerSectionText(String text, ResumeFormatType formatType) {
        if (formatType == ResumeFormatType.SARAMIN) {
            int end = text.indexOf("경력기술서");
            return end == -1 ? text : text.substring(0, end);
        }
        // 잡코리아 PDF 앞 ~150자는 목차(nav) 섹션 — 200자 이후에서 실제 경력 헤더를 찾음
        int start = text.indexOf("경력사항");
        if (start == -1 || start < 200) {
            start = text.indexOf("경력", 200);
        }
        if (start == -1) return text;

        int end = text.length();
        // "프로젝트"·"학력"·"자격증" 등은 경력 내 프로젝트 설명에 수십 번 등장 → end marker 제외
        // "인턴·대외활동" 은 부분 문자열로 검색해 " / 해외경험" 유무 무관하게 잡음
        for (String marker : new String[]{"교육", "인턴·대외활동"}) {
            int idx = text.indexOf(marker, start + 3);
            if (idx != -1 && idx < end) end = idx;
        }
        return text.substring(start, end);
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

    public static String maskPersonalInfo(String text, String name) {
        String maskedText = text;

        String combinedPattern = "[\\p{IsHangul}]{2,4}\\s*\\(?\\s*(남|여)\\s*\\)?\\s*[/]?\\s*\\d{4}년?";
        maskedText = maskedText.replaceAll(combinedPattern, "[개인정보 삭제]");

        if (name != null && !name.trim().isEmpty() && name.trim().length() >= 2) {
            maskedText = maskedText.replaceAll(Pattern.quote(name.trim()), "[이름 삭제]");
        }

        maskedText = maskedText.replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}", "[이메일 삭제]");
        
        // 수정된 휴대폰 번호 정규식
        maskedText = maskedText.replaceAll("01[016789][-\\s]?\\d{3,4}[-\\s]?\\d{4}", "[전화번호 삭제]");

        maskedText = maskedText.replaceAll("\\d{4}년\\s*\\d{1,2}월\\s*\\d{1,2}일", "[생년월일 삭제]");
        maskedText = maskedText.replaceAll("\\d{4}[-./]\\s*\\d{1,2}[-./]\\s*\\d{1,2}", "[생년월일 삭제]");
        maskedText = maskedText.replaceAll("만\\s*\\d{1,2}세", "[나이 삭제]");

        return maskedText;
    }
}
