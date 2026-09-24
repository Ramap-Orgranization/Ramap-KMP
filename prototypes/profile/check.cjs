// Run with Playwright available: PROFILE_URL=http://127.0.0.1:8767 node check.cjs
const assert = require('node:assert/strict');
const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: true, ...(process.env.CHROME_PATH ? { executablePath: process.env.CHROME_PATH } : {}) });
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.goto(process.env.PROFILE_URL || 'http://127.0.0.1:8767');
    const initial = await page.locator('#profile-name').textContent();
    assert.match(initial, /^[가-힣]{2,10}$/);
    await page.reload();
    assert.equal(await page.locator('#profile-name').textContent(), initial);
    await page.locator('#edit-button').click();
    assert.equal(await page.locator('#save-button').isDisabled(), true);
    for (const invalid of ['', '면', '라멘 🍜', '가'.repeat(11), '<script>']) {
      await page.locator('#nickname').fill(invalid);
      assert.equal(await page.locator('#save-button').isDisabled(), true);
      assert.equal(await page.locator('#nickname').getAttribute('aria-invalid'), 'true');
    }
    assert.equal(await page.locator('#random-button').count(), 0);
    assert.equal(await page.locator('#nickname-hint').textContent(), '한글·영문·숫자·밑줄만 사용할 수 있어요.');
    for (const valid of ['라멘', '가'.repeat(10)]) {
      await page.locator('#nickname').fill(valid);
      assert.equal(await page.locator('#save-button').isDisabled(), false);
      assert.equal(await page.locator('#counter').textContent(), valid.length + ' / 10');
      assert.equal(await page.locator('#nickname-hint').isVisible(), false);
      assert.equal(await page.locator('#nickname').getAttribute('placeholder'), '2~10글자를 입력해 주세요.');
    }
    await page.locator('#nickname').fill('산책하는차슈');
    await page.locator('#back-button').click();
    assert.equal(await page.locator('#discard-dialog').isVisible(), true);
    await page.getByRole('button', { name: '계속 수정', exact: true }).click();
    assert.equal(await page.locator('#nickname').inputValue(), '산책하는차슈');
    await page.locator('#save-button').click();
    await page.reload();
    assert.equal(await page.locator('#profile-name').textContent(), '산책하는차슈');

    await page.locator('#edit-button').click();
    await page.locator('#nickname').fill('취소할닉네임');
    await page.locator('#back-button').click();
    await page.locator('#discard-button').click();
    assert.equal(await page.locator('#profile-name').textContent(), '산책하는차슈');
    await page.locator('#edit-button').click();
    const png = await page.evaluate(() => {
      const canvas = document.createElement('canvas');
      canvas.width = 160; canvas.height = 90;
      const context = canvas.getContext('2d');
      context.fillStyle = '#e95432'; context.fillRect(0, 0, 160, 90);
      return canvas.toDataURL('image/png').split(',')[1];
    });
    await page.locator('#photo-input').setInputFiles({ name: 'sample.png', mimeType: 'image/png', buffer: Buffer.from(png, 'base64') });
    await page.waitForSelector('#edit-avatar img');
    await page.locator('#save-button').click();
    await page.reload();
    assert.equal(await page.locator('#profile-avatar img').count(), 1);
    await page.locator('#edit-button').click();
    await page.locator('#photo-button').click();
    await page.locator('#remove-photo-button').click();
    await page.locator('#back-button').click();
    await page.locator('#discard-button').click();
    assert.equal(await page.locator('#profile-avatar img').count(), 1);
    await page.locator('#edit-button').click();
    await page.locator('#photo-button').click();
    await page.locator('#remove-photo-button').click();
    await page.locator('#save-button').click();
    assert.equal(await page.locator('#profile-avatar img').count(), 0);
    await page.locator('#edit-button').click();
    await page.locator('#photo-input').setInputFiles({ name: 'broken.png', mimeType: 'image/png', buffer: Buffer.from('not an image') });
    await page.waitForFunction(() => document.getElementById('toast').textContent.includes('사진을 읽지 못했어요'));
    assert.equal(await page.locator('#save-button').isDisabled(), true);
    await page.locator('#photo-input').setInputFiles({ name: 'large.png', mimeType: 'image/png', buffer: Buffer.alloc(5 * 1024 * 1024 + 1) });
    await page.waitForFunction(() => document.getElementById('toast').textContent.includes('5MB 이하'));
    await page.locator('#back-button').click();
    await page.locator('[data-scenario="reset"]').click();
    await page.locator('#reset-button').click();
    assert.notEqual(await page.locator('#profile-name').textContent(), '산책하는차슈');
    await page.locator('#back-button').click();
    await page.locator('#settings-profile-button').click();
    for (const width of [320, 390, 768, 1440]) {
      await page.setViewportSize({ width, height: 844 });
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true, `Profile overflow at ${width}px`);
      await page.locator('#edit-button').click();
      await page.locator('#nickname').fill('가'.repeat(10 - String(width).length) + String(width));
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true, `Edit overflow at ${width}px`);
      await page.locator('#save-button').click();
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true, `Long nickname overflow at ${width}px`);
    }
    assert.deepEqual(errors, []);
    console.log('PASS: nickname generation, persistence, validation, save/discard, photo upload/removal/errors, first visit, settings, responsive layout.');
  } finally {
    await browser.close();
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
