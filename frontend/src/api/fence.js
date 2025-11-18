const base = '/api/fences'

export async function createFence(fence) {
  const res = await fetch(base, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(fence)
  })
  if (!res.ok) throw new Error(`createFence failed ${res.status}`)
  return res.json()
}

export default { createFence }

