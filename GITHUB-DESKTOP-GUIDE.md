# GitHub Desktop으로 SenseTouch 공개하기

이 안내는 아직 GitHub에 SenseTouch 저장소를 만들지 않은 경우를 기준으로 합니다.
동봉된 APK는 전용 배포 키로 서명한 정식 1.0.0입니다. 비공개 서명 키 보관 파일은 GitHub에 올리지 마세요.

## 1. ZIP 압축 풀기

압축을 풀면 SenseTouch 폴더와 SenseTouch-1.0.0.apk, SHA256SUMS.txt가 나옵니다.
SenseTouch 폴더 안의 파일은 저장소에 올릴 소스이고, APK는 Releases에 첨부할 설치 파일입니다.

## 2. GitHub Desktop에서 새 저장소 만들기

GitHub Desktop에 로그인한 뒤 File → New repository를 선택합니다.

| 입력 항목 | 설정 |
| --- | --- |
| Name | SenseTouch |
| Description | 삼성 Galaxy의 지문 인식률 향상 화면을 간편하게 여는 앱 |
| Local path | 저장소를 보관할 위치. 예: 문서의 GitHub 폴더 |
| Initialize this repository with a README | 체크하지 않음 |
| Git ignore | None |
| License | None |

README, .gitignore, LICENSE가 프로젝트에 이미 있으므로 새로 생성하지 않습니다. License의 None은 기존 라이선스를 없앤다는 뜻이 아닙니다.
Create repository를 누르면 지정한 위치 아래 SenseTouch 폴더가 생깁니다.

## 3. 프로젝트 파일 복사하기

GitHub Desktop에서 Repository → Show in Explorer로 새 저장소 폴더를 엽니다.
압축에서 꺼낸 SenseTouch 폴더의 **내용물 전체**를 이곳에 복사합니다. SenseTouch 폴더 자체를 한 겹 더 넣지 마세요.

새 저장소 폴더를 열었을 때 README.md, LICENSE, app, gradle, build.gradle.kts가 바로 보여야 합니다.
.github 폴더와 .gitignore 파일도 함께 복사합니다. 기존 .git 폴더는 그대로 둡니다.

APK와 다운로드 ZIP은 이 폴더에 넣지 않아도 됩니다. 서명 키(.jks, .keystore)와 keystore.properties는 공개하지 마세요.

## 4. 커밋하고 공개하기

GitHub Desktop의 Changes에 파일 목록이 나타나는지 확인합니다.
Summary에 `Release SenseTouch 1.0.0`를 입력하고 Commit to main을 누릅니다. 브랜치 이름이 다르면 버튼의 이름도 다를 수 있습니다.

상단 Publish repository를 누른 뒤 이름을 SenseTouch로 확인합니다.
공개 저장소로 만들려면 Keep this code private의 체크를 해제하고 Publish repository를 누릅니다.

Repository → View on GitHub로 이동해 README와 소스가 보이는지 확인합니다.
이미 같은 이름의 GitHub 저장소가 있다면 새로 만들지 말고 File → Clone repository로 기존 저장소를 내려받은 뒤, 그 폴더에 내용을 반영하고 Commit → Push origin을 진행하세요.

## 5. 빌드 결과 확인하기

GitHub 저장소의 Actions → Build SenseTouch에서 자동 실행 결과를 확인합니다.
초록색 체크로 완료되면 개발 빌드와 자동 검사에 성공한 것입니다.
배포에는 동봉된 정식 APK를 사용하세요. 자동 검사에서 생성하는 debug APK는 배포용이 아닙니다. 정식 APK를 다시 만들려면 RELEASE-BUILD.md의 서명 설정 후 워크플로를 수동 실행하세요.

이 워크플로는 APK를 Artifacts에 보관하며 Releases를 자동으로 발행하지는 않습니다.

## 6. APK 배포하기

릴리스 작성은 브라우저의 GitHub 저장소에서 진행합니다.

1. Releases → Create a new release 또는 Draft a new release를 누릅니다.
2. Choose a tag에서 `v1.0.0`를 입력하고 새 태그를 만듭니다.
3. Target은 소스를 올린 브랜치(main)를 선택합니다.
4. 제목은 `SenseTouch 1.0.0`로 입력합니다.
5. 설명에는 주요 기능, 지원 조건, 첫 정식 배포임을 적습니다.
6. 첨부 영역에 `SenseTouch-1.0.0.apk`와 `SHA256SUMS.txt`를 넣습니다.
7. This is a pre-release는 체크하지 않습니다. Set as latest release를 선택합니다.
8. 내용을 확인한 뒤 Publish release를 누릅니다.

사용자는 Releases의 Assets에서 APK를 받으면 됩니다. GitHub가 자동으로 붙이는 Source code ZIP은 개발용 소스입니다.
README의 APK 다운로드 링크는 Releases 목록으로 연결됩니다.

## 다음 버전도 같은 키 사용하기

별도로 제공한 SenseTouch-Release-Key-PRIVATE.zip을 안전하게 보관하세요. APK 업데이트에는 같은 서명 키가 필요합니다. 이 파일과 안의 비밀번호는 저장소나 Releases에 올리지 마세요.

RELEASE-BUILD.md의 설명대로 기존 배포 키를 등록해 계속 사용합니다. 새로운 키를 다시 만들지 마세요.

## 다음 수정부터

같은 저장소 폴더에서 파일을 수정한 뒤 GitHub Desktop에서 Summary 입력 → Commit → Push origin 순서로 반영합니다.
새 APK는 새 버전 태그의 Release에 첨부합니다. 기존 버전의 소스와 APK가 서로 대응하도록 유지하세요.

## 참고

- GitHub Desktop: https://docs.github.com/en/desktop/overview/creating-your-first-repository-using-github-desktop
- Releases: https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository?tool=webui
