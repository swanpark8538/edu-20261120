package egovframework.example.cmm.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.egovframe.rte.fdl.idgnr.EgovIdGnrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

/**
 * 첨부파일 디스크 IO + atchFileId 채번을 담당하는 유틸리티 컴포넌트.
 *
 * `EgovFileMngService` 가 DB 메타 CRUD 라면, 본 클래스는 multipart → 디스크 + FileVO[]
 * 변환과 다운로드 스트리밍을 책임진다. 두 클래스가 협력해 표준 첨부파일 패턴을 구성.
 *
 * 보안 방어선:
 * - {@link #safeChild(Path, String)} 으로 path traversal(../) 차단
 * - 저장 파일명은 timestamp + 순번으로 재생성하므로 사용자 입력 파일명이 디스크에 직접 닿지 않음
 * - {@link #DEFAULT_BLOCKED_EXTENSIONS} 로 실행 가능 확장자 차단 (P0 — exe/bat/sh/jsp/php/svg/html 등)
 * - {@code egov.attach.blockedExtensions} CSV 로 운영 정책 차단 확장자 추가 (P1)
 * - {@code egov.attach.allowedExtensions} CSV 로 화이트리스트 모드 활성화 (P1, 비어 있으면 블랙리스트만)
 * - {@code egov.attach.maxFileSize} 로 도메인 정책 파일 크기 한도 — Spring multipart 와 별개 검사 (P1)
 * - {@link #init()} (@PostConstruct) 로 저장 경로가 webroot/classpath 와 겹치면 거부
 *
 * Bean name 은 "EgovFileMngUtil" (대문자 E) — 공통컴포넌트와 동일한 표준값.
 */
