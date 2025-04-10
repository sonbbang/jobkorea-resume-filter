package main.java;

import java.util.List;

public record ResumeInfo(String experienceYears, String gender, String age, String desiredSalary,
                         List<String> technicalSkills) {
}
