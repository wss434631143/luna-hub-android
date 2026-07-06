import { useMemo, useRef, useState } from 'react'
import {
  Check,
  ChevronLeft,
  ChevronRight,
  Download,
  Filter,
  Image as ImageIcon,
  MoreHorizontal,
  Play,
  PlugZap,
  Search,
  Share2,
  SlidersHorizontal,
  Video,
  Wifi,
  X,
} from 'lucide-react'
import './App.css'

type MediaKind = 'image' | 'video'

interface MediaItem {
  id: string
  kind: MediaKind
  title: string
  time: string
  size: string
  src: string
  poster?: string
  duration?: string
  accent: string
}

const mediaItems: MediaItem[] = [
  {
    id: 'luna-001',
    kind: 'image',
    title: '晨间湖岸',
    time: '08:12',
    size: '5.8 MB',
    src: 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80',
    accent: '#8c9f76',
  },
  {
    id: 'luna-002',
    kind: 'video',
    title: '骑行穿桥',
    time: '08:31',
    size: '148 MB',
    src: 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4',
    poster: 'https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80',
    duration: '00:42',
    accent: '#ad6f54',
  },
  {
    id: 'luna-003',
    kind: 'image',
    title: '街角黄昏',
    time: '09:05',
    size: '6.2 MB',
    src: 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?auto=format&fit=crop&w=1200&q=80',
    accent: '#b78b43',
  },
  {
    id: 'luna-004',
    kind: 'image',
    title: '山路云影',
    time: '09:18',
    size: '7.4 MB',
    src: 'https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80',
    accent: '#596f8f',
  },
  {
    id: 'luna-005',
    kind: 'video',
    title: '海边慢镜',
    time: '10:09',
    size: '224 MB',
    src: 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4',
    poster: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80',
    duration: '01:18',
    accent: '#508aa8',
  },
  {
    id: 'luna-006',
    kind: 'image',
    title: '窗边静物',
    time: '11:27',
    size: '4.1 MB',
    src: 'https://images.unsplash.com/photo-1494438639946-1ebd1d20bf85?auto=format&fit=crop&w=1200&q=80',
    accent: '#966d5f',
  },
  {
    id: 'luna-007',
    kind: 'image',
    title: '露营夜色',
    time: '12:02',
    size: '9.5 MB',
    src: 'https://images.unsplash.com/photo-1478131143081-80f7f84ca84d?auto=format&fit=crop&w=1200&q=80',
    accent: '#4b5c77',
  },
  {
    id: 'luna-008',
    kind: 'video',
    title: '城市掠影',
    time: '12:44',
    size: '310 MB',
    src: 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4',
    poster: 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80',
    duration: '02:05',
    accent: '#7e6959',
  },
  {
    id: 'luna-009',
    kind: 'image',
    title: '草地日光',
    time: '13:16',
    size: '6.9 MB',
    src: 'https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=1200&q=80',
    accent: '#6c8a62',
  },
]

function formatSelected(count: number): string {
  return count > 0 ? `${count} 项` : '媒体库'
}

