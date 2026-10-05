# SenseTouch 1.0.0 배포

동봉된 SenseTouch-1.0.0.apk는 전용 배포 키로 서명한 정식 설치 파일입니다.
GitHub Desktop으로 소스를 올리고 Releases에 APK를 첨부하는 순서는 [GITHUB-DESKTOP-GUIDE.md](GITHUB-DESKTOP-GUIDE.md)에 있습니다.
릴리스 태그와 제목은 각각 `v1.0.0`, `SenseTouch 1.0.0`을 사용하며 Pre-release는 체크하지 않습니다.

## 서명 키 보관

별도로 제공한 `SenseTouch-Release-Key-PRIVATE.zip`에 이번 APK의 서명 키와 비밀번호가 들어 있습니다.
이 파일은 본인만 보관하고 GitHub 저장소나 Releases에는 올리지 마세요. 분실하면 같은 앱의 후속 업데이트를 정상적으로 서명할 수 없습니다.
업데이트할 때 새 키를 만들지 말고 이번 키를 계속 사용합니다.

## GitHub Actions로 정식 APK 다시 만들기

저장소 Settings → Secrets and variables → Actions → New repository secret에 다음 값을 등록합니다.

| Secret | 값 |
| --- | --- |
| KEYSTORE_BASE64 | 제공된 sensetouch-release.jks 파일의 Base64 문자열 |
| STORE_PASSWORD | 비공개 보관 파일의 Store password |
| KEY_ALIAS | sensetouch |
| KEY_PASSWORD | 비공개 보관 파일의 Key password |

Windows PowerShell에서 키 파일을 Base64로 복사하려면 실제 파일 경로를 넣어 실행합니다.

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('C:\비공개경로\sensetouch-release.jks')) | Set-Clipboard
```

복사한 값을 KEYSTORE_BASE64 Secret에 붙여넣습니다.
이후 Actions → Build SenseTouch → Run workflow를 누릅니다.
검사 작업 성공 후 release 작업이 실행되고, SenseTouch-release Artifact에서 정식 서명 APK를 받을 수 있습니다.
서명 Secret이 없으면 정식 빌드는 실패합니다. 일반 push와 PR에서는 개발 빌드와 검사를 수행합니다.
Actions는 Releases를 자동으로 발행하지 않으므로 APK는 별도로 릴리스에 첨부합니다.

## 로컬 빌드

비공개 보관 파일의 `keystore.properties`와 `sensetouch-release.jks`를 프로젝트 루트에 두고 Java 17 및 Android SDK 34 환경에서 실행합니다.

```sh
./gradlew assembleRelease testDebugUnitTest lintRelease
```

Windows PowerShell에서는 `./gradlew.bat assembleRelease testDebugUnitTest lintRelease`를 사용합니다.
출력 APK는 `app/build/outputs/apk/release/app-release.apk`입니다.
서명 설정 없이 정식 APK를 패키징하려 하면 중단하도록 구성되어 있습니다.
키 파일과 keystore.properties는 .gitignore로 제외되지만, 커밋 목록에도 들어가지 않았는지 확인하세요.

## 다음 버전

앱에 표시할 버전은 app/build.gradle.kts의 versionName, Android 내부 업데이트 번호는 versionCode입니다.
현재 versionName은 1.0.0, versionCode는 10입니다. 내부 번호는 개발 버전보다 크게 유지했습니다.
다음 업데이트에서는 versionName과 versionCode를 각각 올리고 같은 키로 서명하세요.
이전 개발용 APK는 다른 서명으로 제작되어 이번 정식 버전과 업데이트 설치가 호환되지 않습니다.
