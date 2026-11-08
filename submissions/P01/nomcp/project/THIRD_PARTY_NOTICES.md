# Third-party resources

## KRDS HTML Component Kit

- Distribution: `krds-uiux`, version `1.0.3`, author `krds`.
- Official guidance: https://www.krds.go.kr/html/site/outline/outline_03.html
- Package: https://www.npmjs.com/package/krds-uiux/v/1.0.3
- Package-declared license: ISC.
- Included resources: `static/vendor/krds/krds.min.css`, icon SVG assets and Pretendard GOV font files from the same distribution.
- Changes: external icon URLs in the bundled CSS are replaced with relative URLs for offline use. Application-specific styles are in `static/css/board.css`.

Pretendard GOV fonts are based on Pretendard; see https://github.com/orioncactus/pretendard for the SIL Open Font License and upstream attribution. The original font filenames have been retained.

Java dependencies are resolved by Maven; their license metadata is available in their respective Maven artifacts.
