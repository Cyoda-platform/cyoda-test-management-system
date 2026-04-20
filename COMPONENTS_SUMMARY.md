# Defect Modal Components Summary

## 📦 Созданные компоненты

### 1. DefectAttachmentsUpload.tsx
**Назначение:** Переиспользуемый компонент для управления загрузкой и отображением файлов

**Режимы:**
- **Edit mode** (`readonly={false}`): Показывает drag & drop зону + список файлов + кнопки удаления
- **View mode** (`readonly={true}`): Только список файлов, без upload зоны и кнопок удаления

**Props:**
```typescript
interface DefectAttachmentsUploadProps {
  files: AttachmentFile[];
  onFilesChange: (files: AttachmentFile[]) => void;
  readonly?: boolean;
}
```

**Функции:**
- ✅ Drag & drop зона с hover effects
- ✅ Click to browse для выбора файлов
- ✅ Отображение размера файла (B, KB, MB)
- ✅ Иконки для разных типов файлов (image, PDF, text, generic)
- ✅ Удаление файлов (только в edit режиме)
- ✅ Пустое состояние "No attachments"

---

### 2. EditDefectModal.tsx
**Назначение:** Модальное окно для редактирования дефекта с поддержкой загрузки файлов

**Props:**
```typescript
interface EditDefectModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  defect: Defect | null;
  displayId?: string; // e.g., "DEF-1"
  existingAttachments?: AttachmentFile[];
  onSave: (defect: Defect, newFiles: File[], removedAttachmentIds: string[]) => Promise<void>;
}
```

**Структура:**
```
Edit Defect Modal
├── Header
│   ├── Title: "Edit Defect"
│   └── Subtitle: Defect ID (e.g., "DEF-1")
├── Body
│   ├── Title (input)
│   ├── Description (textarea)
│   ├── Severity, Status, Source (3-col grid, dropdowns + input)
│   ├── External Link (input)
│   └── Attachments (DefectAttachmentsUpload, editable)
└── Footer
    ├── Cancel button
    └── Save Changes button
```

**Функции:**
- ✅ Редактирование всех полей дефекта
- ✅ Загрузка новых файлов через DefectAttachmentsUpload
- ✅ Удаление существующих файлов (отслеживание removedAttachmentIds)
- ✅ Валидация обязательного поля (title)
- ✅ Loading state при сохранении
- ✅ Обработка ошибок через toast

---

### 3. ViewDefectModal.tsx
**Назначение:** Модальное окно для просмотра дефекта (все поля read-only)

**Props:**
```typescript
interface ViewDefectModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  defect: Defect | null;
  displayId?: string;
  existingAttachments?: AttachmentFile[];
  formatDate?: (date: string) => string;
}
```

**Структура:**
```
View Defect Modal
├── Header
│   ├── Title: "[DEF-1] Defect Title"
│   └── Subtitle: "Defect details"
├── Body
│   ├── Description (read-only box, scrollable)
│   ├── Source, Created Date (2-col grid, read-only)
│   ├── Severity, Status, Source (3-col grid, read-only)
│   ├── External Link (clickable link, if present)
│   └── Attachments (DefectAttachmentsUpload, readonly)
└── Footer
    └── Close button
```

**Функции:**
- ✅ Отображение всех полей как read-only
- ✅ Красивые read-only boxes с borders
- ✅ Внешние ссылки как кликабельные элементы
- ✅ Дата форматируется через `formatDate` функцию
- ✅ UUID в source truncated на первые 8 символов
- ✅ Пустое состояние для пусто

---

## 📐 Единообразный дизайн

### Размеры и проблемы
- **Modal width:** `sm:max-w-3xl` (768px на десктопе)
- **Input height:** `h-9` (36px)
- **Textarea height:** `min-h-[140px]`
- **Padding:** 24px в header и body
- **Gap:** 20px между form groups, 12px между grid items

### Стили
- **Labels:** Uppercase, monospace, gray, small
- **Inputs (Edit):** White bg, thin border, subtle focus ring
- **Read-only (View):** White bg, thin border, no focus ring
- **Upload zone:** Dashed border, hover effect, centered text

---

## 🔄 Интеграция в Defects.tsx

### Before (Inline модалы)
```tsx
<Dialog open={editOpen} onOpenChange={setEditOpen}>
  <DialogContent className="sm:max-w-3xl bg-card">
    {/* ~100 строк inline кода */}
    <div>Title</div>
    <div>Description</div>
    {/* ... etc */}
  </DialogContent>
</Dialog>
```

