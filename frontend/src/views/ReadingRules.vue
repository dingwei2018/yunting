<template>
  <div class="reading-rules-page">
    <div class="rules-container">
      <div class="page-header">
        <el-button @click="handleBack" :icon="ArrowLeft" circle />
        <h1 class="page-title">阅读规则设置</h1>
      </div>

      <el-tabs v-model="activeTab" class="rules-tabs">
        <el-tab-pane label="数字英文" name="digital">
          <div class="tab-content">
            <div class="table-header">
              <el-button type="primary" @click="handleAdd">添加</el-button>
            </div>
            <el-table :data="digitalRules" style="width: 100%">
              <el-table-column prop="text" label="文本" width="200" />
              <el-table-column prop="reading" label="读法" width="200" />
              <el-table-column label="生效范围" width="200">
                <template #default="{ row }">
                  <el-radio-group v-model="row.scope" size="small">
                    <el-radio label="全文">全文</el-radio>
                    <el-radio label="本处">本处</el-radio>
                  </el-radio-group>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button type="danger" link size="small" @click="handleDelete(row, 'digital')">
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
              <el-table-column label="生效范围" width="200">
                <template #default="{ row }">
                  <el-radio-group v-model="row.scope" size="small">
                    <el-radio label="全文">全文</el-radio>
                    <el-radio label="本处">本处</el-radio>
                  </el-radio-group>
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
              <el-table-column label="生效范围" width="200">
                <template #default="{ row }">
                  <el-radio-group v-model="row.scope" size="small">
                    <el-radio label="全文">全文</el-radio>
                    <el-radio label="本处">本处</el-radio>
                  </el-radio-group>
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
        <el-form-item label="生效范围">
          <el-radio-group v-model="newRule.scope">
            <el-radio label="全文">全文</el-radio>
            <el-radio label="本处">本处</el-radio>
          </el-radio-group>
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
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const activeTab = ref('digital')

// 示例数据
const digitalRules = ref([
  { id: 1, text: '2025', reading: '二零二五', scope: '全文' },
  { id: 2, text: 'APP', reading: '阿坡坡', scope: '全文' },
  { id: 3, text: '1234', reading: '一二三四', scope: '全文' },
  { id: 4, text: 'Android', reading: '安卓', scope: '全文' }
])

const phoneticRules = ref([])
const vocabularyRules = ref([])

const addDialogVisible = ref(false)
const newRule = ref({
  text: '',
  reading: '',
  scope: '全文'
})

const getTabLabel = (tab) => {
  const labels = {
    digital: '数字英文',
    phonetic: '音标调整',
    vocabulary: '专有词汇'
  }
  return labels[tab] || ''
}

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

const handleAdd = () => {
  newRule.value = {
    text: '',
    reading: '',
    scope: '全文'
  }
  addDialogVisible.value = true
}

const handleAddConfirm = () => {
  if (!newRule.value.text || !newRule.value.reading) {
    ElMessage.warning('请填写文本和读法')
    return
  }

  const rules = getCurrentRules()
  const newId = rules.value.length > 0 
    ? Math.max(...rules.value.map(r => r.id)) + 1 
    : 1
  
  rules.value.push({
    id: newId,
    ...newRule.value
  })

  addDialogVisible.value = false
  ElMessage.success('添加成功')
}

const handleDelete = (row, type) => {
  ElMessageBox.confirm('确定要删除这条规则吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const rules = getCurrentRules()
    const index = rules.value.findIndex(r => r.id === row.id)
    if (index !== -1) {
      rules.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  }).catch(() => {
    // 取消删除
  })
}

const handleBack = () => {
  router.back()
}

const handleConfirm = () => {
  // TODO: 保存规则到后端
  ElMessage.success('规则已保存')
  router.back()
}
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

