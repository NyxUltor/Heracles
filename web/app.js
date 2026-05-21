const apiBase = '/api'
let exercises = []

function el(id){return document.getElementById(id)}

function render(){
  const container = el('exercises')
  container.innerHTML = ''
  exercises.forEach((name, idx)=>{
    const row = document.createElement('div')
    row.className = 'row'
    row.innerText = name
    container.appendChild(row)
  })
  el('sessionTotal').innerText = 'SESSION TOTAL: ' + 0
}

el('add').addEventListener('click', ()=>{
  const v = el('exercise').value.trim()
  if(!v) return
  exercises.push(v)
  el('exercise').value = ''
  render()
  autosave()
})

el('save').addEventListener('click', async ()=>{
  const payload = {
    exercises: exercises.map(name=>({name, sets: []})),
    volume: 0,
    workout_duration: el('duration').value || null,
    saved_at: new Date().toISOString()
  }
  const res = await fetch(apiBase + '/sessions', {method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify(payload)})
  const body = await res.json()
  if(body.ok){
    alert('Saved as ' + body.filename)
  } else {
    alert('Save failed')
  }
})

async function autosave(){
  const payload = {exercises: exercises, workout_duration: el('duration').value || null}
  try{
    await fetch(apiBase + '/draft',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify(payload)})
  }catch(e){console.warn('autosave failed',e)}
}

render()
