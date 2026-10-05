const state = {
  tasks: [],
  filter: "all",
  search: "",
  sort: "updated-desc"
};

const el = {
  taskList: document.querySelector("#task-list"),
  emptyState: document.querySelector("#empty-state"),
  searchInput: document.querySelector("#search-input"),
  sortSelect: document.querySelector("#sort-select"),
  resultsLabel: document.querySelector("#results-label"),
  viewTitle: document.querySelector("#view-title"),
  viewSubtitle: document.querySelector("#view-subtitle"),
  dialog: document.querySelector("#task-dialog"),
  form: document.querySelector("#task-form"),
  taskId: document.querySelector("#task-id"),
  taskTitle: document.querySelector("#task-title"),
  taskStatus: document.querySelector("#task-status"),
  dialogTitle: document.querySelector("#dialog-title"),
  deleteBtn: document.querySelector("#delete-task-btn"),
  toast: document.querySelector("#toast")
};

async function api(path = "", options = {}) {
  const response = await fetch(`/api/tasks${path}`, {
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.error || `Request failed (${response.status})`);
  }
  if (response.status === 204) return null;
  return response.json();
}

async function loadTasks() {
  state.tasks = await api();
  render();
}

async function createTask(description, status = "todo") {
  await api("", {
    method: "POST",
    body: JSON.stringify({ description: description.trim(), status })
  });
  await loadTasks();
  toast("Task created");
}

async function updateTask(id, updates) {
  await api(`/${id}`, {
    method: "PUT",
    body: JSON.stringify(updates)
  });
  await loadTasks();
  toast("Task updated");
}

async function deleteTask(id) {
  await api(`/${id}`, { method: "DELETE" });
  await loadTasks();
  toast("Task deleted");
}

async function cycleStatus(id) {
  const order = ["todo", "in-progress", "done"];
  const task = state.tasks.find(t => t.id === id);
  if (!task) return;
  const next = order[(order.indexOf(task.status) + 1) % order.length];
  await updateTask(id, { status: next });
}

async function clearCompleted() {
  const completed = state.tasks.filter(t => t.status === "done");
  if (!completed.length) {
    toast("No completed tasks");
    return;
  }
  await Promise.all(completed.map(task => api(`/${task.id}`, { method: "DELETE" })));
  await loadTasks();
  toast("Completed tasks cleared");
}

function visibleTasks() {
  let tasks = [...state.tasks];
  if (state.filter !== "all") tasks = tasks.filter(t => t.status === state.filter);

  const q = state.search.trim().toLowerCase();
  if (q) tasks = tasks.filter(t => t.description.toLowerCase().includes(q));

  const sorters = {
    "updated-desc": (a, b) => new Date(b.updatedAt) - new Date(a.updatedAt),
    "created-desc": (a, b) => new Date(b.createdAt) - new Date(a.createdAt),
    "created-asc": (a, b) => new Date(a.createdAt) - new Date(b.createdAt),
    "title-asc": (a, b) => a.description.localeCompare(b.description)
  };
  tasks.sort(sorters[state.sort]);
  return tasks;
}

function formatDate(iso) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso || "";
  return new Intl.DateTimeFormat(undefined, {
    month: "short", day: "numeric", hour: "numeric", minute: "2-digit"
  }).format(date);
}

function statusText(status) {
  return { todo: "Todo", "in-progress": "In progress", done: "Done" }[status] || status;
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, ch => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#039;"
  })[ch]);
}

function taskRow(task) {
  const row = document.createElement("article");
  row.className = `task-row ${task.status === "done" ? "done" : ""}`;
  row.innerHTML = `
    <div class="task-copy">
      <p class="task-title">${escapeHtml(task.description)}</p>
      <div class="task-meta">Updated ${formatDate(task.updatedAt)}</div>
    </div>
    <button class="status-badge ${task.status}" data-action="status" title="Click to change status">${statusText(task.status)}</button>
    <div class="row-actions">
      <button data-action="edit" title="Edit task">✎</button>
      <button data-action="delete" title="Delete task">⌫</button>
    </div>`;

  row.querySelector('[data-action="status"]').addEventListener("click", () => safe(() => cycleStatus(task.id)));
  row.querySelector('[data-action="edit"]').addEventListener("click", () => openEditDialog(task));
  row.querySelector('[data-action="delete"]').addEventListener("click", () => safe(() => deleteTask(task.id)));
  return row;
}

