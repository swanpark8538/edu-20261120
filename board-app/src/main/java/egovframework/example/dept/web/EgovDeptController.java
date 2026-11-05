package egovframework.example.dept.web;

import java.util.List;

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

import egovframework.example.dept.service.EgovDeptService;
import egovframework.example.dept.service.DeptVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class EgovDeptController {

	@Resource(name = "deptService")
	private EgovDeptService deptService;

	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

	@GetMapping("/")
	public String index(@ModelAttribute("deptVO") DeptVO deptVO, ModelMap model) throws Exception {
		return this.selectDeptList(deptVO, model);
	}

	@GetMapping("/egovDeptList.do")
	public String selectDeptList(@ModelAttribute("deptVO") DeptVO deptVO, ModelMap model) throws Exception {

		deptVO.setPageUnit(propertiesService.getInt("pageUnit"));
		deptVO.setPageSize(propertiesService.getInt("pageSize"));

		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(deptVO.getPageIndex());
		paginationInfo.setRecordCountPerPage(deptVO.getPageUnit());
		paginationInfo.setPageSize(deptVO.getPageSize());

		deptVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		deptVO.setLastIndex(paginationInfo.getLastRecordIndex());
		deptVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		List<?> deptList = deptService.selectDeptList(deptVO);
		model.addAttribute("resultList", deptList);

		int totCnt = deptService.selectDeptListTotCnt(deptVO);
		paginationInfo.setTotalRecordCount(totCnt);

		model.addAttribute("paginationInfo", paginationInfo);

		return "dept/egovDeptList";
	}

	@PostMapping("/addDeptView.do")
	public String addDeptView(@ModelAttribute("deptVO") DeptVO deptVO, Model model) throws Exception {
		model.addAttribute("deptVO", deptVO);
		return "dept/egovDeptRegister";
	}

	@PostMapping("/addDept.do")
	public String addDept(@Valid @ModelAttribute("deptVO") DeptVO deptVO, BindingResult bindingResult, Model model, SessionStatus status) throws Exception {
		if (bindingResult.hasErrors()) {
			model.addAttribute("deptVO", deptVO);
			return "dept/egovDeptRegister";
		}
		deptService.insertDept(deptVO);
		status.setComplete();
		return "redirect:/egovDeptList.do";
	}

	@PostMapping("/updateDeptView.do")
	public String updateDeptView(@ModelAttribute("deptVO") DeptVO deptVO, Model model) throws Exception {
		DeptVO detail = deptService.selectDept(deptVO);
		detail.setSearchCondition(deptVO.getSearchCondition());
		detail.setSearchKeyword(deptVO.getSearchKeyword());
		detail.setPageIndex(deptVO.getPageIndex());
		model.addAttribute("deptVO", detail);
		return "dept/egovDeptRegister";
	}

	@PostMapping("/updateDept.do")
	public String updateDept(@Valid @ModelAttribute("deptVO") DeptVO deptVO, BindingResult bindingResult,
			Model model, RedirectAttributes redirectAttributes, SessionStatus status) throws Exception {
		if (bindingResult.hasErrors()) {
			model.addAttribute("deptVO", deptVO);
			return "dept/egovDeptRegister";
		}
		deptService.updateDept(deptVO);
		status.setComplete();
		redirectAttributes.addAttribute("searchCondition", deptVO.getSearchCondition());
		redirectAttributes.addAttribute("searchKeyword", deptVO.getSearchKeyword());
		redirectAttributes.addAttribute("pageIndex", deptVO.getPageIndex());
		return "redirect:/egovDeptList.do";
	}

	@PostMapping("/deleteDept.do")
	public String deleteDept(@ModelAttribute("deptVO") DeptVO deptVO, RedirectAttributes redirectAttributes, SessionStatus status) throws Exception {
		deptService.deleteDept(deptVO);
		status.setComplete();
		redirectAttributes.addAttribute("searchCondition", deptVO.getSearchCondition());
		redirectAttributes.addAttribute("searchKeyword", deptVO.getSearchKeyword());
		redirectAttributes.addAttribute("pageIndex", deptVO.getPageIndex());
		return "redirect:/egovDeptList.do";
	}

}
