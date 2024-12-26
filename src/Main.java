import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main {
    public static void main(String[] args) {
        // 특정 폴더 경로 지정 (여기서는 예시로 C:\pdfs 폴더를 사용)
        String folderPath = "C:\\Users\\n3299\\Desktop\\NateOn개발 지원서";

        // 폴더 내의 모든 PDF 파일을 가져옴
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));

        if (listOfFiles != null && listOfFiles.length > 0) {
            for (File file : listOfFiles) {
                System.out.println("Processing file: " + file.getName());

                try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(file))) {
                    // PDFTextStripper를 사용하여 전체 텍스트 추출
                    PDFTextStripper pdfStripper = new PDFTextStripper();

                    // 각 페이지의 텍스트를 추출하여 출력
                    int totalPages = document.getNumberOfPages();
                    StringBuilder fullText = new StringBuilder();

                    for (int i = 0; i < totalPages; i++) {
                        pdfStripper.setStartPage(i + 1);  // 페이지 번호는 1부터 시작
                        pdfStripper.setEndPage(i + 1);

                        // 해당 페이지에서 텍스트 추출
                        String pageText = pdfStripper.getText(document);
                        fullText.append(pageText);

                        //System.out.println("Page " + (i + 1) + " Content:");
                        System.out.println(pageText);
                        //System.out.println("--------------------------------------------------");
                    }


                    // 텍스트에서 "총 x년" 패턴을 찾아서 숫자 추출 => 연차
                    String pattern = "총\\s*(\\d+)\\s*년"; // "총 x년" 형태의 정규식 패턴
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(fullText.toString());

                    if (m.find()) {
                        String years = m.group(1); // "x"에 해당하는 숫자 부분
                        System.out.println("연차: " + years);

                        // 정규 표현식: 성별, 출생 연도, 나이 추출
                        String genderAgePattern = "(남|여)\\s*(\\d{4})년\\s*\\(만\\s*(\\d+)세\\)";  // 성별, 출생 연도, 나이

                        // 정규식 패턴 컴파일
                        Pattern r3 = Pattern.compile(genderAgePattern);
                        Matcher m3 = r3.matcher(fullText);

                        String gender = null;
                        String age = null;

                        // 일치하는 나이 추출하여 출력
                        while (m3.find()) {
                            gender = m3.group(1);  // 성별 (남/여)
                            age = m3.group(3);  // 나이 (33)

                            System.out.println("성별: " + gender);
                            System.out.println("나이: " + age + "세");
                        }

                        // "연봉"과 "만원" 사이에 아무 텍스트나 공백이 올 수 있도록 정규식
                        String salaryPattern = "연봉\\s*(.*?)\\s*만원"; // 연봉과 만원 사이의 텍스트

                        // 정규식 패턴 컴파일
                        Pattern r2 = Pattern.compile(salaryPattern);
                        Matcher m2 = r2.matcher(fullText);

                        String salary = null;
                        // 일치하는 모든 연봉 금액을 파싱하여 출력
                        if (m2.find()) {
                            // 추출된 연봉 금액
                            salary = m2.group(1);
                            System.out.println("Parsed salary: " + salary + "만원");
                        }

                        if (salary == null) {
                            // "연봉"과 "만원" 사이에 아무 텍스트나 공백이 올 수 있도록 정규식
                            String beforeSalaryPattern = "(.*?)\\s*만원연봉"; // 연봉과 만원 사이의 텍스트

                            // 정규식 패턴 컴파일
                            Pattern r5 = Pattern.compile(beforeSalaryPattern);
                            Matcher m5 = r5.matcher(fullText);

                            // 일치하는 모든 연봉 금액을 파싱하여 출력
                            if (m5.find()) {
                                // 추출된 연봉 금액
                                salary = m5.group(1);
                                System.out.println("Parsed before salary: " + salary + "만원");
                            }
                        }

                        // 키워드 추출
                        ArrayList<String> etcs = new ArrayList<>();
                        if (fullText.indexOf("JPA") > 0 || fullText.indexOf("jpa") > 0) {
                            etcs.add("JPA");
                        }

                        if (fullText.indexOf("PHP") > 0 || fullText.indexOf("Php") > 0 || fullText.indexOf("php") > 0) {
                            etcs.add("PHP");
                        }

                        if (fullText.indexOf("AWS") > 0 || fullText.indexOf("aws") > 0) {
                            etcs.add("AWS");
                        }

                        if (fullText.indexOf("정보통신학") > 0 || fullText.indexOf("전산계산학") > 0 || fullText.indexOf("전산학") > 0 || fullText.indexOf("소프트웨어학") > 0 || fullText.indexOf("소프트웨어공학") > 0 || fullText.indexOf("소프트웨어전공") > 0 || fullText.indexOf("컴퓨터공학") > 0 || fullText.indexOf("컴퓨터과학") > 0) {
                            etcs.add("컴공");
                        }

                        if (fullText.indexOf("채팅") > 0) {
                            etcs.add("채팅");
                        }

                        if (fullText.indexOf(" 팀장") > 0 || fullText.indexOf(" 팀장,") > 0) {
                            etcs.add("팀장");
                        }

                        // "프리랜서"의 여러 번 출현 체크
                        int index = fullText.indexOf("프리랜서");
                        int count = 0;

                        // "프리랜서"가 두 번 이상 등장하는지 체크
                        while (index != -1) {
                            count++;
                            if (count == 2) { // 두 번째 등장 시에만 추가
                                etcs.add("프리랜서");
                            }
                            // "프리랜서" 이후에 다시 검색
                            index = fullText.indexOf("프리랜서", index + 1);
                        }

                        System.out.println(String.join(",", etcs));

                        // 파일명을 변경 (x_ 붙이기)
                        String newFileName = years + "년차_" + gender + "_" + age + "세_" + (salary == null ? "" : salary + "만원_" ) + (etcs.size() == 0 ? "" : String.join(",", etcs) + "_") + file.getName();
                        File newFile = new File(file.getParent(), newFileName);

                        // 파일명 변경
                        boolean renamed = file.renameTo(newFile);

                        if (renamed) {
                            System.out.println("File renamed to: " + newFile.getName());
                        } else {
                            System.out.println("Failed to rename file: " + file.getName());
                        }

                    } else {
                        System.out.println("No '총 x년' pattern found in: " + file.getName());
                    }

                } catch (IOException e) {
                    System.err.println("Error processing file: " + file.getName());
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("No PDF files found in the specified directory.");
        }
    }

}
