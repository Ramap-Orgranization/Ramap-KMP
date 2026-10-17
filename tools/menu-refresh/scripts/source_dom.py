"""Bind a canonical capture to saved Naver menu DOM, without executing page code."""
import json
import re
from html.parser import HTMLParser
from urllib.parse import parse_qs, urlparse


def compact(value):
    return re.sub(r'\s+', '', (value or '').strip().lower())


def text_content(value):
    return re.sub(r'\s+', '', value or '')


def image_origin(value):
    if not value:
        return None
    url = urlparse(value)
    return parse_qs(url.query).get('src', [value])[0] if url.hostname == 'search.pstatic.net' else value


class Element:
    def __init__(self, tag='', attrs=(), parent=None):
        self.tag, self.attrs, self.parent = tag, dict(attrs), parent
        self.children = []

    def walk(self):
        yield self
        for child in self.children:
            if isinstance(child, Element):
                yield from child.walk()

    def text(self):
        if self.tag == 'br':
            return '\n'
        return ''.join(c.text() if isinstance(c, Element) else c for c in self.children)

    def has_class(self, names):
        return bool(set(self.attrs.get('class', '').split()) & set(names))


class MenuParser(HTMLParser):
    VOID = {'img', 'br', 'hr', 'meta', 'link', 'input', 'source', 'wbr', 'area', 'embed', 'param', 'track', 'col'}

    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.root = self.current = Element()

    def handle_starttag(self, tag, attrs):
        element = Element(tag, attrs, self.current)
        self.current.children.append(element)
        if tag not in self.VOID:
            self.current = element

    def handle_startendtag(self, tag, attrs):
        self.current.children.append(Element(tag, attrs, self.current))

    def handle_endtag(self, tag):
        element = self.current
        while element.parent and element.tag != tag:
            element = element.parent
        if element.parent:
            self.current = element.parent

    def handle_data(self, data):
        self.current.children.append(data)


def first(nodes, classes):
    return next((node for node in nodes if node.has_class(classes)), None)


def heading(element):
    parent = element.parent
    while parent:
        if parent.has_class({'place_section'}):
            title = first(parent.walk(), {'place_section_header_title'})
            return title.text().strip() if title else ''
        parent = parent.parent
    return ''


def price_key(value):
    text = re.sub(r'[,\s원]', '', value or '')
    return int(text) if text.isdigit() else compact(value)


def signature(row):
    return (text_content(row['name']), price_key(row['price_text']), text_content(row['description']),
        image_origin(row['image_url']), row['is_featured'])


def recommendation_signature(row):
    # Same name/price alone can hide distinct source configurations or photos.
    return (compact(row['name']), price_key(row['price_text']), text_content(row['description']),
        image_origin(row['image_url']))


def canonical_dom(raw_dom, place_id):
    # Legacy compact captures may contain the same complete DOM panel twice.
    # Remove only byte-identical whole panels, never duplicate regular rows.
    duplicate_panels = 0
    if raw_dom.lstrip().startswith('{'):
        capture = json.loads(raw_dom)
        panels = capture.get('menu_sections')
        if not isinstance(panels, list) or not panels or not all(isinstance(p, str) for p in panels):
            raise ValueError('compact DOM capture requires menu_sections')
        unique = list(dict.fromkeys(panels))
        duplicate_panels = len(panels) - len(unique)
        raw_dom = '\n'.join(unique)
    parser = MenuParser()
    parser.feed(raw_dom)
    rows = []
    for element in parser.root.walk():
        if element.tag != 'li':
            continue
        nodes = list(element.walk())
        name = first(nodes, {'lZCdr', 'lPzHi'})
        link = next((n for n in nodes if n.attrs.get('data-nlog-area') in ('plc_bmv.menu', 'plc_bmv.recmenu')), None)
        if name is None or link is None:
            continue
        params = json.loads(link.attrs.get('data-nlog-params', '{}'))
        if str(params.get('place_id')) != place_id:
            raise ValueError('raw DOM branch ID is missing or mismatched')
        price = first(nodes, {'N4UFh', 'p2H02'})
        if price is None:
            raise ValueError('raw DOM menu price is missing')
        description = first(nodes, {'NjVSW', 'okI98'})
        image = next((n for n in nodes if n.tag == 'img' and n.attrs.get('src')), None)
        featured = any(n.has_class({'place_blind'}) and n.text().strip() == '대표' for n in nodes)
        rows.append({'name': name.text().strip(), 'price_text': price.text().strip(),
            'description': description.text().strip() if description else None,
            'image_url': image.attrs['src'] if image else None, 'is_featured': featured,
            'recommended': link.attrs['data-nlog-area'] == 'plc_bmv.recmenu', 'heading': heading(element)})
    if not rows:
        raise ValueError('raw DOM contains no recognized menu rows; inspect changed source markup')
    summaries = [r for r in rows if compact(r['heading']) == '대표메뉴']
    menus = [r for r in rows if compact(r['heading']) != '대표메뉴']
    actual_signatures = {signature(r) for r in menus}
    if any(signature(r) not in actual_signatures for r in summaries):
        raise ValueError('representative summary contains a menu missing from actual categories')
    # Representative panels are rendered before the general categories even
    # when Naver labels their links as plc_bmv.menu rather than recmenu.
    recommendations = []
    recommended_keys = set()
    for row in rows:
        if row['recommended'] or compact(row['heading']) == '대표메뉴':
            key = recommendation_signature(row)
            if key not in recommended_keys:
                recommendations.append(row)
                recommended_keys.add(key)
    regular = [r for r in menus if not r['recommended']
        and recommendation_signature(r) not in recommended_keys]
    canonical = recommendations + regular
    return canonical, {'parsed_rows': len(rows), 'summary_rows': len(summaries),
        'duplicate_complete_panels': duplicate_panels, 'recommendation_duplicates': len(menus) - len(canonical)}


def reconcile_source(source, raw_dom, source_url):
    match = re.search(r'/(?:place|restaurant)/(\d+)', source_url)
    if not match:
        raise ValueError('source URL has no place ID')
    canonical, evidence = canonical_dom(raw_dom, match.group(1))
    if len(source) != len(canonical):
        raise ValueError('canonical source count contradicts raw DOM')
    for index, (item, raw) in enumerate(zip(source, canonical)):
        fixed = type(item.get('price')) is int
        actual_price = price_key(raw['price_text'])
        if text_content(item['name']) != text_content(raw['name']):
            raise ValueError(f'canonical source order/name contradicts raw DOM at row {index + 1}')
        if (item['price'] != actual_price if fixed else type(actual_price) is int
            or compact(item.get('price_text')) != compact(raw['price_text'])):
            raise ValueError(f'canonical source price contradicts raw DOM at row {index + 1}')
        if text_content(item.get('description')) != text_content(raw['description']):
            raise ValueError(f'canonical source description contradicts raw DOM at row {index + 1}')
        if image_origin(item.get('image_url')) != image_origin(raw['image_url']):
            raise ValueError(f'canonical source image contradicts raw DOM at row {index + 1}')
        if bool(item.get('is_featured')) != raw['is_featured']:
            raise ValueError(f'canonical source featured flag contradicts raw DOM at row {index + 1}')
    evidence.update(canonical_rows=len(canonical), canonical_image_rows=sum(bool(r['image_url']) for r in canonical))
    return evidence
