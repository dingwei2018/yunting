import { createRouter, createWebHashHistory } from 'vue-router'
import TestPage from '../views/TestPage.vue'
import TextInput from '../views/TextInput.vue'
import Sentences from '../views/Sentences.vue'
import Edit from '../views/Edit.vue'

const routes = [
  {
    path: '/',
    name: 'TestPage',
    component: TestPage
  },
  {
    path: '/text-input',
    name: 'TextInput',
    component: TextInput
  },
  {
    path: '/sentences',
    name: 'Sentences',
    component: Sentences
  },
  {
    path: '/edit',
    name: 'Edit',
    component: Edit
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router