@Component("EgovFileMngUtil")
public class EgovFileMngUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(EgovFileMngUtil.class);

	/**
	 * 업로드를 차단할 실행 가능 확장자 (대문자 비교). 더블 확장자(`a.jpg.exe`) 의 마지막 확장자 기준으로 매칭한다.
	 * 운영 정책으로 추가 차단이 필요하면 {@code egov.attach.blockedExtensions} CSV 에 더한다 — 본 셋과 병합되어 적용된다.
	 *
	 * 그룹별 차단 사유:
	 * - OS 실행파일: 다운로드 받은 사용자가 더블클릭하면 즉시 실행
	 * - 셸 스크립트: OS 쉘에서 직접 실행 가능
	 * - 서버사이드 스크립트(JSP/PHP/ASP 등): storePath 가 webroot 하위에 놓이는 사고가 발생해도 한 번 더 막는다 (defense-in-depth)
	 * - HTML/SVG: 브라우저가 인라인 렌더링 시 XSS 페이로드 실행
	 * - LNK/URL: 사회공학 (Windows 바로가기로 위장)
	 */
	private static final Set<String> DEFAULT_BLOCKED_EXTENSIONS;
	static {
		Set<String> s = new HashSet<>(Arrays.asList(
				// OS 실행파일
				"EXE", "BAT", "CMD", "COM", "SCR", "MSI", "DLL", "APP", "DMG", "PKG", "DEB", "RPM",
				// 셸 / 매크로 스크립트
				"SH", "BASH", "ZSH", "PS1", "PSM1", "VBS", "WSF", "VBE", "JS", "MJS",
				// 서버사이드 스크립트
				"JSP", "JSPX", "JSPF", "PHP", "PHTML", "PHP3", "PHP4", "PHP5", "PHP7", "PHP8",
				"ASP", "ASPX", "CER", "ASA", "CGI", "PL", "PY", "RB",
				// Java 아카이브 / 클래스
				"JAR", "WAR", "EAR", "CLASS",
				// Apache 설정
				"HTACCESS", "HTPASSWD",
				// 브라우저 렌더링(XSS 가능)
				"HTML", "HTM", "SHTML", "XHTML", "SVG", "MHT", "MHTML",
				// 사회공학
				"LNK", "URL", "DESKTOP", "INF", "REG"));
		DEFAULT_BLOCKED_EXTENSIONS = Collections.unmodifiableSet(s);
	}

	/**
	 * {@link #validateStorePath()} 가 webroot/classpath 겹침을 감지할 때 사용하는 금지 path 조각.
	 * 절대경로를 정규화한 후 forward-slash 기준으로 contains 매칭한다.
	 */
	private static final List<String> FORBIDDEN_STORE_PATH_SEGMENTS = Collections.unmodifiableList(Arrays.asList(
			"/src/main/resources/static",
			"/src/main/resources/public",
			"/src/main/webapp",
			"/target/classes/static",
			"/target/classes/public",
			"/build/resources/main/static",
			"/build/resources/main/public",
			"/BOOT-INF/classes/static",
			"/BOOT-INF/classes/public"));

	@Value("${Globals.fileStorePath:}")
	private String defaultStorePath;

	/** 도메인 정책 파일 크기 한도. Spring multipart 한도와 별개로 본 클래스가 한 번 더 검사한다 (P1-2). */
	@Value("${egov.attach.maxFileSize:10MB}")
	private String maxFileSizeStr;

	/** 화이트리스트 (CSV). 비어 있으면 화이트리스트 비활성화 — 블랙리스트만 적용 (P1-1). */
	@Value("${egov.attach.allowedExtensions:}")
	private String allowedExtensionsCsv;

	/** 추가 블랙리스트 (CSV). {@link #DEFAULT_BLOCKED_EXTENSIONS} 와 병합되어 적용 (P1-1). */
	@Value("${egov.attach.blockedExtensions:}")
	private String additionalBlockedCsv;

	@Resource(name = "egovFileIdGnrService")
	private EgovIdGnrService idgenService;

	/** {@link #init()} 에서 결정. 빈 셋이면 화이트리스트 비활성. */
	private Set<String> allowedExtensions = Collections.emptySet();
	/** {@link #init()} 에서 DEFAULT + additional 합산. */
	private Set<String> effectiveBlockedExtensions = DEFAULT_BLOCKED_EXTENSIONS;
	/** {@link #init()} 에서 DataSize 파싱 결과 (bytes). */
	private long maxFileSizeBytes;

	/**
	 * 시작 시점에 한 번만 보안 정책을 검증/초기화한다.
	 * - storePath 가 webroot/classpath 하위면 거부 (P0)
	 * - maxFileSize 파싱 + 화이트/블랙리스트 셋 구성 (P1)
	 *
	 * 정책 변경 시 application.properties 의 키만 수정 후 재시작하면 반영된다.
	 */
	@PostConstruct
	public void init() {
		if (defaultStorePath != null && !defaultStorePath.isEmpty()) {
			assertSafeStorePath(defaultStorePath);
		}
		try {
			this.maxFileSizeBytes = DataSize.parse(maxFileSizeStr).toBytes();
		} catch (Exception e) {
			throw new IllegalStateException(
					"egov.attach.maxFileSize 파싱에 실패했습니다 (값: '" + maxFileSizeStr + "'). 예: 10MB, 50MB, 1GB", e);
		}
		this.allowedExtensions = parseExtensionCsv(allowedExtensionsCsv);
		Set<String> blocked = new HashSet<>(DEFAULT_BLOCKED_EXTENSIONS);
		blocked.addAll(parseExtensionCsv(additionalBlockedCsv));
		this.effectiveBlockedExtensions = Collections.unmodifiableSet(blocked);
		LOGGER.info(
				"EgovFileMngUtil 초기화: maxFileSize={} ({}B), 화이트리스트={}건, 블랙리스트={}건 (기본+추가)",
				maxFileSizeStr, maxFileSizeBytes, allowedExtensions.size(), effectiveBlockedExtensions.size());
	}

	/** CSV 문자열을 대문자 확장자 셋으로 변환. 빈 문자열은 emptySet. */
	private static Set<String> parseExtensionCsv(String csv) {
		if (csv == null || csv.isEmpty()) return Collections.emptySet();
		Set<String> out = new HashSet<>();
		for (String s : csv.split(",")) {
			String t = s.trim().toUpperCase(Locale.ROOT);
			if (t.startsWith(".")) t = t.substring(1);
			if (!t.isEmpty()) out.add(t);
		}
		return Collections.unmodifiableSet(out);
	}

	/**
	 * storePath 가 web 정적 리소스 / classpath 와 겹치는지 검사한다.
	 * 부적격이면 {@link IllegalStateException} 으로 거부 — 운영 안전성 우선.
	 */
	private static void assertSafeStorePath(String storePath) {
		String slashed;
		try {
			slashed = Paths.get(storePath).normalize().toAbsolutePath().toString().replace('\\', '/');
		} catch (Exception e) {
			throw new IllegalStateException("Globals.fileStorePath 를 정규화할 수 없습니다: " + storePath, e);
		}
		if (slashed.startsWith("classpath:")) {
			throw new IllegalStateException(
					"Globals.fileStorePath 에 classpath: 경로를 지정할 수 없습니다 (" + storePath + "). 외부 파일시스템 절대경로를 사용하세요.");
		}
		for (String bad : FORBIDDEN_STORE_PATH_SEGMENTS) {
			if (slashed.contains(bad)) {
				throw new IllegalStateException(
						"Globals.fileStorePath 가 web 정적 리소스/classpath 경로와 겹칩니다 (" + storePath + "). " +
								"업로드 파일이 URL 로 직접 노출되어 실행될 위험이 있으니, 외부 디렉터리(예: /opt/app/uploads, /var/lib/app/uploads)를 사용하세요.");
			}
		}
	}

	/**
	 * multipart 파일들을 디스크에 저장하고 FileVO 목록을 생성한다. DB INSERT 는 하지 않는다
	 * — 호출자가 이어서 `EgovFileMngService.insertFileInfs(...)` 또는 `updateFileInfs(...)` 를 부른다.
	 *
	 * @param files          업로드된 multipart 파일들 (보통 `multiRequest.getFiles("file_1")`)
	 * @param keyStr         저장 파일명 prefix (도메인별, 예: "FAQ_")
	 * @param fileKeyParam   첫 FILE_SN 값. 신규 그룹이면 1, 기존 그룹에 추가면 `getMaxFileSN()` 결과
	 * @param atchFileId     기존 그룹에 append 시 그룹 키; 빈 문자열이면 새로 채번
	 * @param storePath      저장 경로 — 빈 문자열이면 `Globals.fileStorePath` 기본값 사용
	 * @return 생성된 FileVO 목록 (모두 동일 atchFileId 공유, FILE_SN 은 fileKeyParam 부터 증가)
	 */
	public List<FileVO> parseFileInf(List<MultipartFile> files, String keyStr, int fileKeyParam,
			String atchFileId, String storePath) throws Exception {

		int fileKey = (fileKeyParam <= 0) ? 1 : fileKeyParam;
		String resolvedStorePath = (storePath == null || storePath.isEmpty()) ? defaultStorePath : storePath;
		if (resolvedStorePath == null || resolvedStorePath.isEmpty()) {
			throw new IllegalStateException(
					"파일 저장 경로가 비어 있습니다. application.properties 의 'Globals.fileStorePath' 를 설정하세요.");
		}
		// 호출 시점의 storePath 도 한 번 더 검증 — 동적으로 들어오는 값이 webroot 안일 수 있음
		assertSafeStorePath(resolvedStorePath);

		String resolvedAtchFileId =
				(atchFileId == null || atchFileId.isEmpty()) ? idgenService.getNextStringId() : atchFileId;

		Path storeDir = Paths.get(resolvedStorePath).normalize().toAbsolutePath();
		Files.createDirectories(storeDir);

		List<FileVO> result = new ArrayList<>();
		try {
		for (MultipartFile file : files) {
			String origin = file.getOriginalFilename();
			if (origin == null || origin.isEmpty()) {
				continue;
			}
			origin = origin.replace('\\', '/');
            origin = origin.substring(origin.lastIndexOf('/') + 1);
            if (origin.length() > 255 || origin.chars().anyMatch(c -> Character.isISOControl(c))) {
                throw new SecurityException("올바르지 않은 파일명입니다.");
            }
            String ext = getExtensionUpper(origin);
            if (ext.length() > 20) throw new SecurityException("확장자가 너무 깁니다.");
			// P0/P1: 블랙리스트 차단 (기본 셋 + 운영 추가). 더블 확장자(a.jpg.exe) 도 마지막 dot 기준.
			if (effectiveBlockedExtensions.contains(ext)) {
				throw new SecurityException(
						"허용되지 않은 파일 형식입니다 (확장자: " + ext + ", 파일명: " + origin + "). " +
								"보안 정책상 차단된 확장자입니다.");
			}
			// P1-1: 화이트리스트 (지정된 경우만). 도메인 정책으로 특정 확장자만 허용.
			if (!allowedExtensions.isEmpty() && !allowedExtensions.contains(ext)) {
				throw new SecurityException(
						"허용 목록에 없는 확장자입니다 (확장자: " + ext + ", 파일명: " + origin + "). " +
								"허용된 확장자: " + String.join(",", allowedExtensions));
			}
			// P1-2: 도메인 정책 파일 크기 한도 (Spring multipart 와 별개로 한 번 더 검사).
			if (maxFileSizeBytes > 0 && file.getSize() > maxFileSizeBytes) {
				throw new SecurityException(
						"파일 크기가 한도를 초과했습니다 (" + file.getSize() + " bytes > " + maxFileSizeStr +
								", 파일명: " + origin + ").");
			}
			String storedName = keyStr + java.util.UUID.randomUUID();
			Path target = safeChild(storeDir, storedName);
			try {
                file.transferTo(target.toFile());
            } catch (Exception ex) {
                Files.deleteIfExists(target);
                throw ex;
            }

			FileVO vo = new FileVO();
			vo.setAtchFileId(resolvedAtchFileId);
			vo.setFileSn(String.valueOf(fileKey));
			vo.setFileStreCours(storeDir.toString());
			vo.setStreFileNm(storedName);
			vo.setOrignlFileNm(origin);
			vo.setFileExtsn(ext);
			vo.setFileMg(Long.toString(file.getSize()));
			result.add(vo);

			fileKey++;
		}
        } catch (Exception ex) {
            result.forEach(this::deleteStoredFile);
            throw ex;
        }
		return result;
	}

    /** 롤백 또는 게시물 삭제 확정 후 저장 파일을 정리한다. */
    public void deleteStoredFile(FileVO file) {
        try {
            Path directory = Paths.get(file.getFileStreCours()).toAbsolutePath().normalize();
            Files.deleteIfExists(safeChild(directory, file.getStreFileNm()));
        } catch (IOException ex) {
            LOGGER.warn("첨부파일 정리에 실패했습니다.", ex);
        }
    }

	/**
	 * 파일 다운로드 — 단일 FileVO 의 디스크 파일을 response 스트림으로 전송.
	 * 정합한 path 만 허용 (저장 디렉터리 외부 접근 차단).
	 */
	public void downFile(jakarta.servlet.http.HttpServletResponse response,
			String fileStreCours, String streFileNm, String orignlFileNm) throws IOException {
		Path storeDir = Paths.get(fileStreCours).normalize().toAbsolutePath();
		Path target = safeChild(storeDir, streFileNm);

		if (!Files.exists(target) || !Files.isRegularFile(target)) {
			throw new FileNotFoundException(streFileNm);
		}
		long size = Files.size(target);

		String safeOrigin = orignlFileNm == null ? streFileNm : orignlFileNm.replaceAll("[\r\n]", "");
		response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", org.springframework.http.ContentDisposition.attachment()
                .filename(safeOrigin, java.nio.charset.StandardCharsets.UTF_8).build().toString());
		response.setContentLengthLong(size);

		try (java.io.OutputStream out = response.getOutputStream()) {
			Files.copy(target, out);
			out.flush();
		} catch (IOException ex) {
			// "Connection reset by peer" 같은 클라이언트측 종료는 정상 흐름의 일부 — 로그만 남기고 흡수.
			LOGGER.debug("Download stream interrupted: {}", ex.getMessage());
		}
	}

	/**
	 * `storeDir` 의 직계 자식인지 보장하는 path traversal 가드.
	 * 사용자 입력이 닿는 모든 경로 조립 지점에서 호출한다.
	 */
	private static Path safeChild(Path storeDir, String childName) {
		Path candidate = storeDir.resolve(childName).normalize().toAbsolutePath();
		if (!candidate.startsWith(storeDir)) {
			throw new SecurityException("Invalid file path (traversal denied): " + childName);
		}
		return candidate;
	}

	/**
	 * 원본 파일명에서 확장자(점 이후)를 추출해 대문자로 반환한다.
	 *
	 * commons-io {@code FilenameUtils.getExtension} 의 단순 대체 — 외부 의존성을 피한다.
	 * 일부 브라우저가 path 가 섞인 풀 경로를 보내는 케이스를 방어하기 위해 마지막 path
	 * separator 이후 부분에서만 점을 찾는다.
	 */
	private static String getExtensionUpper(String filename) {
		if (filename == null || filename.isEmpty()) {
			return "";
		}
		int sep = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
		String name = (sep >= 0) ? filename.substring(sep + 1) : filename;
		int dot = name.lastIndexOf('.');
		if (dot < 0 || dot >= name.length() - 1) {
			return "";
		}
		return name.substring(dot + 1).toUpperCase(Locale.ROOT);
	}
}
