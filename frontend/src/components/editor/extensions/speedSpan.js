import { Node, mergeAttributes } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import { Fragment } from '@tiptap/pm/model'
import SpeedSpanNodeView from '../nodeviews/SpeedSpanNodeView.vue'

export const SpeedSpanNodeName = 'speedSpan'

const clampLocalSpeed = (value) => {
  const num = Number(value)
  if (Number.isNaN(num)) return 0
  if (num > 10) return 10
  if (num < -10) return -10
  return Math.round(num)
}

export default Node.create({
  name: SpeedSpanNodeName,

  inline: true,
  group: 'inline',
  content: 'inline*',
  defining: true,
  selectable: false,
  draggable: false,

  addAttributes() {
    return {
      speed: {
        default: 0,
        parseHTML: (element) => clampLocalSpeed(element.getAttribute('data-speed-span')),
        renderHTML: (attributes) => ({
          'data-speed-span': clampLocalSpeed(attributes.speed)
        })
      },
      uid: {
        default: '',
        parseHTML: (element) => element.getAttribute('data-speed-id') || '',
        renderHTML: (attributes) =>
          attributes.uid
            ? {
                'data-speed-id': attributes.uid
              }
            : {}
      }
    }
  },

  parseHTML() {
    return [{ tag: 'span[data-speed-span]' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['span', mergeAttributes(HTMLAttributes), 0]
  },

  addCommands() {
    return {
      wrapSpeedSpan:
        (options = {}) =>
        ({ state, dispatch }) => {
          const { selection, doc } = state
          if (!selection || selection.empty) return false
          const { from, to } = selection

          let overlapsExisting = false
          doc.nodesBetween(from, to, (node) => {
            if (node.type.name === this.name) {
              overlapsExisting = true
              return false
            }
            return true
          })

          if (overlapsExisting) {
            return false
          }

          const slice = selection.content()
          if (!slice || slice.size === 0) return false

          const speed = clampLocalSpeed(options.speed)
          const uid = options.uid || `speed-${Date.now()}-${Math.floor(Math.random() * 1000)}`
          const node = this.type.create(
            {
              speed,
              uid
            },
            slice.content
          )

          if (dispatch) {
            const tr = state.tr.replaceSelectionWith(node, false)
            dispatch(tr.scrollIntoView())
          }
          return true
        },
      removeSpeedSpanAt:
        (pos) =>
        ({ state, dispatch }) => {
          if (typeof pos !== 'number') return false
          const node = state.doc.nodeAt(pos)
          if (!node || node.type.name !== this.name) return false
          
          if (dispatch) {
            const from = pos
            const to = pos + node.nodeSize
            
            // 提取语速标签的内容，移除其中的段落节点
            let contentToInsert = node.content
            
            // 检查语速标签内容中是否有段落节点
            if (node.content.childCount > 0) {
              // 收集所有非段落节点的内容，以及段落节点内的内容
              const contentNodes = []
              
              node.content.forEach((child) => {
                if (child.type.name === 'paragraph') {
                  // 如果是段落节点，提取段落的内容（递归处理，因为段落内可能还有段落）
                  const extractFromParagraph = (paraNode) => {
                    paraNode.content.forEach((paraChild) => {
                      if (paraChild.type.name === 'paragraph') {
                        // 如果段落内还有段落，递归处理
                        extractFromParagraph(paraChild)
                      } else {
                        // 其他节点直接添加
                        contentNodes.push(paraChild)
                      }
                    })
                  }
                  extractFromParagraph(child)
                } else {
                  // 其他节点直接添加
                  contentNodes.push(child)
                }
              })
              
              // 如果有段落节点被处理，重新构建内容片段
              if (contentNodes.length > 0) {
                try {
                  // 使用 Fragment.from 创建内容片段
                  contentToInsert = Fragment.from(contentNodes)
                } catch (e) {
                  // 如果 Fragment.from 失败，尝试手动构建
                  try {
                    let fragment = null
                    contentNodes.forEach((contentNode) => {
                      if (fragment === null) {
                        fragment = state.schema.node('doc', null, [contentNode]).content
                      } else {
                        fragment = fragment.append(state.schema.node('doc', null, [contentNode]).content)
                      }
                    })
                    if (fragment) {
                      contentToInsert = fragment
                    }
                  } catch (e2) {
                    // 如果构建失败，使用原始内容
                    console.warn('Failed to rebuild content without paragraphs:', e2)
                  }
                }
              }
            }
            
            // 检查父节点是否是段落节点，并且段落只包含这个语速标签
            const $pos = state.doc.resolve(pos)
            const parent = $pos.parent
            
            if (parent && parent.type.name === 'paragraph') {
              const paragraphContent = parent.content
              let meaningfulChildCount = 0
              let foundSpeedSpan = false
              
              paragraphContent.forEach((child) => {
                if (child.type.name === this.name) {
                  foundSpeedSpan = true
                  meaningfulChildCount++
                } else if (child.isText && child.text.trim().length > 0) {
                  meaningfulChildCount++
                } else if (!child.isText) {
                  meaningfulChildCount++
                }
              })
              
              // 如果段落只包含这个语速标签，移除整个段落
              if (foundSpeedSpan && meaningfulChildCount === 1) {
                const paragraphFrom = $pos.start($pos.depth)
                const paragraphTo = $pos.end($pos.depth)
                const tr = state.tr.replaceRangeWith(paragraphFrom, paragraphTo, contentToInsert)
                dispatch(tr.scrollIntoView())
                return true
              }
            }
            
            // 否则只移除语速标签本身
            dispatch(state.tr.replaceRangeWith(from, to, contentToInsert).scrollIntoView())
          }
          return true
        }
    }
  },

  addNodeView() {
    return VueNodeViewRenderer(SpeedSpanNodeView)
  }
})


