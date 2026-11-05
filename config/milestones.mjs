// 마일스톤 정의 (채점기·집계·벤치가 공유). 순서 = 검사 순서.
export const MILESTONES = [
  ["B1", "기본", "빌드 성공 (mvn package)"],
  ["B2", "기본", "앱 기동 + 게시판 목록 화면 200"],
  ["F1", "기능", "등록(C): 게시글 등록 후 목록에 표시"],
  ["F2", "기능", "조회(R): 수정 화면에 게시글 내용 표시"],
  ["F3", "기능", "수정(U): 수정 내용이 목록에 반영"],
  ["F4", "기능", "삭제(D): 삭제 후 목록에서 제거"],
  ["F5", "기능", "업로드: 첨부 등록 후 수정 화면에 첨부 표시"],
  ["F6", "기능", "다운로드: 내용 일치 + Content-Disposition attachment"],
  ["F7", "기능", "업로드 보안: 실행 파일 차단 + 10MB 초과 거부"],
  ["Q1", "품질", "Config 클래스: Board 설정이 @Configuration 클래스에 정의"],
  ["Q2", "품질", "페이징: 10건 초과 시 페이지 분할"],
  ["Q3", "품질", "검색: searchKeyword 로 목록 필터링"],
  ["Q4", "품질", "트랜잭션: Board 서비스가 트랜잭션 관리 대상"],
  ["Q5", "품질", "ID 생성: 표준 ID Generation 서비스로 자동 채번"],
  ["Q6", "품질", "예외 처리 + 입력 검증 + 다국어(i18n)"],
  ["S1", "표준 준수", "표준프레임워크 적용률 (egov_check_compliance)"],
];
export const MILESTONE_IDS = MILESTONES.map(([id]) => id);
export const GROUPS = [...new Set(MILESTONES.map(([, g]) => g))];
