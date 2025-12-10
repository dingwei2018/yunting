<template>
  <div class="reading-rules-page">
    <div class="rules-container">
      <div class="page-header">
        <el-button @click="handleBack" :icon="ArrowLeft" circle />
        <h1 class="page-title">阅读规则设置</h1>
      </div>

      <el-tabs v-model="activeTab" class="rules-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="数字英文" name="digital">
          <div class="tab-content">
            <div class="table-header">
              <el-button type="primary" @click="handleAdd">添加</el-button>
            </div>
            <el-table v-loading="loading" :data="digitalRules" style="width: 100%">
              <el-table-column prop="pattern" label="文本" width="200" />
              <el-table-column prop="ruleValue" label="读法" width="200" />
              <el-table-column label="全局开关" width="150">
                <template #default="{ row }">
                  <el-switch
                    v-model="row.status"
                    :active-value="1"
                    :inactive-value="0"
                    @change="handleStatusChange(row)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button type="danger" link size="small" @click="handleDelete(row, activeTab)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane label="音标调整" name="phonetic">
          <div class="tab-content">
            <div class="table-header">
              <el-button type="primary" @click="handleAdd">添加</el-button>
            </div>
            <el-table :data="phoneticRules" style="width: 100%">
              <el-table-column prop="text" label="文本" width="200" />
              <el-table-column prop="reading" label="读法" width="200" />
              <el-table-column label="全局开关" width="150">
                <template #default="{ row }">
                  <el-switch
                    v-model="row.status"
                    :active-value="1"
                    :inactive-value="0"
                    @change="handleStatusChange(row)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button type="danger" link size="small" @click="handleDelete(row, 'phonetic')">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane label="专有词汇" name="vocabulary">
          <div class="tab-content">
            <div class="table-header">
              <el-button type="primary" @click="handleAdd">添加</el-button>
            </div>
            <el-table :data="vocabularyRules" style="width: 100%">
              <el-table-column prop="text" label="文本" width="200" />
              <el-table-column prop="reading" label="读法" width="200" />
              <el-table-column label="全局开关" width="150">
                <template #default="{ row }">
                  <el-switch
                    v-model="row.status"
                    :active-value="1"
                    :inactive-value="0"
                    @change="handleStatusChange(row)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button type="danger" link size="small" @click="handleDelete(row, 'vocabulary')">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>

      <div class="footer-actions">
        <el-button type="primary" size="large" @click="handleConfirm">确定</el-button>
      </div>
    </div>

    <!-- 添加规则对话框 -->
    <el-dialog
      v-model="addDialogVisible"
      :title="`添加${getTabLabel(activeTab)}规则`"
      width="500px"
    >
      <el-form :model="newRule" label-width="80px">
        <el-form-item label="文本">
          <el-input v-model="newRule.text" placeholder="请输入文本" />
        </el-form-item>
        <el-form-item label="读法">
          <el-input v-model="newRule.reading" placeholder="请输入读法" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddConfirm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  getReadingRuleList,
  createReadingRule,
  setGlobalSetting
} from '@/api/readingRule'

const route = useRoute()
const router = useRouter()

const activeTab = ref('digital')
const loading = ref(false)

// 从路由参数获取 taskId
const taskId = computed(() => {
  return route.query.taskId ? parseInt(route.query.taskId) : null
})

// 规则类型映射（前端分类 -> 后端 ruleType）
const ruleTypeMap = {
  digital: 'SAY_AS', // 数字英文
  phonetic: 'PHONETIC_SYMBOL', // 音标调整
  vocabulary: 'ALIAS' // 专有词汇
}

const digitalRules = ref([])
const phoneticRules = ref([])
const vocabularyRules = ref([])

// 分页相关
const pagination = ref({
  digital: { page: 1, pageSize: 20, total: 0 },
  phonetic: { page: 1, pageSize: 20, total: 0 },
  vocabulary: { page: 1, pageSize: 20, total: 0 }
})

const addDialogVisible = ref(false)
const newRule = ref({
  text: '',
  reading: '',
  ruleType: ''
})

const getTabLabel = (tab) => {
  const labels = {
    digital: '数字英文',
    phonetic: '音标调整',
    vocabulary: '专有词汇'
  }
  return labels[tab] || ''
}

