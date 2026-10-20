const fs=require('node:fs');
const path=require('node:path');
const source=process.argv[2];
if(!source)throw new Error('Provide the source question folder');
const languages=['ru','en','de','es','he'];
const banks=Object.fromEntries(languages.map(language=>[language,JSON.parse(fs.readFileSync(path.join(source,`new_750_questions_${language}.json`),'utf8'))]));
const ids=['cars','geography','geek','wordplay','art','italy','cooking','literature','music','scandinavia','sports','future','tennis','football','discoveries'];
const icons=['🏎️','🌍','🎮','💬','🎨','🏛️','🍋','📚','🎧','🏔️','🏆','🪐','🎾','⚽','💡'];
const referenceTopics=banks.ru.Topics.map(({Topic},index)=>({id:ids[index],icon:icons[index],label:Topic,questionIds:banks.ru.Questions.filter(q=>q.Topic===Topic).map(q=>q.Question_ID)}));
const packs=languages.map(language=>{
 const bank=banks[language];
 const filename=`new_750_questions_${language}.json`;
 const seen=new Set();
 if(bank.Questions.length!==750||bank.Topics.length!==15)throw new Error(`Unexpected bank ${filename}`);
 for(const question of bank.Questions){
  if(seen.has(question.Question_ID)||question.Correct_Answer_Index!==0||question.Answers.length!==4||!question.Question||question.Answers.some(answer=>!answer))throw new Error(`Invalid question ${filename}:${question.Question_ID}`);
  seen.add(question.Question_ID);
 }
 const topics=referenceTopics.map(topic=>{
  const questions=topic.questionIds.map(id=>bank.Questions.find(question=>question.Question_ID===id));
  if(questions.length!==50||questions.some(question=>!question)||new Set(questions.map(question=>question.Topic)).size!==1)throw new Error(`Translation mismatch ${filename}:${topic.id}`);
  return {id:topic.id,icon:topic.icon,label:topic.label,title:questions[0].Topic,count:questions.length};
 });
 return {id:filename.replace('.json',''),filename,language,title:'Дополнительный пакет №1',count:bank.Questions.length,topics,status:'paid',price:'99'};
});
fs.writeFileSync(path.join(__dirname,'dist','catalog.js'),`window.QUIZMAN_CATALOG = ${JSON.stringify(packs)};\n`);
console.log(`Imported ${packs.length} files as ${packs.length} independent packs, 750 questions and 15 topics each. Source files unchanged.`);
