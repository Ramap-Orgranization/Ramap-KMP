() => {
  // Run only through the documented CUA read-only DOM evaluator.
  const normalized = v => (v || '').trim().toLowerCase().replace(/\s+/g, '');
  const fixed = v => /^[\d,\s]+(?:원)?$/.test(v || '') ? Number(v.replace(/[,\s원]/g, '')) : null;
  const photo = v => typeof v === 'string' && /^https:\/\//.test(v) && !/placeholder|blank\.(gif|png)/i.test(v);
  const original = v => v && (new URL(v).searchParams.get('src') || v);
  const issues = [];
  const placeId = location.pathname.match(/\/restaurant\/(\d+)/)?.[1];
  const marker = 'window.__APOLLO_STATE__ = ';
  const script = [...document.querySelectorAll('script')].find(s => s.textContent?.includes(marker))?.textContent;
  let state = {};
  if (script) {
    const start = script.indexOf(marker) + marker.length;
    let depth = 0, quoted = false, escaped = false, end = start;
    for (; end < script.length; end++) {
      const c = script[end];
      if (quoted) {
        if (escaped) escaped = false;
        else if (c === '\\') escaped = true;
        else if (c === '"') quoted = false;
      } else if (c === '"') quoted = true;
      else if (c === '{') depth++;
      else if (c === '}' && --depth === 0) { end++; break; }
    }
    try { state = JSON.parse(script.slice(start, end)); }
    catch (_) { issues.push('직렬화 메뉴 JSON 파싱 실패'); }
  } else issues.push('직렬화 메뉴 상태 미확인');
  const stateRows = Object.entries(state).filter(([, v]) => ['PlaceMenuItem', 'PlaceDetail_BaeminMenu'].includes(v?.__typename));
  const statePrice = row => typeof row.price === 'object' ? row.price?.displayText : String(row.price ?? '');
  const images = row => [row.thumbnailUrl, ...(row.images || []).map(i => typeof i === 'string' ? i : i?.url)].filter(photo);
  const base = Object.values(state).find(v => v?.__typename === 'PlaceDetailBase' && String(v.id) === placeId);
  const sections = [];
  for (const section of document.querySelectorAll('.place_section_content')) {
    const rows = [...section.querySelectorAll('li')].filter(row => row.querySelector('.lZCdr,.lPzHi'));
    if (!rows.length) continue;
    const items = rows.map(row => {
      const name = row.querySelector('.lZCdr,.lPzHi').textContent.trim();
      const priceText = row.querySelector('.N4UFh,.p2H02')?.textContent?.trim() || null;
      const price = fixed(priceText);
      const matches = stateRows.map(([, v]) => v).filter(v => normalized(v.name) === normalized(name)
        && fixed(statePrice(v)) === price && (price !== null || normalized(statePrice(v)) === normalized(priceText)));
      const img = row.querySelector('.place_thumb img');
      const domUrl = img && [img.currentSrc, img.getAttribute('src'), img.getAttribute('data-src'),
        img.getAttribute('data-lazy-src'), img.getAttribute('srcset')?.split(',')[0]?.trim()?.split(/\s+/)[0]].find(photo);
      const stateUrls = matches.length === 1 ? images(matches[0]) : [];
      const imageUrl = domUrl || stateUrls[0] || null;
      const exposed = !!img || matches.some(v => v.hasImage || images(v).length);
      const description = row.querySelector('.NjVSW,.okI98');
      if (exposed && !imageUrl) issues.push('사진 미수집: ' + name);
      if (img && !(img.complete && img.naturalWidth > 0)) issues.push('이미지 로딩 재확인 필요: ' + name);
      if (matches.length > 1) issues.push('같은 이름·가격의 상태 매칭 불명확: ' + name);
      if (stateUrls.length && imageUrl && !stateUrls.map(original).includes(original(imageUrl)))
        issues.push('같은 메뉴 DOM/상태 이미지 충돌: ' + name);
      if (!priceText) issues.push('가격 미수집: ' + name);
      return {source_id: matches.length === 1 ? String(matches[0].id ?? '') || null : null,
        name, price, price_text: priceText, description: description ? description.textContent.trim() : null,
        image_url: imageUrl, source_image_url: original(imageUrl),
        is_featured: [...row.querySelectorAll('.place_blind')].some(e => e.textContent.trim() === '대표'),
        image_exposed: exposed, image_loaded: img ? img.complete && img.naturalWidth > 0 : null,
        state_matches: matches.length};
    });
    const container = section.closest('.place_section') || section;
    sections.push({heading: container.querySelector('h2,h3')?.textContent?.trim() || '',
      items, raw_html: container.outerHTML});
  }
  if (!sections.length) issues.push('수집 가능한 메뉴 목록 없음');
  if (!base) issues.push('같은 Place ID의 매장 식별 정보 없음');
  const boardImages = [...document.querySelectorAll('a[data-nlog-area="plc_bmv.menupanimg"] img,img[alt*="메뉴판"]')]
    .map(img => {
      const url = [img.currentSrc, img.getAttribute('src'), img.getAttribute('data-src')].find(photo) || null;
      return {image_url: url, source_image_url: original(url), alt: img.getAttribute('alt'),
        loaded: img.complete && img.naturalWidth > 0, raw_html: img.closest('a')?.outerHTML || img.outerHTML};
    });
  // Text-only sections omit menu-board galleries and cannot prove absence.
  const rawHtml = document.documentElement?.outerHTML || null;
  if (!rawHtml) issues.push('페이지 전체 원문 미수집');
  return {source_url: location.href, captured_at: new Date().toISOString(), parser_version: 'naver-menu-rendered-v2',
    source_shop: base ? {id: String(base.id), name: base.name, address: base.roadAddress || base.address,
      legacy_address: base.address} : null, sections, raw_html: rawHtml, menu_board_images: boardImages,
    source_coverage: {scope: rawHtml ? 'full_document' : 'text_sections_only',
      menu_board_count: boardImages.length, menu_board_unloaded: boardImages.filter(b => !b.image_url || !b.loaded).length},
    state_menu_records: stateRows,
    state_menu_categories: Object.entries(state).filter(([, v]) => /MenuCategory/.test(v?.__typename || '')),
    issues};
}
