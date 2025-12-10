import { Extension } from '@tiptap/core'
import { Plugin, PluginKey } from '@tiptap/pm/state'
import { Decoration, DecorationSet } from '@tiptap/pm/view'

export const ReadingRulesPluginKey = new PluginKey('readingRules')

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
          'reading-rule-marker',
          marker.applied ? 'reading-rule-marker--applied' : 'reading-rule-marker--pending'
        ].join(' '),
        'data-rule-id': marker.ruleId,
        'data-rule-pattern': marker.pattern,
        'data-rule-applied': marker.applied ? 'true' : 'false'
      })
    )

  return DecorationSet.create(doc, decorations)
}

export default Extension.create({
  name: 'readingRules',

  addStorage() {
    return {
      markers: []
    }
  },

  addCommands() {
    return {
      setReadingRuleMarkers:
        (markers = []) =>
        ({ tr, dispatch }) => {
          tr.setMeta(ReadingRulesPluginKey, { markers })
          if (dispatch) {
            dispatch(tr)
          }
          return true
        },
      clearReadingRuleMarkers:
        () =>
        ({ tr, dispatch }) => {
          tr.setMeta(ReadingRulesPluginKey, { markers: [] })
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
        key: ReadingRulesPluginKey,
        state: {
          init: (_, { doc }) => DecorationSet.empty,
          apply: (tr, old) => {
            const meta = tr.getMeta(ReadingRulesPluginKey)
            if (meta?.markers !== undefined) {
              this.storage.markers = meta.markers || []
              return buildDecorations(tr.doc, meta.markers || [])
            }
            if (tr.docChanged) {
              return buildDecorations(tr.doc, this.storage.markers || [])
            }
            return old
          }
        },
        props: {
          decorations: (state) => ReadingRulesPluginKey.getState(state)
        }
      })
    ]
  }
})

