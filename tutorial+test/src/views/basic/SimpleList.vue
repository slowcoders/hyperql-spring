
<template>
  <LessonView
      :js_code="code"
      target_table="customer">
    <template v-slot:description>
      <div class="details">
        <h5> Select: 검색 결과 계층적 선별 </h5>
        HQL 의 <b>select</b> 문은 검색 결과에 포함할 프로퍼티를 계층적으로 선별하기 위해 사용된다.<br>
        HQL 은 다음 2 가지로 프로퍼티 유형을 구분한다.<br>
        <ul>
          <li> Leaf Property : 숫자, 문자열, 날짜 등 단일값을 가지는 프로퍼티</li>
          <li> Reference Property : Json 객체형 {} 또는 어레이형 [] 값을 가진 프로퍼티</li>
        </ul>
        검색 결과에 포함할 프로퍼티명을 콤마로 구분하여 나열하되, Reference Property 의 경우 아래와 같이 JSONPath 또는 ( ) 를 이용하여 선택 범위를 지정할 수 있다. (아래 네 문장은 결과가 동일하다.)
        <ul>
          <li> ref.p1, ref.p2, ref.ref2.p3</li>
          <li> ref(p1, p2), ref.ref2.p3</li>
          <li> ref(p1, p2, ref2.p3)</li>
          <li> ref(p1, p2, ref2(p3))</li>
        </ul>
        프로퍼티명 대신에 아래의 Alias 를 이용하여 축약된 문법을 사용할 수 있다.<br>
        <ol>
        <li><b>*</b> (All Leaf Properties) </li>
        <li><b>0</b> (All Primary Keys) </li>
        </ol>
        Reference Property 의 하위 Property 를 지정하지 않으면, Reference Property 의 모든 Leaf Properties 가 선택된다.<br>
        즉, 'ref' 는 'ref.*' 과 같다.<br>
        참고로, select 값을 지정하지 않은 경우, filter 의 내용에 따라 검색 결과에 포함할 프로퍼티들이 자동 선택된다.<br>
      </div>
    </template>
  </LessonView>
</template>

<script>
import LessonView from "@/components/LessonView";

const sample_code = `
/* 아래의 주석을 한 줄씩 번갈아 해제하면서 검색 결과의 차이를 비교해 보십시오. */
const hql_select = "*";
// const hql_select = "0"
// const hql_select = "name, book_"
// const hql_select = "name, book_.title"
// const hql_select = "name, book_(title, price)"
// const hql_select = "0, name, friend_.name"

const hql_filter = {}
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