export default function App() {
  const [filter, setFilter] = useState<'all' | MediaKind>('all')
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())
  const [previewId, setPreviewId] = useState<string | null>(null)
  const [dragSelecting, setDragSelecting] = useState(false)
  const longPressTimer = useRef<number | null>(null)
  const swipeStart = useRef<{ x: number; y: number } | null>(null)

  const visibleItems = useMemo(
    () => mediaItems.filter((item) => filter === 'all' || item.kind === filter),
    [filter],
  )
  const selectedCount = selectedIds.size
  const previewIndex = previewId ? visibleItems.findIndex((item) => item.id === previewId) : -1
  const previewItem = previewIndex >= 0 ? visibleItems[previewIndex] : null

  function updateSelected(id: string, force = true): void {
    setSelectedIds((current) => {
      const next = new Set(current)
      if (force) next.add(id)
      else if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  function clearLongPress(): void {
    if (longPressTimer.current != null) {
      window.clearTimeout(longPressTimer.current)
      longPressTimer.current = null
    }
  }

  function beginCardPress(id: string): void {
    clearLongPress()
    longPressTimer.current = window.setTimeout(() => {
      setDragSelecting(true)
      updateSelected(id)
      if (navigator.vibrate) navigator.vibrate(18)
    }, 260)
  }

  function finishPointer(): void {
    clearLongPress()
    setDragSelecting(false)
  }

  function selectCardUnderPoint(clientX: number, clientY: number): void {
    const element = document.elementFromPoint(clientX, clientY)?.closest<HTMLElement>('[data-media-id]')
    const id = element?.dataset.mediaId
    if (id) updateSelected(id)
  }

  function openItem(id: string): void {
    if (selectedIds.size > 0) {
      updateSelected(id, false)
      return
    }
    setPreviewId(id)
  }

  function shiftPreview(delta: number): void {
    if (previewIndex < 0) return
    const nextIndex = Math.min(Math.max(previewIndex + delta, 0), visibleItems.length - 1)
    setPreviewId(visibleItems[nextIndex]?.id ?? null)
  }

  function handlePreviewPointerDown(clientX: number, clientY: number): void {
    swipeStart.current = { x: clientX, y: clientY }
  }

  function handlePreviewPointerUp(clientX: number, clientY: number): void {
    const start = swipeStart.current
    swipeStart.current = null
    if (!start) return
    const dx = clientX - start.x
    const dy = clientY - start.y
    if (Math.abs(dx) < 48 || Math.abs(dx) < Math.abs(dy) * 1.25) return
    shiftPreview(dx < 0 ? 1 : -1)
  }

  return (
    <main className="app-shell">
      <header className="top-bar">
        <div>
          <p className="eyebrow">Luna Mobile</p>
          <h1>{formatSelected(selectedCount)}</h1>
        </div>
        <div className="status-pill">
          <Wifi size={15} />
          <span>Luna Ultra</span>
        </div>
      </header>

      <section className="device-strip" aria-label="设备状态">
        <div className="device-orbit">
          <PlugZap size={20} />
        </div>
        <div>
          <strong>已连接</strong>
          <span>DCIM / Camera01 · 9 个文件</span>
        </div>
        <button type="button" className="icon-button" aria-label="更多设备操作">
          <MoreHorizontal size={18} />
        </button>
      </section>

      <nav className="tool-row" aria-label="媒体筛选">
        <button className={filter === 'all' ? 'active' : ''} onClick={() => setFilter('all')} type="button">
          <Filter size={16} />
          全部
        </button>
        <button className={filter === 'image' ? 'active' : ''} onClick={() => setFilter('image')} type="button">
          <ImageIcon size={16} />
          图片
        </button>
        <button className={filter === 'video' ? 'active' : ''} onClick={() => setFilter('video')} type="button">
          <Video size={16} />
          视频
        </button>
        <button className="square-tool" type="button" aria-label="搜索">
          <Search size={17} />
        </button>
        <button className="square-tool" type="button" aria-label="筛选设置">
          <SlidersHorizontal size={17} />
        </button>
      </nav>

      <section
        className="media-grid"
        onPointerMove={(event) => {
          if (dragSelecting) selectCardUnderPoint(event.clientX, event.clientY)
        }}
        onPointerUp={finishPointer}
        onPointerCancel={finishPointer}
      >
        {visibleItems.map((item, index) => {
          const selected = selectedIds.has(item.id)
          return (
            <article
              className={`media-card ${item.kind} ${selected ? 'selected' : ''} ${index % 5 === 0 ? 'tall' : ''}`}
              data-media-id={item.id}
              key={item.id}
              onClick={() => openItem(item.id)}
              onPointerDown={() => beginCardPress(item.id)}
              onPointerUp={finishPointer}
              style={{ '--accent': item.accent } as React.CSSProperties}
            >
              <img src={item.poster ?? item.src} alt={item.title} draggable={false} />
              <div className="media-scrim" />
              {item.kind === 'video' && (
                <div className="video-badge">
                  <Play size={12} fill="currentColor" />
                  {item.duration}
                </div>
              )}
              <button
                className="select-dot"
                aria-label={selected ? '取消选择' : '选择'}
                onClick={(event) => {
                  event.stopPropagation()
                  updateSelected(item.id, false)
                }}
                type="button"
              >
                {selected && <Check size={13} />}
              </button>
              <footer>
                <strong>{item.title}</strong>
                <span>{item.time} · {item.size}</span>
              </footer>
            </article>
          )
        })}
      </section>

      <div className={`selection-bar ${selectedCount > 0 ? 'visible' : ''}`}>
        <button type="button" className="icon-button" aria-label="清除选择" onClick={() => setSelectedIds(new Set())}>
          <X size={18} />
        </button>
        <strong>{selectedCount} 个已选</strong>
        <div className="selection-actions">
          <button type="button">
            <Share2 size={16} />
            分享
          </button>
          <button type="button" className="primary">
            <Download size={16} />
            保存
          </button>
        </div>
      </div>

      {previewItem && (
        <section
          className="preview-layer"
          onPointerDown={(event) => handlePreviewPointerDown(event.clientX, event.clientY)}
          onPointerUp={(event) => handlePreviewPointerUp(event.clientX, event.clientY)}
        >
          <header className="preview-top">
            <button type="button" className="icon-button light" onClick={() => setPreviewId(null)} aria-label="关闭预览">
              <X size={20} />
            </button>
            <div>
              <strong>{previewItem.title}</strong>
              <span>{previewIndex + 1}/{visibleItems.length}</span>
            </div>
            <button type="button" className="icon-button light" aria-label="保存">
              <Download size={19} />
            </button>
          </header>
          <div className="preview-stage">
            {previewItem.kind === 'video' ? (
              <video controls playsInline poster={previewItem.poster} src={previewItem.src} />
            ) : (
              <img src={previewItem.src} alt={previewItem.title} />
            )}
          </div>
          <footer className="preview-bottom">
            <button type="button" className="icon-button light" onClick={() => shiftPreview(-1)} disabled={previewIndex <= 0} aria-label="上一项">
              <ChevronLeft size={22} />
            </button>
            <div className="preview-track">
              {visibleItems.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className={item.id === previewItem.id ? 'active' : ''}
                  onClick={() => setPreviewId(item.id)}
                  aria-label={item.title}
                >
                  <img src={item.poster ?? item.src} alt="" />
                </button>
              ))}
            </div>
            <button type="button" className="icon-button light" onClick={() => shiftPreview(1)} disabled={previewIndex >= visibleItems.length - 1} aria-label="下一项">
              <ChevronRight size={22} />
            </button>
          </footer>
        </section>
      )}
    </main>
  )
}
