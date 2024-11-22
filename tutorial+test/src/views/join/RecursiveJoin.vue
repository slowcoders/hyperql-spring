
<template>
  <LessonView
      :js_code="code"
      target_table="customer">
    <template v-slot:description>
      <H5> Recursive 검색 </H5>
      <div class="details">
        Associative Table 의 두 개의 Foreign key 가 동일한 PK-Table 을 참조하는 경우, <br>
        PK-Table 명을 포함한 Foreign key 를 기본 Join key 로 자동 선정한다. <br>
        아래는 customer_friend_link table 을 통해 친구 관계를 recursive 하게 검색하는 예제이다.
      </div>
    </template>
  </LessonView>
</template>

<script>
import LessonView from "@/components/LessonView";

const sample_code = `
const hql_select = 'name, friend_.(name)';

const hql_filter = {
  /*
   Luke 의 친구 중 특정 도서를 구입한 친구를 검색한다.
  */
  "name@like": "Luke%",
  "friend_": {
      "book_" : {
//          "title" : "불사조 기사단"
          "title" : "소년이 온다"
      },
  },
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
