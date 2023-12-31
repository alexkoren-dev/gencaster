<script setup lang="ts">
import type { NodeSubscription } from "@/graphql";
import { useInterfaceStore } from "@/stores/InterfaceStore";
import { storeToRefs } from "pinia";
import { computed, ref, type Ref } from "vue";
import draggable from "vuedraggable";
import NodeEditorCell from "./NodeEditorCell.vue";

const props = defineProps<{
  scriptCells: NodeSubscription["node"]["scriptCells"];
}>();

const drag: Ref<boolean> = ref(false);
const interfaceStore = useInterfaceStore();
const { waitForNodeUpdate, newScriptCellUpdates } = storeToRefs(interfaceStore);

const updateOrder = async (
  newOrder: NodeSubscription["node"]["scriptCells"],
) => {
  // block dragging if script cell has not been updated
  console.log("Calculate new script cell order");
  waitForNodeUpdate.value = true;
  newOrder.map((x, i) => {
    let update = newScriptCellUpdates.value.get(x.uuid);
    if (update) {
      update.cellOrder = i;
    } else {
      newScriptCellUpdates.value.set(x.uuid, {
        uuid: x.uuid,
        cellOrder: i,
      });
    }
  });
  interfaceStore.executeUpdates();
};

const moveableScriptCells = computed<NodeSubscription["node"]["scriptCells"]>({
  get() {
    return props.scriptCells;
  },
  set(value) {
    updateOrder(value);
    return value;
  },
});
</script>

<template>
  <div class="blocks">
    <draggable
      v-model="moveableScriptCells"
      group="blocks"
      item-key="uuid"
      @start="drag = true"
      @end="drag = false"
    >
      <!-- hack is necessary to track v-for with v-model, see
        https://stackoverflow.com/a/71378972
      -->
      <template #item="{ element, index }">
        <div>
          <NodeEditorCell
            v-if="moveableScriptCells[index]"
            v-model:script-cell="moveableScriptCells[index]"
            v-bind="{ item: element }"
          />
          <!--  binding an empty item to prevent the linter from complaining -->
        </div>
      </template>
    </draggable>
  </div>
</template>

<style lang="scss" scoped>
@import "@/assets/scss/variables.module.scss";

.blocks {
  display: block;
  position: relative;
  padding-left: 15px;
  padding-right: 15px;

  .cell {
    position: relative;
    margin-bottom: 25px;
    border-radius: 4px;
    background-color: $grey-light;
    padding: 20px 20px 15px 30px;
    border: 1px solid $grey;

    &:has(.block-audio) {
      padding: 0;
    }

    &.dragging {
      pointer-events: none;
      cursor: grabbing !important;

      .editor-python,
      .editor-supercollider {
        .cm-scroller {
          overflow-x: hidden; // hides the scrollbar on drag
        }
      }
    }

    .cell-type {
      position: absolute;
      top: 4px;
      right: 8px;
      color: $grey-dark;
      font-style: italic;
    }

    &:hover {
      :deep(.scriptcell-tools) {
        opacity: 1;
      }
    }

    :deep(.scriptcell-tools) {
      display: flex;
      align-items: center;
      background-color: $grey-light;
      position: absolute;
      bottom: -12px;
      right: 8px;
      height: 32px;
      border-radius: 4px;
      border: 1px solid $grey;
      opacity: 0;
      transition: opacity 0.2s ease-in-out;
      z-index: 1;

      .divider {
        height: 32px;
        width: 1px;
        background-color: $grey;
      }

      .celltype {
        height: 32px;
        display: flex;
        justify-content: center;
        align-items: center;

        p {
          margin: 0;
          margin-left: 12px;
          margin-right: 12px;
          text-transform: lowercase;

          &::first-letter {
            text-transform: uppercase;
          }
        }
      }

      .icon {
        height: 32px;
        width: 32px;
        display: flex;
        justify-content: center;
        align-items: center;
        cursor: pointer;

        &:hover {
          background-color: $grey;
        }

        img {
          height: 20px;
          width: 20px;
        }
      }
    }
  }
}
</style>
