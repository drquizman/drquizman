const fs=require('node:fs');
const path=require('node:path');
const crypto=require('node:crypto');
const source=process.argv[2];
if(!source)throw new Error('Provide source folder');
const output=path.resolve(__dirname,'../outputs/supabase-upload');
fs.mkdirSync(output,{recursive:true});
const titles=Object.fromEntries(Object.entries(require('./pack-interface-translations.json')).map(([language,strings])=>[language,strings.extraOne]));
const sqlQuote=value=>"'"+String(value).replace(/'/g,"''")+"'";
const manifest=[];
for(const [language,title] of Object.entries(titles)){
 const filename=`new_750_questions_${language}.json`;
 const bytes=fs.readFileSync(path.join(source,filename));
 const data=JSON.parse(bytes.toString('utf8'));
 const questions=data.Questions;
 if(questions.length!==750||new Set(questions.map(q=>q.Question_ID)).size!==750)throw Error(`Invalid count/IDs: ${filename}`);
 for(const q of questions)if(!q.Question?.trim()||!q.Topic?.trim()||q.Correct_Answer_Index!==0||q.Answers.length!==4||q.Answers.some(a=>!a.trim())||new Set(q.Answers).size!==4)throw Error(`Invalid question: ${filename}:${q.Question_ID}`);
 const topics=[...new Set(questions.map(q=>q.Topic))];
 if(topics.length!==15)throw Error(`Invalid topics: ${filename}`);
 fs.writeFileSync(path.join(output,filename),bytes);
 manifest.push({id:filename.replace('.json',''),title,language,question_count:questions.length,topic_count:topics.length,topics,storage_path:filename,sha256:crypto.createHash('sha256').update(bytes).digest('hex')});
}
const statements=manifest.map(pack=>`insert into public.qm_packs(id,title,language,question_count,topic_count,topics,storage_path,sha256,is_free,price_stars) values (${sqlQuote(pack.id)},${sqlQuote(pack.title)},${sqlQuote(pack.language)},${pack.question_count},${pack.topic_count},${sqlQuote(JSON.stringify(pack.topics))}::jsonb,${sqlQuote(pack.storage_path)},${sqlQuote(pack.sha256)},false,null) on conflict(id) do nothing;`);
fs.writeFileSync(path.join(output,'catalog.sql'),'begin;\n'+statements.join('\n')+'\ncommit;\n');
fs.writeFileSync(path.join(output,'manifest.json'),JSON.stringify(manifest,null,2));
console.log(`Prepared ${manifest.length} verified packs in ${output}. Prices unset; no public access.`);
