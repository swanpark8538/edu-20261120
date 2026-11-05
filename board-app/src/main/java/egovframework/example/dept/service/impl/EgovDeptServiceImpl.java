package egovframework.example.dept.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.fdl.idgnr.EgovIdGnrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import egovframework.example.dept.service.EgovDeptService;
import egovframework.example.dept.service.DeptVO;
import jakarta.annotation.Resource;

@Service("deptService")
public class EgovDeptServiceImpl extends EgovAbstractServiceImpl implements EgovDeptService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EgovDeptServiceImpl.class);

	@Resource(name = "deptMapper")
	private DeptMapper deptMapper;

	@Resource(name = "egovDeptIdGnrService")
	private EgovIdGnrService egovDeptIdGnrService;

	@Override
	public void insertDept(DeptVO vo) throws Exception {
		// PII 노출 방지: VO 전체(vo.toString()) 가 아니라 생성된 PK 만 로깅.
		// parameterized form 을 사용해 DEBUG 비활성 시 문자열 조립 비용도 회피.
		String id = egovDeptIdGnrService.getNextStringId();
		vo.setDeptId(id);
		LOGGER.debug("insertDept: deptId={}", id);
		deptMapper.insertDept(vo);
	}

	@Override
	public void updateDept(DeptVO vo) throws Exception {
		deptMapper.updateDept(vo);
	}

	@Override
	public void deleteDept(DeptVO vo) throws Exception {
		deptMapper.deleteDept(vo);
	}

	@Override
	public DeptVO selectDept(DeptVO vo) throws Exception {
		DeptVO resultVO = deptMapper.selectDept(vo);
		if (resultVO == null)
			throw processException("info.nodata.msg");
		return resultVO;
	}

	@Override
	public List<?> selectDeptList(DeptVO vo) throws Exception {
		return deptMapper.selectDeptList(vo);
	}

	@Override
	public int selectDeptListTotCnt(DeptVO vo) {
		return deptMapper.selectDeptListTotCnt(vo);
	}

}
