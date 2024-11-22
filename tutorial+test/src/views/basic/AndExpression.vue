
<template>
  <LessonView
      :js_code="code"
      target_table="customer">
    <template v-slot:description>
      <H5> 검색 조건의 And 결합 </H5>
      <div class="details">
        { } 내부의 비교식은 And 연산으로 자동 결합된다.<br>
      </div>
    </template>
  </LessonView>
</template>

<script>
import LessonView from "@/components/LessonView";

const sample_code = `
const hql_select = AUTO;

/* 아래의 주석을 한 줄씩 해제하면서 검색 결과의 차이를 비교해 보십시오. */
const hql_filter = {
  //  "height@gt": 1.2,
  //  "height@lt": 2.0,
  //  "mass@gt": 60,
  //  "memo.homePlanet": "Tatooine",
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
