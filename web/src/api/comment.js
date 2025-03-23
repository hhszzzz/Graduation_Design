import request from '@/utils/request'

/**
 * 获取评论列表
 * @param {Object} params 查询参数
 * @returns {Promise} 请求结果
 */
export function getComments(params) {
  return request({
    url: '/api/comments/list',
    method: 'get',
    params
  })
}

/**
 * 获取回复列表
 * @param {Number} parentId 父评论ID
 * @returns {Promise} 请求结果
 */
export function getReplies(parentId) {
  return request({
    url: `/api/comments/replies/${parentId}`,
    method: 'get'
  })
}

/**
 * 添加评论
 * @param {Object} data 评论数据
 * @returns {Promise} 请求结果
 */
export function addComment(data) {
  return request({
    url: '/api/comments/add',
    method: 'post',
    data
  })
}

/**
 * 点赞评论
 * @param {Number} commentId 评论ID
 * @returns {Promise} 请求结果
 */
export function likeComment(commentId) {
  return request({
    url: `/api/comments/like/${commentId}`,
    method: 'post'
  })
} 