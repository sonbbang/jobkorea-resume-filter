package job;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatGPTService {

    private final String apiKey;
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    public ChatGPTService(String apiKey) {
        this.apiKey = apiKey;
    }

    public Map<String, String> getScoreAndReason(String resumeText, String jdText) throws IOException {
        String prompt = "다음은 지원자의 이력서와 채용 공고(JD)입니다. JD를 기준으로 이력서의 적합도를 0점에서 100점 사이의 점수로 평가하고, 평가에 대한 핵심 근거를 20자 이내로 작성해주세요. " +
                "응답은 반드시 다음 JSON 형식으로만 반환해주세요: {\"score\": [점수], \"reason\": \"[평가 근거]\"}\n\n" +
                "--- 이력서 ---\n" + resumeText + "\n\n" +
                "--- 채용 공고 (JD) ---\n" + jdText;

        URL url = new URL(API_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("Authorization", "Bearer " + apiKey);
        con.setDoOutput(true);

        String jsonTemplate = "{\"model\": \"gpt-3.5-turbo-1106\", \"response_format\": { \"type\": \"json_object\" }, \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}], \"max_tokens\": 150}";
        String escapedPrompt = escapeJson(prompt);
        String jsonInputString = String.format(jsonTemplate, escapedPrompt);
        
        System.out.println("[DEBUG] ChatGPT Request Body: " + jsonInputString);

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = con.getResponseCode();
        StringBuilder response = new StringBuilder();
        InputStream inputStream;

        if (responseCode == HttpURLConnection.HTTP_OK) {
            inputStream = con.getInputStream();
        } else {
            System.err.println("[ERROR] ChatGPT request failed with response code: " + responseCode);
            inputStream = con.getErrorStream();
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            System.err.println("[ERROR] ChatGPT Error Response: " + response.toString());
            Map<String, String> errorResult = new HashMap<>();
            errorResult.put("score", "0");
            errorResult.put("reason", "API Error: " + responseCode);
            return errorResult;
        }
        
        System.out.println("[DEBUG] ChatGPT Response Body: " + response.toString());
        return parseScoreAndReason(response.toString());
    }

    private String escapeJson(String text) {
        if (text == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '/': sb.append("\\/"); break;
                default:
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u" + t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private Map<String, String> parseScoreAndReason(String response) {
        Map<String, String> result = new HashMap<>();
        result.put("score", "0");
        result.put("reason", "N/A");

        try {
            // 1. "content" 필드의 값(이스케이프된 JSON 문자열)을 추출
            String contentKey = "\"content\": \"";
            int contentValueStart = response.indexOf(contentKey);
            if (contentValueStart == -1) {
                System.err.println("[ERROR] 'content' key not found in response.");
                return result;
            }
            contentValueStart += contentKey.length();

            // 2. content 시작점부터, 이스케이프되지 않은 첫 번째 큰따옴표를 찾아 content의 끝으로 간주
            int contentValueEnd = -1;
            for (int i = contentValueStart; i < response.length(); i++) {
                if (response.charAt(i) == '"' && response.charAt(i - 1) != '\\') {
                    contentValueEnd = i;
                    break;
                }
            }

            if (contentValueEnd == -1) {
                System.err.println("[ERROR] Could not find the end of the 'content' value.");
                return result;
            }

            String escapedContentJson = response.substring(contentValueStart, contentValueEnd);
            
            // 3. 이스케이프된 문자열을 깨끗한 JSON으로 복원
            String contentJson = escapedContentJson.replace("\\\"", "\"").replace("\\\\", "\\");
            System.out.println("[DEBUG] Cleaned Content for Parsing: " + contentJson);

            // 4. 깨끗한 JSON에서 score와 reason 추출
            Pattern scorePattern = Pattern.compile("\"score\":\\s*(\\d+)");
            Matcher scoreMatcher = scorePattern.matcher(contentJson);
            if (scoreMatcher.find()) {
                result.put("score", scoreMatcher.group(1));
                System.out.println("[DEBUG] Score Parsing Success: " + result.get("score"));
            } else {
                System.err.println("[ERROR] Score Parsing Failed from content: " + contentJson);
            }

            Pattern reasonPattern = Pattern.compile("\"reason\":\\s*\"(.*?)\"");
            Matcher reasonMatcher = reasonPattern.matcher(contentJson);
            if (reasonMatcher.find()) {
                result.put("reason", reasonMatcher.group(1));
                System.out.println("[DEBUG] Reason Parsing Success: " + result.get("reason"));
            } else {
                System.err.println("[ERROR] Reason Parsing Failed from content: " + contentJson);
            }

        } catch (Exception e) {
            System.err.println("Error parsing ChatGPT JSON response: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
}
