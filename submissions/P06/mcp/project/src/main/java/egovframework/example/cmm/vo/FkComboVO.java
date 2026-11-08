package egovframework.example.cmm.vo;

/**
 * FK lookup 콤보박스 옵션을 위한 가벼운 DTO.
 *
 * eGovFrame 의 `EgovMap` 을 콤보 결과로 쓰면 alias 케이스 변환 모호함(snake_case →
 * camelCase) 때문에 Thymeleaf SpEL 에서 `${c.deptId}` 같은 접근이 실패할 수 있다.
 * 명시적 DTO 를 쓰면 MyBatis 가 SELECT alias 와 필드명을 1:1 매핑해 모호함이 사라진다.
 *
 * Mapper SQL 규약:
 *   SELECT pk_column AS value, (display_expr) AS label FROM PARENT_TABLE p ...
 *
 * Thymeleaf 사용:
 *   <option th:each="c : ${parentList}"
 *           th:value="${c.value}"
 *           th:text="${c.label}"
 *           th:selected="${myVO.fkField != null and myVO.fkField == c.value}"></option>
 */
public class FkComboVO {

	private String value;
	private String label;

	public FkComboVO() {}

	public FkComboVO(String value, String label) {
		this.value = value;
		this.label = label;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
}
