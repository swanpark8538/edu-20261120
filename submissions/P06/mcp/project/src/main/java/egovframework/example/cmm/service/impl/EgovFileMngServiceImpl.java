package egovframework.example.cmm.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import egovframework.example.cmm.service.EgovFileMngService;
import egovframework.example.cmm.service.FileVO;
import jakarta.annotation.Resource;

/**
 * 첨부파일 그룹 메타 CRUD 서비스 구현.
 *
 * 저수준 SQL 은 `FileManageMapper` (@EgovMapper 인터페이스) 가 담당하고, 본 서비스는
 * 마스터 1행 + 상세 N행을 묶는 그룹 단위 오퍼레이션을 조립한다.
 *
 * Bean name 은 "EgovFileMngService" (대문자 E) — 공통컴포넌트와 동일한 표준값이라
 * 다른 도메인 컨트롤러 코드(`@Resource(name = "EgovFileMngService")`) 와 호환된다.
 */
@Service("EgovFileMngService")
public class EgovFileMngServiceImpl extends EgovAbstractServiceImpl implements EgovFileMngService {

	@Resource(name = "fileManageMapper")
	private FileManageMapper fileMngMapper;

	@Override
	public void deleteFileInfs(List<FileVO> fvoList) throws Exception {
		for (FileVO vo : fvoList) {
			fileMngMapper.deleteFileDetail(vo);
		}
	}

	@Override
	public String insertFileInf(FileVO fvo) throws Exception {
		fileMngMapper.insertFileMaster(fvo);
		fileMngMapper.insertFileDetail(fvo);
		return fvo.getAtchFileId();
	}

	@Override
	public String insertFileInfs(List<FileVO> fvoList) throws Exception {
		if (fvoList == null || fvoList.isEmpty()) {
			return null;
		}
		FileVO first = fvoList.get(0);
		String atchFileId = first.getAtchFileId();

		// 마스터 1행 (그룹 키 생성) + 상세 N행. 트랜잭션 경계는 호출자/AOP 에 위임.
		fileMngMapper.insertFileMaster(first);
		for (FileVO vo : fvoList) {
			fileMngMapper.insertFileDetail(vo);
		}
		return atchFileId;
	}

	@Override
	public List<FileVO> selectFileInfs(FileVO fvo) throws Exception {
		return fileMngMapper.selectFileList(fvo);
	}

	@Override
	public void updateFileInfs(List<FileVO> fvoList) throws Exception {
		// 기존 그룹에 상세만 추가 (마스터는 이미 존재). 호출자는 fvoList 의 fileSn 이
		// getMaxFileSN() 결과부터 시작하도록 미리 보장해야 한다.
		for (FileVO vo : fvoList) {
			fileMngMapper.insertFileDetail(vo);
		}
	}

	@Override
	public void deleteFileInf(FileVO fvo) throws Exception {
		fileMngMapper.deleteFileDetail(fvo);
	}

	@Override
	public FileVO selectFileInf(FileVO fvo) throws Exception {
		return fileMngMapper.selectFileInf(fvo);
	}

	@Override
	public int getMaxFileSN(FileVO fvo) throws Exception {
		return fileMngMapper.getMaxFileSN(fvo);
	}

	@Override
	public void deleteAllFileInf(FileVO fvo) throws Exception {
		fileMngMapper.deleteCOMTNFILE(fvo);
	}

	@Override
	public Map<String, Object> selectFileListByFileNm(FileVO fvo) throws Exception {
		List<FileVO> resultList = fileMngMapper.selectFileListByFileNm(fvo);
		int cnt = fileMngMapper.selectFileListCntByFileNm(fvo);

		Map<String, Object> map = new HashMap<>();
		map.put("resultList", resultList);
		map.put("resultCnt", Integer.toString(cnt));
		return map;
	}

	@Override
	public List<FileVO> selectImageFileList(FileVO vo) throws Exception {
		return fileMngMapper.selectImageFileList(vo);
	}
}
