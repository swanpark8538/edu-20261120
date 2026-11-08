package egovframework.example.cmm.service;

import java.util.List;
import java.util.Map;

/**
 * 파일정보 관리를 위한 서비스 인터페이스.
 *
 * eGovFrame 표준 첨부파일 패턴(`atchFileId` 그룹 키 + COMTNFILE/COMTNFILEDETAIL 2테이블) 의
 * 메타 CRUD 를 책임진다. 디스크 파일 IO 는 `EgovFileMngUtil` 에서 별도로 다룬다.
 *
 * @see EgovFileMngUtil
 * @see FileVO
 */
public interface EgovFileMngService {

	/** 첨부파일 그룹의 상세 목록을 조회한다. */
	List<FileVO> selectFileInfs(FileVO fvo) throws Exception;

	/** 단일 파일의 마스터 + 상세를 등록한다. 반환값은 atchFileId. */
	String insertFileInf(FileVO fvo) throws Exception;

	/**
	 * 여러 파일의 마스터 1행 + 상세 N행을 등록한다.
	 * 입력 fvoList 의 모든 FileVO 는 동일 atchFileId 를 공유해야 한다.
	 * 반환값이 그 atchFileId — 도메인 VO 에 세팅해서 INSERT 한다.
	 */
	String insertFileInfs(List<FileVO> fvoList) throws Exception;

	/**
	 * 기존 그룹에 새 파일을 추가한다. 호출 전 `getMaxFileSN` 으로 다음 FILE_SN 을 받아
	 * `EgovFileMngUtil.parseFileInf(..., cnt, atchFileId, ...)` 결과를 넘긴다.
	 */
	void updateFileInfs(List<FileVO> fvoList) throws Exception;

	/** 여러 파일을 그룹·연번 단위로 삭제. */
	void deleteFileInfs(List<FileVO> fvoList) throws Exception;

	/** 단일 파일(atchFileId + fileSn) 삭제. */
	void deleteFileInf(FileVO fvo) throws Exception;

	/** 단일 파일의 상세를 조회. */
	FileVO selectFileInf(FileVO fvo) throws Exception;

	/** 그룹의 다음 FILE_SN (= MAX+1) 을 반환. update 시 PK 충돌 방지용. */
	int getMaxFileSN(FileVO fvo) throws Exception;

	/**
	 * 그룹 전체를 논리 삭제(USE_AT='N'). 도메인 DELETE 와 함께 호출해 orphan 방지.
	 * 물리 삭제가 아니라 USE_AT 플래그를 끄는 방식 — 복구 가능, 감사 로그 친화적.
	 */
	void deleteAllFileInf(FileVO fvo) throws Exception;

	/** 파일명 검색 + 페이징. resultMap: `resultList`(List<FileVO>), `resultCnt`(String). */
	Map<String, Object> selectFileListByFileNm(FileVO fvo) throws Exception;

	/** 이미지 확장자(GIF/JPG/BMP/PNG) 만 필터한 그룹 목록. */
	List<FileVO> selectImageFileList(FileVO vo) throws Exception;
}
