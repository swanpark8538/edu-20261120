package egovframework.example.cmm.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import egovframework.example.cmm.service.FileVO;

/**
 * 첨부파일 메타 SQL 매퍼 (eGovFrame v5.0 — `@EgovMapper` 인터페이스 스타일).
 *
 * 본 인터페이스의 메서드 이름이 `EgovFileMng_SQL.xml` 의 `<insert id="..."/>` /
 * `<select id="..."/>` id 와 1:1 로 매핑된다 (namespace = 본 인터페이스의 FQN).
 *
 * Service 계층(`EgovFileMngServiceImpl`) 이 이 매퍼의 단일 메서드들을 조합해
 * `insertFileInfs(List<FileVO>)` 같은 고수준 오퍼레이션을 구성한다 — 따라서 매퍼는
 * "한 메서드 = 한 SQL" 원칙을 유지한다.
 */
@EgovMapper("fileManageMapper")
public interface FileManageMapper {

	/** COMTNFILE 마스터 1행 등록 (그룹 키 생성). */
	void insertFileMaster(FileVO vo) throws Exception;

	/** COMTNFILEDETAIL 상세 1행 등록. */
	void insertFileDetail(FileVO vo) throws Exception;

	/** 단일 상세 행 삭제 (atchFileId + fileSn). */
	void deleteFileDetail(FileVO vo) throws Exception;

	/**
	 * 그룹 전체 논리 삭제 (USE_AT='N'). 물리 DELETE 가 아닌 UPDATE 라 SQL 이름은 update,
	 * 호출 시점은 도메인 삭제와 함께.
	 */
	void deleteCOMTNFILE(FileVO vo) throws Exception;

	/** 그룹 상세 목록 (USE_AT='Y' 필터). */
	List<FileVO> selectFileList(FileVO vo) throws Exception;

	/** 단일 상세 조회. */
	FileVO selectFileInf(FileVO vo) throws Exception;

	/** 다음 FILE_SN 계산 (= MAX+1, 빈 그룹이면 1). */
	int getMaxFileSN(FileVO vo) throws Exception;

	/** 파일명 검색 + 페이징 결과 목록. */
	List<FileVO> selectFileListByFileNm(FileVO vo) throws Exception;

	/** 파일명 검색 결과 총 건수. */
	int selectFileListCntByFileNm(FileVO vo) throws Exception;

	/** 이미지 확장자만 필터한 그룹 목록. */
	List<FileVO> selectImageFileList(FileVO vo) throws Exception;
}
