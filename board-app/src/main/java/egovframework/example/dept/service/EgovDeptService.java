package egovframework.example.dept.service;

import java.util.List;

public interface EgovDeptService {

	void insertDept(DeptVO vo) throws Exception;

	void updateDept(DeptVO vo) throws Exception;

	void deleteDept(DeptVO vo) throws Exception;

	DeptVO selectDept(DeptVO vo) throws Exception;

	List<?> selectDeptList(DeptVO vo) throws Exception;

	int selectDeptListTotCnt(DeptVO vo);

}