// 获取当前标签页对应的规则列表
const getCurrentRules = () => {
  switch (activeTab.value) {
    case 'digital':
      return digitalRules
    case 'phonetic':
      return phoneticRules
    case 'vocabulary':
      return vocabularyRules
    default:
      return digitalRules
  }
}

// 获取当前标签页的分页信息
const getCurrentPagination = () => {
  return pagination.value[activeTab.value]
}

// 加载阅读规范列表
const loadReadingRules = async () => {
  if (!taskId.value) {
    console.warn('缺少任务ID，无法加载阅读规范列表')
    return
  }

  loading.value = true
  try {
    const ruleType = ruleTypeMap[activeTab.value]
    const currentPagination = getCurrentPagination()
    
    const response = await getReadingRuleList({
      task_id: taskId.value,
      ruleType: ruleType,
      page: currentPagination.page,
      pageSize: currentPagination.pageSize
    })

    if (response && response.readingRuleList) {
      const rules = response.readingRuleList.map(rule => ({
        ruleId: rule.ruleId,
        pattern: rule.pattern,
        ruleType: rule.ruleType,
        ruleValue: rule.ruleValue,
        status: rule.status || 0 // 0-关闭，1-打开
      }))

      const currentRules = getCurrentRules()
      currentRules.value = rules
      
      if (response.total !== undefined) {
        currentPagination.total = response.total
      }
    }
  } catch (error) {
    console.error('加载阅读规范列表失败:', error)
    ElMessage.error('加载阅读规范列表失败')
  } finally {
    loading.value = false
  }
}

// 切换标签页时加载数据
const handleTabChange = () => {
  loadReadingRules()
}

// 处理全局开关变化
const handleStatusChange = async (row) => {
  if (!taskId.value) {
    ElMessage.warning('缺少任务ID')
    row.status = row.status === 1 ? 0 : 1 // 恢复原状态
    return
  }

  const statusText = row.status === 1 ? '打开' : '关闭'
  try {
    await ElMessageBox.confirm(
      `确定要${statusText}这条阅读规范吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await setGlobalSetting({
      taskId: taskId.value,
      ruleId: row.ruleId,
      status: row.status
    })

    ElMessage.success(`${statusText}成功`)
  } catch (error) {
    if (error === 'cancel') {
      // 用户取消，恢复原状态
      row.status = row.status === 1 ? 0 : 1
    } else {
      console.error('设置全局开关失败:', error)
      ElMessage.error('设置失败，请重试')
      // 恢复原状态
      row.status = row.status === 1 ? 0 : 1
    }
  }
}

const handleAdd = () => {
  newRule.value = {
    text: '',
    reading: '',
    ruleType: ruleTypeMap[activeTab.value]
  }
  addDialogVisible.value = true
}

const handleAddConfirm = async () => {
  if (!newRule.value.text || !newRule.value.reading) {
    ElMessage.warning('请填写文本和读法')
    return
  }

  try {
    await createReadingRule({
      pattern: newRule.value.text,
      ruleType: newRule.value.ruleType,
      ruleValue: newRule.value.reading
  })

  addDialogVisible.value = false
  ElMessage.success('添加成功')
    // 重新加载列表
    await loadReadingRules()
  } catch (error) {
    console.error('创建阅读规范失败:', error)
    ElMessage.error('添加失败，请重试')
  }
}

const handleDelete = (row, type) => {
  ElMessageBox.confirm('确定要删除这条规则吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    // TODO: 实现删除接口（如果API文档中有删除接口）
    ElMessage.warning('删除功能待实现')
    // const rules = getCurrentRules()
    // const index = rules.value.findIndex(r => r.ruleId === row.ruleId)
    // if (index !== -1) {
    //   rules.value.splice(index, 1)
    //   ElMessage.success('删除成功')
    // }
  }).catch(() => {
    // 取消删除
  })
}

const handleBack = () => {
  router.back()
}

const handleConfirm = () => {
  // 阅读规范已实时保存，这里直接返回
  router.back()
}

// 页面加载时获取数据
onMounted(() => {
  if (taskId.value) {
    loadReadingRules()
  }
})
</script>

<style scoped>
.reading-rules-page {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.rules-container {
  max-width: 1200px;
  margin: 0 auto;
  background: #fff;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.rules-tabs {
  margin-bottom: 30px;
}

.tab-content {
  padding: 20px 0;
}

.table-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.footer-actions {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}

.footer-actions .el-button {
  min-width: 120px;
}
</style>

