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
        List<String> technicalSkills,
        String currentSalary,
        String isEmployed,
        String applicationPath,
        String jdMatchScore,
        String jobChangeCount,
        String maxGapMonths,
        String avgTenureMonths,
        String jobChangeFrequency
) {
}
