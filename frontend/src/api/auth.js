// 简单的前端 API 辅助函数集合，演示如何与后端交互
// 注意：实际接口路径可能需要根据后端实现调整（/api/register, /login 等）

export async function register(username, password) {
  const res = await fetch('/api/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ username, password })
  })
  return res
}

// 这里我们没有实现 fetch 登录，因为项目使用 Spring Security 的表单登录。

