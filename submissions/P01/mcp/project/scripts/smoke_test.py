#!/usr/bin/env python3
"""실행 중인 board-app HTTP 검증. Python 3 표준 라이브러리만 사용."""
import html
import http.cookiejar
import re
import sys
import uuid
from concurrent.futures import ThreadPoolExecutor
from urllib.error import HTTPError
from urllib.parse import urlencode, unquote
from urllib.request import Request, build_opener, HTTPCookieProcessor, HTTPRedirectHandler

BASE = (sys.argv[1] if len(sys.argv) > 1 else 'http://localhost:8080').rstrip('/')
class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None
opener = build_opener(HTTPCookieProcessor(http.cookiejar.CookieJar()), NoRedirect())

def request(path, data=None, files=None, token=True):
    headers = {}
    if data is not None:
        data = dict(data)
        if token:
            data['_csrf'] = csrf
        if files is not None:
            boundary = uuid.uuid4().hex
            parts = []
            for key, val in data.items():
                parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{val}\r\n'.encode())
            for name, content in files:
                parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="file_1"; filename="{name}"\r\nContent-Type: application/octet-stream\r\n\r\n'.encode() + content + b'\r\n')
            data = b''.join(parts) + f'--{boundary}--\r\n'.encode()
            headers['Content-Type'] = 'multipart/form-data; boundary=' + boundary
        else:
            data = urlencode(data).encode()
            headers['Content-Type'] = 'application/x-www-form-urlencoded'
    try:
        res = opener.open(Request(BASE + path, data=data, headers=headers), timeout=20)
    except HTTPError as ex:
        res = ex
    return res.code, res.headers, res.read()

def text(response):
    return response[2].decode('utf-8')
def check(ok, message):
    assert ok, message
    print('PASS:', message)
def detail(board):
    res = request('/updateBoardView.do', {'boardId': board})
    check(res[0] == 200, '수정 화면 조회')
    return text(res)
def links(page):
    return [html.unescape(x) for x in re.findall(r'href="([^"]*FileDown.do[^"]*)"', page)]

initial = request('/egovBoardList.do')
check(initial[0] == 200, '내장 HSQL 목록 화면')
csrf = re.search(r'<meta name="_csrf" content="([^"]+)"', text(initial)).group(1)
check(initial[1].get('X-Content-Type-Options') == 'nosniff', '보안 응답 헤더')
check(request('/addBoardView.do', {}, token=False)[0] == 403, 'CSRF 없는 POST 거부')
check(request('/addBoardView.do', {})[0] == 200, '등록 화면')
mark = '검증_' + uuid.uuid4().hex[:8]
fields = {'title': mark, 'content': '본문검증 <script>alert(1)</script>', 'writer': '검증작성자'}
res = request('/addBoard.do', fields, [('first.txt', b'first attachment'), ('second.txt', b'second attachment')])
check(res[0] == 302, '게시물과 첨부파일 2개 등록')
search = text(request('/egovBoardList.do?' + urlencode({'searchCondition':'0', 'searchKeyword':mark})))
board = re.search(r'BOARD-[0-9]+', search).group(0)
check('&lt;script&gt;' in search and '<script>alert(1)</script>' not in search, '게시물 XSS 출력 이스케이프')
page = detail(board)
urls = links(page)
check(len(urls) == 2, '수정 화면 첨부 목록 2개')
for url, expected in zip(urls, [b'first attachment', b'second attachment']):
    down = request(url)
    check(down[0] == 200 and down[2] == expected and down[1]['Content-Disposition'].startswith('attachment;'), '첨부 다운로드 헤더 및 원본 바이트')
group = re.search(r'name="atchFileId" value="([^"]+)"', page).group(1)
fields.update(boardId=board, atchFileId='FILE_999999999999999', title=mark + '_수정')
check(request('/updateBoard.do', fields, [('third.txt', b'third attachment')])[0] == 302, '수정 및 첨부 추가')
page = detail(board)
check(group in page and len(links(page)) == 3, '요청의 첨부 ID 변조 무시 및 기존 그룹 유지')
with ThreadPoolExecutor(max_workers=2) as pool:
    concurrent = list(pool.map(lambda i: request('/updateBoard.do', fields, [('parallel'+str(i)+'.txt', str(i).encode())])[0], range(2)))
check(concurrent == [302, 302] and len(links(detail(board))) == 5, '동시 첨부 추가의 연번 충돌 방지')
for condition, keyword in [('0', mark), ('1', '본문검증'), ('2', '검증작성자')]:
    check(board in text(request('/egovBoardList.do?' + urlencode({'searchCondition': condition, 'searchKeyword':keyword}))), '검색 조건 ' + condition)
check(board not in text(request('/egovBoardList.do?' + urlencode({'searchKeyword': "' OR 1=1 --"}))), '검색 SQL 주입 문자열 처리')
invalid = dict(fields, title=' ')
res = request('/updateBoard.do', invalid, [])
check(res[0] == 200 and '제목을 입력하세요.' in text(res), '서버 필수값 검증')
res = request('/updateBoard.do', dict(fields, title='x'*201), [])
check(res[0] == 200 and '200자 이내' in text(res), '서버 길이 검증')
res = request('/updateBoard.do', fields, [('good.txt', b'rollback me'), ('bad.jsp', b'blocked')])
check(res[0] < 500 and len(links(detail(board))) == 5, '실행 파일 차단과 복수 업로드 롤백')
check(request('/deleteBoard.do', {'boardId':board, 'atchFileId':'wrong'})[0] == 302, '게시물 삭제')
check(request(urls[0])[0] == 404, '삭제한 게시물의 첨부 다운로드 차단')
check(board not in text(request('/egovBoardList.do?' + urlencode({'searchKeyword':mark}))), '삭제 목록 반영')
# 11개 게시물로 페이지당 10건 계약 확인 후 정리.
ids = []
for i in range(11):
    check(request('/addBoard.do', {'title':mark+'_page_'+str(i), 'content':'', 'writer':'페이지검증'}, [])[0] == 302, '내용 선택 입력 등록 ' + str(i))
for index, count in [(1,10), (2,1)]:
    page = text(request('/egovBoardList.do?' + urlencode({'searchKeyword':mark+'_page_', 'pageIndex':index})))
    found = set(re.findall(r'BOARD-[0-9]+', page))
    check(len(found) == count, '10건 단위 페이징 ' + str(index))
    ids.extend(found)
for board in ids:
    check(request('/deleteBoard.do', {'boardId':board})[0] == 302, '검증 데이터 정리')
print('모든 HTTP 검증 통과')
