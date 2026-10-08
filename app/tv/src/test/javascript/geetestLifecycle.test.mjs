import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { runInNewContext } from 'node:vm';
import test from 'node:test';

const repository = fileURLToPath(new URL('../../../../../', import.meta.url));
const pages = {
  tv: 'app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/component/GeetestTvVerifyDialog.kt',
  phone: 'app/shared/src/main/kotlin/dev/aaa1115910/bv/network/GeetestCompanionService.kt',
};

function createPage(kind) {
  // Execute the production inline script with a fake SDK; no live captcha or network is needed.
  const source = readFileSync(resolve(repository, pages[kind]), 'utf8');
  const builder = source.slice(source.indexOf('fun buildGeetestHtml('));
  const html = builder.match(/return\s+"""([\s\S]*?)"""\.trimIndent\(\)/)?.[1];
  assert.ok(html, `Missing ${kind} verification HTML`);
  const script = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)].at(-1)?.[1];
  assert.ok(script);

  const events = {};
  const timers = new Map();
  const submissions = [];
  const requests = [];
  const elements = new Map();
  for (const match of html.matchAll(/\bid="([^"]+)"/g)) {
    elements.set(match[1], { style: {}, textContent: '', onclick: null });
  }
  let timerId = 0;
  let verificationCount = 0;
  let validation = {
    geetest_validate: 'validate',
    geetest_seccode: 'validate|jordan',
    geetest_challenge: 'result-challenge',
  };
  const captcha = {
    appendTo() {},
    verify() { verificationCount += 1; },
    getValidate() { return validation; },
    onReady(callback) { events.ready = callback; },
    onSuccess(callback) { events.success = callback; },
    onClose(callback) { events.close = callback; },
    onError(callback) { events.error = callback; },
  };
  const element = (id) => {
    assert.ok(elements.has(id), `Missing ${kind} HTML element: ${id}`);
    return elements.get(id);
  };
  runInNewContext(script, {
    window: {
      Android: {
        onStatusUpdate() {},
        onVerificationType() {},
        onSliderPosition() {},
        onGeetestResult(...payload) { submissions.push(payload); },
      },
    },
    document: { getElementById: (id) => elements.get(id) ?? null, querySelectorAll: () => [] },
    initGeetest(_config, callback) { callback(captcha); },
    setTimeout(callback, delay) {
      const id = ++timerId;
      timers.set(id, { callback, delay });
      return id;
    },
    clearTimeout(id) { timers.delete(id); },
    fetch(url, options) {
      submissions.push(JSON.parse(options.body));
      return new Promise((resolve, reject) => requests.push({ url, resolve, reject }));
    },
  });
  events.ready();
  return {
    events, submissions, requests, element,
    get verificationCount() { return verificationCount; },
    clearValidation() { validation = null; },
    emptyValidation() { validation = {}; },
    runReopenTimers() {
      for (const [id, timer] of [...timers]) {
        if (timer.delay !== 500) continue;
        timers.delete(id);
        timer.callback();
      }
    },
  };
}

for (const kind of Object.keys(pages)) {
  test(`${kind}: SDK close after success does not request another captcha`, () => {
    const page = createPage(kind);
    page.events.success();
    page.events.close();
    page.runReopenTimers();
    assert.equal(page.verificationCount, 1);
    assert.equal(page.submissions.length, 1);
  });

  test(`${kind}: success cancels a reopen already queued by SDK close`, () => {
    const page = createPage(kind);
    page.events.close();
    page.events.success();
    page.runReopenTimers();
    assert.equal(page.verificationCount, 1);
  });

  test(`${kind}: repeated success callbacks submit the result only once`, () => {
    const page = createPage(kind);
    page.events.success();
    page.events.close();
    page.events.success();
    assert.equal(page.submissions.length, 1);
  });

  test(`${kind}: closing an incomplete captcha still allows verification`, () => {
    const page = createPage(kind);
    page.events.close();
    page.runReopenTimers();
    assert.equal(page.verificationCount, 2);
    assert.equal(page.submissions.length, 0);
  });

  test(`${kind}: success without a valid result does not suppress verification`, () => {
    const page = createPage(kind);
    page.clearValidation();
    page.events.success();
    page.events.close();
    page.runReopenTimers();
    assert.equal(page.verificationCount, 2);
    assert.equal(page.submissions.length, 0);
  });

  test(`${kind}: an incomplete validation payload does not suppress verification`, () => {
    const page = createPage(kind);
    page.emptyValidation();
    page.events.success();
    page.events.close();
    page.runReopenTimers();
    assert.equal(page.verificationCount, 2);
    assert.equal(page.submissions.length, 0);
  });
}

test('phone: failed result delivery can retry without solving another captcha', async () => {
  const page = createPage('phone');
  page.events.success();
  page.events.close();
  page.requests[0].reject(new Error('connection lost'));
  await new Promise(setImmediate);
  assert.equal(page.element('retry').style.display, 'block');
  page.element('retry').onclick();
  assert.equal(page.submissions.length, 2);
  assert.deepEqual(page.submissions[0], page.submissions[1]);
  page.requests[1].resolve({ ok: true });
  await new Promise(setImmediate);
  page.events.close();
  page.events.success();
  page.runReopenTimers();
  assert.equal(page.verificationCount, 1);
  assert.equal(page.submissions.length, 2);
  assert.equal(page.element('ok').style.display, 'block');
});
