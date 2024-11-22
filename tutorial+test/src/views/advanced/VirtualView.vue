
<template>
  <LessonView
      :js_code="code"
      target_table="best_seller">
    <template v-slot:description>
      <H5> Join Query. </H5>
      <div class="details">
        JQL 서버에 VirtualView 를 추가하여 SQL View 와 같이 가상의 테이블을 생성할 수 있다.<br>
        아래는 SampleApplection 서버에서 생성한 판매실적 통계 데이터를 검색하는 예제이다.
      </div>
    </template>
  </LessonView>
</template>

<script>
import LessonView from "@/components/LessonView";

const sample_code = `
const hql_select = ['title', 'author.name', 'count'];

const hql_filter = {

  /* 특정 도서의 판매량.
  ----------------------------------------------------------*/
  // "title": "소년이 온다"

  /* 3 권 이상 판매된 도서
  ----------------------------------------------------------*/
  // "count@ge": 3
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
