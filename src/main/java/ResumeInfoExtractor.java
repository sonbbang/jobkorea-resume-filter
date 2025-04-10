package main.java;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResumeInfoExtractor {

    public static ResumeInfo extractResumeInfo(String fullText) {
        String years = PDFTextExtractor.extractFirstMatch(fullText, PDFConfig.getYearsPattern(), 1);
        String[] genderAndAge = extractGenderAndAge(fullText);
        String salary = extractSalary(fullText);
        List<String> keywords = extractKeywords(fullText);

        return new ResumeInfo(years, genderAndAge[0], genderAndAge[1], salary, keywords);
    }

    private static String extractFirstMatch(String text, String pattern, int group) {
        Matcher matcher = Pattern.compile(pattern).matcher(text);
        return matcher.find() ? matcher.group(group) : null;
    }

    private static String[] extractGenderAndAge(String text) {
        Matcher matcher = Pattern.compile(PDFConfig.getGenderAgePattern()).matcher(text);
        return matcher.find() ? new String[]{matcher.group(1), matcher.group(3)} : new String[]{null, null};
    }

    private static String extractSalary(String text) {
        String salary = extractFirstMatch(text, PDFConfig.getSalaryPattern(), 1);
        return salary != null ? salary : extractFirstMatch(text, PDFConfig.getBeforeSalaryPattern(), 1);
    }

    private static ArrayList<String> extractKeywords(String text) {
        ArrayList<String> keywords = new ArrayList<>();
        String lowerCaseText = text.toLowerCase();

        for (String keyword : PDFConfig.getKeywords()) {
            if (lowerCaseText.contains(keyword.toLowerCase())) {
                keywords.add(keyword);
            }
        }

        for (String term : PDFConfig.getComputerScienceTerms()) {
            if (text.contains(term)) {
                keywords.add("컴공");
                break;
            }
        }

        if (countOccurrences(text, "프리랜서") >= 2) {
            keywords.add("프리랜서");
        }

        return keywords;
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

}
