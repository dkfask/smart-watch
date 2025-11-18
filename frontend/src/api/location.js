const base = '/api/locations'

export async function recentByDevice(deviceId, limit = 50, offset = 0) {
  const url = new URL(`${base}/device/${deviceId}`, window.location.origin)
  url.searchParams.set('limit', limit)
  url.searchParams.set('offset', offset)
  const res = await fetch(url.toString(), { credentials: 'include' })
  if (!res.ok) throw new Error(`recentByDevice failed ${res.status}`)
  return res.json()
}

export async function rangeByDevice(deviceId, startIso, endIso, limit = 200, offset = 0) {
  const url = new URL(`${base}/device/${deviceId}/range`, window.location.origin)
  url.searchParams.set('start', startIso)
  url.searchParams.set('end', endIso)
  url.searchParams.set('limit', limit)
  url.searchParams.set('offset', offset)
  const res = await fetch(url.toString(), { credentials: 'include' })
  if (!res.ok) throw new Error(`rangeByDevice failed ${res.status}`)
  return res.json()
}

export default { recentByDevice, rangeByDevice }

