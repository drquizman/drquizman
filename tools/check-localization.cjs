const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const read = p => fs.readFileSync(path.join(root, p), 'utf8');
const json = p => JSON.parse(read(p));
for (const language of ['ru', 'he', 'en', 'es', 'de']) {
  const bank = json(`tv/src/main/assets/questions/${language}.json`);
  assert.equal(new Set(bank.Questions.map(q => q.Question_ID)).size, bank.Questions.length);
  for (const q of bank.Questions) {
    assert.ok(q.Question && q.Answers.length >= 2);
    assert.ok(Number.isInteger(q.Correct_Answer_Index) && q.Correct_Answer_Index >= 0 && q.Correct_Answer_Index < q.Answers.length);
  }
  console.log(`${language}: ${bank.Questions.length} valid multiple-choice questions`);
}
for (const language of ['he', 'en', 'es', 'de']) {
for (const file of ['strings.json', 'remote.json']) {
  const ru = json(`core/src/main/resources/locales/ru/${file}`);
  const he = json(`core/src/main/resources/locales/${language}/${file}`);
  for (const [key, text] of Object.entries(he)) {
    assert.ok(key in ru, key);
    assert.deepEqual([...text.matchAll(/\{\d+\}/g)].map(m => m[0]).sort(), [...ru[key].matchAll(/\{\d+\}/g)].map(m => m[0]).sort(), key);
  }
  console.log(`${language}/${file}: ${Object.keys(ru).length} keys, ${Object.keys(ru).filter(k => !(k in he)).length} translations pending`);
}
}
for (const language of ['ru', 'he', 'en', 'es', 'de']) {
const dictionary = { ...json('core/src/main/resources/locales/ru/remote.json'), ...json(`core/src/main/resources/locales/${language}/remote.json`) };
const html = read('tv/src/main/assets/remote/index.html');
const script = html.match(/<script>([\s\S]*)<\/script>/)[1].replace('__REMOTE_DICTIONARY__', JSON.stringify(dictionary));
const app = { innerHTML: '', addEventListener() {} };
const sandbox = {
  document: { getElementById: () => app, addEventListener() {}, visibilityState: 'hidden' },
  localStorage: { getItem: () => '', setItem() {}, removeItem() {} },
  location: { search: '', host: 'localhost', protocol: 'http:' },
  navigator: { vibrate() {} }, window: { addEventListener() {} }, URLSearchParams,
  setTimeout() {}, clearTimeout() {}, console,
};
vm.createContext(sandbox);
vm.runInContext(script, sandbox);
assert.ok(app.innerHTML.includes(dictionary['remote.049']));
for (const phase of ['LOBBY', 'TOPIC', 'BET', 'QUESTION', 'REVEAL', 'INTRO', 'GAME_OVER']) {
  for (const competition of ['FIRST', 'BOMB', 'BETS', 'PUZZLE', 'FINAL']) {
    for (const flag of [false, true]) {
      vm.runInContext(`state.playerId = 'p'; state.snap = {
        phase: '${phase}', competition: '${competition}', players: [{id:'p', name:'Test', ready:${flag}, bet:${flag ? 100 : 0}, contestMarks:${flag ? 4 : 0}, eliminated:${flag}, finalTimeMs:30000}],
        topics:['Topic'], chosenTopic:'Topic', bettingOpen:${flag}, bombHolderId:${competition === 'BOMB' ? (flag ? "'p'" : "'other'") : 'null'}, question:{answers:['a','b']}
      }; render();`, sandbox);
      assert.ok(!app.innerHTML.includes('remote.'), `${phase}/${competition}`);
    }
  }
}
console.log(`${language}: remote render checks passed for 70 game-state combinations`);
}
