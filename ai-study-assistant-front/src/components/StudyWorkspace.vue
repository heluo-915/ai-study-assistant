<script setup>
import { ref, watch } from 'vue'
const props = defineProps({ api: { type: Function, required: true }, notes: { type: Array, required: true } })
const emit = defineEmits(['notify', 'reload-notes', 'open-note'])
const tab = ref('import'), title = ref(''), file = ref(null), preview = ref(null), busy = ref(false), result = ref('')
const noteId = ref(''), quizzes = ref([]), current = ref(null), selection = ref(''), answer = ref(null), attempts = ref([])
const wrong = ref([]), filter = ref(''), statistics = ref(null), listLoaded = ref(false), nextQuiz = ref(null), nextAttempt = ref(null)
const notify = (message) => emit('notify', message)
const report = (error) => notify(error.message)
function selectFile(event) { file.value = event.target.files[0] || null; preview.value = null; result.value = '' }
function validateFile() {
  if (!file.value) throw new Error('请选择 TXT 或 PDF 文件。')
  if (!title.value.trim() || title.value.trim().length > 50) throw new Error('请填写不超过 50 字的标题。')
  const pdf = file.value.name.toLowerCase().endsWith('.pdf'), txt = file.value.name.toLowerCase().endsWith('.txt')
  if (!pdf && !txt) throw new Error('请选择 TXT 或 PDF 文件。')
  if (file.value.size > (pdf ? 2 * 1024 * 1024 : 10 * 1024)) throw new Error(pdf ? 'PDF 文件不能超过 2 MiB。' : 'TXT 文件不能超过 10 KiB。')
  return pdf
}
async function previewPdf() {
  try { if (!validateFile()) throw new Error('预览功能仅支持 PDF。'); busy.value = true; const form = new FormData(); form.append('file', file.value); preview.value = await props.api('/documents/pdf/preview', { method: 'POST', body: form }) }
  catch (e) { report(e) } finally { busy.value = false }
}
async function upload() {
  if (busy.value) return
  try { const pdf = validateFile(); busy.value = true; result.value = ''; const form = new FormData(); form.append('file', file.value); form.append('title', title.value.trim()); const saved = await props.api(pdf ? '/documents/pdf/import' : '/notes/upload', { method: 'POST', body: form }); result.value = saved.chunksGenerated ? '导入成功，笔记片段已生成。' : '导入成功，但笔记片段未生成；可稍后补建。'; emit('reload-notes'); notify(result.value) }
  catch (e) { report(e) } finally { busy.value = false }
}
async function rebuild() {
  if (busy.value) return
  try { busy.value = true; const r = await props.api('/notes/chunks/build-missing', { method: 'POST' }); result.value = `补建完成：共 ${r.total} 篇，成功 ${r.succeeded} 篇，失败 ${r.failed} 篇。${r.failedNoteIds?.length ? `失败笔记 ID：${r.failedNoteIds.join('、')}` : ''}`; notify(result.value) }
  catch (e) { report(e) } finally { busy.value = false }
}
async function loadQuizzes(more = false) {
  if (!noteId.value || busy.value) return
  try { busy.value = true; const list = await props.api(`/notes/${noteId.value}/quizzes${more ? `?beforeId=${nextQuiz.value}` : ''}`); quizzes.value = more ? [...quizzes.value, ...list] : list; nextQuiz.value = list.length === 50 ? list[list.length - 1].quizId : null; listLoaded.value = true }
  catch (e) { report(e) } finally { busy.value = false }
}
watch(noteId, () => { quizzes.value = []; current.value = null; answer.value = null; listLoaded.value = false; nextQuiz.value = null; if (noteId.value) loadQuizzes() })
async function generate() {
  if (!noteId.value || busy.value) return
  try { busy.value = true; current.value = await props.api(`/notes/${noteId.value}/quiz`, { method: 'POST' }); selection.value = ''; answer.value = null; attempts.value = []; notify('练习题已生成。') }
  catch (e) { report(e) } finally { busy.value = false; await loadQuizzes() }
}
async function choose(quiz) { current.value = quiz; selection.value = ''; answer.value = null; attempts.value = []; nextAttempt.value = null; await loadAttempts() }
async function loadAttempts(more = false) {
  if (!current.value) return
  try { const list = await props.api(`/quiz/${current.value.quizId}/attempts${more ? `?beforeId=${nextAttempt.value}` : ''}`); attempts.value = more ? [...attempts.value, ...list] : list; nextAttempt.value = list.length === 50 ? list[list.length - 1].attemptId : null }
  catch (e) { report(e) }
}
async function submit() {
  if (!current.value || !selection.value || busy.value) return
  try { busy.value = true; answer.value = await props.api(`/quiz/${current.value.quizId}/answer`, { method: 'POST', body: JSON.stringify({ answer: selection.value }) }); await loadAttempts(); if (tab.value === 'review') await loadReview() }
  catch (e) { report(e) } finally { busy.value = false }
}
async function loadReview() {
  try { const [w, s] = await Promise.all([props.api(`/quiz/wrong${filter.value ? `?latestCorrect=${filter.value}` : ''}`), props.api('/quiz/statistics')]); wrong.value = w; statistics.value = s }
  catch (e) { report(e) }
}
watch([tab, filter], () => { if (tab.value === 'review') loadReview() })
</script>
<template>
  <main class="main-content study-page">
    <div class="page-heading"><div><div class="heading-label"><span class="heading-icon">✦</span> 让知识留下来</div><h1>学习工具<span class="heading-period">.</span></h1><p>导入资料、练习与复习，让每一次记录都有回响。</p></div></div>
    <div class="study-tabs"><button v-for="item in [{id:'import',name:'资料导入'}, {id:'practice',name:'笔记练习'}, {id:'review',name:'错题与统计'}]" :key="item.id" :class="{selected: tab === item.id}" @click="tab = item.id">{{ item.name }}</button></div>
    <div v-if="tab === 'import'" class="study-grid"><section class="study-card"><span class="study-eyebrow">YOUR LIBRARY · 01</span><h2>把资料放进知识库</h2><p>导入后会创建一篇笔记，并尝试生成语义检索片段。</p><form class="study-form" @submit.prevent="upload"><label>笔记标题<input v-model="title" maxlength="50" placeholder="例如：Java 集合学习笔记"></label><label>选择文件<input type="file" accept=".txt,.pdf,text/plain,application/pdf" @change="selectFile"></label><p class="study-hint">TXT 不超过 10 KiB；PDF 不超过 2 MiB、50 页。PDF 仅提取文本，不支持扫描图片识别。</p><div class="study-actions"><button v-if="file?.name.toLowerCase().endsWith('.pdf')" type="button" class="outline-button" :disabled="busy" @click="previewPdf">预览 PDF 文本</button><button class="primary" :disabled="busy">{{ busy ? '处理中…' : '导入为笔记' }}</button></div></form><div v-if="preview" class="study-preview"><strong>提取预览 · {{ preview.totalCodePoints }} 字符</strong><p>{{ preview.preview }}</p><small v-if="preview.truncated">仅展示部分内容。</small></div><p v-if="result" class="study-result">{{ result }}</p></section><aside class="study-card study-aside"><span class="panel-symbol">◈</span><h2>让旧笔记也能被找到</h2><p>语义问答使用笔记片段。对于导入或创建时未生成片段的笔记，可以尝试补建。</p><button class="outline-button" :disabled="busy" @click="rebuild">补建缺失片段</button><p class="study-hint">首次运行本地向量模型可能需要一些时间。</p></aside></div>
    <div v-else class="study-grid"><section class="study-card"><template v-if="tab === 'practice'"><span class="study-eyebrow">PRACTICE · 02</span><h2>从笔记生成练习</h2><p>选择笔记，生成一道题；每次提交都会记录一次答题。</p><div class="study-form"><label>选择笔记<select v-model="noteId"><option value="">请选择一篇笔记</option><option v-for="note in notes" :key="note.id" :value="note.id">{{ note.title }}</option></select></label><div class="study-actions"><button class="primary" :disabled="!noteId || busy" @click="generate">{{ busy ? '处理中…' : '生成练习题' }}</button></div><small>生成题目会调用 AI 模型。</small></div><div v-if="listLoaded" class="study-list"><strong>这篇笔记的题目</strong><p v-if="!quizzes.length">还没有题目，试着生成一道吧。</p><button v-for="quiz in quizzes" :key="quiz.quizId" @click="choose(quiz)"><span>{{ quiz.question }}</span> ↗</button><button v-if="nextQuiz" :disabled="busy" @click="loadQuizzes(true)">加载更早的题目</button></div></template>
    <template v-else><span class="study-eyebrow">REVIEW · 03</span><h2>把错过的，再弄明白</h2><div v-if="statistics" class="study-stats"><div><strong>{{ statistics.totalAttempts }}</strong><span>答题次数</span></div><div><strong>{{ statistics.correctAttempts }}</strong><span>答对次数</span></div><div><strong>{{ statistics.incorrectAttempts }}</strong><span>答错次数</span></div></div><label class="filter-label">最近一次答题<select v-model="filter"><option value="">全部曾答错的题</option><option value="false">仍未答对</option><option value="true">已答对</option></select></label><div class="study-list"><p v-if="!wrong.length">暂时没有符合条件的错题。</p><button v-for="quiz in wrong" :key="quiz.quizId" @click="choose(quiz)"><span>{{ quiz.question }} <small>{{ quiz.latestCorrect ? '已答对' : '待复习' }}</small></span> ↗</button><small>最多显示最近 50 道曾答错的题。</small></div></template></section>
    <aside class="study-card study-question"><template v-if="current"><span class="study-eyebrow">QUESTION · #{{ current.quizId }}</span><h2>{{ current.question }}</h2><form @submit.prevent="submit"><label v-for="option in current.options" :key="option.label" class="quiz-option" :class="{chosen: selection === option.label}"><input v-model="selection" type="radio" name="answer" :value="option.label"><b>{{ option.label }}</b><span>{{ option.content }}</span></label><button class="primary" :disabled="!selection || busy">{{ busy ? '提交中…' : '提交答案' }}</button></form><div v-if="answer" class="quiz-answer"><strong>{{ answer.correct ? '回答正确' : '再想一想' }} · 正确答案 {{ answer.correctAnswer }}</strong><p>{{ answer.explanation }}</p></div><div class="study-list"><strong>答题记录</strong><p v-if="!attempts.length">还没有答题记录。</p><div v-for="attempt in attempts" :key="attempt.attemptId" class="attempt">{{ attempt.correct ? '答对' : '答错' }} · 选择 {{ attempt.selectedAnswer }} <small>{{ attempt.createdAt?.replace('T', ' ') }}</small></div><button v-if="nextAttempt" @click="loadAttempts(true)">加载更早的记录</button></div></template><template v-else><span class="panel-symbol">✧</span><h2>准备好了吗？</h2><p>生成或选择一道题，答案和解析会在提交后显示在这里。</p></template></aside></div>
  </main>
</template>
