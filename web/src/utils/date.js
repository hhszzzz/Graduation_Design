/**
 * 格式化日期时间
 * @param {String|Date} date 日期时间
 * @param {String} format 格式化模式，默认'YYYY-MM-DD'
 * @returns {String} 格式化后的日期字符串
 */
export function formatDate(date, format = 'YYYY-MM-DD') {
  if (!date) return '';
  
  let d = new Date(date);
  
  if (isNaN(d.getTime())) {
    return '';
  }
  
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  const seconds = String(d.getSeconds()).padStart(2, '0');
  
  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds);
}

/**
 * 格式化为友好时间显示
 * @param {String|Date} date 日期时间
 * @returns {String} 友好时间，如"刚刚"、"5分钟前"、"1小时前"
 */
export function formatTimeAgo(date) {
  if (!date) return '';
  
  const d = new Date(date);
  if (isNaN(d.getTime())) {
    return '';
  }
  
  const now = new Date();
  const diff = now.getTime() - d.getTime();
  
  // 计算时间差
  const seconds = Math.floor(diff / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);
  
  if (seconds < 60) {
    return '刚刚';
  } else if (minutes < 60) {
    return `${minutes}分钟前`;
  } else if (hours < 24) {
    return `${hours}小时前`;
  } else if (days < 7) {
    return `${days}天前`;
  } else {
    return formatDate(date, 'YYYY-MM-DD');
  }
} 