### After (Новые компоненты)
```tsx
import EditDefectModal from '@/components/EditDefectModal';
import ViewDefectModal from '@/components/ViewDefectModal';

<EditDefectModal
  open={editOpen}
  onOpenChange={setEditOpen}
  defect={editTarget}
  displayId={defectDisplayIdMap[editTarget?.id]}
  existingAttachments={editAttachments}
  onSave={handleEditDefectSave}
/>

<ViewDefectModal
  open={viewOpen}
  onOpenChange={setViewOpen}
  defect={viewTarget}
  displayId={defectDisplayIdMap[viewTarget?.id]}
  existingAttachments={viewAttachments}
  formatDate={formatDate}
/>
```

**Преимущества:**
- ✅ Код в Defects.tsx становится чище
- ✅ Нет дублирования логики между modals
- ✅ Легче тестировать
- ✅ Стили согласованы автоматически

---

## 🔗 Интеграция в RunExecution.tsx

Используется точно так же, как в Defects.tsx:
```tsx
<EditDefectModal {...props} />
<ViewDefectModal {...props} />
```

Теперь defect modals имеют одинаковый вид независимо от страницы, на которой они открыты.

---

## 📝 Дополнительно: CreateDefectModal

Существующий компонент CreateDefectModal уже имеет хороший дизайн, но для полной унификации:

**Вариант 1 (минимальные изменения):**
- Убедиться, что размер modal соответствует: `sm:max-w-3xl` ✅
- Убедиться, что стили input/textarea совпадают ✅
- Стили file upload zone уже хорошие ✅

**Вариант 2 (рефакторинг):**
- Заменить file upload логику на DefectAttachmentsUpload
- Это даст полную унификацию всех трёх компонентов

---

## ✅ Checklist для внедрения

### 1. Копирование компонентов
- [ ] DefectAttachmentsUpload.tsx скопирован в `apps/frontend/src/components/`
- [ ] EditDefectModal.tsx скопирован
- [ ] ViewDefectModal.tsx скопирован

### 2. Обновление Defects.tsx
- [ ] Import новых компонентов
- [ ] Добавить state для `editAttachments` и `editOpen`
- [ ] Добавить state для `viewAttachments` и `viewOpen`
- [ ] Заменить inline Edit modal на `<EditDefectModal />`
- [ ] Заменить inline View modal на `<ViewDefectModal />`
- [ ] Обновить обработчик сохранения для файлов

### 3. Обновление RunExecution.tsx
- [ ] Import новых компонентов
- [ ] Аналогичные изменения как в Defects.tsx

### 4. Тестирование
- [ ] Создание дефекта (должно работать как раньше)
- [ ] Редактирование дефекта + загрузка файла
- [ ] Удаление файла из дефекта
- [ ] Просмотр дефекта (read-only)
- [ ] Внешние ссылки открываются в новой вкладке
- [ ] Проверить на обеих страницах (Defects и RunExecution)

### 5. API интеграция
- [ ] POST /projects/:projectId/defects/:defectId/attachments (upload)
- [ ] DELETE /projects/:projectId/defects/:defectId/attachments/:attachmentId (delete)
- [ ] GET /projects/:projectId/defects/:defectId/attachments (list)

---

## 🎯 Результаты

После внедрения:
- ✅ Все модалы дефектов имеют одинаковый размер и стиль
- ✅ Поле для загрузки файлов присутствует в Edit модале
- ✅ View модал полностью read-only
- ✅ Файлы отображаются согласованно во всех модалах
- ✅ Стиль загрузки файлов соответствует тест кейсам
- ✅ Компоненты переиспользуются между страницами
- ✅ Код более чистый и легче поддерживается

---

## 📚 Документация

- `DEFECT_MODAL_DESIGN.md` — Полная спецификация дизайна
- `DEFECT_MODAL_INTEGRATION.md` — Примеры интеграции и API
- `DEFECT_MODAL_MOCKUP.html` — Визуальная демонстрация
- `DEFECT_MODAL_README.md` — Быстрый старт и overview

**Все файлы находятся в корне проекта (в одной папке с этим файлом).**

---

## 🚀 Готово к использованию!

Все три компонента готовы к интеграции в приложение. Просто скопируйте их в `apps/frontend/src/components/` и начните использовать согласно документации.
