const fs=require('fs'),path=require('path'),crypto=require('crypto');
const root='C:/Users/akhin/Downloads/new_300_questions_all_languages';
const out='outputs/new-300-upload';fs.mkdirSync(out,{recursive:true});
const names={ru:['Пак 250','Экстра'],en:['250 questions','Extra'],de:['250 Fragen','Extra'],es:['250 preguntas','Extra'],he:['250 שאלות','תוספת']};
const quote=s=>"'"+String(s).replace(/'/g,"''")+"'";
const packs=[];
for(const lang of Object.keys(names)){
 const old=JSON.parse(fs.readFileSync(`outputs/supabase-upload/new_750_questions_${lang}.json`,'utf8')).Questions;
 const seen=new Set(old.map(q=>q.Question.trim().toLowerCase()+'|'+q.Answers[0].trim().toLowerCase()));
 for(let i=0;i<=5;i++){
 const filename=i?`extra_${lang}_0${i}.json`:`new_250_questions_${lang}.json`;
 const source=path.join(root,i?`extras/${lang}`:'main_250',filename);
 const bytes=fs.readFileSync(source),data=JSON.parse(bytes.toString('utf8')),qs=data.Questions;
 if(qs.length!==(i?10:250)||new Set(qs.map(q=>q.Question_ID)).size!==qs.length)throw Error(filename+' count/IDs');
 for(const q of qs){
 if(!q.Question?.trim()||!q.Topic?.trim()||q.Correct_Answer_Index!==0||q.Answers.length!==4||q.Answers.some(a=>!a.trim())||new Set(q.Answers).size!==4)throw Error(filename+' invalid '+q.Question_ID);
 const key=q.Question.trim().toLowerCase()+'|'+q.Answers[0].trim().toLowerCase();if(seen.has(key))throw Error(filename+' duplicate '+q.Question_ID);seen.add(key);
 }
 const topics=[...new Set(qs.map(q=>q.Topic))];
 fs.writeFileSync(path.join(out,filename),bytes);
 packs.push({id:filename.slice(0,-5),title:i?`${names[lang][1]} №${i}`:names[lang][0],language:lang,count:qs.length,topics,filename,sha256:crypto.createHash('sha256').update(bytes).digest('hex'),free:i===0,test:i>0});
 }
 console.log(lang+': 750 + 250 + 5×10 = '+seen.size+' unique questions');
}
const sql=packs.map(p=>`insert into public.qm_packs(id,title,language,question_count,topic_count,topics,storage_path,sha256,is_free,test_purchase_enabled) values(${quote(p.id)},${quote(p.title)},${quote(p.language)},${p.count},${p.topics.length},${quote(JSON.stringify(p.topics))}::jsonb,${quote(p.filename)},${quote(p.sha256)},${p.free},${p.test}) on conflict(id) do nothing;`).join('\n');
fs.writeFileSync(path.join(out,'catalog.sql'),'begin;\n'+sql+'\ncommit;\n');fs.writeFileSync(path.join(out,'manifest.json'),JSON.stringify(packs,null,2));
console.log('Prepared '+packs.length+' files; originals unchanged.');
