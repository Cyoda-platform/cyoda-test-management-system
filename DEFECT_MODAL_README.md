# Unifying Defect Modal Design

## Summary

Это решение унифицирует дизайн всех модальных окон дефектов (просмотр, редактирование, создание) в приложении.

**Главные улучшения:**
1. ✅ **Одинаковый размер и стиль** для всех модальных окон (`sm:max-w-3xl`, 768px)
2. ✅ **Единая структура содержимого**: Title → Description → Classification (3-col grid) → External Link → Attachments
3. ✅ **Поле для загрузки файлов в режиме редактирования** (было отсутствующей функцией)
4. ✅ **Read-only дизайн для режима просмотра** (все поля как display-only boxes)
5. ✅ **Переиспользуемый компонент для загрузки файлов** (`DefectAttachmentsUpload`)
6. ✅ **Стиль загрузки файлов согласуется с тест кейсами** (drag & drop, дashed border)

---

## Созданные файлы

### Компоненты (React)

1. **`apps/frontend/src/components/DefectAttachmentsUpload.tsx`**
   - Reusable компонент для загрузки/отображения файлов
   - Поддерживает режимы edit и view
   - Drag & drop + click to browse
   - Список файлов с удалением (только в edit)

2. **`apps/frontend/src/components/EditDefectModal.tsx`**
   - Модальное окно редактирования дефекта
   - Полная поддержка загрузки новых файлов
   - Удаление существующих файлов
   - Валидация обязательных полей

3. **`apps/frontend/src/components/ViewDefectModal.tsx`**
   - Модальное окно просмотра дефекта
   - Все поля read-only
   - Отображение существующих файлов
   - Внешние ссылки как кликабельные элементы

### Документация

4. **`DEFECT_MODAL_DESIGN.md`**
   - Полная спецификация дизайна
   - Описание структуры модала
   - Mode-specific стили (Create, Edit, View)
   - Компоненты для обновления
   - API endpoints
   - Accessibility требования

5. **`DEFECT_MODAL_INTEGRATION.md`**
   - Как использовать новые компоненты
   - Примеры интеграции в Defects.tsx и RunExecution.tsx
   - API интеграция
   - Тестирование
   - Пошаговые инструкции миграции

6. **`DEFECT_MODAL_MOCKUP.html`**
   - Визуальная демонстрация дизайна
   - Сравнение Create, Edit, View модалов
   - Стили и компоновка

7. **`DEFECT_MODAL_README.md`** (этот файл)
   - Обзор и быстрый старт

---

## Быстрый старт

### 1. Просмотр дизайна

Откройте `DEFECT_MODAL_MOCKUP.html` в браузере, чтобы увидеть визуальное представление:
```bash
open DEFECT_MODAL_MOCKUP.html
# или
firefox DEFECT_MODAL_MOCKUP.html
```

### 2. Использование компонентов в коде

#### Edit Modal
```tsx
import EditDefectModal from '@/components/EditDefectModal';

<EditDefectModal
  open={editOpen}
  onOpenChange={setEditOpen}
  defect={defect}
  displayId="DEF-1"
  existingAttachments={attachments}
  onSave={async (defect, newFiles, removedIds) => {
    // Handle save logic
  }}
/>
```

#### View Modal
```tsx
import ViewDefectModal from '@/components/ViewDefectModal';

<ViewDefectModal
  open={viewOpen}
  onOpenChange={setViewOpen}
  defect={defect}
  displayId="DEF-1"
  existingAttachments={attachments}
  formatDate={formatDate}
/>
```

#### File Upload Component
```tsx
import DefectAttachmentsUpload from '@/components/DefectAttachmentsUpload';

<DefectAttachmentsUpload
  files={attachments}
  onFilesChange={setAttachments}
  readonly={false} // or true for view mode
/>
```

### 3. Интеграция в Defects.tsx

Детальные инструкции в `DEFECT_MODAL_INTEGRATION.md`:
- Замените inline modals на новые компоненты
- Управляйте состоянием attachments
- Вызывайте API endpoints для загрузки/удаления файлов

### 4. Интеграция в RunExecution.tsx

Используйте те же компоненты, что и в Defects.tsx.

---

## Ключевые особенности

### DefectAttachmentsUpload

**Режим Edit:**
- Drag & drop зона с дashed border
- Кнопка "click to browse"
- Список загруженных файлов
- Кнопка удаления для каждого файла
- Отображает количество файлов

**Режим View:**
- Только список файлов
- Без upload зоны
- Без кнопок удаления
- "No attachments" если пусто

### EditDefectModal

- Все поля редактируемы (как было в Create modal)
- Поддержка загрузки новых файлов
- Отслеживание удаленных файлов
- Валидация обязательных полей (title)
- Callback функция для save logic

### ViewDefectModal

- Все поля read-only
- Красивое отображение в box стиле
- Внешние ссылки как кликабельные
- Дата отображается через `formatDate` функцию
- Файлы отображаются без возможности редактирования

---

## API Integration

Компоненты ожидают следующие API endpoints:

```
POST   /projects/:projectId/defects/:defectId/attachments
GET    /projects/:projectId/defects/:defectId/attachments
DELETE /projects/:projectId/defects/:defectId/attachments/:attachmentId
PUT    /projects/:projectId/defects/:defectId
```

Примеры в `DEFECT_MODAL_INTEGRATION.md`.

---

## Стилизация и дизайн

### Переменные Tailwind

```
- Размер modal: sm:max-w-3xl (768px)
- Input height: h-9
- Border: border-input (обычно #ddd)
- Background: bg-white (inputs), bg-card (modal)
- Focus ring: focus-visible:ring-1 focus-visible:ring-ring
```

### CSS классы (в коде)

```typescript
const labelCls = 'text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest';
const inputCls = 'mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring';
const readOnlyFieldCls = 'mt-0 h-9 px-3 bg-white border border-input rounded-md text-sm text-foreground flex items-center';
const textareaCls = 'mt-0 min-h-[140px] bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring text-sm resize-none';
```

---

## Миграция существующего кода

### Шаг 1: Скопировать новые компоненты
```bash
# Файлы уже созданы в:
apps/frontend/src/components/DefectAttachmentsUpload.tsx
apps/frontend/src/components/EditDefectModal.tsx
apps/frontend/src/components/ViewDefectModal.tsx
```

### Шаг 2: Обновить Defects.tsx

Замените:
```tsx
// OLD
<Dialog open={editOpen} onOpenChange={setEditOpen}>
  <DialogContent className="sm:max-w-3xl bg-card">
    {/* inline edit modal code */}
  </DialogContent>
</Dialog>
```

На:
```tsx
// NEW
<EditDefectModal
  open={editOpen}
  onOpenChange={setEditOpen}
  defect={editTarget}
  displayId={defectDisplayIdMap[editTarget?.id]}
  existingAttachments={editAttachments}
  onSave={handleEditDefectSave}
/>
```

### Шаг 3: Обновить RunExecution.tsx
Аналогично Defects.tsx

### Шаг 4: Тестирование
Проверьте все flows:
- Create defect
- Edit defect + upload file
- Delete attachment
- View defect
- View from defects page
- View from test run page

---

## Accessibility

- ✅ Form labels связаны с inputs
- ✅ Read-only fields clearly identified
- ✅ File upload zone имеет hover states
- ✅ Delete buttons имеют aria-label
- ✅ Links в view mode имеют `rel="noopener noreferrer"`
- ✅ Color contrast соответствует WCAG AA

---

## Next Steps

1. **Примите дизайн** - посмотрите `DEFECT_MODAL_MOCKUP.html`
2. **Изучите документацию** - прочитайте `DEFECT_MODAL_DESIGN.md` и `DEFECT_MODAL_INTEGRATION.md`
3. **Интегрируйте компоненты** - обновите Defects.tsx и RunExecution.tsx
4. **Тестируйте** - выполните тестовый чек-лист из `DEFECT_MODAL_INTEGRATION.md`
5. **Деплойте** - разверните на production и мониторьте

---

## Questions & Issues

Если у вас есть вопросы по дизайну или реализации:
1. Обратитесь к `DEFECT_MODAL_DESIGN.md` для спецификации
2. Проверьте `DEFECT_MODAL_INTEGRATION.md` для примеров кода
3. Посмотрите `DEFECT_MODAL_MOCKUP.html` для визуального подтверждения

---

## Component Tree

```
EditDefectModal
├── DialogContent
│   ├── DialogHeader
│   ├── Form fields (title, description, dropdowns, etc)
│   ├── DefectAttachmentsUpload (editable)
│   └── DialogFooter (Cancel, Save)

ViewDefectModal
├── DialogContent
│   ├── DialogHeader
│   ├── Read-only fields
│   ├── DefectAttachmentsUpload (readonly)
│   └── DialogFooter (Close)

CreateDefectModal (existing, already good)
├── DialogContent
│   ├── DialogHeader
│   ├── Form fields
│   ├── DefectAttachmentsUpload or file upload zone
│   └── DialogFooter (Cancel, Create)

DefectAttachmentsUpload (reusable)
├── Upload zone (edit mode only)
├── File list (both modes)
└── Empty state (view mode only)
```

---

## File Structure After Integration

```
apps/frontend/src/
├── components/
│   ├── CreateDefectModal.tsx (existing, may refactor to use DefectAttachmentsUpload)
│   ├── EditDefectModal.tsx (NEW)
│   ├── ViewDefectModal.tsx (NEW)
│   ├── DefectAttachmentsUpload.tsx (NEW)
│   └── ... other components
├── pages/
│   ├── Defects.tsx (updated to use new modals)
│   ├── RunExecution.tsx (updated to use new modals)
│   └── ... other pages
└── ... rest of frontend
```

---

## Support & Feedback

Все компоненты готовы к использованию. При необходимости:
- Пересчитайте spacing и padding в зависимости от вашего дизайн-системы
- Обновите цвета согласно вашей палитре
- Добавьте дополнительные валидации согласно требованиям

**Удачи с внедрением! 🚀**
