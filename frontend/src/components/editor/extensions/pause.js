import { Node, mergeAttributes } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import PauseNodeView from '../nodeviews/PauseNodeView.vue'

export const PauseNodeName = 'pause'

export default Node.create({
  name: PauseNodeName,

  inline: true,
  group: 'inline',
  atom: true,
  selectable: false,
  draggable: false,

  addAttributes() {
    return {}
  },

  parseHTML() {
    return [{ tag: 'span[data-pause]' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['span', mergeAttributes(HTMLAttributes, { 'data-pause': 'true' })]
  },

  addNodeView() {
    return VueNodeViewRenderer(PauseNodeView)
  }
})

