#!/usr/bin/env python3
"""Import ARC Portugal verses from a pinned snapshot; standard library only."""
import hashlib
import json
from pathlib import Path
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'
SOURCE = ROOT / 'data'
COMMIT = 'f8f3ceeb66859c19e86bcb1f6d8de8c8975ba7e4'
URL = (f'https://raw.githubusercontent.com/mrk214/snapshots/{COMMIT}/'
       'pt___por___por_pt/ARC_vid_215.json')
BOOK_IDS = ('GEN EXO LEV NUM DEU JOS JDG RUT 1SA 2SA 1KI 2KI 1CH 2CH EZR NEH '
            'EST JOB PSA PRO ECC SNG ISA JER LAM EZK DAN HOS JOL AMO OBA JON '
            'MIC NAM HAB ZEP HAG ZEC MAL MAT MRK LUK JHN ACT ROM 1CO 2CO GAL '
            'EPH PHP COL 1TH 2TH 1TI 2TI TIT PHM HEB JAS 1PE 2PE 1JN 2JN '
            '3JN JUD REV').split()

# European Portuguese book names. The verses themselves are never rewritten.
NAMES = ('Génesis|Êxodo|Levítico|Números|Deuteronómio|Josué|Juízes|Rute|'
         '1 Samuel|2 Samuel|1 Reis|2 Reis|1 Crónicas|2 Crónicas|Esdras|Neemias|'
         'Ester|Job|Salmos|Provérbios|Eclesiastes|Cântico dos Cânticos|Isaías|'
         'Jeremias|Lamentações|Ezequiel|Daniel|Oseias|Joel|Amós|Obadias|Jonas|'
         'Miqueias|Naum|Habacuc|Sofonias|Ageu|Zacarias|Malaquias|Mateus|Marcos|'
         'Lucas|João|Atos|Romanos|1 Coríntios|2 Coríntios|Gálatas|Efésios|'
         'Filipenses|Colossenses|1 Tessalonicenses|2 Tessalonicenses|1 Timóteo|'
         '2 Timóteo|Tito|Filémon|Hebreus|Tiago|1 Pedro|2 Pedro|1 João|2 João|'
         '3 João|Judas|Apocalipse').split('|')


def main():
    SOURCE.mkdir(exist_ok=True)
    ASSETS.mkdir(parents=True, exist_ok=True)
    raw_path = SOURCE / 'arc-pt-pt.json'
    if not raw_path.exists():
        request = urllib.request.Request(URL, headers={'User-Agent': 'BibliaWear/1.0'})
        with urllib.request.urlopen(request, timeout=120) as response:
            raw_path.write_bytes(response.read())
    raw = raw_path.read_bytes()
    bible = json.loads(raw)
    assert bible['version_id'] == 215
    assert bible['language']['language_tag'] == 'por_pt'
    books = bible['books']
    assert len(books) == len(NAMES) == 66
    assert [book['usfm'] for book in books] == BOOK_IDS
    index = []
    total = 0
    for number, book in enumerate(books, 1):
        chapters = book['chapters']
        counts = []
        folder = ASSETS / 'chapters' / str(number)
        folder.mkdir(parents=True, exist_ok=True)
        for chapter_number, chapter in enumerate(chapters, 1):
            assert chapter['usfm'] == f'{book["usfm"]}.{chapter_number}'
            verses = [item for item in chapter['items'] if item['type'] == 'verse']
            assert [v['verse_numbers'] for v in verses] == [[i] for i in range(1, len(verses) + 1)]
            texts = [' '.join(verse['lines']).strip() for verse in verses]
            assert all(texts)
            (folder / f'{chapter_number}.json').write_text(
                json.dumps(texts, ensure_ascii=False, separators=(',', ':')), encoding='utf-8')
            counts.append(len(texts))
            total += len(texts)
        index.append({'name': NAMES[number - 1], 'verses': counts})
    (ASSETS / 'books.json').write_text(json.dumps(index, ensure_ascii=False), encoding='utf-8')
    metadata = {'source': URL, 'snapshot_commit': COMMIT,
                'translation': bible['local_title'], 'version_id': bible['version_id'],
                'publisher': bible['publisher']['name'], 'copyright': bible['copyright']['text'],
                'sha256': hashlib.sha256(raw).hexdigest(),
                'books': 66, 'chapters': sum(len(b['verses']) for b in index), 'verses': total}
    assert metadata['chapters'] == 1189 and total == 31105
    (SOURCE / 'provenance.json').write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(metadata, indent=2))


if __name__ == '__main__':
    main()
