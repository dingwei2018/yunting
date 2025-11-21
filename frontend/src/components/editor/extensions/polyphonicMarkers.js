import { Extension } from '@tiptap/core'
import { Plugin, PluginKey } from '@tiptap/pm/state'
import { Decoration, DecorationSet } from '@tiptap/pm/view'

export const PolyphonicPluginKey = new PluginKey('polyphonicMarkers')

const buildDecorations = (doc, markers = []) => {
  if (!markers?.length) {
    return DecorationSet.empty
  }

  const decorations = markers
    .filter(
      (marker) =>
        typeof marker.from === 'number' &&
        typeof marker.to === 'number' &&
        marker.from < marker.to
    )
    .map((marker) =>
      Decoration.inline(marker.from, marker.to, {
        class: [
          'polyphonic-marker',
          marker.selected ? 'polyphonic-marker--resolved' : 'polyphonic-marker--pending'
        ].join(' '),
        'data-poly-id': marker.id,
        'data-poly-status': marker.selected ? 'resolved' : 'pending'
      })
    )

  return DecorationSet.create(doc, decorations)
}

export default Extension.create({
  name: 'polyphonicMarkers',

  addStorage() {
    return {
      markers: []
    }
  },

  addCommands() {
    return {
      setPolyphonicMarkers:
        (markers = []) =>
        ({ tr, dispatch }) => {
          tr.setMeta(PolyphonicPluginKey, { markers })
          if (dispatch) {
            dispatch(tr)
          }
          return true
        }
    }
  },

  addProseMirrorPlugins() {
    return [
      new Plugin({
        key: PolyphonicPluginKey,
        state: {
          init: (_, { doc }) => DecorationSet.empty,
          apply: (tr, old) => {
            const meta = tr.getMeta(PolyphonicPluginKey)
            if (meta?.markers) {
              this.storage.markers = meta.markers
              return buildDecorations(tr.doc, meta.markers)
            }
            if (tr.docChanged) {
              return buildDecorations(tr.doc, this.storage.markers || [])
            }
            return old
          }
        },
        props: {
          decorations: (state) => PolyphonicPluginKey.getState(state)
        }
      })
    ]
  }
})


