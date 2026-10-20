// Optional editorial report only; never invoked by the game or Gradle.
// node tools/check-question-limits.cjs [ru|he]
const fs = require('node:fs');
const path = require('node:path');
const language = process.argv[2] || 'ru';
if (!/^[a-z]{2}$/.test(language)) throw new Error('Expected language code, e.g. ru or he');
const bank = JSON.parse(fs.readFileSync(path.resolve(__dirname, `../tv/src/main/assets/questions/${language}.json`), 'utf8'));
const violations = [];
let maxQuestion = 0;
let maxAnswer = 0;
for (const q of bank.Questions) {
  maxQuestion = Math.max(maxQuestion, q.Question.length);
  if (q.Question.length > 110) violations.push(`ID ${q.Question_ID}: question ${q.Question.length}/110`);
  q.Answers.forEach((a, i) => {
    maxAnswer = Math.max(maxAnswer, a.length);
    if (a.length > 50) violations.push(`ID ${q.Question_ID}: answer ${i + 1} ${a.length}/50`);
  });
}
if (violations.length) {
  console.error(violations.join('\n'));
  process.exitCode = 1;
} else {
  console.log(`Checked ${bank.Questions.length} questions. Maximum question: ${maxQuestion}/110; answer: ${maxAnswer}/50.`);
}
