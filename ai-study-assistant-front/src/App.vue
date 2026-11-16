<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import StudyWorkspace from './components/StudyWorkspace.vue'
import LearningHub from './components/LearningHub.vue'
import { readEventStream } from './lib/sse.js'

const token = ref(sessionStorage.getItem('study-token') || '')
const username = ref(sessionStorage.getItem('study-name') || '')
const authMode = ref('login')
const authForm = ref({ username: '', password: '' })
const authBusy = ref(false)
const active = ref('chat')
const sessions = ref([])
const notes = ref([])
const activeSession = ref(null)
const messages = ref([])
const loadingMessages = ref(false)
const selectedNote = ref(null)
const editingNote = ref(false)
const noteForm = ref({ title: '', content: '' })
const noteBusy = ref(false)
const chatInput = ref('')
const chatBusy = ref(false)
const question = ref('')
const noteAnswer = ref(null)
const askBusy = ref(false)
const notice = ref('')
const noteSearch = ref('')
const searchResults = ref(null)
const searchBusy = ref(false)
const searchedKeyword = ref('')
const searchSequence = ref(0)
const knowledgeMode = ref('semantic')
const knowledgeKeyword = ref('')
const knowledgeQuestion = ref('')
const knowledgeAnswer = ref(null)
const knowledgeBusy = ref(false)
const similarityA = ref('')
const similarityB = ref('')
const similarityResult = ref(null)
const similarityBusy = ref(false)
const dimensionText = ref('')
const dimensions = ref(null)
const dimensionBusy = ref(false)
const chatScroll = ref(null)
const showNewSession = ref(false)
const sessionTitle = ref('')
const sessionBusy = ref(false)
let noticeTimer

const sortedSessions = computed(() => [...sessions.value].sort((a, b) => b.id - a.id))
const sortedNotes = computed(() => [...notes.value].sort((a, b) => b.id - a.id))
const filteredNotes = computed(() => {
  return searchResults.value === null ? sortedNotes.value : searchResults.value
})
const noteCount = computed(() => notes.value.length)
const initials = computed(() => (username.value || '学').slice(0, 1).toUpperCase())

