<script setup lang="ts">
import { useInterfaceStore, Tab } from "@/stores/InterfaceStore";
import { storeToRefs } from "pinia";

const { tab } = storeToRefs(useInterfaceStore());
</script>

<template>
  <div class="menu-items left">
    <ElRadioGroup v-model="tab">
      <ElRadioButton :label="Tab.Edit">
        Build
      </ElRadioButton>
      <ElRadioButton :label="Tab.Play">
        Listen
      </ElRadioButton>
      <ElRadioButton :label="Tab.Meta">
        Meta
      </ElRadioButton>
    </ElRadioGroup>
  </div>
</template>
