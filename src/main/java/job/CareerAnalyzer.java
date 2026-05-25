package job;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CareerAnalyzer {

    // 잡코리아: 2018.03 ~ 2021.06, 2010. 10 ~ 현재
    // 사람인:   2022.07\x002023.09 — PDFBox가 테이블 셀 간격을 null 바이트(U+0000)로 추출
    private static final Pattern DATE_RANGE = Pattern.compile(
        "(\\d{4})[.년\\-]\\s*(\\d{1,2})[월.]?(?:\\.\\d{1,2})?" +
        "(?:\\s*[~～\\-–—]\\s*|[\\s\\x00]{1,})" +
        "(?:(\\d{4})[.년\\-]\\s*(\\d{1,2})[월.]?(?:\\.\\d{1,2})?" +
        "|(현재|재직\\s*중|재직중|이직가능|퇴직))"
    );

    public static int currentAbsoluteMonth() {
        LocalDate now = LocalDate.now();
        return toAbsMonth(now.getYear(), now.getMonthValue());
    }

    private static int toAbsMonth(int year, int month) {
        return year * 12 + month;
    }

    // 학력·자격증·어학 줄에 포함된 날짜는 경력으로 오인하지 않도록 제외
    private static final String[] NON_CAREER_KEYWORDS = {
        "졸업", "재학", "수료", "대학교", "대학원", "고등학교", "중학교",
        "자격증", "토익", "토플", "ielts", "toeic", "학점", "gpa",
        "수상", "어학", "병역", "보충역"
    };

    public static List<int[]> parseCareerPeriods(String fullText) {
        List<int[]> periods = new ArrayList<>();
        for (String line : fullText.split("\\R")) {
            if (isNonCareerLine(line)) continue;
            Matcher m = DATE_RANGE.matcher(line);
            while (m.find()) {
                try {
                    int startYear  = Integer.parseInt(m.group(1));
                    int startMonth = Integer.parseInt(m.group(2));
                    int endAbs;
                    if (m.group(5) != null) {
                        endAbs = currentAbsoluteMonth();
                    } else {
                        int endYear  = Integer.parseInt(m.group(3));
                        int endMonth = Integer.parseInt(m.group(4));
                        endAbs = toAbsMonth(endYear, endMonth);
                    }
                    int startAbs = toAbsMonth(startYear, startMonth);
                    if (endAbs >= startAbs) {
                        periods.add(new int[]{startAbs, endAbs});
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        periods.sort(Comparator.comparingInt(p -> p[0]));
        return mergeOverlapping(periods);
    }

    private static List<int[]> mergeOverlapping(List<int[]> sorted) {
        if (sorted.isEmpty()) return sorted;
        List<int[]> merged = new ArrayList<>();
        int[] cur = sorted.get(0).clone();
        for (int i = 1; i < sorted.size(); i++) {
            int[] next = sorted.get(i);
            if (next[0] < cur[1]) {  // 엄격한 overlap: 동일 달 경계는 분리 유지
                cur[1] = Math.max(cur[1], next[1]);
            } else {
                merged.add(cur);
                cur = next.clone();
            }
        }
        merged.add(cur);
        return merged;
    }

    private static boolean isNonCareerLine(String line) {
        String lower = line.toLowerCase();
        for (String kw : NON_CAREER_KEYWORDS) {
            if (lower.contains(kw)) return true;
        }
        return false;
    }

    public static String jobChangeCount(List<int[]> periods) {
        if (periods.isEmpty()) return "-";
        return String.valueOf(periods.size() - 1);
    }

    public static String maxGapMonths(List<int[]> periods) {
        if (periods.size() < 2) return "-";
        int maxGap = 0;
        for (int i = 1; i < periods.size(); i++) {
            int gap = Math.max(0, periods.get(i)[0] - periods.get(i - 1)[1]);
            maxGap = Math.max(maxGap, gap);
        }
        return String.valueOf(maxGap);
    }

    public static String avgTenureMonths(List<int[]> periods) {
        if (periods.isEmpty()) return "-";
        int total = 0;
        for (int[] p : periods) total += (p[1] - p[0]);
        return String.valueOf(total / periods.size());
    }

    public static String jobChangeFrequency(List<int[]> periods, double totalYears) {
        if (periods.isEmpty() || totalYears <= 0) return "-";
        return String.format("%.2f", (periods.size() - 1) / totalYears);
    }
}
