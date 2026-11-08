package egovframework.example.board.web;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.exception.EgovBizException;
import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import egovframework.example.board.service.BoardDefaultVO;
import egovframework.example.board.service.BoardVO;
import egovframework.example.board.service.EgovBoardService;
import egovframework.example.cmmn.file.service.EgovFileMngService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * 게시물 목록, 등록, 수정, 삭제 컨트롤러.
 */
@Controller
public class EgovBoardController {

    private static final String REDIRECT_LIST = "redirect:/egovBoardList.do";
    private static final String REGISTER_VIEW = "board/boardRegister";
    private static final String UPDATE_VIEW = "board/boardUpdate";
    private static final String MESSAGE = "message";

    @Resource(name = "boardService")
    private EgovBoardService boardService;

    @Resource(name = "fileMngService")
    private EgovFileMngService fileMngService;

    @Resource(name = "propertiesService")
    private EgovPropertyService propertiesService;

    @GetMapping("/")
    public String home() {
        return REDIRECT_LIST;
    }

    /** pageIndex 형식 오류는 BindingResult 로 받아 첫 페이지를 보여 준다. */
    @GetMapping("/egovBoardList.do")
    public String list(@ModelAttribute("searchVO") BoardDefaultVO searchVO, BindingResult bindingResult, Model model)
            throws Exception {
        int totalCount = boardService.selectBoardListTotCnt(searchVO);

        PaginationInfo paginationInfo = new PaginationInfo();
        paginationInfo.setRecordCountPerPage(propertiesService.getInt("pageUnit"));
        paginationInfo.setPageSize(propertiesService.getInt("pageSize"));
        paginationInfo.setCurrentPageNo(searchVO.getPageIndex());
        paginationInfo.setTotalRecordCount(totalCount);
        paginationInfo.setCurrentPageNo(Math.min(searchVO.getPageIndex(), paginationInfo.getTotalPageCount()));

        searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
        searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

        model.addAttribute("resultList", boardService.selectBoardList(searchVO));
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("paginationInfo", paginationInfo);
        return "board/boardList";
    }

    @PostMapping("/addBoardView.do")
    public String addBoardView(Model model) {
        model.addAttribute("boardVO", new BoardVO());
        return REGISTER_VIEW;
    }

    @PostMapping("/addBoard.do")
    public String addBoard(@Valid @ModelAttribute("boardVO") BoardVO boardVO, BindingResult bindingResult,
            @RequestParam(value = "file_1", required = false) List<MultipartFile> files,
            HttpServletResponse response, RedirectAttributes redirectAttributes) throws Exception {
        if (bindingResult.hasErrors()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return REGISTER_VIEW;
        }
        try {
            boardService.insertBoard(boardVO, files);
        } catch (EgovBizException e) {
            bindingResult.reject("file", e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return REGISTER_VIEW;
        }
        redirectAttributes.addFlashAttribute(MESSAGE, "게시물을 등록했습니다.");
        return REDIRECT_LIST;
    }

    @PostMapping("/updateBoardView.do")
    public String updateBoardView(@RequestParam("boardId") String boardId, Model model) throws Exception {
        BoardVO board = boardService.selectBoard(boardId);
        model.addAttribute("boardVO", board);
        model.addAttribute("fileList", fileMngService.selectFileList(board.getAtchFileId()));
        return UPDATE_VIEW;
    }

    @PostMapping("/updateBoard.do")
    public String updateBoard(@Valid @ModelAttribute("boardVO") BoardVO boardVO, BindingResult bindingResult,
            @RequestParam(value = "file_1", required = false) List<MultipartFile> files,
            HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) throws Exception {
        // 게시물이 없으면 여기서 404 오류 화면으로 이동한다.
        BoardVO stored = boardService.selectBoard(boardVO.getBoardId());

        if (!bindingResult.hasErrors()) {
            try {
                boardService.updateBoard(boardVO, files);
                redirectAttributes.addFlashAttribute(MESSAGE, "게시물을 수정했습니다.");
                return REDIRECT_LIST;
            } catch (EgovBizException e) {
                bindingResult.reject("file", e.getMessage());
            }
        }
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        boardVO.setAtchFileId(stored.getAtchFileId());
        model.addAttribute("fileList", fileMngService.selectFileList(stored.getAtchFileId()));
        return UPDATE_VIEW;
    }

    @PostMapping("/deleteBoard.do")
    public String deleteBoard(@RequestParam("boardId") String boardId, RedirectAttributes redirectAttributes)
            throws Exception {
        boardService.deleteBoard(boardId);
        redirectAttributes.addFlashAttribute(MESSAGE, "게시물을 삭제했습니다.");
        return REDIRECT_LIST;
    }
}
