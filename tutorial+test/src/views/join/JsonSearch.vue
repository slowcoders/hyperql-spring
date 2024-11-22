
<template>
  <LessonView
      :js_code="code"
      target_table="customer">
    <template v-slot:description>
      <H5> Json column search. </H5>
      <div class="details">
        Json 객체를 저장한 Column 은 Joined Entity 와 동일한 문법으로 검색한다.
      </div>
    </template>
  </LessonView>
</template>

<script>
import LessonView from "@/components/LessonView";

const sample_code = `
const hql_select = "name, memo";

/* 아래의 주석을 한 줄씩 번갈아 해제하면서 검색 결과의 차이를 비교해 보십시오. */
const hql_filter = { "memo" : { "shoeSize@ge" : 270 } };
// const hql_filter = { "memo" : { "shoeSize@le" : 260, "shoeSize@le" : 280 } };

/* 판타지와 무협 소설을 모두 좋아하는 고객 검색 */
// const hql_filter = {  "memo" : { "favoriteGenre@contains" : ["무협", "판타지"] } };

/* 판타지 또는 로맨스 소설 중 하나를 좋아하는 고객 검색 */
// const hql_filter = {  "memo" : { "favoriteGenre@intersects" : ["판타지", "로맨스"] } };
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

<style>
b.alias {
  display: inline-block;
  font-weight: bold;
  width: 2ex;
  text-align: center;
  margin-left: 2ex;
}
</style>
