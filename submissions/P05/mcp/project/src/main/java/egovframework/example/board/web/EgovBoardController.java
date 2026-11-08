package egovframework.example.board.web;

import java.util.List;

import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import egovframework.example.board.service.EgovBoardService;
import egovframework.example.board.service.BoardVO;
import egovframework.example.cmm.service.EgovFileMngService;
import egovframework.example.cmm.service.EgovFileMngUtil;
import egovframework.example.cmm.service.FileVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class EgovBoardController {

	private static final List<String> SEARCH_CONDITIONS = List.of("0", "1", "2");
	private static final String FILE_PARAM = "file_1";
	private static final String STORED_FILE_PREFIX = "BOARD_";

	@Resource(name = "boardService")
	private EgovBoardService boardService;

	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

	@Resource(name = "EgovFileMngService")
	private EgovFileMngService fileMngService;

	@Resource(name = "EgovFileMngUtil")
	private EgovFileMngUtil fileMngUtil;

	@GetMapping("/")
	public String index(@ModelAttribute("boardVO") BoardVO boardVO, ModelMap model) throws Exception {
		return this.selectBoardList(boardVO, model);
	}

	@GetMapping("/egovBoardList.do")
	public String selectBoardList(@ModelAttribute("boardVO") BoardVO boardVO, ModelMap model) throws Exception {

		boardVO.setPageUnit(propertiesService.getInt("pageUnit"));
		boardVO.setPageSize(propertiesService.getInt("pageSize"));
		// 범위를 벗어난 페이지 번호가 음수 OFFSET 으로 이어지지 않도록 보정한다.
		boardVO.setPageIndex(Math.max(1, Math.min(boardVO.getPageIndex(), Integer.MAX_VALUE / boardVO.getPageUnit())));
		if (!SEARCH_CONDITIONS.contains(boardVO.getSearchCondition())) {
			boardVO.setSearchCondition("0");
		}

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
	public String addBoard(final MultipartHttpServletRequest multiRequest, @Valid @ModelAttribute("boardVO") BoardVO boardVO,
			BindingResult bindingResult, Model model, SessionStatus status) throws Exception {
		if (bindingResult.hasErrors()) {
			model.addAttribute("boardVO", boardVO);
			return "board/egovBoardRegister";
		}
		boardVO.setAtchFileId(saveAttachments(multiRequest, null));
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
		model.addAttribute("boardVO", detail);
		model.addAttribute("attachList", selectAttachments(detail.getAtchFileId()));
		return "board/egovBoardRegister";
	}

	@PostMapping("/updateBoard.do")
	public String updateBoard(final MultipartHttpServletRequest multiRequest, @Valid @ModelAttribute("boardVO") BoardVO boardVO,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes, SessionStatus status) throws Exception {
		// 첨부 묶음 키는 요청값이 아니라 저장된 값을 쓴다 — 다른 게시물의 첨부 묶음을 가리키도록 변조하는 것을 막는다.
		String storedAtchFileId = boardService.selectBoard(boardVO).getAtchFileId();
		boardVO.setAtchFileId(storedAtchFileId);
		if (bindingResult.hasErrors()) {
			model.addAttribute("boardVO", boardVO);
			model.addAttribute("attachList", selectAttachments(storedAtchFileId));
			return "board/egovBoardRegister";
		}
		boardVO.setAtchFileId(saveAttachments(multiRequest, storedAtchFileId));
		boardService.updateBoard(boardVO);
		status.setComplete();
		redirectAttributes.addAttribute("searchCondition", boardVO.getSearchCondition());
		redirectAttributes.addAttribute("searchKeyword", boardVO.getSearchKeyword());
		redirectAttributes.addAttribute("pageIndex", boardVO.getPageIndex());
		return "redirect:/egovBoardList.do";
	}

	@PostMapping("/deleteBoard.do")
	public String deleteBoard(@ModelAttribute("boardVO") BoardVO boardVO, RedirectAttributes redirectAttributes, SessionStatus status) throws Exception {
		// 삭제 요청에는 boardId 만 오므로 첨부 묶음 키는 저장된 값에서 얻는다.
		String atchFileId = boardService.selectBoard(boardVO).getAtchFileId();
		boardService.deleteBoard(boardVO);
		if (StringUtils.hasText(atchFileId)) {
			FileVO fileKey = new FileVO();
			fileKey.setAtchFileId(atchFileId);
			fileMngService.deleteAllFileInf(fileKey);
		}
		status.setComplete();
		redirectAttributes.addAttribute("searchCondition", boardVO.getSearchCondition());
		redirectAttributes.addAttribute("searchKeyword", boardVO.getSearchKeyword());
		redirectAttributes.addAttribute("pageIndex", boardVO.getPageIndex());
		return "redirect:/egovBoardList.do";
	}

	/** 업로드 파일을 저장하고 첨부 묶음 키를 돌려준다. 파일이 없으면 기존 키를 그대로 돌려준다. */
	private String saveAttachments(MultipartHttpServletRequest multiRequest, String atchFileId) throws Exception {
		List<MultipartFile> files = multiRequest.getFiles(FILE_PARAM).stream()
				.filter(file -> StringUtils.hasText(file.getOriginalFilename()))
				.toList();
		if (files.isEmpty()) {
			return atchFileId;
		}
		if (!StringUtils.hasText(atchFileId)) {
			return fileMngService.insertFileInfs(fileMngUtil.parseFileInf(files, STORED_FILE_PREFIX, 0, "", ""));
		}
		FileVO fileKey = new FileVO();
		fileKey.setAtchFileId(atchFileId);
		int nextFileSn = fileMngService.getMaxFileSN(fileKey);
		fileMngService.updateFileInfs(fileMngUtil.parseFileInf(files, STORED_FILE_PREFIX, nextFileSn, atchFileId, ""));
		return atchFileId;
	}

	private List<FileVO> selectAttachments(String atchFileId) throws Exception {
		if (!StringUtils.hasText(atchFileId)) {
			return List.of();
		}
		FileVO fileKey = new FileVO();
		fileKey.setAtchFileId(atchFileId);
		return fileMngService.selectFileInfs(fileKey);
	}

}
