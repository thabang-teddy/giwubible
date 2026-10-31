const { chromium } = require('playwright');
const path = require('path');

const WIDTH = 1080, HEIGHT = 1920;
const times = [0.5, 1.8, 2.3, 2.8, 4.0, 6.3, 6.5, 8.5, 9.5, 10.5, 12.0, 13.5, 15.5, 16.5, 17.9, 18.6, 19.2];

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: WIDTH, height: HEIGHT } });
  const fileUrl = 'file://' + path.resolve(__dirname, 'scene_vertical.html').replace(/\\/g, '/');
  await page.goto(fileUrl);
  await page.waitForTimeout(200);
  for (const t of times) {
    await page.evaluate((tt) => window.__setTime(tt), t);
    const name = 'vtest_' + t.toFixed(2).replace('.', '_') + '.png';
    await page.screenshot({ path: path.resolve(__dirname, 'frames_v_test', name) });
  }
  await browser.close();
  console.log('done');
})();
