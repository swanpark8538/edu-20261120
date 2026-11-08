import urllib.request as u, urllib.parse as q, http.cookiejar, re
jar=http.cookiejar.CookieJar(); client=u.build_opener(u.HTTPCookieProcessor(jar)); base='http://localhost:18084'
def get(path): return client.open(base+path)
def post(path, fields, files=[]):
    fields = dict(fields, _csrf=token)
    if not files:
        data=q.urlencode(fields).encode(); ct='application/x-www-form-urlencoded'
    else:
        b='boardSmokeBoundary'; parts=[]
        for k,v in fields.items(): parts.append(f'--{b}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode())
        for name,content in files: parts.append(f'--{b}\r\nContent-Disposition: form-data; name="file_1"; filename="{name}"\r\nContent-Type: text/plain\r\n\r\n'.encode()+content+b'\r\n')
        data=b''.join(parts)+f'--{b}--\r\n'.encode(); ct='multipart/form-data; boundary='+b
    return client.open(u.Request(base+path,data,{'Content-Type':ct}))
html=get('/egovBoardList.do').read().decode(); token=re.search(r'name="_csrf" content="([^"]+)"',html)[1]
assert post('/addBoardView.do',{}).status==200
post('/addBoard.do',dict(title='SmokeTitle',content='SmokeContent',writer='SmokeWriter'),[('first.txt',b'first'),('second.txt',b'second')])
html=get('/egovBoardList.do?searchCondition=0&searchKeyword=SmokeTitle').read().decode(); assert 'SmokeTitle' in html
board=re.search(r'BOARD-[0-9]+',html)[0]
html=post('/updateBoardView.do',dict(boardId=board)).read().decode()
id=re.search(r'name="atchFileId" value="([^"]+)"',html)[1]; assert html.count('/cmm/fms/FileDown.do')==2
res=get('/cmm/fms/FileDown.do?atchFileId='+id+'&fileSn=1'); assert 'attachment' in res.headers['Content-Disposition']; assert res.read()==b'first'
post('/updateBoard.do',dict(boardId=board,title='UpdatedTitle',content='UpdatedContent',writer='UpdatedWriter',atchFileId='tampered'),[('third.txt',b'third')])
html=post('/updateBoardView.do',dict(boardId=board)).read().decode(); assert id in html and html.count('/cmm/fms/FileDown.do')==3
for condition,keyword in [(0,'UpdatedTitle'),(1,'UpdatedContent'),(2,'UpdatedWriter')]:
    assert 'UpdatedTitle' in get(f'/egovBoardList.do?searchCondition={condition}&searchKeyword={keyword}').read().decode()
post('/deleteBoard.do',dict(boardId=board)); assert 'UpdatedTitle' not in get('/egovBoardList.do').read().decode()
try: u.urlopen(u.Request(base+'/deleteBoard.do',q.urlencode(dict(boardId=board)).encode()))
except u.HTTPError as e: assert e.code==403
else: raise AssertionError('CSRF protection missing')
print('PASS: CRUD, search 0/1/2, multi-file upload, download, append/group preservation, CSRF')



