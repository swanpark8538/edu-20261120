package egovframework.example.dept.service;

import org.egovframe.rte.ptl.reactive.validation.EgovNullCheck;

public class DeptVO extends DeptDefaultVO {

	private static final long serialVersionUID = 1L;

	private String deptId;
	@EgovNullCheck(message="{confirm.required.dept.deptNm}")
	private String deptNm;
	@EgovNullCheck(message="{confirm.required.dept.useYn}")
	private String useYn;

	public String getDeptId() {
		return deptId;
	}

	public void setDeptId(String deptId) {
		this.deptId = deptId;
	}

	public String getDeptNm() {
		return deptNm;
	}

	public void setDeptNm(String deptNm) {
		this.deptNm = deptNm;
	}

	public String getUseYn() {
		return useYn;
	}

	public void setUseYn(String useYn) {
		this.useYn = useYn;
	}

}
