# Resume Filter

## 프로젝트 소개
PDF 형식의 잡코리아 이력서 파일을 자동으로 분류하고 관리하는 Java 애플리케이션입니다.

## 주요 기능
- PDF 이력서 파일 자동 처리
- 경력, 나이, 성별, 연봉 정보 추출
- 기술 스택 키워드 분석
- 병렬 처리 지원 (10개 이상의 파일 처리 시 자동 활성화)
- 설정 파일을 통한 커스터마이징

## 기술 스택
- Java
- Apache PDFBox (PDF 처리)
- JUnit (테스트)

## 프로젝트 구조
jobkorea-resume-filter/  
├── src/  
│ ├── main/  
│ │ └── java/  
│ │ ├── ResumeFilter.java # 메인 애플리케이션  
│ │ ├── ResumeConfig.java # 설정 관리  
│ │ ├── FileHandler.java # 파일 처리  
│ │ └── ResumeInfoExtractor.java # 정보 추출  
│ ├── test/  
│ │ └── java/  
│ │ └── ResumeFilterTest.java # 테스트 코드  
│ └── resources/  
│ └── config.properties # 설정 파일  
├── lib/ # 외부 라이브러리  
└── out/ # 처리된 이력서 파일  

## 설정 방법
1. `src/resources/config.properties` 파일에서 다음 설정 가능:
    - 입력/출력 폴더 경로
    - 검색 키워드
    - 정규식 패턴

## 실행 방법
1. Java 8 이상 설치
2. 프로젝트 빌드:
   ```bash
   javac -cp "lib/*" src/main/java/*.java
   ```
3. 프로그램 실행:
   ```bash
   java -cp "lib/*:src" ResumeFilter
   ```

## 출력 형식
처리된 파일명 형식: `{경력}년차_{성별}_{나이}세_{연봉}만원_{키워드}_원본파일명.pdf`  

예시: 5년차_남_32세_4000만원_JAVA,AWS,컴공_원본이력서.pdf

## 테스트
JUnit 테스트 실행:
```bash
javac -cp "lib/*;src" src/test/java/*.java
java -cp "lib/*;src;src/test/java" org.junit.runner.JUnitCore ResumeFilterTest
```

## 주의사항
- PDF 파일의 텍스트가 추출 가능한 형식이어야 합니다
- 대용량 파일 처리 시 충분한 메모리 할당이 필요할 수 있습니다

## 라이선스
이 프로젝트는 MIT 라이선스 하에 있습니다.

