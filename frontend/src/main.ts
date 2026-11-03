import './style.css'

type Role = 'client' | 'admin' | 'analyst'
type ApiResult = { status: number; body: unknown }
type FormValues = Record<string, FormDataEntryValue>

const tokens: Partial<Record<Role, string>> = {}
const get = <T extends Element>(selector: string) => document.querySelector<T>(selector)!
const formValues = (form: HTMLFormElement): FormValues => Object.fromEntries(new FormData(form).entries())

function solicitationId(selector: string): string {
  const id = get<HTMLInputElement>(selector).value.trim()
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id)) {
    throw new Error('Informe um ID de solicitação válido.')
  }
  return id
}

async function api(path: string, method = 'GET', body?: unknown, role?: Role): Promise<ApiResult> {
  if (role && !tokens[role]) throw new Error('Entre como ' + roleName(role) + ' primeiro.')
  const response = await fetch(path, {
    method,
    headers: {
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
      ...(role ? { Authorization: 'Bearer ' + tokens[role] } : {}),
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  })
  const raw = await response.text()
  let parsed: unknown = raw
  try { parsed = raw ? JSON.parse(raw) : null } catch { /* Exibe texto se não for JSON. */ }
  return { status: response.status, body: parsed }
}

function roleName(role: Role): string {
  return { client: 'cliente', admin: 'administrador', analyst: 'analista' }[role]
}

function roleInToken(token: string): string | null {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const claims = JSON.parse(atob(payload))
    return typeof claims.role === 'string' ? claims.role : null
  } catch {
    return null
  }
}

function show(operation: string, result: ApiResult): void {
  get<HTMLElement>('#operation').textContent = operation
  const status = get<HTMLElement>('#status')
  status.textContent = result.status ? String(result.status) : 'Erro local'
  status.className = 'badge ' + (result.status > 0 && result.status < 400 ? 'ok' : 'fail')
  get<HTMLElement>('#output').textContent = JSON.stringify(result.body, null, 2)
}

function showError(operation: string, error: unknown): void {
  show(operation, { status: 0, body: { error: error instanceof Error ? error.message : 'Falha de conexão.' } })
}

function renderResults(body: unknown): void {
  const container = get<HTMLDivElement>('#results')
  container.replaceChildren()
  if (!body || typeof body !== 'object' || !('content' in body) || !Array.isArray(body.content)) return
  if (body.content.length === 0) { container.textContent = 'Nenhuma solicitação encontrada.'; return }
  for (const item of body.content) {
    const row = document.createElement('div')
    row.className = 'result'
    const details = document.createElement('div')
    const title = document.createElement('strong')
    title.textContent = String(item.title ?? item.id)
    const meta = document.createElement('small')
    meta.textContent = String(item.status) + ' · ' + String(item.state ?? 'UF não informada') + ' · ' + String(item.id)
    const button = document.createElement('button')
    button.type = 'button'
    button.textContent = 'Usar ID'
    button.addEventListener('click', () => { get<HTMLInputElement>('#analyst-id').value = String(item.id) })
    details.append(title, meta)
    row.append(details, button)
    container.append(row)
  }
}

