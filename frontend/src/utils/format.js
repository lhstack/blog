import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

export function formatDate(value, template = 'YYYY年MM月DD日') {
  if (!value) return '—'
  return dayjs(value).format(template)
}

export function fromNow(value) {
  if (!value) return ''
  return dayjs(value).fromNow()
}

export function formatMonth(year, month) {
  return `${year} 年 ${Number(month)} 月`
}

export function formatNumber(value) {
  if (value === null || value === undefined) return '0'
  if (value >= 10000) {
    return `${(value / 10000).toFixed(1)}w`
  }
  if (value >= 1000) {
    return `${(value / 1000).toFixed(1)}k`
  }
  return String(value)
}

export default dayjs
