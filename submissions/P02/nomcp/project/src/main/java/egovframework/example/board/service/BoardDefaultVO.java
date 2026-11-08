package egovframework.example.board.service;

import java.io.Serializable;

/**
 * 게시물 목록 검색 조건 VO.
 */
public class BoardDefaultVO implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int MAX_KEYWORD_LENGTH = 100;

    /** 검색 조건: 0 제목, 1 내용, 2 작성자 */
    private String searchCondition = "";
    private String searchKeyword = "";
    private int pageIndex = 1;
    private int firstIndex;
    private int recordCountPerPage = 10;

    public String getSearchCondition() {
        return searchCondition;
    }

    public void setSearchCondition(String searchCondition) {
        this.searchCondition = searchCondition == null ? "" : searchCondition;
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }

    public void setSearchKeyword(String searchKeyword) {
        String keyword = searchKeyword == null ? "" : searchKeyword.trim();
        this.searchKeyword = keyword.length() > MAX_KEYWORD_LENGTH ? keyword.substring(0, MAX_KEYWORD_LENGTH) : keyword;
    }

    /** LIKE 검색에서 %, _ 를 일반 문자로 다루도록 변환한 검색어. */
    public String getEscapedKeyword() {
        return searchKeyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = Math.max(pageIndex, 1);
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