async function execute(action: string, form?: HTMLFormElement): Promise<void> {
  const data = form ? formValues(form) : {}
  let result: ApiResult
  if (action === 'register') {
    result = await api('/auth/register', 'POST', data)
    if (result.status === 201) {
      get<HTMLInputElement>('form[data-action="login-client"] input[name="email"]').value = String(data.email)
      get<HTMLInputElement>('form[data-action="login-client"] input[name="password"]').value = String(data.password)
    }
  } else if (action.startsWith('login-')) {
    const role = action.slice(6) as Role
    delete tokens[role]
    const badge = get<HTMLElement>('#' + role + '-session')
    badge.textContent = 'Não autenticado'
    badge.classList.remove('ok')
    result = await api('/auth/login', 'POST', data)
    if (result.status < 400 && result.body && typeof result.body === 'object' && 'accessToken' in result.body) {
      const token = String(result.body.accessToken)
      if (roleInToken(token) !== role.toUpperCase()) {
        result = { status: 0, body: { error: 'Esta conta não tem o perfil de ' + roleName(role) + '.' } }
      } else {
        tokens[role] = token
        badge.textContent = 'Autenticado'
        badge.classList.add('ok')
        result = { ...result, body: { message: 'Autenticado com sucesso', expiresIn: 'expiresIn' in result.body ? result.body.expiresIn : undefined } }
      }
    }
  } else if (action === 'create-analyst') {
    const states = String(data.coveredStates).split(',').map(s => s.trim().toUpperCase()).filter(Boolean)
    result = await api('/admin/users', 'POST', { name: data.name, email: data.email, password: data.password, role: 'ANALYST', coveredStates: states }, 'admin')
    if (result.status === 201) {
      get<HTMLInputElement>('form[data-action="login-analyst"] input[name="email"]').value = String(data.email)
      get<HTMLInputElement>('form[data-action="login-analyst"] input[name="password"]').value = String(data.password)
    }
  } else if (action === 'create') {
    result = await api('/solicitations', 'POST', data, 'client')
    if (result.status < 400 && result.body && typeof result.body === 'object' && 'id' in result.body) {
      get<HTMLInputElement>('#client-id').value = String(result.body.id)
      get<HTMLInputElement>('#analyst-id').value = String(result.body.id)
    }
  } else if (action === 'address') {
    result = await api('/solicitations/' + solicitationId('#client-id') + '/address', 'PUT', {
      postalCode: data.postalCode, number: data.number, complement: data.complement || null,
    }, 'client')
  } else if (action === 'confirmation') {
    result = await api('/solicitations/' + solicitationId('#client-id') + '/confirmation', 'PUT', {
      priority: data.priority,
      preferredDate: data.preferredDate,
      estimatedValue: data.estimatedValue === '' ? null : Number(data.estimatedValue),
      termsAccepted: data.termsAccepted === 'on',
    }, 'client')
  } else if (action === 'submit') {
    result = await api('/solicitations/' + solicitationId('#client-id') + '/submit', 'POST', undefined, 'client')
  } else if (action === 'list') {
    result = await api('/analyst/solicitations?page=0&size=20', 'GET', undefined, 'analyst')
    if (result.status < 400) renderResults(result.body)
  } else if (action === 'search') {
    const params = new URLSearchParams({ page: '0', size: '20' })
    for (const [key, value] of Object.entries(data)) if (value) params.set(key, String(value).trim())
    result = await api('/analyst/solicitations/search?' + params, 'GET', undefined, 'analyst')
    if (result.status < 400) renderResults(result.body)
  } else if (action === 'detail') {
    result = await api('/analyst/solicitations/' + solicitationId('#analyst-id'), 'GET', undefined, 'analyst')
  } else if (action === 'start') {
    result = await api('/analyst/solicitations/' + solicitationId('#analyst-id') + '/start', 'POST', undefined, 'analyst')
  } else if (action === 'decision') {
    result = await api('/analyst/solicitations/' + solicitationId('#analyst-id') + '/decide', 'POST', data, 'analyst')
  } else return
  show(action, result)
}

document.querySelectorAll<HTMLButtonElement>('[data-tab]').forEach(button => button.addEventListener('click', () => {
  document.querySelectorAll<HTMLButtonElement>('[data-tab]').forEach(tab => tab.classList.toggle('active', tab === button))
  document.querySelectorAll<HTMLElement>('[data-panel]').forEach(panel => { panel.hidden = panel.dataset.panel !== button.dataset.tab })
}))

document.querySelectorAll<HTMLFormElement>('form[data-action]').forEach(form => form.addEventListener('submit', async event => {
  event.preventDefault()
  try { await execute(form.dataset.action!, form) } catch (error) { showError(form.dataset.action!, error) }
}))

document.querySelectorAll<HTMLButtonElement>('button[data-action]').forEach(button => button.addEventListener('click', async () => {
  try { await execute(button.dataset.action!) } catch (error) { showError(button.dataset.action!, error) }
}))

const preferredDate = new Date()
preferredDate.setDate(preferredDate.getDate() + 7)
get<HTMLInputElement>('input[name="preferredDate"]').value = preferredDate.toISOString().slice(0, 10)

api('/actuator/health').then(result => {
  get<HTMLElement>('#health').textContent = result.status === 200 ? '● API conectada' : '● API indisponível'
}).catch(() => { get<HTMLElement>('#health').textContent = '● API indisponível' })
