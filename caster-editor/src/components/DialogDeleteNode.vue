<script setup lang="ts">
import { ref, type Ref } from "vue";
import { type Node, useDeleteNodeMutation } from "@/graphql";

export type NodeDelete = Pick<Node, "name" | "uuid">;

const emit = defineEmits<{
  (e: "deleted"): void;
  (e: "cancel"): void;
}>();

const props = defineProps<{
  node: NodeDelete;
}>();

const showDialog: Ref<boolean> = ref(true);

const deleteNodeMutation = useDeleteNodeMutation();

const deleteNode = async () => {
  const { error } = await deleteNodeMutation.executeMutation({
    nodeUuid: props.node.uuid,
  });
  if (error) {
    alert(`Failed to delete Node: ${error.message}`);
  } else {
    emit("deleted");
  }
};
</script>

<template>
  <div>
    <ElDialog
      v-model="showDialog"
      title="Careful"
      center
      lock-scroll
      :show-close="false"
      align-center
    >
      <span> Are you sure to delete Node "{{ node.name }}"? </span>
      <template #footer>
        <span class="dialog-footer">
          <ElButton
            type="info"
            @click="emit('cancel')"
          >Cancel</ElButton>
          <ElButton
            type="danger"
            @click="deleteNode()"
          > Delete Node </ElButton>
        </span>
      </template>
    </ElDialog>
  </div>
</template>
