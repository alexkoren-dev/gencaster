<script lang="ts" setup>
import { storeToRefs } from "pinia";
import { watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import Graph from "@/components/Graph.vue";
import Meta from "@/components/Meta.vue";
import Menu from "@/components/Menu.vue";
import NodeEditor from "@/components/NodeEditor.vue";
import { useInterfaceStore, Tab } from "@/stores/InterfaceStore";
import { useGraphSubscription } from "@/graphql";
import { ElMessage } from "element-plus";

const { showNodeEditor, selectedNodeForEditorUuid, tab } = storeToRefs(
  useInterfaceStore(),
);

const router = useRouter();
const route = useRoute();

const graphSubscription = useGraphSubscription({
  variables: {
    uuid: route.params.uuid,
  },
  pause: route.params.uuid === undefined,
});

watch(graphSubscription.error, () => {
  if (graphSubscription.error.value?.name === "CombinedError") {
    ElMessage.error("Accessed unknown graph - redirect to graph selection");
    router.push("/graph");
  }
});
</script>

<template>
  <div
    v-if="graphSubscription.data.value"
    class="edit-page"
  >
    <Menu :graph="graphSubscription.data.value.graph" />

    <Graph :graph="graphSubscription.data.value.graph" />

    <Meta
      v-if="tab === Tab.Meta"
      :graph-uuid="graphSubscription.data.value.graph.uuid"
    />

    <Transition name="slide">
      <NodeEditor
        v-if="showNodeEditor && selectedNodeForEditorUuid"
        :uuid="selectedNodeForEditorUuid"
        class="node-editor-outer"
      />
    </Transition>
  </div>
  <div v-else>
    Failed to fetch data
  </div>
</template>

<style lang="scss" scoped>
@import "@/assets/scss/variables.module.scss";

// transition
.slide-enter-active {
  transition: all 0.3s cubic-bezier(0.215, 0.61, 0.355, 1); /* easeOutCubic */
}

.slide-leave-active {
  transition: all 0.3s cubic-bezier(0.55, 0.055, 0.675, 0.19); /* easeInCubic */
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(900px);
  opacity: 1;
}

.edit-page {
  overflow-y: hidden;
}
</style>
