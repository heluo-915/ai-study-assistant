export async function readEventStream(response, onMessage) {
  if (!response.ok) throw new Error(response.status === 401 ? '登录已过期，请重新登录。' : response.status === 404 ? '会话不存在或无权访问。' : '请求失败，请检查输入或稍后重试。')
  if (!response.body || !response.headers.get('content-type')?.includes('text/event-stream')) throw new Error('服务没有返回有效的流式响应。')
  const reader = response.body.getReader(), decoder = new TextDecoder()
  let buffer = '', event = '', data = [], done = false
  function dispatch() {
    if (!data.length) { event = ''; return }
    const value = data.join('\n')
    if (event === 'error') throw new Error(value || '回答或保存失败。')
    if (event === 'done') done = true
    else if (!event || event === 'message') onMessage(value)
    event = ''; data = []
  }
  function line(value) {
    if (value.endsWith('\r')) value = value.slice(0, -1)
    if (!value) return dispatch()
    if (value.startsWith(':')) return
    const colon = value.indexOf(':'), field = colon < 0 ? value : value.slice(0, colon)
    let content = colon < 0 ? '' : value.slice(colon + 1)
    if (content.startsWith(' ')) content = content.slice(1)
    if (field === 'event') event = content
    if (field === 'data') data.push(content)
  }
  try {
    while (true) {
      const part = await reader.read()
      buffer += decoder.decode(part.value, { stream: !part.done })
      let position
      while ((position = buffer.indexOf('\n')) !== -1) { line(buffer.slice(0, position)); buffer = buffer.slice(position + 1); if (done) { await reader.cancel(); return } }
      if (part.done) break
    }
    if (!done) throw new Error('连接已中断，无法确认回答是否保存。请重新打开会话检查后再重试。')
  } finally { reader.releaseLock() }
}
