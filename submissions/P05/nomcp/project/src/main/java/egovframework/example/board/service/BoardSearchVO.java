package egovframework.example.board.service;

import java.util.Set;

public class BoardSearchVO {

    private static final Set<String> SEARCH_CONDITIONS = Set.of("0", "1", "2");

    private int pageIndex = 1;
    private String searchCondition = "0";
    private String searchKeyword = "";
    private int firstIndex;
    private int recordCountPerPage = 10;

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = Math.max(pageIndex, 1);
    }

    public String getSearchCondition() {
        return searchCondition;
    }

    public void setSearchCondition(String searchCondition) {
        this.searchCondition = SEARCH_CONDITIONS.contains(searchCondition) ? searchCondition : "0";
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }

    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword == null ? "" : searchKeyword.strip();
    }

    /** LIKE 와일드카드를 글자 그대로 찾도록 이스케이프한 검색어. */
    public String getLikeKeyword() {
        return searchKeyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    public int getFirstIndex() {
        return firstIndex;
    }

    public void setFirstIndex(int firstIndex) {
        this.firstIndex = firstIndex;
    }

    public int getRecordCountPerPage() {
        return recordCountPerPage;
    }

    public void setRecordCountPerPage(int recordCountPerPage) {
        this.recordCountPerPage = recordCountPerPage;
    }
}
