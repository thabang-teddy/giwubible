const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  const fileUrl = 'file://' + path.resolve(__dirname, 'audio.html').replace(/\\/g, '/');
  await page.goto(fileUrl);
  const b64 = await page.evaluate(() => window.renderAudio());
  fs.writeFileSync(path.resolve(__dirname, 'track.wav'), Buffer.from(b64, 'base64'));
  await browser.close();
  console.log('audio written');
})();
