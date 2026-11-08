package egovframework.example.board.web;

import java.util.List;

import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import egovframework.example.board.service.BoardSearchVO;
import egovframework.example.board.service.BoardVO;
import egovframework.example.board.service.EgovBoardService;
import egovframework.example.cmmn.file.service.EgovFileMngService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;

@Controller
public class EgovBoardController {

    private static final String LIST_REDIRECT = "redirect:/egovBoardList.do";
    private static final String FORM_VIEW = "board/egovBoardRegister";

    @Resource(name = "boardService")
    private EgovBoardService boardService;

    @Resource(name = "fileMngService")
    private EgovFileMngService fileMngService;

    @Resource(name = "propertiesService")
    private EgovPropertyService propertiesService;

    /** 화면이 보낼 수 있는 입력만 바인딩한다. atchFileId 는 서버 값을 쓰므로 받지 않는다. */
    @InitBinder("boardVO")
    public void initBoardBinder(WebDataBinder binder) {
        binder.setAllowedFields("boardId", "title", "content", "writer");
    }

    @InitBinder("searchVO")
    public void initSearchBinder(WebDataBinder binder) {
        binder.setAllowedFields("pageIndex", "searchCondition", "searchKeyword");
    }

    /** bindingResult 를 받아 숫자가 아닌 pageIndex 는 기본값 1 로 처리한다. */
    @GetMapping("/egovBoardList.do")
    public String list(@ModelAttribute("searchVO") BoardSearchVO searchVO, BindingResult bindingResult, Model model)
            throws Exception {
        PaginationInfo paginationInfo = new PaginationInfo();
        paginationInfo.setRecordCountPerPage(propertiesService.getInt("pageUnit"));
        paginationInfo.setPageSize(propertiesService.getInt("pageSize"));

        int totalCount = boardService.selectBoardListCnt(searchVO);
        paginationInfo.setTotalRecordCount(totalCount);
        // 마지막 페이지를 넘는 요청은 마지막 페이지로 맞춘다. 큰 pageIndex 의 OFFSET 넘침도 함께 막는다.
        searchVO.setPageIndex(Math.min(searchVO.getPageIndex(), paginationInfo.getTotalPageCount()));
        paginationInfo.setCurrentPageNo(searchVO.getPageIndex());

        searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
        searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

        model.addAttribute("resultList", boardService.selectBoardList(searchVO));
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("paginationInfo", paginationInfo);
        return "board/egovBoardList";
    }

    @PostMapping("/addBoardView.do")
    public String addBoardView(Model model) {
        model.addAttribute("boardVO", new BoardVO());
        model.addAttribute("registerFlag", "create");
        return FORM_VIEW;
    }

    @PostMapping("/addBoard.do")
    public String addBoard(@Valid @ModelAttribute("boardVO") BoardVO boardVO, BindingResult bindingResult,
            @RequestParam(name = "file_1", required = false) List<MultipartFile> files, Model model) throws Exception {
        if (bindingResult.hasErrors()) {
            model.addAttribute("registerFlag", "create");
            return FORM_VIEW;
        }
        boardService.insertBoard(boardVO, files);
        return LIST_REDIRECT;
    }

    @PostMapping("/updateBoardView.do")
    public String updateBoardView(@RequestParam String boardId, Model model) throws Exception {
        BoardVO board = requireBoard(boardId);
        model.addAttribute("boardVO", board);
        addUpdateFormAttributes(board, model);
        return FORM_VIEW;
    }

    @PostMapping("/updateBoard.do")
    public String updateBoard(@Valid @ModelAttribute("boardVO") BoardVO boardVO, BindingResult bindingResult,
            @RequestParam(name = "file_1", required = false) List<MultipartFile> files, Model model) throws Exception {
        BoardVO current = requireBoard(boardVO.getBoardId());
        if (bindingResult.hasErrors()) {
            boardVO.setAtchFileId(current.getAtchFileId());
            addUpdateFormAttributes(current, model);
            return FORM_VIEW;
        }
        boardService.updateBoard(boardVO, files);
        return LIST_REDIRECT;
    }

    @PostMapping("/deleteBoard.do")
    public String deleteBoard(@RequestParam String boardId) throws Exception {
        requireBoard(boardId);
        boardService.deleteBoard(boardId);
        return LIST_REDIRECT;
    }

    private void addUpdateFormAttributes(BoardVO board, Model model) throws Exception {
        model.addAttribute("registerFlag", "modify");
        model.addAttribute("fileList", fileMngService.selectFileList(board.getAtchFileId()));
    }

    private BoardVO requireBoard(String boardId) throws Exception {
        BoardVO board = boardId == null ? null : boardService.selectBoard(boardId);
        if (board == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return board;
    }
}
