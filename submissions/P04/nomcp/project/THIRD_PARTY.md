# 외부 자산

## KRDS HTML Component Kit 1.0.3

- 작성자: krds
- 프로젝트: https://github.com/KRDS-uiux/krds-uiux
- 공식 안내: https://www.krds.go.kr/html/site/outline/outline_03.html
- 원본 CSS: https://cdn.jsdelivr.net/npm/krds-uiux@1.0.3/resources/cdn/krds.min.css
- npm 메타데이터: https://cdn.jsdelivr.net/npm/krds-uiux@1.0.3/package.json
- npm 메타데이터의 라이선스 표기: ISC (`static/vendor/krds/package.json` 보존)
- 아이콘 원본: `https://www.krds.go.kr/resources/img/component/icon/` 아래 CSS 참조 SVG 75개

CSS와 아이콘을 `src/main/resources/static/vendor/krds/`에 포함했습니다. CSS의 공식 사이트 아이콘 URL을 같은 디렉터리 아래 `resources/` 상대 경로로 변경했습니다. 서비스는 외부 스크립트나 CDN을 호출하지 않습니다. 앱별 스타일은 `static/css/board.css`에 있습니다.
