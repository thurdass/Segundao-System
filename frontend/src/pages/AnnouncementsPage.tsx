import {
  Bell,
  CircleAlert,
  Megaphone,
  Pencil,
  Pin,
  Plus,
  Trash2,
  X as CloseIcon,
} from 'lucide-react'
import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { announcementsApi } from '../api/announcements'
import { getApiErrorMessage } from '../api/client'
import { useAuth } from '../hooks/useAuth'
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
    <section className="form-card" aria-labelledby="announcement-form-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">{editingAnnouncement ? 'Atualização' : 'Novo aviso'}</p>
          <h3 id="announcement-form-title">
            {editingAnnouncement ? 'Editar aviso' : 'Publique no mural da turma'}
          </h3>
        </div>
        <button className="icon-button" type="button" aria-label="Fechar formulário" onClick={onCancel}>
          <CloseIcon size={18} aria-hidden="true" />
        </button>
      </div>

      <form className="data-form" onSubmit={onSubmit} noValidate>
        <div className="form-field form-field-wide">
          <label htmlFor="announcement-title">Título</label>
          <input
            id="announcement-title"
            type="text"
            value={form.title}
            onChange={(event) => onChange('title', event.target.value)}
            placeholder="Ex.: Mudança no horário de amanhã"
            disabled={isSubmitting}
            required
          />
        </div>
        <div className="form-field form-field-wide">
          <label htmlFor="announcement-content">Mensagem</label>
          <textarea
            id="announcement-content"
            value={form.content}
            onChange={(event) => onChange('content', event.target.value)}
            placeholder="Escreva a informação que a turma precisa saber"
            rows={5}
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
          <span>Fixar este aviso no topo <small>(disponível para administradores)</small></span>
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

export function AnnouncementsPage() {
  const { user } = useAuth()
  const [announcements, setAnnouncements] = useState<Announcement[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [isFormOpen, setFormOpen] = useState(false)
  const [editingAnnouncement, setEditingAnnouncement] = useState<Announcement | null>(null)
  const [form, setForm] = useState<AnnouncementFormState>(initialForm)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const fetchAnnouncements = useCallback(async (): Promise<Announcement[]> => {
    return announcementsApi.list()
  }, [])

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

  function updateForm(field: keyof AnnouncementFormState, value: string | boolean) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  function openCreateForm() {
    setEditingAnnouncement(null)
    setForm(initialForm)
    setFormOpen(true)
  }

  function openEditForm(announcement: Announcement) {
    setEditingAnnouncement(announcement)
    setForm({
      title: announcement.title,
      content: announcement.content,
      pinned: announcement.pinned,
    })
    setFormOpen(true)
  }

  function closeForm() {
    setFormOpen(false)
    setEditingAnnouncement(null)
    setForm(initialForm)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErrorMessage('')

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

    try {
      await announcementsApi.remove(announcement.id)
      await loadAnnouncements()
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível excluir o aviso.'))
    }
  }

  function canManage(announcement: Announcement): boolean {
    return user?.role === 'ADMIN' || user?.username === announcement.createdBy
  }

  return (
    <div className="data-page">
      <section className="page-heading">
        <div>
          <p className="eyebrow">Mural da turma</p>
          <h2>Avisos</h2>
          <p>Compartilhe informações importantes com todos os colegas da sua turma.</p>
        </div>
        <button className="primary-button page-primary-button" type="button" onClick={openCreateForm}>
          <Plus size={17} aria-hidden="true" />
          Novo aviso
        </button>
      </section>

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

      {errorMessage && (
        <div className="inline-error" role="alert">
          <CircleAlert size={18} aria-hidden="true" />
          <span>{errorMessage}</span>
        </div>
      )}

      {isLoading ? (
        <div className="data-list-skeleton" aria-label="Carregando avisos">
          <div />
          <div />
          <div />
        </div>
      ) : announcements.length === 0 ? (
        <div className="content-card empty-state page-empty-state">
          <Megaphone size={24} aria-hidden="true" />
          <div>
            <strong>Nenhum aviso publicado</strong>
            <p>As informações importantes da turma aparecerão aqui.</p>
          </div>
        </div>
      ) : (
        <section className="announcement-list" aria-label="Lista de avisos">
          {announcements.map((announcement) => {
            const managed = canManage(announcement)

            return (
              <article className={`announcement-card ${announcement.pinned ? 'is-pinned' : ''}`} key={announcement.id}>
                <div className="announcement-card-icon">
                  {announcement.pinned ? <Pin size={19} aria-hidden="true" /> : <Bell size={19} aria-hidden="true" />}
                </div>
                <div className="announcement-card-main">
                  <div className="announcement-card-topline">
                    {announcement.pinned && <span className="status-badge status-pinned">Fixado</span>}
                    <span>{formatDateTime(announcement.createdAt)}</span>
                  </div>
                  <h3>{announcement.title}</h3>
                  <p>{announcement.content}</p>
                  <small>Publicado por @{announcement.createdBy}</small>
                </div>
                {managed && (
                  <div className="row-actions announcement-actions">
                    <button className="icon-button small-icon-button" type="button" aria-label={`Editar ${announcement.title}`} onClick={() => openEditForm(announcement)}>
                      <Pencil size={16} aria-hidden="true" />
                    </button>
                    <button className="icon-button small-icon-button danger-icon" type="button" aria-label={`Excluir ${announcement.title}`} onClick={() => void handleDelete(announcement)}>
                      <Trash2 size={16} aria-hidden="true" />
                    </button>
                  </div>
                )}
              </article>
            )
          })}
        </section>
      )}
    </div>
  )
}
