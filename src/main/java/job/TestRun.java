package job;

import job.PDFConfig.ResumeFormatType;

public class TestRun {
    public static void main(String[] args) throws Exception {
        String folder = args[0];
        ResumeFormatType fmt = args.length > 1 && "saramin".equalsIgnoreCase(args[1])
            ? ResumeFormatType.SARAMIN : ResumeFormatType.JOBKOREA;
        String jdText = args.length > 2 ? args[2] : null;
        ResumeFilter.run(folder, fmt, jdText);
    }
}
