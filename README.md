This app

- update mp3s' title and authors with ID3v1 to ID3v2
- if both ID3v1 and ID3v2 tags are missing, fetch title and author from filename
  - ignore files that have 'feat', '(', but mark them unprocessed by appending '_unprocessed' to thier filename_
  - ignore files that have '_unprocessed'
- depend on library [mp3agic](https://github.com/mpatric/mp3agic), "A java library for reading mp3 files and reading / manipulating the ID3 tags (ID3v1 and ID3v2.2 through ID3v2.4)."