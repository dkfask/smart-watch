// Frontend Downlink API helper
const base = '/api/downlink'

export async function getOnline() {
  const res = await fetch(`${base}/online`, { credentials: 'include' })
  if (!res.ok) throw new Error(`getOnline failed ${res.status}`)
  return res.json()
}

export async function sendBp00(imei, timezone) {
  const url = new URL(`${base}/bp00`, window.location.origin)
  url.searchParams.set('imei', imei)
  if (timezone != null) url.searchParams.set('timezone', timezone)
  const res = await fetch(url.toString(), { method: 'POST', credentials: 'include' })
  if (!res.ok) throw new Error(`sendBp00 failed ${res.status}`)
  return res.json()
}

export async function sendBp12(body) {
  const res = await fetch(`${base}/bp12`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(body)
  })
  if (!res.ok) throw new Error(`sendBp12 failed ${res.status}`)
  return res.json()
}

export async function sendCustom(body) {
  const res = await fetch(`${base}/custom`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(body)
  })
  if (!res.ok) throw new Error(`sendCustom failed ${res.status}`)
  return res.json()
}

export default { getOnline, sendBp00, sendBp12, sendCustom }

