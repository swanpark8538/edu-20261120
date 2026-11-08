package egovframework.board.web;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import jakarta.validation.Valid;
import egovframework.board.service.*;
import egovframework.board.service.impl.FileStorage;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BoardController {
 private final BoardService service; private final FileStorage storage;
 public BoardController(BoardService service,FileStorage storage){this.service=service;this.storage=storage;}
 @InitBinder("board") void fields(WebDataBinder binder){binder.setAllowedFields("boardId","title","content","writer","atchFileId");}
 @GetMapping("/") public String home(){return "redirect:/egovBoardList.do";}
 @GetMapping("/egovBoardList.do") public String list(@Valid @ModelAttribute("search") BoardSearch search,BindingResult result,Model model){
  if(result.hasErrors())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"검색 조건이 올바르지 않습니다.");
  int total=service.count(search);
  int lastPage=Math.max(1,(total+9)/10);
  if(search.getPageIndex()>lastPage)search.setPageIndex(lastPage);
  var page=new PaginationInfo(); page.setCurrentPageNo(search.getPageIndex());page.setRecordCountPerPage(10);page.setPageSize(10);page.setTotalRecordCount(total);
  model.addAttribute("boards",service.list(search));model.addAttribute("pagination",page);return "list";
 }
 @PostMapping("/addBoardView.do") public String addView(Model model){model.addAttribute("board",new Board());model.addAttribute("files",List.of());return "form";}
 @PostMapping("/updateBoardView.do") public String updateView(@RequestParam String boardId,Model model){return edit(boardId,model);}
 @GetMapping("/boardDetail.do") public String detail(@RequestParam String boardId,Model model){
  model.addAttribute("board",service.get(boardId));model.addAttribute("files",service.files(boardId));return "detail";
 }
 private String edit(String id,Model model){model.addAttribute("board",service.get(id));model.addAttribute("files",service.files(id));return "form";}
 @PostMapping("/addBoard.do") public String add(@Valid @ModelAttribute("board") Board board,BindingResult result,
   @RequestParam(value="file_1",required=false) List<MultipartFile> files,Model model,RedirectAttributes redirect) throws Exception {
  board.setBoardId(null);board.setAtchFileId(null);
  if(result.hasErrors()){model.addAttribute("files",List.of());return "form";}
  try { service.create(board,safe(files)); }
  catch(ResponseStatusException ex){board.setBoardId(null);board.setAtchFileId(null);return invalid(ex,model,List.of());}
  redirect.addFlashAttribute("message","게시물이 등록되었습니다.");return "redirect:/egovBoardList.do";
 }
 @PostMapping("/updateBoard.do") public String update(@Valid @ModelAttribute("board") Board board,BindingResult result,
   @RequestParam(value="file_1",required=false) List<MultipartFile> files,Model model,RedirectAttributes redirect) throws Exception {
  var old=service.get(board.getBoardId());
  if(result.hasErrors()){model.addAttribute("files",service.files(old.getBoardId()));return "form";}
  try {service.update(board,safe(files));}
  catch(ResponseStatusException ex){board.setAtchFileId(old.getAtchFileId());return invalid(ex,model,service.files(old.getBoardId()));}
  redirect.addFlashAttribute("message","게시물이 수정되었습니다.");return "redirect:/egovBoardList.do";
 }
 private String invalid(ResponseStatusException ex,Model model,List<Attachment> files){
  if(ex.getStatusCode()!=HttpStatus.BAD_REQUEST)throw ex;
  model.addAttribute("formError",ex.getReason());model.addAttribute("files",files);return "form";
 }
 private List<MultipartFile> safe(List<MultipartFile> files){return files==null?List.of():files;}
 @PostMapping("/deleteBoard.do") public String delete(@RequestParam String boardId,RedirectAttributes redirect){service.delete(boardId);redirect.addFlashAttribute("message","게시물이 삭제되었습니다.");return "redirect:/egovBoardList.do";}
 @GetMapping("/downloadFile.do") public ResponseEntity<Resource> download(@RequestParam String fileId) throws Exception {
  var file=service.file(fileId);var path=storage.resolve(file.storedName());
  if(!Files.isRegularFile(path))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"첨부파일을 찾을 수 없습니다.");
  return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(Files.size(path))
   .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.originalName(),StandardCharsets.UTF_8).build().toString())
   .body(new FileSystemResource(path));
 }
}
