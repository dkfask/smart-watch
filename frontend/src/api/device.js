// 前端设备 API 封装：与后端 `DeviceController` 对应的接口交互
// 包含：list, get, getByImei, create, update, remove

const base = '/api/devices'

async function list(limit = 20, offset = 0) {
  const url = new URL(base, window.location.origin)
  url.searchParams.set('limit', limit)
  url.searchParams.set('offset', offset)
  const res = await fetch(url.toString(), { credentials: 'include' })
  if (!res.ok) throw new Error(`list failed ${res.status}`)
  return res.json()
}

async function get(id) {
  const res = await fetch(`${base}/${id}`, { credentials: 'include' })
  if (!res.ok) throw new Error(`get failed ${res.status}`)
  return res.json()
}

async function getByImei(imei) {
  const res = await fetch(`${base}/by-imei/${encodeURIComponent(imei)}`, { credentials: 'include' })
  if (!res.ok) throw new Error(`getByImei failed ${res.status}`)
  return res.json()
}

async function create(device) {
  const res = await fetch(base, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(device)
  })
  if (!res.ok) throw new Error(`create failed ${res.status}`)
  return res.json()
}

async function update(id, device) {
  const res = await fetch(`${base}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(device)
  })
  if (!res.ok) throw new Error(`update failed ${res.status}`)
  return res
}

async function remove(id) {
  const res = await fetch(`${base}/${id}`, {
    method: 'DELETE',
    credentials: 'include'
  })
  if (!res.ok) throw new Error(`delete failed ${res.status}`)
  return res
}

export default { list, get, getByImei, create, update, remove }

