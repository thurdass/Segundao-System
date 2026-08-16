import { CircleAlert, Megaphone, Pencil, Pin, Plus, Trash2, X as CloseIcon } from 'lucide-react'
import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { announcementsApi } from '../api/announcements'
import { getApiErrorMessage } from '../api/client'
import type { Announcement, AnnouncementRequest } from '../types/api'
import { formatDateTime } from '../utils/date'

interface AnnouncementFormState {
  title: string
  content: string
  pinned: boolean
}

const initialForm: AnnouncementFormState = {
  title: '',
  content: '',
  pinned: false,
}

function AnnouncementForm({
  editingAnnouncement,
  form,
  isSubmitting,
  onCancel,
  onChange,
  onSubmit,
}: {
  editingAnnouncement: Announcement | null
  form: AnnouncementFormState
  isSubmitting: boolean
  onCancel(): void
  onChange(field: keyof AnnouncementFormState, value: string | boolean): void
  onSubmit(event: FormEvent<HTMLFormElement>): void
}) {
  return (
    <section className="form-card admin-inline-form" aria-labelledby="admin-announcement-form-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">{editingAnnouncement ? 'Atualização' : 'Novo aviso'}</p>
          <h3 id="admin-announcement-form-title">
            {editingAnnouncement ? 'Editar aviso' : 'Publicar aviso para a turma'}
          </h3>
        </div>
        <button className="icon-button" type="button" aria-label="Fechar formulário" onClick={onCancel}>
          <CloseIcon size={18} aria-hidden="true" />
        </button>
      </div>
      <form className="data-form" onSubmit={onSubmit} noValidate>
        <div className="form-field form-field-wide">
          <label htmlFor="admin-announcement-title">Título</label>
          <input
            id="admin-announcement-title"
            type="text"
            value={form.title}
            onChange={(event) => onChange('title', event.target.value)}
            disabled={isSubmitting}
            required
          />
        </div>
        <div className="form-field form-field-wide">
          <label htmlFor="admin-announcement-content">Mensagem</label>
          <textarea
            id="admin-announcement-content"
            value={form.content}
            onChange={(event) => onChange('content', event.target.value)}
            rows={4}
            disabled={isSubmitting}
            required
          />
        </div>
        <label className="checkbox-field form-field-wide">
          <input
            type="checkbox"
            checked={form.pinned}
            onChange={(event) => onChange('pinned', event.target.checked)}
            disabled={isSubmitting}
          />
          <span>Fixar este aviso no topo</span>
        </label>
        <div className="form-actions form-field-wide">
          <button className="secondary-button muted-button" type="button" onClick={onCancel} disabled={isSubmitting}>
            Cancelar
          </button>
          <button className="primary-button page-primary-button" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Salvando...' : editingAnnouncement ? 'Salvar alterações' : 'Publicar aviso'}
          </button>
        </div>
      </form>
    </section>
  )
}

