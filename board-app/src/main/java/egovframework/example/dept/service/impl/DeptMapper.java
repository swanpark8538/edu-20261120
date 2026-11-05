package egovframework.example.dept.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import egovframework.example.dept.service.DeptVO;

@EgovMapper("deptMapper")
public interface DeptMapper {

	void insertDept(DeptVO vo) throws Exception;

	void updateDept(DeptVO vo) throws Exception;

	void deleteDept(DeptVO vo) throws Exception;

	DeptVO selectDept(DeptVO vo) throws Exception;

	List<?> selectDeptList(DeptVO vo) throws Exception;

	int selectDeptListTotCnt(DeptVO vo);

}
