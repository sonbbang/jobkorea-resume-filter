package job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import kr.co.shineware.nlp.komoran.model.Token;

public class JdScorer {

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
        "및", "등", "에", "의", "이", "을", "를", "가", "은", "는", "로", "으로",
        "에서", "에게", "이상", "관련", "대한", "통한", "보유", "경험", "운영", "관리",
        "구축", "서비스", "기반", "능력", "역량", "활용", "대응", "개발", "처리",
        "환경", "학력무관", "경력", "있는", "있으신", "하신", "위한", "통해", "대해",
        "업무", "수행", "담당", "가능", "필요", "우수한", "원활한", "이해", "전반",
        "분야", "시스템", "솔루션", "플랫폼", "다양한", "전문", "이력",
        "자격요건", "우대사항", "또는", "커뮤니케이션", "등에", "외부", "유관부서", "협력사와의",
        // 섹션 헤더 분리 형태 (Komoran이 명사로 추출하는 것 방지)
        "자격", "요건", "우대", "사항", "담당", "트러블", "슈팅"
    ));

    public static List<String> extractRequiredKeywords(String jdText, List<String> ignored) {
        String section = extractSection(jdText,
                new String[]{"자격요건", "필수요건", "지원자격", "자격조건", "지원 자격"},
                new String[]{"우대사항", "우대조건", "우대자", "기타", "복리후생"});
        if (section.isEmpty()) section = jdText;
        return tokenize(section);
    }

    public static List<String> extractPreferredKeywords(String jdText, List<String> ignored) {
        String section = extractSection(jdText,
                new String[]{"우대사항", "우대조건", "우대자"},
                new String[]{"기타", "복리후생", "마감일", "전형절차", "제출서류"});
        return tokenize(section);
    }

    // 이력서 전체 텍스트와 JD 추출 단어를 매칭해 0-100점 반환
    // 영문/숫자 포함 기술스택 토큰은 가중치 3배, 순수 한국어는 1배
    // 이력서 내 등장 횟수(최대 3회)를 선형 반영
    public static String score(String resumeFullText, List<String> requiredTerms, List<String> preferredTerms) {
        List<String> reqUniq  = deduplicate(requiredTerms);
        List<String> prefUniq = deduplicate(preferredTerms);

        double maxScore = 0, earned = 0;
        String lowerResume = resumeFullText.toLowerCase();

        for (String term : reqUniq) {
            int w = termWeight(term) * 2;
            maxScore += w;
            earned   += w * freqFactor(lowerResume, term.toLowerCase());
        }
        for (String term : prefUniq) {
            int w = termWeight(term);
            maxScore += w;
            earned   += w * freqFactor(lowerResume, term.toLowerCase());
        }

        if (maxScore == 0) return "-";
        return String.valueOf(Math.min(100, (int)(earned / maxScore * 100)));
    }

    // 영문자·숫자 포함 여부로 기술스택 판단 → 가중치 3, 순수 한국어 → 1
    private static int termWeight(String term) {
        for (char c : term.toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) return 3;
        }
        return 1;
    }

    // 이력서 내 등장 횟수 → [0, 1] 선형 보정 (최대 3회)
    private static double freqFactor(String lowerResume, String lowerTerm) {
        int count = 0, idx = 0;
        while ((idx = lowerResume.indexOf(lowerTerm, idx)) != -1) {
            count++;
            idx += lowerTerm.length();
            if (count >= 3) break;
        }
        return count / 3.0;
    }

    // 중복 제거 (순서 유지)
    private static List<String> deduplicate(List<String> terms) {
        return new ArrayList<>(new LinkedHashSet<>(terms));
    }

    // JD 텍스트가 있을 때만 초기화 (lazy) — 초기화 1~2초 소요
    private static volatile Komoran komoran;
    private static Komoran getKomoran() {
        if (komoran == null) {
            synchronized (JdScorer.class) {
                if (komoran == null) komoran = new Komoran(DEFAULT_MODEL.LIGHT);
            }
        }
        return komoran;
    }

    // 하이브리드 토크나이저:
    // Phase 1 - 영문/숫자 포함 토큰(k8s, docker, AWS 등)은 regex split으로 추출
    // Phase 2 - 순수 한국어 명사는 Komoran 형태소 분석으로 추출 (조사 자동 제거)
    private static List<String> tokenize(String text) {
        List<String> terms = new ArrayList<>();
        if (text == null || text.isBlank()) return terms;

        // Phase 1: 영문/숫자 포함 토큰 → Komoran이 k8s 등을 오분석하므로 regex로 직접 추출
        for (String t : text.split("[\\s,./()·\\-·•\\[\\]\"']+")) {
            t = t.trim().replaceAll("[()\\[\\]]", "");
            if (t.length() < 2) continue;
            if (t.matches("\\d+")) continue;
            if (!t.matches(".*[a-zA-Z0-9].*")) continue;  // 순수 한국어는 Phase 2로
            if (STOPWORDS.contains(t) || STOPWORDS.contains(t.toLowerCase())) continue;
            terms.add(t);
        }

        // Phase 2: 순수 한국어 명사(NNG/NNP) → Komoran으로 추출 (조사·어미 자동 제거)
        KomoranResult result = getKomoran().analyze(text);
        for (Token token : result.getTokenList()) {
            String pos = token.getPos();
            if (!pos.equals("NNG") && !pos.equals("NNP")) continue;
            String word = token.getMorph().trim();
            if (word.length() < 2) continue;
            if (word.matches("\\d+")) continue;
            if (STOPWORDS.contains(word)) continue;
            if (word.matches(".*[a-zA-Z0-9].*")) continue;
            // "Xen가상화" 같은 영한 복합 토큰 내부 분리 방지:
            // 직전 문자가 공백/구두점이 아니면 복합 토큰의 일부로 간주 → skip
            int begin = token.getBeginIndex();
            if (begin > 0) {
                char prev = text.charAt(begin - 1);
                if (!Character.isWhitespace(prev) && " \t\n\r,./()[]·-•\"'".indexOf(prev) == -1) continue;
            }
            terms.add(word);
        }
        return new ArrayList<>(new LinkedHashSet<>(terms));
    }

    private static String extractSection(String text, String[] startMarkers, String[] endMarkers) {
        String lower = text.toLowerCase();
        int start = -1;
        for (String m : startMarkers) {
            int idx = lower.indexOf(m.toLowerCase());
            if (idx != -1 && (start == -1 || idx < start)) start = idx;
        }
        if (start == -1) return "";

        int end = text.length();
        for (String m : endMarkers) {
            int idx = lower.indexOf(m.toLowerCase(), start + 1);
            if (idx != -1 && idx < end) end = idx;
        }
        return text.substring(start, end);
    }
}
