package egovframework.example.board.web;

import java.util.List;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import egovframework.example.cmm.service.*;

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
 @Resource(name="EgovFileMngService") private EgovFileMngService fileMngService;
 @Resource(name="EgovFileMngUtil") private EgovFileMngUtil fileUtil;
 private void attachments(BoardVO vo, Model model) throws Exception {
   if (vo.getAtchFileId() != null && !vo.getAtchFileId().isBlank()) {
     FileVO f = new FileVO(); f.setAtchFileId(vo.getAtchFileId());
     model.addAttribute("attachList", fileMngService.selectFileInfs(f));
   }
 }
 private void upload(BoardVO vo, MultipartHttpServletRequest request) throws Exception {
   var files = request.getFiles("file_1").stream().filter(f -> !f.isEmpty()).toList();
   if (files.isEmpty()) return;
   String id = vo.getAtchFileId();
   if (id == null || id.isBlank()) {
     vo.setAtchFileId(fileMngService.insertFileInfs(fileUtil.parseFileInf(files,"BOARD_",0,"","")));
   } else {
     FileVO f = new FileVO(); f.setAtchFileId(id);
     fileMngService.updateFileInfs(fileUtil.parseFileInf(files,"BOARD_",fileMngService.getMaxFileSN(f),id,""));
   }
 }

	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

	@GetMapping("/")
	public String index(@ModelAttribute("boardVO") BoardVO boardVO, ModelMap model) throws Exception {
		return this.selectBoardList(boardVO, model);
	}

	@GetMapping("/egovBoardList.do")
	public String selectBoardList(@ModelAttribute("boardVO") BoardVO boardVO, ModelMap model) throws Exception {

		boardVO.setPageUnit(propertiesService.getInt("pageUnit"));
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
	public String addBoard(MultipartHttpServletRequest request, @Valid @ModelAttribute("boardVO") BoardVO boardVO, BindingResult bindingResult, Model model, SessionStatus status) throws Exception {
		if (bindingResult.hasErrors()) {
			model.addAttribute("boardVO", boardVO);
			return "board/egovBoardRegister";
		}
		boardVO.setAtchFileId(null);
 upload(boardVO, request);
 boardService.insertBoard(boardVO);
		status.setComplete();
		return "redirect:/egovBoardList.do";
	}

	@PostMapping("/updateBoardView.do")
	public String updateBoardView(@ModelAttribute("boardVO") BoardVO boardVO, Model model) throws Exception {
		BoardVO detail = boardService.selectBoard(boardVO);
		detail.setSearchCondition(boardVO.getSearchCondition());
		detail.setSearchKeyword(boardVO.getSearchKeyword());
		detail.setPageIndex(boardVO.getPageIndex());
		attachments(detail, model);
 model.addAttribute("boardVO", detail);
		return "board/egovBoardRegister";
	}

	@PostMapping("/updateBoard.do")
	public String updateBoard(MultipartHttpServletRequest request, @Valid @ModelAttribute("boardVO") BoardVO boardVO, BindingResult bindingResult,
			Model model, RedirectAttributes redirectAttributes, SessionStatus status) throws Exception {
		if (bindingResult.hasErrors()) {
			model.addAttribute("boardVO", boardVO);
			return "board/egovBoardRegister";
		}
		boardVO.setAtchFileId(boardService.selectBoard(boardVO).getAtchFileId());
 upload(boardVO, request);
 boardService.updateBoard(boardVO);
		status.setComplete();
		redirectAttributes.addAttribute("searchCondition", boardVO.getSearchCondition());
		redirectAttributes.addAttribute("searchKeyword", boardVO.getSearchKeyword());
		redirectAttributes.addAttribute("pageIndex", boardVO.getPageIndex());
		return "redirect:/egovBoardList.do";
	}

	@PostMapping("/deleteBoard.do")
	public String deleteBoard(@ModelAttribute("boardVO") BoardVO boardVO, RedirectAttributes redirectAttributes, SessionStatus status) throws Exception {
		BoardVO saved = boardService.selectBoard(boardVO);
 boardService.deleteBoard(boardVO);
 if (saved.getAtchFileId() != null && !saved.getAtchFileId().isBlank()) {
   FileVO f = new FileVO(); f.setAtchFileId(saved.getAtchFileId()); fileMngService.deleteAllFileInf(f);
 }
		status.setComplete();
		redirectAttributes.addAttribute("searchCondition", boardVO.getSearchCondition());
		redirectAttributes.addAttribute("searchKeyword", boardVO.getSearchKeyword());
		redirectAttributes.addAttribute("pageIndex", boardVO.getPageIndex());
		return "redirect:/egovBoardList.do";
	}

}
