import { createRouter, createWebHashHistory } from 'vue-router'
import TextInput from '../views/TextInput.vue'
import Sentences from '../views/Sentences.vue'
import Edit from '../views/Edit.vue'
import ReadingRules from '../views/ReadingRules.vue'

const routes = [
  {
    path: '/',
    name: 'TextInput',
    component: TextInput
  },
  {
    path: '/sentences',
    name: 'Sentences',
    component: Sentences,
    meta: { keepAlive: true }
  },
  {
    path: '/edit',
    name: 'Edit',
    component: Edit
  },
  {
    path: '/reading-rules',
    name: 'ReadingRules',
    component: ReadingRules
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router

