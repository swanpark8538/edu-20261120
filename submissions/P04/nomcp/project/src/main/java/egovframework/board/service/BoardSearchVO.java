package egovframework.board.service;

import jakarta.validation.constraints.*;

public class BoardSearchVO {
    @Min(1) @Max(1000000)
    private int pageIndex = 1;
    @Pattern(regexp = "[012]", message = "검색 조건이 올바르지 않습니다.")
    private String searchCondition = "0";
    @Size(max = 200)
    private String searchKeyword = "";
    public int getPageIndex() { return pageIndex; }
    public void setPageIndex(int v) { pageIndex = v; }
    public String getSearchCondition() { return searchCondition; }
    public void setSearchCondition(String v) { searchCondition = v; }
    public String getSearchKeyword() { return searchKeyword; }
    public void setSearchKeyword(String v) { searchKeyword = v == null ? "" : v.trim(); }
    public int getOffset() { return (pageIndex - 1) * 10; }
    public String getLikeKeyword() { return "%" + searchKeyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%"; }
}
