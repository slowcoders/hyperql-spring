
<template>
  <LessonView
      :js_code="code"
      target_table="customer">
    <template v-slot:description>
      <H5> 다중값 비교 </H5>
      <div class="details">
        비교값이 어레이 형태이면 property 값이 다중값 중 하나와 일치하는 가를 검사한다.<br>
        SLQ 로 변경 시 IN 또는 OR 표현식으로 변경된다.<br>
      </div>
    </template>
  </LessonView>
</template>

<script>
import LessonView from "@/components/LessonView";

const sample_code = `
const hql_select = AUTO;

/* 아래의 주석을 한 줄씩 번갈아 해제하면서 검색 결과의 차이를 비교해 보십시오. */
const hql_filter = {
  // "id": [1000, 1003]
  // "id !=": [1000, 1003]

  // "name": ["Darth Vader", "Luke Skywalker"]
  // "name !=": ["Darth Vader", "Luke Skywalker"]

  // "name like": [ "L%", "D%" ]
  // "name !like": [ "L%", "D%" ]
}
`

export default {
  components: {
    LessonView
  },

  data() {
    return {
      code: sample_code
    }
  }
}
</script>