function toast(message) { notice.value = message; clearTimeout(noticeTimer); noticeTimer = setTimeout(() => { notice.value = '' }, 4500) }
function date(value) { if (!value) return '刚刚'; const d = new Date(value); return Number.isNaN(d.valueOf()) ? value : new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric' }).format(d) }
function time(value) { if (!value) return ''; const d = new Date(value); return Number.isNaN(d.valueOf()) ? '' : new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit' }).format(d) }
async function api(path, options = {}) {
  const headers = { ...(options.body && !(options.body instanceof FormData) ? { 'Content-Type': 'application/json' } : {}), ...(token.value ? { Authorization: `Bearer ${token.value}` } : {}) }
  let response
  try { response = await fetch(`/api${path}`, { ...options, headers: { ...headers, ...options.headers } }) }
  catch { throw new Error('无法连接服务，请确认后端已启动。') }
  const text = await response.text()
  let data = text
  if (text && response.headers.get('content-type')?.includes('application/json')) { try { data = JSON.parse(text) } catch { /* keep text */ } }
  if (!response.ok) {
    if (response.status === 401 && token.value && !['/login', '/register'].includes(path)) { logout(); throw new Error('登录已过期，请重新登录。') }
    const fallback = response.status === 401 ? '用户名或密码错误。' : response.status === 404 ? '内容不存在或无权访问。' : response.status === 409 ? '用户名已存在。' : response.status === 400 ? '请检查输入内容。' : '服务暂时不可用，请稍后重试。'
    const error = new Error(typeof data === 'string' && data.trim() ? data : (typeof data?.message === 'string' && data.message.trim() ? data.message : fallback)); error.status = response.status; throw error
  }
  return data
}
async function authenticate() {
  if (authBusy.value) return
  const form = authForm.value
  if (!form.username.trim() || !form.password.trim()) { toast('请输入用户名和密码。'); return }
  authBusy.value = true
  try {
    const payload = JSON.stringify({ username: form.username.trim(), password: form.password })
    if (authMode.value === 'register') { await api('/register', { method: 'POST', body: payload }); toast('注册成功，请登录。'); authMode.value = 'login'; authForm.value.password = ''; return }
    const result = await api('/login', { method: 'POST', body: payload })
    if (!result?.token) throw new Error('登录响应缺少凭证。')
    token.value = result.token; username.value = form.username.trim()
    sessionStorage.setItem('study-token', result.token); sessionStorage.setItem('study-name', username.value)
    authForm.value.password = ''; await refreshAll()
  } catch (error) { toast(error.message) } finally { authBusy.value = false }
}
function logout() { token.value = ''; username.value = ''; sessionStorage.removeItem('study-token'); sessionStorage.removeItem('study-name'); sessions.value = []; notes.value = []; messages.value = []; activeSession.value = null; selectedNote.value = null; noteSearch.value = ''; searchResults.value = null; searchedKeyword.value = ''; searchSequence.value++; knowledgeKeyword.value = ''; knowledgeQuestion.value = ''; knowledgeAnswer.value = null; similarityA.value = ''; similarityB.value = ''; similarityResult.value = null; dimensionText.value = ''; dimensions.value = null; active.value = 'chat' }
async function refreshAll() {
  try { const [s, n] = await Promise.all([api('/chat/sessions'), api('/notes')]); sessions.value = s; notes.value = n }
  catch (error) { toast(error.message) }
}
async function loadSessions() { sessions.value = await api('/chat/sessions') }
async function loadNotes() { notes.value = await api('/notes') }
async function reloadNotesSafely() { try { await loadNotes(); await refreshSearch() } catch (error) { toast(error.message) } }
async function searchNotes() {
  const keyword = noteSearch.value.trim()
  if (!keyword) { clearSearch(); return }
  const sequence = ++searchSequence.value
  searchBusy.value = true
  try {
    const results = await api(`/notes/search?keyword=${encodeURIComponent(keyword)}`)
    if (sequence !== searchSequence.value) return
    searchResults.value = results; searchedKeyword.value = keyword
  } catch (error) { if (sequence === searchSequence.value) toast(error.message) }
  finally { if (sequence === searchSequence.value) searchBusy.value = false }
}
function clearSearch() { searchSequence.value++; noteSearch.value = ''; searchedKeyword.value = ''; searchResults.value = null; searchBusy.value = false }
async function refreshSearch() { if (searchedKeyword.value) { noteSearch.value = searchedKeyword.value; await searchNotes() } }
async function askKnowledge() {
  const keyword = knowledgeKeyword.value.trim(), question = knowledgeQuestion.value.trim()
  if ((knowledgeMode.value === 'keyword' && !keyword) || !question || knowledgeBusy.value) return
  knowledgeBusy.value = true; knowledgeAnswer.value = null
  try { knowledgeAnswer.value = knowledgeMode.value === 'semantic' ? await api('/notes/chunks/ask', { method: 'POST', body: JSON.stringify({ message: question }) }) : await api('/notes/ask', { method: 'POST', body: JSON.stringify({ keyword, question }) }) }
  catch (error) { toast(error.message) } finally { knowledgeBusy.value = false }
}
async function openSource(source) {
  try { const note = await api(`/notes/${source.noteId}`); active.value = 'notes'; openNote(note) }
  catch (error) { toast(error.message) }
}
async function compareTexts() {
  const textA = similarityA.value.trim(), textB = similarityB.value.trim()
  if (!textA || !textB || similarityBusy.value) return
  similarityBusy.value = true; similarityResult.value = null
  try { similarityResult.value = await api('/embedding/similarity', { method: 'POST', body: JSON.stringify({ textA, textB }) }) }
  catch (error) { toast(error.message) } finally { similarityBusy.value = false }
}
async function checkDimensions() {
  const message = dimensionText.value.trim()
  if (!message || dimensionBusy.value) return
  dimensionBusy.value = true; dimensions.value = null
  try { dimensions.value = await api('/embedding/dimensions', { method: 'POST', body: JSON.stringify({ message }) }) }
  catch (error) { toast(error.message) } finally { dimensionBusy.value = false }
}
function scrollChat() { nextTick(() => { if (chatScroll.value) chatScroll.value.scrollTop = chatScroll.value.scrollHeight }) }
async function selectSession(session) {
  if (chatBusy.value) return
  active.value = 'chat'; activeSession.value = session; messages.value = []; loadingMessages.value = true
  try { messages.value = await api(`/chat/sessions/${session.id}/messages`); scrollChat() }
  catch (error) { toast(error.message) } finally { loadingMessages.value = false }
}
function startNewChat() { if (chatBusy.value) return; active.value = 'chat'; showNewSession.value = true; sessionTitle.value = '' }
async function createSession() {
  if (sessionBusy.value) return
  const title = sessionTitle.value.trim()
  if (!title || title.length > 50) { toast('会话标题需为 1–50 个字符。'); return }
  sessionBusy.value = true
  try { const id = await api('/chat/sessions', { method: 'POST', body: JSON.stringify({ title }) }); await loadSessions(); showNewSession.value = false; await selectSession(sessions.value.find(s => s.id === id) || { id, title }); toast('新会话已创建。') }
  catch (error) { toast(error.message) } finally { sessionBusy.value = false }
}
async function sendMessage() {
  const text = chatInput.value.trim()
  if (!text || chatBusy.value || !activeSession.value) return
  if (new TextEncoder().encode(text).length > 65535) { toast('消息不能超过 65535 字节。'); return }
  const sessionId = activeSession.value.id
  chatBusy.value = true; chatInput.value = ''
  messages.value.push({ role: 'user', content: text, pending: true }, { role: 'assistant', content: '', pending: true })
  const assistant = messages.value[messages.value.length - 1]
  scrollChat()
  let saved = false
  try {
    const response = await fetch(`/api/chat/sessions/${sessionId}/messages/stream`, { method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token.value}` }, body: JSON.stringify({ message: text }) })
    if (response.status === 401) { logout(); throw new Error('登录已过期，请重新登录。') }
    await readEventStream(response, chunk => { assistant.content += chunk; scrollChat() })
    saved = true
    messages.value = await api(`/chat/sessions/${sessionId}/messages`); scrollChat()
  } catch (error) {
    if (saved) { messages.value.forEach(m => { m.pending = false }); toast('回答已保存，但刷新记录失败，请重新打开会话。') }
    else { assistant.pending = false; assistant.failed = true; if (!assistant.content) assistant.content = '暂未收到回答。'; messages.value.forEach(m => { if (m.pending) { m.pending = false; m.failed = true } }); toast(error.message || '连接中断，请重新打开会话检查是否保存。') }
  } finally { chatBusy.value = false }
}
function newNote() { selectedNote.value = null; noteForm.value = { title: '', content: '' }; editingNote.value = true; noteAnswer.value = null; question.value = '' }
function openNote(note) { selectedNote.value = note; editingNote.value = false; noteAnswer.value = null; question.value = '' }
function editNote() { if (!selectedNote.value) return; noteForm.value = { title: selectedNote.value.title, content: selectedNote.value.content }; editingNote.value = true }
async function saveNote() {
  if (noteBusy.value) return
  const title = noteForm.value.title.trim(), content = noteForm.value.content.trim()
  if (!title || title.length > 50 || !content) { toast('请填写标题（不超过 50 字）和正文。'); return }
  noteBusy.value = true
  try {
    const previousId = selectedNote.value?.id
    const saved = await api(previousId ? `/notes/${previousId}` : '/notes', { method: previousId ? 'PUT' : 'POST', body: JSON.stringify({ title, content }) })
    await loadNotes(); await refreshSearch(); selectedNote.value = notes.value.find(n => n.id === (previousId || saved.noteId)) || null
    editingNote.value = false; toast(saved.chunksGenerated === false ? '笔记已保存，但片段生成失败，可在学习工具中补建。' : previousId ? '笔记已保存。' : '笔记已创建。')
  } catch (error) { toast(error.message) } finally { noteBusy.value = false }
}
async function deleteNote() {
  if (!selectedNote.value || noteBusy.value || !window.confirm(`确定删除“${selectedNote.value.title}”吗？`)) return
  noteBusy.value = true
  try { await api(`/notes/${selectedNote.value.id}`, { method: 'DELETE' }); selectedNote.value = null; noteAnswer.value = null; await loadNotes(); await refreshSearch(); toast('笔记已删除。') }
  catch (error) { toast(error.message) } finally { noteBusy.value = false }
}
async function askNote() {
  const text = question.value.trim(); if (!text || !selectedNote.value || askBusy.value) return
  const noteId = selectedNote.value.id; askBusy.value = true; noteAnswer.value = null
  try { const answer = await api(`/notes/${noteId}/ask`, { method: 'POST', body: JSON.stringify({ message: text }) }); if (selectedNote.value?.id === noteId) noteAnswer.value = answer }
  catch (error) { toast(error.message) } finally { askBusy.value = false }
}
onMounted(() => { if (token.value) refreshAll() })
</script>

<template>
  <div v-if="notice" class="toast" role="status">{{ notice }}</div>
  <main v-if="!token" class="auth-page">
    <div class="auth-brand"><span class="brand-mark">知</span><span>知页 <small>你的学习空间</small></span></div>
    <div class="auth-layout">
      <section class="auth-intro"><div class="intro-art"><div class="art-grid"></div><div class="art-paper paper-one"><span>思考</span><i></i><i></i><i></i></div><div class="art-paper paper-two"><span>记录</span><i></i><i></i><i></i></div><div class="art-orbit">✳</div></div><p class="auth-kicker">为每一次认真学习，留一页。</p><h1>想明白的事，<br>从这里开始。</h1><p class="auth-description">记录你的思考，和 AI 一起把复杂的问题拆开、理解，再变成自己的知识。</p><div class="intro-points"><span>✳ 知识随手记</span><span>✳ 提问有上下文</span><span>✳ 找回学习线索</span></div></section>
      <section class="auth-card"><div class="auth-card-top"><span class="auth-icon">✳</span><span>个人学习空间</span></div><h2>{{ authMode === 'login' ? '欢迎回来' : '创建你的空间' }}</h2><p>{{ authMode === 'login' ? '登录后，继续你的学习旅程。' : '创建账号，开始记录与探索。' }}</p><form @submit.prevent="authenticate"><label>用户名<input v-model="authForm.username" autocomplete="username" maxlength="50" placeholder="输入你的用户名"></label><label>密码<input v-model="authForm.password" type="password" :autocomplete="authMode === 'login' ? 'current-password' : 'new-password'" placeholder="输入密码"></label><button class="primary auth-submit" :disabled="authBusy">{{ authBusy ? '请稍候…' : authMode === 'login' ? '进入学习空间' : '创建账号' }} <span>↗</span></button></form><div class="auth-switch">{{ authMode === 'login' ? '还没有账号？' : '已经有账号？' }} <button class="text-button" @click="authMode = authMode === 'login' ? 'register' : 'login'">{{ authMode === 'login' ? '立即注册' : '返回登录' }}</button></div><p class="auth-foot">你的笔记与对话，只属于你。</p></section>
    </div><footer class="auth-footer"><span>知页 · AI 学习助手</span><span>留给好奇心一个位置。</span></footer>
  </main>

  <div v-else class="shell">
    <aside class="sidebar"><div class="brand"><span class="brand-mark">知</span><div><strong>知页</strong><small>AI 学习助手</small></div></div><div class="side-heading">我的空间</div><nav class="nav"><button :class="{selected: active === 'chat'}" @click="active = 'chat'"><span class="nav-icon">✳</span>学习对话 <span class="nav-arrow">›</span></button><button :class="{selected: active === 'notes'}" @click="active = 'notes'"><span class="nav-icon">▤</span>我的笔记 <span class="nav-arrow">›</span></button><button :class="{selected: active === 'knowledge'}" @click="active = 'knowledge'"><span class="nav-icon">⌕</span>笔记问答 <span class="nav-arrow">›</span></button><button :class="{selected: active === 'embedding'}" @click="active = 'embedding'"><span class="nav-icon">◈</span>语义实验 <span class="nav-arrow">›</span></button><button :class="{selected: active === 'study'}" @click="active = 'study'"><span class="nav-icon">✦</span>学习工具 <span class="nav-arrow">›</span></button><button :class="{selected: active === 'learning'}" @click="active = 'learning'"><span class="nav-icon">☷</span>学习中心 <span class="nav-arrow">›</span></button></nav><div class="side-section"><div class="side-section-title"><span>最近会话</span><button class="icon-button" title="新建会话" aria-label="新建会话" @click="startNewChat">＋</button></div><div v-if="!sortedSessions.length" class="side-empty">创建一场对话，开始探索。</div><button v-for="session in sortedSessions.slice(0, 8)" :key="session.id" class="session-link" :class="{current: active === 'chat' && activeSession?.id === session.id}" @click="selectSession(session)"><span class="session-dot"></span><span>{{ session.title }}</span></button></div><div class="sidebar-bottom"><div class="side-note"><span>✦</span><p>把好问题记下来，<br>答案会慢慢清晰。</p></div><div class="account"><span class="avatar">{{ initials }}</span><div><strong>{{ username }}</strong><small>个人学习空间</small></div><button class="icon-button logout" title="退出登录" aria-label="退出登录" @click="logout">↪</button></div></div></aside>

    <div class="workspace"><header class="topbar"><div class="breadcrumb">我的空间 <span>/</span> <strong>{{ ({ chat: '学习对话', notes: '我的笔记', knowledge: '笔记问答', embedding: '语义实验', study: '学习工具', learning: '学习中心' })[active] }}</strong></div><div class="top-right"><span class="status-dot"></span> 学习空间 <span class="top-avatar">{{ initials }}</span></div></header>
      <main v-if="active === 'chat'" class="main-content chat-layout"><div class="page-heading"><div><div class="heading-label"><span class="heading-icon">✳</span> 你的 AI 学习伙伴</div><h1>一起把问题想明白<span class="heading-period">.</span></h1><p>从一个问题开始，慢慢积累属于你的知识。</p></div><button class="outline-button" @click="startNewChat">＋ <span>新建对话</span></button></div>
        <div class="chat-grid"><section class="chat-panel"><div class="panel-header"><div><span class="panel-symbol">✳</span><div><strong>{{ activeSession?.title || '学习对话' }}</strong><small>{{ activeSession ? '在这里延续你的思考' : '选择或创建一个会话' }}</small></div></div><span class="availability"><i></i> 随时可以提问</span></div>
          <div v-if="activeSession" ref="chatScroll" class="message-area"><div v-if="loadingMessages" class="inline-state">正在加载对话…</div><template v-else><div v-if="!messages.length" class="empty-chat"><div class="empty-glyph">✳</div><h2>这次想学点什么？</h2><p>提一个问题，或者把一个难懂的概念交给我。</p><div class="suggestions"><button @click="chatInput = '请用简单的例子解释 Java 接口。'">解释一个概念 <span>↗</span></button><button @click="chatInput = '帮我梳理 MyBatis 的学习思路。'">梳理学习思路 <span>↗</span></button></div></div><div v-for="(message, index) in messages" :key="message.id || `pending-${index}`" class="message" :class="message.role"><span class="message-avatar">{{ message.role === 'user' ? initials : '✳' }}</span><div class="message-content"><div class="message-meta"><strong>{{ message.role === 'user' ? '你' : '知页助手' }}</strong><span>{{ message.failed ? '状态未确认，请重新打开会话检查' : message.pending ? '正在生成' : time(message.createdAt) }}</span></div><div class="message-text">{{ message.content }}</div></div></div><div v-if="chatBusy && !messages[messages.length - 1]?.content" class="message assistant"><span class="message-avatar">✳</span><div class="message-content"><div class="message-meta"><strong>知页助手</strong></div><div class="typing">正在思考<span>···</span></div></div></div></template></div>
          <div v-else class="empty-chat initial-empty"><div class="empty-glyph">✳</div><h2>从一个好问题开始</h2><p>创建一场对话，AI 会结合最近的聊天继续回答。</p><button class="primary" @click="startNewChat">＋ 开始一场对话</button></div>
          <form class="composer-wrap" @submit.prevent="sendMessage"><div class="composer"><textarea v-model="chatInput" :disabled="!activeSession || chatBusy" placeholder="输入你的问题…" rows="2" @keydown.enter.exact.prevent="sendMessage"></textarea><div class="composer-bottom"><span>Enter 发送 · Shift + Enter 换行</span><button class="send-button" :disabled="!activeSession || !chatInput.trim() || chatBusy" aria-label="发送消息">↑</button></div></div><p>AI 的回答仅供参考，重要信息请自行核实。</p></form></section>
          <aside class="context-column"><div class="context-top"><div class="context-icon">✴</div><h2>让灵感<br>有处安放。</h2><p>学习不是记住所有答案，而是不断找到更好的问题。</p><button @click="active = 'notes'">打开我的笔记 <span>↗</span></button><div class="abstract-leaf leaf-a"></div><div class="abstract-leaf leaf-b"></div></div><div class="context-section"><div class="context-title"><h3>最近的笔记</h3><button @click="active = 'notes'">查看全部</button></div><p v-if="!sortedNotes.length" class="muted">还没有笔记，先记录一个想法吧。</p><button v-for="note in sortedNotes.slice(0, 3)" :key="note.id" class="mini-note" @click="active = 'notes'; openNote(note)"><span class="mini-note-icon">▤</span><span><strong>{{ note.title }}</strong><small>{{ date(note.createdAt) }}</small></span><b>›</b></button></div><div class="context-tip"><span>✧</span><div><strong>试试基于笔记提问</strong><p>打开一篇笔记，让助手围绕它回答。</p></div></div></aside></div>
      </main>

      <main v-else-if="active === 'notes'" class="main-content notes-page"><div class="page-heading"><div><div class="heading-label"><span class="heading-icon">▤</span> 你的知识库</div><h1>把想法留在这里<span class="heading-period">.</span></h1><p>记录知识，也记录那些逐渐清晰的时刻。</p></div><button class="primary" @click="newNote">＋ 新建笔记</button></div><div class="notes-layout"><section class="notes-list-panel"><div class="notes-list-head"><div><h2>全部笔记</h2><span>{{ noteCount }} 篇记录</span></div><form class="search-box" @submit.prevent="searchNotes"><span>⌕</span><input v-model="noteSearch" placeholder="搜索标题或正文" aria-label="搜索笔记"><button v-if="searchedKeyword || noteSearch" type="button" class="search-clear" aria-label="清除搜索" @click="clearSearch">×</button><button type="submit" class="search-submit" :disabled="searchBusy">{{ searchBusy ? '搜索中' : '搜索' }}</button></form><p v-if="searchedKeyword" class="search-caption">“{{ searchedKeyword }}”的搜索结果 · 最多 5 条</p></div><div class="notes-list"><div v-if="!filteredNotes.length" class="list-empty">{{ searchedKeyword ? '没有找到匹配的笔记。试试其他关键词。' : '还没有笔记，写下第一个想法吧。' }}</div><button v-for="note in filteredNotes" :key="note.id" class="note-row" :class="{active: selectedNote?.id === note.id && !editingNote}" @click="openNote(note)"><div class="note-row-top"><span class="note-icon">▤</span><span>{{ date(note.createdAt) }}</span></div><strong>{{ note.title }}</strong><p>{{ note.content }}</p><div class="note-row-bottom">阅读笔记 <span>↗</span></div></button></div></section>
        <section class="note-detail"><template v-if="editingNote"><div class="detail-head"><div><span class="detail-label">{{ selectedNote ? '编辑笔记' : '新建笔记' }}</span><h2>{{ selectedNote ? '修改你的记录' : '写下一个想法' }}</h2></div><button class="icon-button" aria-label="取消编辑" @click="editingNote = false">×</button></div><form class="note-editor" @submit.prevent="saveNote"><label>标题<input v-model="noteForm.title" maxlength="50" placeholder="给这篇笔记一个标题"></label><label>正文<textarea v-model="noteForm.content" placeholder="从这里开始记录…"></textarea></label><div class="editor-actions"><button type="button" class="outline-button" @click="editingNote = false">取消</button><button class="primary" :disabled="noteBusy">{{ noteBusy ? '保存中…' : '保存笔记' }}</button></div></form></template>
          <template v-else-if="selectedNote"><div class="detail-head"><div><span class="detail-label">笔记 · {{ date(selectedNote.createdAt) }}</span><h2>{{ selectedNote.title }}</h2></div><div class="detail-actions"><button class="outline-button" @click="editNote">编辑</button><button class="icon-button danger" aria-label="删除笔记" title="删除笔记" @click="deleteNote">×</button></div></div><div class="note-body">{{ selectedNote.content }}</div><div class="note-ask"><div class="ask-heading"><span class="panel-symbol">✳</span><div><strong>基于这篇笔记提问</strong><p>助手会结合当前笔记内容回答。</p></div></div><form class="ask-form" @submit.prevent="askNote"><input v-model="question" placeholder="关于这篇笔记，你想了解什么？"><button class="send-button" :disabled="!question.trim() || askBusy" aria-label="发送笔记问题">↑</button></form><div v-if="askBusy" class="inline-state">正在阅读笔记并思考…</div><div v-if="noteAnswer" class="note-answer"><div class="answer-label">✳ 知页助手</div><p>{{ noteAnswer.answer }}</p><small>参考笔记：{{ noteAnswer.noteTitle }} · #{{ noteAnswer.noteId }}</small></div></div></template>
          <div v-else class="detail-empty"><div class="detail-empty-art">▤</div><h2>选一篇笔记，继续思考</h2><p>查看、编辑，或基于笔记向 AI 提问。</p><button v-if="!notes.length" class="outline-button" @click="newNote">＋ 新建笔记</button></div></section></div></main>
      <LearningHub v-else-if="active === 'learning'" :api="api" :notes="notes" @notify="toast" @reload-notes="reloadNotesSafely" @open-note="openSource" />
      <StudyWorkspace v-else-if="active === 'study'" :api="api" :notes="notes" @notify="toast" @reload-notes="reloadNotesSafely" @open-note="openSource" />
      <main v-else-if="active === 'knowledge'" class="main-content knowledge-page">
        <div class="page-heading"><div><div class="heading-label"><span class="heading-icon">⌕</span> 查找与思考</div><h1>从笔记中寻找答案<span class="heading-period">.</span></h1><p>输入关键词，助手会结合匹配的笔记回答问题。</p></div></div>
        <div class="knowledge-layout"><section class="knowledge-main"><div class="knowledge-top"><span class="panel-symbol">✳</span><div><h2>问问你的笔记</h2><p>先找到相关内容，再带着问题深入理解。</p></div></div>
          <div class="study-tabs"><button :class="{selected: knowledgeMode === 'semantic'}" @click="knowledgeMode = 'semantic'; knowledgeAnswer = null">语义问答</button><button :class="{selected: knowledgeMode === 'keyword'}" @click="knowledgeMode = 'keyword'; knowledgeAnswer = null">关键词问答</button></div><form class="knowledge-form" @submit.prevent="askKnowledge"><label v-if="knowledgeMode === 'keyword'">检索关键词<span>用于查找标题或正文中包含此词的笔记</span><input v-model="knowledgeKeyword" placeholder="例如：Java 接口" maxlength="200"></label><label>你的问题<textarea v-model="knowledgeQuestion" rows="4" placeholder="例如：接口适合在什么场景下使用？"></textarea></label><div class="knowledge-actions"><p>将检索匹配的笔记作为参考，提问可能调用模型。</p><button class="primary" :disabled="(knowledgeMode === 'keyword' && !knowledgeKeyword.trim()) || !knowledgeQuestion.trim() || knowledgeBusy">{{ knowledgeBusy ? '正在查找并思考…' : '查找并提问' }}</button></div></form>
          <div v-if="knowledgeBusy" class="knowledge-loading" role="status">正在检索笔记并准备回答，请稍候…</div><section v-if="knowledgeAnswer" class="knowledge-result"><div class="answer-heading"><span class="panel-symbol">✳</span><div><strong>知页助手</strong><small>{{ knowledgeMode === 'semantic' ? '根据语义相关的笔记片段生成' : '根据关键词匹配的笔记生成' }}</small></div></div><p class="answer-content">{{ knowledgeAnswer.answer }}</p><div class="source-area"><strong>参考笔记</strong><p v-if="!knowledgeAnswer.sources?.length">没有找到匹配的笔记，可以换一种说法再试。</p><div v-else class="source-list"><button v-for="source in knowledgeAnswer.sources" :key="`${source.noteId}-${source.chunkIndex ?? 0}`" class="source-chip" @click="openSource(source)"><span>▤</span>{{ source.noteTitle || source.title }} <small>#{{ source.noteId }}<template v-if="source.chunkIndex != null"> · 片段 {{ source.chunkIndex }}</template></small> ↗</button></div><p v-if="knowledgeAnswer.sources?.length">来源是提供给模型的笔记，回答仍需结合原文核实。</p></div></section>
          <div v-else-if="!knowledgeBusy" class="knowledge-empty"><span>✧</span><div><strong>从一个问题开始</strong><p>输入问题，相关笔记与回答会显示在这里。</p></div></div>
        </section><aside class="knowledge-aside"><div class="context-top knowledge-tip"><div class="context-icon">⌕</div><h2>已有的记录，<br>新的发现。</h2><p>语义问答根据含义寻找相关片段；关键词问答查找原词。</p><div class="abstract-leaf leaf-a"></div><div class="abstract-leaf leaf-b"></div></div><div class="context-section"><div class="context-title"><h3>怎样获得更好的回答</h3></div><div class="guide-step"><b>1</b><p><strong>选择一个关键词</strong><span>输入笔记中出现过的词语。</span></p></div><div class="guide-step"><b>2</b><p><strong>提出具体问题</strong><span>描述你想理解的知识点。</span></p></div><div class="guide-step"><b>3</b><p><strong>查看引用笔记</strong><span>打开原文，核对回答依据。</span></p></div></div></aside></div>
      </main>
      <main v-else class="main-content embedding-page"><div class="page-heading"><div><div class="heading-label"><span class="heading-icon">◈</span> 向量与语义</div><h1>看看文字如何关联<span class="heading-period">.</span></h1><p>通过向量表示，探索两段文本之间的语义相似度。</p></div></div>
        <div class="embedding-layout"><section class="embedding-card"><div class="knowledge-top"><span class="panel-symbol">◈</span><div><h2>文本相似度</h2><p>输入两段文本，计算它们的余弦相似度。</p></div></div><form class="knowledge-form" @submit.prevent="compareTexts"><label>第一段文本<textarea v-model="similarityA" rows="4" placeholder="例如：Java 接口规定了类的行为。"></textarea></label><label>第二段文本<textarea v-model="similarityB" rows="4" placeholder="例如：接口定义了对象应该提供的方法。"></textarea></label><div class="knowledge-actions"><p>首次运行可能需要等待本地模型加载。</p><button class="primary" :disabled="!similarityA.trim() || !similarityB.trim() || similarityBusy">{{ similarityBusy ? '计算中…' : '计算相似度' }}</button></div></form><div v-if="similarityResult !== null" class="similarity-result"><span>余弦相似度</span><strong>{{ Number(similarityResult).toFixed(4) }}</strong><p>数值越接近 1，表示这两段文本的向量方向越接近；结果仍需结合具体语境理解。</p></div></section><aside class="embedding-side"><section class="dimension-card"><span class="panel-symbol">▦</span><h2>向量有多少维？</h2><p>输入一句话，查看模型生成的向量长度。</p><form @submit.prevent="checkDimensions"><textarea v-model="dimensionText" rows="4" placeholder="在这里输入一段文字…"></textarea><button class="outline-button" :disabled="!dimensionText.trim() || dimensionBusy">{{ dimensionBusy ? '计算中…' : '查看维度' }}</button></form><div v-if="dimensions !== null" class="dimension-result"><strong>{{ dimensions }}</strong><span>维向量</span></div></section><div class="context-tip embedding-tip"><span>✧</span><div><strong>关于语义检索</strong><p>这里展示向量计算；你也可以在笔记问答中尝试语义检索。</p></div></div></aside></div>
      </main>
      <footer class="app-footer"><span>知页 · 让学习留下痕迹</span><span>专注当下，慢慢积累。</span></footer>
    </div>
    <div v-if="showNewSession" class="modal-backdrop" @click.self="showNewSession = false"><section class="modal" role="dialog" aria-modal="true" aria-labelledby="session-modal-title"><div class="modal-head"><span class="panel-symbol">✳</span><button class="icon-button" aria-label="关闭" @click="showNewSession = false">×</button></div><h2 id="session-modal-title">开启一场新对话</h2><p>给这次学习取个名字，方便以后找回来。</p><form @submit.prevent="createSession"><label>会话标题<input v-model="sessionTitle" maxlength="50" autofocus placeholder="例如：Java 集合学习"></label><div class="modal-actions"><button type="button" class="outline-button" @click="showNewSession = false">取消</button><button class="primary" :disabled="sessionBusy">{{ sessionBusy ? '创建中…' : '创建会话' }}</button></div></form></section></div>
  </div>
</template>
