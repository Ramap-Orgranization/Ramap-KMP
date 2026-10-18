---
authority: reference
status: active
last_verified: 2026-10-18
---

# 개발 참고

정책을 구현할 때 필요한 개발 지침의 진입점만 모읍니다. 클래스·함수·모듈 목록, SDK 버전, API 명세, 실행 결과는 소유 코드와 개발 자료에서 관리합니다.

## 개발 규칙의 소유자

| 관심사 | 소유 지침 |
| --- | --- |
| 계층 책임과 의존성 | `.agents/skills/ramap-architecture/SKILL.md` |
| 모듈·소스 세트·파일 배치 | `.agents/skills/ramap-module-structure/SKILL.md` |
| 상태·로딩·데이터 접근·공통 UI 패턴 | `.agents/skills/ramap-design-patterns/SKILL.md` |
| 이름·서식·테스트·보안·변경 기록 | `.agents/skills/ramap-coding-conventions/SKILL.md` |

이 지침이 있는 작업 환경에서는 해당 파일을 읽습니다. 위키에서 개발 규칙을 복제하거나 새 우선순위를 만들지 않습니다. [DEC-0002](decisions.md#dec-0002-현재-구현을-기준으로-개발-규칙-정합성-확정)의 mapper 소유권·analytics 의존성·모듈 배치 결정도 소유 지침에서 유지합니다.

## 구현을 찾는 범위

[도메인 코드](../../domain), [데이터 코드](../../data), [기능 코드](../../feature), [공통 기반](../../core), [서버 코드](../../supabase)에서 작업할 개념을 검색합니다. Gradle 모듈의 원천은 `settings.gradle.kts`이고 실행·조립 경계는 앱 소스에서 확인합니다. 위키에 전체 의존성 그래프나 현재 클래스 위치를 다시 작성하지 않습니다.

## 이미지 처리의 선택

기본 일괄 발급은 기존 열람 권한을 유지하면서 운영 복잡성을 줄이기 위한 선택입니다. [선택 이유](decisions.md#dec-0003-프로필리뷰-이미지-url-기본-배치-발급-채택)와 [이미지 접근 경계](backend.md#이미지-링크)를 따릅니다. 특정 측정의 성능 개선을 다른 화면·기기·부하나 Disk IO 개선으로 일반화하지 않습니다. 배치 크기·TTL·SDK 모델 대응은 구현 가까이에서 관리합니다.

## 검증

검사 명령과 범위는 `AGENTS.md`와 관련 소유 스킬을 따릅니다. 결과는 실행한 코드·환경·검사 범위를 구분해 PR이나 CI 결과에 적습니다. 문서의 링크 검사는 제품 규칙이나 서버 동작을 증명하지 않습니다.

이 위키와 검사기를 변경한 경우 아래 검사를 실행합니다.

```bash
python3 scripts/check_wiki.py
python3 -m unittest discover -s scripts/tests -v
```

운영 DB 변경, 실제 알림 발송, 외부 수집과 기기 테스트는 단위 테스트와 구분합니다. 위키 편집만으로 이런 실행이 승인되지는 않습니다.
