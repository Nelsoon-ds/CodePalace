import pg from 'pg'
const { Pool, Client } = pg
 

const client = new Client({
  user: 'codepalace',
  password: 'password',
  host: 'localhost',
  port: 5432,
  database: 'codepalace',
})
 
await client.connect()
 
//console.log(await client.query('SELECT NOW()'))
//console.log(await client.query('SELECT * FROM katas'))
/* console.log(await client.query(
  `INSERT INTO katas (title, statement_md, difficulty, programming_language, starter_code, created_by)
   VALUES ($1, $2, $3, $4, $5, $6)
   RETURNING id, title`,
  ['Test Kata', 'This is a test kata.', 'easy', 'js', '', 'user']
)) */
const rows = await client.query('SELECT * FROM katas');

console.log(rows.rows)

await client.end()