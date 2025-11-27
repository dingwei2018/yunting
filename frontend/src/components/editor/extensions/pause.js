import { Node, mergeAttributes } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import PauseNodeView from '../nodeviews/PauseNodeView.vue'

export const PauseNodeName = 'pause'
export const DEFAULT_PAUSE_DURATION = '0.5'

export default Node.create({
  name: PauseNodeName,

  inline: true,
  group: 'inline',
  atom: true,
  selectable: false,
  draggable: false,

  addAttributes() {
    return {
      duration: {
        default: DEFAULT_PAUSE_DURATION,
        parseHTML: (element) => element.getAttribute('data-pause') || DEFAULT_PAUSE_DURATION,
        renderHTML: (attributes) => ({
          'data-pause': attributes.duration || DEFAULT_PAUSE_DURATION
        })
      }
    }
  },

  parseHTML() {
    return [{ tag: 'span[data-pause]' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['span', mergeAttributes(HTMLAttributes)]
  },

  addNodeView() {
    return VueNodeViewRenderer(PauseNodeView)
  }
})

