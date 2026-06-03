const HAS_TIME_ZONE = /(?:Z|[+-]\d{2}:?\d{2})$/i

const normalizeDateInput = (value) => {
  if (value === null || value === undefined || value === '') return null
  if (value instanceof Date) return value
  if (typeof value === 'number') {
    return new Date(value < 100000000000 ? value * 1000 : value)
  }

  const text = String(value).trim()
  if (!text) return null
  if (/^\d{10}$/.test(text)) return new Date(Number(text) * 1000)
  if (/^\d{13}$/.test(text)) return new Date(Number(text))

  const normalized = text.replace(' ', 'T')
  return new Date(HAS_TIME_ZONE.test(normalized) ? normalized : `${normalized}+08:00`)
}

export const formatBeijingTime = (value, fallback = '') => {
  const date = normalizeDateInput(value)
  if (!date || Number.isNaN(date.getTime())) return fallback

  return new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).format(date)
}
