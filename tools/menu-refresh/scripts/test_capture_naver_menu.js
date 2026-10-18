const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

const source = fs.readFileSync(__dirname + '/capture_naver_menu.js', 'utf8');
function capture(images) {
  const state = {
    base: {__typename: 'PlaceDetailBase', id: '123', name: '테스트', roadAddress: '서울 1'},
    menu: {__typename: 'PlaceDetail_BaeminMenu', id: 'menu-1', name: '쇼유라멘', price: '10000', images}
  };
  const row = {
    querySelector: selector => selector === '.lZCdr,.lPzHi' ? {textContent: '쇼유라멘'}
      : selector === '.N4UFh,.p2H02' ? {textContent: '10,000원'} : null,
    querySelectorAll: () => []
  };
  const container = {querySelector: () => ({textContent: '라멘'}), outerHTML: '<section></section>'};
  const section = {querySelectorAll: () => [row], closest: () => container};
  const document = {querySelectorAll: selector => selector === 'script'
    ? [{textContent: 'window.__APOLLO_STATE__ = ' + JSON.stringify(state)}] : [section]};
  return vm.runInNewContext('(' + source + ')()', {
    document, location: {pathname: '/restaurant/123/menu/list', href: 'https://m.place.naver.com/restaurant/123/menu/list'}, URL
  }).sections[0].items[0];
}
assert.equal(capture(['https://example.com/menu.jpg']).image_url, 'https://example.com/menu.jpg');
assert.equal(capture([{url: 'https://example.com/menu.jpg'}]).image_url, 'https://example.com/menu.jpg');
const empty = capture(['', null, {url: ''}, 'data:image/png;base64,AA==']);
assert.equal(empty.image_url, null);
assert.equal(empty.image_exposed, false);
console.log('capture_naver_menu: 3 image representation regressions passed');
