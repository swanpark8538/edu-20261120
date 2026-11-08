package egovframework.example.board.web;

import java.util.List;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import egovframework.example.cmm.service.EgovFileMngService;
import egovframework.example.cmm.service.FileVO;

import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import egovframework.example.board.service.EgovBoardService;
import egovframework.example.board.service.BoardVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class EgovBoardController {

	@Resource(name = "boardService")
	private EgovBoardService boardService;

	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

    @Resource(name = "EgovFileMngService")
    private EgovFileMngService fileService;

    @InitBinder("boardVO")
    public void bindFields(WebDataBinder binder) {
        binder.setAllowedFields("boardId", "title", "content", "writer", "atchFileId", "pageIndex", "searchCondition", "searchKeyword");
    }

    private void attachments(BoardVO board, Model model) throws Exception {
        if (board.getAtchFileId() != null && !board.getAtchFileId().isEmpty()) {
            FileVO key = new FileVO();
            key.setAtchFileId(board.getAtchFileId());
            model.addAttribute("attachList", fileService.selectFileInfs(key));
        }
    }

	@GetMapping("/")
	public String index(@ModelAttribute("boardVO") BoardVO boardVO, ModelMap model) throws Exception {
		return this.selectBoardList(boardVO, model);
	}

	@GetMapping("/egovBoardList.do")
	public String selectBoardList(@ModelAttribute("boardVO") BoardVO boardVO, ModelMap model) throws Exception {

		boardVO.setPageIndex(Math.max(1, Math.min(boardVO.getPageIndex(), 1000000)));
		boardVO.setPageUnit(10);
		boardVO.setPageSize(propertiesService.getInt("pageSize"));

		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(boardVO.getPageIndex());
		paginationInfo.setRecordCountPerPage(boardVO.getPageUnit());
		paginationInfo.setPageSize(boardVO.getPageSize());

		boardVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		boardVO.setLastIndex(paginationInfo.getLastRecordIndex());
		boardVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		List<?> boardList = boardService.selectBoardList(boardVO);
		model.addAttribute("resultList", boardList);

		int totCnt = boardService.selectBoardListTotCnt(boardVO);
		paginationInfo.setTotalRecordCount(totCnt);

		model.addAttribute("paginationInfo", paginationInfo);

		return "board/egovBoardList";
	}

	@PostMapping("/addBoardView.do")
	public String addBoardView(@ModelAttribute("boardVO") BoardVO boardVO, Model model) throws Exception {
		model.addAttribute("boardVO", boardVO);
		return "board/egovBoardRegister";
	}

	@PostMapping("/addBoard.do")
	public String addBoard(final MultipartHttpServletRequest multiRequest, @Valid @ModelAttribute("boardVO") BoardVO boardVO, BindingResult bindingResult, Model model, SessionStatus status) throws Exception {
		if (bindingResult.hasErrors()) {
			model.addAttribute("boardVO", boardVO);
			return "board/egovBoardRegister";
		}
		boardService.insertBoard(boardVO, multiRequest.getFiles("file_1"));
		status.setComplete();
		return "redirect:/egovBoardList.do";
	}

	@PostMapping("/updateBoardView.do")
	public String updateBoardView(@ModelAttribute("boardVO") BoardVO boardVO, Model model) throws Exception {
		BoardVO detail = boardService.selectBoard(boardVO);
		detail.setSearchCondition(boardVO.getSearchCondition());
		detail.setSearchKeyword(boardVO.getSearchKeyword());
		detail.setPageIndex(boardVO.getPageIndex());
		model.addAttribute("boardVO", detail);
		attachments(detail, model);
		return "board/egovBoardRegister";
	}

	@PostMapping("/updateBoard.do")
	public String updateBoard(final MultipartHttpServletRequest multiRequest, @Valid @ModelAttribute("boardVO") BoardVO boardVO, BindingResult bindingResult,
			Model model, RedirectAttributes redirectAttributes, SessionStatus status) throws Exception {
		if (bindingResult.hasErrors()) {
			boardVO.setAtchFileId(boardService.selectBoard(boardVO).getAtchFileId());
			attachments(boardVO, model);
			model.addAttribute("boardVO", boardVO);
			return "board/egovBoardRegister";
		}
		boardService.updateBoard(boardVO, multiRequest.getFiles("file_1"));
		status.setComplete();
		redirectAttributes.addAttribute("searchCondition", boardVO.getSearchCondition());
		redirectAttributes.addAttribute("searchKeyword", boardVO.getSearchKeyword());
		redirectAttributes.addAttribute("pageIndex", boardVO.getPageIndex());
		return "redirect:/egovBoardList.do";
	}

	@PostMapping("/deleteBoard.do")
	public String deleteBoard(@ModelAttribute("boardVO") BoardVO boardVO, RedirectAttributes redirectAttributes, SessionStatus status) throws Exception {
		boardService.deleteBoard(boardVO);
		status.setComplete();
		redirectAttributes.addAttribute("searchCondition", boardVO.getSearchCondition());
		redirectAttributes.addAttribute("searchKeyword", boardVO.getSearchKeyword());
		redirectAttributes.addAttribute("pageIndex", boardVO.getPageIndex());
		return "redirect:/egovBoardList.do";
	}

}
