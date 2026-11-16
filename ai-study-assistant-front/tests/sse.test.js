import test from 'node:test'
import assert from 'node:assert/strict'
import { readEventStream } from '../src/lib/sse.js'
const encode = new TextEncoder()
function response(parts, status = 200, type = 'text/event-stream') {
  return new Response(new ReadableStream({ start(controller) { for (const p of parts) controller.enqueue(typeof p === 'string' ? encode.encode(p) : p); controller.close() } }), { status, headers: { 'Content-Type': type } })
}
test('流按 UTF-8 字节和 CRLF 任意分块，保留文本并接收 done', async () => {
  const bytes = encode.encode('event:message\r\ndata:你\r\n\r\nevent:message\r\ndata: 好\r\n\r\nevent:done\r\ndata:ok\r\n\r\n')
  const out = []; await readEventStream(response([...bytes].map(x => Uint8Array.of(x))), x => out.push(x))
  assert.equal(out.join(''), '你好')
})
test('未收到 done 的断流不当成成功', async () => {
  const out=[]; await assert.rejects(readEventStream(response(['event:message\ndata:部分\n\n']), x=>out.push(x)), /无法确认/); assert.deepEqual(out,['部分'])
})
test('后端 error 事件应报告失败', async () => { await assert.rejects(readEventStream(response(['event:error\ndata:回答或保存失败\n\n']), () => {}), /回答或保存失败/) })
test('拒绝非 SSE 或失败 HTTP 响应', async () => {
  await assert.rejects(readEventStream(response(['{}'], 200, 'application/json'), () => {}), /有效的流式/)
  await assert.rejects(readEventStream(response([''], 404), () => {}), /不存在/)
})
