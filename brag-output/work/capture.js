// Renders scene.html frame-by-frame with Playwright and saves PNGs.
const { chromium } = require('playwright');
const path = require('path');

const FPS = 30;
const DURATION = 20.0;
const WIDTH = 1920;
const HEIGHT = 1080;

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: WIDTH, height: HEIGHT } });
  const fileUrl = 'file://' + path.resolve(__dirname, 'scene.html').replace(/\\/g, '/');
  await page.goto(fileUrl);
  await page.waitForTimeout(200);

  const totalFrames = Math.round(DURATION * FPS);
  for (let i = 0; i < totalFrames; i++) {
    const t = i / FPS;
    await page.evaluate((tt) => window.__setTime(tt), t);
    const frameName = String(i).padStart(5, '0');
    await page.screenshot({ path: path.resolve(__dirname, 'frames', `f${frameName}.png`) });
    if (i % 30 === 0) console.log('frame', i, '/', totalFrames);
  }

  await browser.close();
  console.log('done');
})();
