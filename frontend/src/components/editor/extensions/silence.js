import { Node, mergeAttributes } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import SilenceNodeView from '../nodeviews/SilenceNodeView.vue'

export const SilenceNodeName = 'silence'

export default Node.create({
  name: SilenceNodeName,

  inline: true,
  group: 'inline',
  atom: true,
  selectable: false,
  draggable: false,

  addAttributes() {
    return {
      duration: {
        default: '0'
      }
    }
  },

  parseHTML() {
    return [
      {
        tag: 'span[data-silence]'
      }
    ]
  },

  renderHTML({ HTMLAttributes }) {
    return [
      'span',
      mergeAttributes(HTMLAttributes, {
        'data-silence': HTMLAttributes.duration || '0'
      })
    ]
  },

  addNodeView() {
    return VueNodeViewRenderer(SilenceNodeView)
  }
})