function render() {
  const tasks = visibleTasks();
  el.taskList.innerHTML = "";
  tasks.forEach(task => el.taskList.appendChild(taskRow(task)));
  el.emptyState.classList.toggle("hidden", tasks.length !== 0);
  el.taskList.classList.toggle("hidden", tasks.length === 0);
  el.resultsLabel.textContent = `${tasks.length} ${tasks.length === 1 ? "task" : "tasks"}`;

  const total = state.tasks.length;
  const todo = state.tasks.filter(t => t.status === "todo").length;
  const progress = state.tasks.filter(t => t.status === "in-progress").length;
  const done = state.tasks.filter(t => t.status === "done").length;

  document.querySelector("#count-all").textContent = total;
  document.querySelector("#count-todo").textContent = todo;
  document.querySelector("#count-progress").textContent = progress;
  document.querySelector("#count-done").textContent = done;
  document.querySelector("#stat-total").textContent = total;
  document.querySelector("#stat-progress").textContent = progress;
  document.querySelector("#stat-done").textContent = done;

  document.querySelectorAll(".nav-item").forEach(btn => btn.classList.toggle("active", btn.dataset.filter === state.filter));
  const titles = {
    all: ["All tasks", "Everything you need to get done."],
    todo: ["Todo", "Tasks waiting to be started."],
    "in-progress": ["In progress", "Tasks you're currently working on."],
    done: ["Done", "Completed work."]
  };
  [el.viewTitle.textContent, el.viewSubtitle.textContent] = titles[state.filter];
}

function openCreateDialog() {
  el.form.reset();
  el.taskId.value = "";
  el.taskStatus.value = "todo";
  el.dialogTitle.textContent = "Create task";
  el.deleteBtn.classList.add("hidden");
  el.dialog.showModal();
  setTimeout(() => el.taskTitle.focus(), 20);
}

function openEditDialog(task) {
  el.taskId.value = task.id;
  el.taskTitle.value = task.description;
  el.taskStatus.value = task.status;
  el.dialogTitle.textContent = "Edit task";
  el.deleteBtn.classList.remove("hidden");
  el.dialog.showModal();
  setTimeout(() => el.taskTitle.focus(), 20);
}

function closeDialog() { el.dialog.close(); }

let toastTimer;
function toast(message) {
  clearTimeout(toastTimer);
  el.toast.textContent = message;
  el.toast.classList.add("show");
  toastTimer = setTimeout(() => el.toast.classList.remove("show"), 1800);
}

async function safe(fn) {
  try { await fn(); }
  catch (error) { console.error(error); toast(error.message || "Something went wrong"); }
}

document.querySelector("#add-task-btn").addEventListener("click", openCreateDialog);
document.querySelector("#empty-add-btn").addEventListener("click", openCreateDialog);
document.querySelector("#dialog-close").addEventListener("click", closeDialog);
document.querySelector("#cancel-btn").addEventListener("click", closeDialog);
document.querySelector("#clear-completed-btn").addEventListener("click", () => safe(clearCompleted));

document.querySelectorAll(".nav-item").forEach(btn => btn.addEventListener("click", () => {
  state.filter = btn.dataset.filter;
  render();
}));

el.searchInput.addEventListener("input", e => { state.search = e.target.value; render(); });
el.sortSelect.addEventListener("change", e => { state.sort = e.target.value; render(); });

el.form.addEventListener("submit", e => {
  e.preventDefault();
  safe(async () => {
    const description = el.taskTitle.value.trim();
    if (!description) return el.taskTitle.focus();
    const id = el.taskId.value;
    const status = el.taskStatus.value;
    if (id) await updateTask(Number(id), { description, status });
    else await createTask(description, status);
    closeDialog();
  });
});

el.deleteBtn.addEventListener("click", () => safe(async () => {
  const id = Number(el.taskId.value);
  if (!Number.isFinite(id)) return;
  await deleteTask(id);
  closeDialog();
}));

el.dialog.addEventListener("click", e => { if (e.target === el.dialog) closeDialog(); });

document.addEventListener("keydown", e => {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
    e.preventDefault(); el.searchInput.focus();
  }
  if (e.key.toLowerCase() === "n" && !["INPUT", "SELECT", "TEXTAREA"].includes(document.activeElement.tagName)) {
    openCreateDialog();
  }
});

safe(loadTasks);