export function AdminAnnouncementsSection() {
  const [announcements, setAnnouncements] = useState<Announcement[]>([])
  const [editingAnnouncement, setEditingAnnouncement] = useState<Announcement | null>(null)
  const [form, setForm] = useState<AnnouncementFormState>(initialForm)
  const [isFormOpen, setFormOpen] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [noticeMessage, setNoticeMessage] = useState('')

  const fetchAnnouncements = useCallback(() => announcementsApi.list(), [])

  const loadAnnouncements = useCallback(async () => {
    setIsLoading(true)
    setErrorMessage('')

    try {
      setAnnouncements(await fetchAnnouncements())
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível carregar os avisos.'))
    } finally {
      setIsLoading(false)
    }
  }, [fetchAnnouncements])

  useEffect(() => {
    let active = true

    void fetchAnnouncements()
      .then((result) => {
        if (active) {
          setAnnouncements(result)
          setErrorMessage('')
        }
      })
      .catch((error: unknown) => {
        if (active) {
          setErrorMessage(getApiErrorMessage(error, 'Não foi possível carregar os avisos.'))
        }
      })
      .finally(() => {
        if (active) {
          setIsLoading(false)
        }
      })

    return () => {
      active = false
    }
  }, [fetchAnnouncements])

  function openCreateForm() {
    setEditingAnnouncement(null)
    setForm(initialForm)
    setNoticeMessage('')
    setFormOpen(true)
  }

  function openEditForm(announcement: Announcement) {
    setEditingAnnouncement(announcement)
    setForm({
      title: announcement.title,
      content: announcement.content,
      pinned: announcement.pinned,
    })
    setNoticeMessage('')
    setFormOpen(true)
  }

  function closeForm() {
    setFormOpen(false)
    setEditingAnnouncement(null)
    setForm(initialForm)
  }

  function updateForm(field: keyof AnnouncementFormState, value: string | boolean) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErrorMessage('')
    setNoticeMessage('')

    if (!form.title.trim() || !form.content.trim()) {
      setErrorMessage('Informe o título e a mensagem do aviso.')
      return
    }

    const payload: AnnouncementRequest = {
      title: form.title.trim(),
      content: form.content.trim(),
      pinned: form.pinned,
    }

    setIsSubmitting(true)

    try {
      if (editingAnnouncement) {
        await announcementsApi.update(editingAnnouncement.id, payload)
      } else {
        await announcementsApi.create(payload)
      }

      closeForm()
      setNoticeMessage(editingAnnouncement ? 'Aviso atualizado.' : 'Aviso publicado.')
      await loadAnnouncements()
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível salvar o aviso.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleDelete(announcement: Announcement) {
    if (!window.confirm(`Excluir o aviso “${announcement.title}”?`)) {
      return
    }

    setErrorMessage('')
    setNoticeMessage('')

    try {
      await announcementsApi.remove(announcement.id)
      setNoticeMessage('Aviso removido.')
      await loadAnnouncements()
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível remover o aviso.'))
    }
  }

  return (
    <section className="admin-management-section" aria-labelledby="admin-announcements-title">
      <div className="admin-management-heading">
        <div>
          <p className="eyebrow">Comunicação da turma</p>
          <h3 id="admin-announcements-title">Avisos</h3>
          <p>Publique, fixe, edite ou remova comunicados para os alunos.</p>
        </div>
        <button className="secondary-button" type="button" onClick={openCreateForm}>
          <Plus size={16} aria-hidden="true" />
          Novo aviso
        </button>
      </div>

      {noticeMessage && <div className="inline-success" role="status">{noticeMessage}</div>}
      {errorMessage && (
        <div className="inline-error" role="alert">
          <CircleAlert size={17} aria-hidden="true" />
          <span>{errorMessage}</span>
        </div>
      )}

      {isFormOpen && (
        <AnnouncementForm
          editingAnnouncement={editingAnnouncement}
          form={form}
          isSubmitting={isSubmitting}
          onCancel={closeForm}
          onChange={updateForm}
          onSubmit={handleSubmit}
        />
      )}

      {isLoading ? (
        <div className="data-list-skeleton" aria-label="Carregando avisos">
          <div />
          <div />
        </div>
      ) : announcements.length === 0 ? (
        <div className="content-card empty-state page-empty-state">
          <Megaphone size={22} aria-hidden="true" />
          <div>
            <strong>Nenhum aviso cadastrado</strong>
            <p>Os comunicados criados pelo administrador aparecerão aqui.</p>
          </div>
        </div>
      ) : (
        <div className="admin-announcement-list">
          {announcements.map((announcement) => (
            <article className={`admin-announcement-row ${announcement.pinned ? 'is-pinned' : ''}`} key={announcement.id}>
              <div className="admin-announcement-icon">
                {announcement.pinned ? <Pin size={17} aria-hidden="true" /> : <Megaphone size={17} aria-hidden="true" />}
              </div>
              <div className="admin-announcement-main">
                <div className="admin-announcement-title-row">
                  <strong>{announcement.title}</strong>
                  {announcement.pinned && <span className="status-badge status-pinned">Fixado</span>}
                </div>
                <p>{announcement.content}</p>
                <small>{formatDateTime(announcement.createdAt)} · @{announcement.createdBy}</small>
              </div>
              <div className="admin-announcement-actions">
                <button className="small-icon-button" type="button" aria-label={`Editar ${announcement.title}`} onClick={() => openEditForm(announcement)}>
                  <Pencil size={15} aria-hidden="true" />
                </button>
                <button className="small-icon-button danger-icon" type="button" aria-label={`Remover ${announcement.title}`} onClick={() => void handleDelete(announcement)}>
                  <Trash2 size={15} aria-hidden="true" />
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
