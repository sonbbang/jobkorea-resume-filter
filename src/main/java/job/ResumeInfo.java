package job;

import java.util.List;

public record ResumeInfo(
        String name,
        String experienceYears,
        String gender,
        String age,
        String desiredSalary,
        String education,
        String mainCareer,
        String score,
        String scoreReason,
        List<String> technicalSkills
) {
}
