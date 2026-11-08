package egovframework.example.board.web;

import java.util.List;
import java.util.Set;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import egovframework.example.cmm.service.EgovFileMngService;
import egovframework.example.cmm.service.EgovFileMngUtil;
import egovframework.example.cmm.service.FileVO;

import egovframework.example.board.service.EgovBoardService;
import egovframework.example.board.service.BoardVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class EgovBoardController {

	private static final Set<String> SEARCH_CONDITIONS = Set.of("0", "1", "2");

	@Resource(name = "boardService")
	private EgovBoardService boardService;

	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

	/** 첨부파일 메타 CRUD (그룹 키 채번/INSERT/UPDATE/DELETE). */
	@Resource(name = "EgovFileMngService")
	private EgovFileMngService fileMngService;

	/** multipart → 디스크 IO + FileVO 변환 유틸. */
	@Resource(name = "EgovFileMngUtil")
	private EgovFileMngUtil fileUtil;

	@GetMapping("/")
	public String index(@ModelAttribute("boardVO") BoardVO boardVO, BindingResult bindingResult, ModelMap model) throws Exception {
		return this.selectBoardList(boardVO, bindingResult, model);
	}

	/**
	 * BindingResult 를 받아 숫자가 아닌 pageIndex 도 오류 없이 기본값(1쪽)으로 처리한다.
	 */
	@GetMapping("/egovBoardList.do")
	public String selectBoardList(@ModelAttribute("boardVO") BoardVO boardVO, BindingResult bindingResult, ModelMap model) throws Exception {

		boardVO.setPageUnit(propertiesService.getInt("pageUnit"));
		boardVO.setPageSize(propertiesService.getInt("pageSize"));

		// 음수/0/오버플로 pageIndex 는 OFFSET 오류를 일으키므로 1쪽으로 보정한다.
		if (boardVO.getPageIndex() < 1 || boardVO.getPageIndex() > Integer.MAX_VALUE / boardVO.getPageUnit()) {
			boardVO.setPageIndex(1);
		}
		// 매퍼의 숫자 비교(OGNL)에서 NumberFormatException 이 나지 않도록 허용 값만 통과시킨다.
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
	public String addBoard(final MultipartHttpServletRequest multiRequest, @Valid @ModelAttribute("boardVO") BoardVO boardVO, BindingResult bindingResult, Model model, SessionStatus status) throws Exception {
		if (bindingResult.hasErrors()) {
			model.addAttribute("boardVO", boardVO);
			return "board/egovBoardRegister";
		}
		// 첨부파일 처리 — parseFileInf 로 디스크에 저장한 뒤 insertFileInfs 로 그룹 키 채번/INSERT.
		// 첨부가 없으면 null 로 저장한다 (CHAR(20) 컬럼에 빈 문자열을 넣으면 공백 20자로 채워진다).
		String atchFileId = null;
		final List<MultipartFile> files = multiRequest.getFiles("file_1");
		if (files != null && !files.isEmpty()) {
			List<FileVO> fvoList = fileUtil.parseFileInf(files, "BOARD_", 0, "", "");
			atchFileId = fileMngService.insertFileInfs(fvoList);
		}
		boardVO.setAtchFileId(atchFileId);
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
		addAttachList(detail.getAtchFileId(), model);
		return "board/egovBoardRegister";
	}

	@PostMapping("/updateBoard.do")
	public String updateBoard(final MultipartHttpServletRequest multiRequest, @Valid @ModelAttribute("boardVO") BoardVO boardVO, BindingResult bindingResult,
			Model model, RedirectAttributes redirectAttributes, SessionStatus status) throws Exception {
		// 첨부 그룹 키는 요청 값이 아니라 DB 값을 기준으로 한다 — 변조된 atchFileId 로
		// 다른 게시물의 첨부 묶음에 파일을 끼워 넣거나 첨부 연결을 끊는 요청을 차단한다.
		BoardVO stored = boardService.selectBoard(boardVO);
		String atchFileId = StringUtils.hasText(stored.getAtchFileId()) ? stored.getAtchFileId() : null;
		boardVO.setAtchFileId(atchFileId);

		if (bindingResult.hasErrors()) {
			model.addAttribute("boardVO", boardVO);
			addAttachList(atchFileId, model);
			return "board/egovBoardRegister";
		}
		// 첨부파일 update — 신규 그룹이면 채번, 기존 그룹이면 다음 FILE_SN 부터 append.
		final List<MultipartFile> files = multiRequest.getFiles("file_1");
		if (files != null && !files.isEmpty()) {
			if (atchFileId == null) {
				// (A) 첨부 그룹이 아직 없음 → 신규 채번
				List<FileVO> fvoList = fileUtil.parseFileInf(files, "BOARD_", 0, "", "");
				boardVO.setAtchFileId(fileMngService.insertFileInfs(fvoList));
			} else {
				// (B) 기존 그룹에 추가 — getMaxFileSN 으로 다음 FILE_SN 부터 (중복 PK 방지)
				FileVO key = new FileVO();
				key.setAtchFileId(atchFileId);
				int cnt = fileMngService.getMaxFileSN(key);
				List<FileVO> fvoList = fileUtil.parseFileInf(files, "BOARD_", cnt, atchFileId, "");
				fileMngService.updateFileInfs(fvoList);
			}
		}
		boardService.updateBoard(boardVO);
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

	/**
	 * 첨부 그룹의 단일 파일(atchFileId + fileSn) 1건 삭제.
	 * 도메인 행은 유지하고 첨부 한 건만 떼어내는 용도 — 수정 화면에서 호출.
	 * 반환: 단순 텍스트 "ok" (성공) — 화면 JS 가 location.reload 로 목록 갱신.
	 */
	@PostMapping("/deleteBoardAttachFile.do")
	@ResponseBody
	public String deleteBoardAttachFile(
			@RequestParam("atchFileId") String atchFileId,
			@RequestParam("fileSn") String fileSn) throws Exception {
		FileVO fvo = new FileVO();
		fvo.setAtchFileId(atchFileId);
		fvo.setFileSn(fileSn);
		fileMngService.deleteFileInf(fvo);
		return "ok";
	}

	/** 수정 화면에 표시할 기존 첨부 목록을 model 에 주입한다. */
	private void addAttachList(String atchFileId, Model model) throws Exception {
		if (StringUtils.hasText(atchFileId)) {
			FileVO attachKey = new FileVO();
			attachKey.setAtchFileId(atchFileId);
			model.addAttribute("attachList", fileMngService.selectFileInfs(attachKey));
		}
	}

}
