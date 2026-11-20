<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { adminApi } from '@/api'

const categories = ref([])
const tags = ref([])
const loading = ref(true)

const categoryForm = reactive({ visible: false, id: null, name: '', slug: '', description: '', sortOrder: 0 })
const tagForm = reactive({ visible: false, id: null, name: '', color: 'blue' })

const colorOptions = ['blue', 'geekblue', 'cyan', 'green', 'gold', 'orange', 'volcano', 'magenta', 'purple']

const categoryColumns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '别名', dataIndex: 'slug', key: 'slug' },
  { title: '文章数', dataIndex: 'articleCount', key: 'articleCount', width: 90, align: 'right' },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 70, align: 'right' },
  { title: '操作', key: 'action', width: 140, align: 'right' }
]

const tagColumns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '颜色', dataIndex: 'color', key: 'color', width: 120 },
  { title: '操作', key: 'action', width: 140, align: 'right' }
]

async function load() {
  loading.value = true
  try {
    const [categoryList, tagList] = await Promise.all([adminApi.categories(), adminApi.tags()])
    categories.value = categoryList
    tags.value = tagList
  } finally {
    loading.value = false
  }
}

function openCategory(record) {
  Object.assign(categoryForm, {
    visible: true,
    id: record?.id ?? null,
    name: record?.name ?? '',
    slug: record?.slug ?? '',
    description: record?.description ?? '',
    sortOrder: record?.sortOrder ?? 0
  })
}

async function submitCategory() {
  if (!categoryForm.name?.trim()) {
    message.warning('分类名称不能为空')
    return
  }
  const payload = {
    name: categoryForm.name.trim(),
    slug: categoryForm.slug?.trim() || null,
    description: categoryForm.description?.trim() || null,
    sortOrder: Number(categoryForm.sortOrder) || 0
  }
  if (categoryForm.id) {
    await adminApi.updateCategory(categoryForm.id, payload)
    message.success('分类已更新')
  } else {
    await adminApi.createCategory(payload)
    message.success('分类已创建')
  }
  categoryForm.visible = false
  load()
}

function removeCategory(record) {
  Modal.confirm({
    title: `删除分类「${record.name}」？`,
    content: '分类下若仍有文章，服务端会拒绝删除。',
    okText: '删除',
    okButtonProps: { danger: true },
    cancelText: '取消',
    async onOk() {
      await adminApi.deleteCategory(record.id)
      message.success('已删除')
      load()
    }
  })
}

function openTag(record) {
  Object.assign(tagForm, {
    visible: true,
    id: record?.id ?? null,
    name: record?.name ?? '',
    color: record?.color ?? 'blue'
  })
}

async function submitTag() {
  if (!tagForm.name?.trim()) {
    message.warning('标签名称不能为空')
    return
  }
  const payload = { name: tagForm.name.trim(), color: tagForm.color }
  if (tagForm.id) {
    await adminApi.updateTag(tagForm.id, payload)
    message.success('标签已更新')
  } else {
    await adminApi.createTag(payload)
    message.success('标签已创建')
  }
  tagForm.visible = false
  load()
}

function removeTag(record) {
  Modal.confirm({
    title: `删除标签「${record.name}」？`,
    content: '标签若被文章使用，服务端会拒绝删除。',
    okText: '删除',
    okButtonProps: { danger: true },
    cancelText: '取消',
    async onOk() {
      await adminApi.deleteTag(record.id)
      message.success('已删除')
      load()
    }
  })
}

onMounted(load)
</script>

<template>
  <div class="taxonomy-page admin-list-page">
    <div class="two-col admin-list-scroll">
      <section class="glass panel">
        <div class="panel-head">
          <h3>分类（{{ categories.length }}）</h3>
          <a-button type="primary" size="small" shape="round" @click="openCategory(null)">
            + 新建分类
          </a-button>
        </div>
        <a-table
          :columns="categoryColumns"
          :data-source="categories"
          :loading="loading"
          row-key="id"
          size="middle"
          :pagination="false"
          :scroll="{ x: 480 }"
          :locale="{ emptyText: '暂无分类' }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'slug'">
              <code class="slug">{{ record.slug }}</code>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" size="small" @click="openCategory(record)">编辑</a-button>
              <a-button type="link" size="small" danger @click="removeCategory(record)">删除</a-button>
            </template>
          </template>
        </a-table>
      </section>

      <section class="glass panel">
        <div class="panel-head">
          <h3>标签（{{ tags.length }}）</h3>
          <a-button type="primary" size="small" shape="round" @click="openTag(null)">+ 新建标签</a-button>
        </div>
        <a-table
          :columns="tagColumns"
          :data-source="tags"
          :loading="loading"
          row-key="id"
          size="middle"
          :pagination="false"
          :scroll="{ x: 320 }"
          :locale="{ emptyText: '暂无标签' }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <a-tag :color="record.color"># {{ record.name }}</a-tag>
            </template>
            <template v-else-if="column.key === 'color'">
              <span class="muted">{{ record.color }}</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" size="small" @click="openTag(record)">编辑</a-button>
              <a-button type="link" size="small" danger @click="removeTag(record)">删除</a-button>
            </template>
          </template>
        </a-table>
      </section>
    </div>

    <a-modal
      v-model:open="categoryForm.visible"
      :title="categoryForm.id ? '编辑分类' : '新建分类'"
      :confirm-loading="false"
      ok-text="保存"
      cancel-text="取消"
      @ok="submitCategory"
    >
      <a-form layout="vertical" style="margin-top: 12px">
        <a-form-item label="名称" required>
          <a-input v-model:value="categoryForm.name" :maxlength="40" placeholder="如：架构设计" />
        </a-form-item>
        <a-form-item label="别名（用于 URL，可留空自动生成）">
          <a-input v-model:value="categoryForm.slug" :maxlength="60" placeholder="architecture" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="categoryForm.description" :rows="2" :maxlength="200" />
        </a-form-item>
        <a-form-item label="排序（越小越靠前）">
          <a-input-number v-model:value="categoryForm.sortOrder" :min="0" :max="999" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="tagForm.visible"
      :title="tagForm.id ? '编辑标签' : '新建标签'"
      ok-text="保存"
      cancel-text="取消"
      @ok="submitTag"
    >
      <a-form layout="vertical" style="margin-top: 12px">
        <a-form-item label="名称" required>
          <a-input v-model:value="tagForm.name" :maxlength="30" placeholder="如：随笔" />
        </a-form-item>
        <a-form-item label="颜色">
          <a-select v-model:value="tagForm.color">
            <a-select-option v-for="color in colorOptions" :key="color" :value="color">
              <a-tag :color="color">{{ color }}</a-tag>
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.two-col {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 14px;
  align-items: stretch;
}

.panel {
  min-width: 0;
  padding: 16px 18px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.panel-head h3 {
  margin: 0;
  font-size: 14.5px;
  font-weight: 700;
}

.slug {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  padding: 1px 7px;
  border-radius: 5px;
  background: rgba(99, 102, 241, 0.1);
  color: #4338ca;
}

@media (max-width: 1000px) {
  .two-col {
    grid-template-columns: minmax(0, 1fr);
    grid-auto-rows: minmax(220px, auto);
  }
}
</style>
