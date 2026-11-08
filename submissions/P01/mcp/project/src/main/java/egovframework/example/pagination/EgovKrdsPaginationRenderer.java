package egovframework.example.pagination;

import org.egovframe.rte.ptl.mvc.tags.ui.pagination.AbstractKrdsPaginationRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class EgovKrdsPaginationRenderer extends AbstractKrdsPaginationRenderer {

	@Autowired
	private MessageSource messageSource;

	public EgovKrdsPaginationRenderer() {
	}

	@PostConstruct
	public void initMessageLabels() {
		initVariables();

		String prevLabel = messageSource.getMessage("page.prev", null, "\uc774\uc804", LocaleContextHolder.getLocale());
		String nextLabel = messageSource.getMessage("page.next", null, "\ub2e4\uc74c", LocaleContextHolder.getLocale());

		previousPageLabel = "<a onclick=\"{0}({1})\" class=\"page-navi prev\" href=\"#\">" + prevLabel + "</a>";
		previousPageDisabledLabel = "<span class=\"page-navi prev disabled\">" + prevLabel + "</span>";
		nextPageLabel = "<a onclick=\"{0}({1})\" class=\"page-navi next\" href=\"#\">" + nextLabel + "</a>";
		nextPageDisabledLabel = "<span class=\"page-navi next disabled\">" + nextLabel + "</span>";
	}

	public void initVariables() {
		previousPageLabel = "<a onclick=\"{0}({1})\" class=\"page-navi prev\" href=\"#\">\uc774\uc804</a>";
		previousPageDisabledLabel = "<span class=\"page-navi prev disabled\">\uc774\uc804</span>";
		firstPageLabel = "<a onclick=\"{0}({1})\" class=\"page-link\" href=\"#\">{2}</a>";
		currentPageLabel = "<a class=\"page-link active\" href=\"#\">{0}</a>";
		otherPageLabel = "<a onclick=\"{0}({1})\" class=\"page-link\" href=\"#\">{2}</a>";
		nextPageLabel = "<a onclick=\"{0}({1})\" class=\"page-navi next\" href=\"#\">\ub2e4\uc74c</a>";
		nextPageDisabledLabel = "<span class=\"page-navi next disabled\">\ub2e4\uc74c</span>";
		lastPageLabel = "<a onclick=\"{0}({1})\" class=\"page-link\" href=\"#\">{2}</a>";
		dotPageLabel = "<span class=\"page-link link-dot\">...</span>";
	}

	@Override
	public String renderPagination(org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo paginationInfo, String jsFunction) {
		updateLabelsWithCurrentLocale();
		return super.renderPagination(paginationInfo, jsFunction);
	}

	private void updateLabelsWithCurrentLocale() {
		String prevLabel = messageSource.getMessage("page.prev", null, "\uc774\uc804", LocaleContextHolder.getLocale());
		String nextLabel = messageSource.getMessage("page.next", null, "\ub2e4\uc74c", LocaleContextHolder.getLocale());

		previousPageLabel = "<a onclick=\"{0}({1})\" class=\"page-navi prev\" href=\"#\">" + prevLabel + "</a>";
		previousPageDisabledLabel = "<span class=\"page-navi prev disabled\">" + prevLabel + "</span>";
		nextPageLabel = "<a onclick=\"{0}({1})\" class=\"page-navi next\" href=\"#\">" + nextLabel + "</a>";
		nextPageDisabledLabel = "<span class=\"page-navi next disabled\">" + nextLabel + "</span>";
	}

}
