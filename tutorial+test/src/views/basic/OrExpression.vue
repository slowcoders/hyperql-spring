
<template>
  <LessonView
      :js_code="code"
      target_table="customer">
    <template v-slot:description>
      <H5> 검색 조건의 Or 결합 </H5>
      <div class="details">
      * JQL 조건절이 다수인 경우, { } 내부는 And, [ ] 내부는 Or 조건으로 결합한다.<br>
          @and 또는 @or 연산자를 사용하여 결합조건을 명시할 수 있다.<br>
          동일한 프로퍼티를 비교하는 경우, 다중값 어레이 또는 @between 을 사용하여 간명하게 표현하는 것이 바람직하다.</div>
    </template>
  </LessonView>
</template>

<script>
import LessonView from "@/components/LessonView";

const sample_code = `
const hql_select = AUTO;
const min_height = 1.2
const max_height = 2.0
const min_weight = 40
const max_weight = 120

const normal_height = {
    "height@ge": min_height,
    "height@le": max_height
}

const normal_mass = {
    "mass@ge": min_weight,
    "mass@le": max_weight
}

const too_small_or_too_tall = {
    "@or": {
        "height@lt": min_height,
        "height@gt": max_height
    }
}

const too_light_or_too_heavy = {
    "@or": {
        "mass@lt": min_weight,
        "mass@gt": max_weight
    }
}

const too_small_or_too_heavy = {
    "@or": {
        "height@lt": min_height,
        "mass@gt": max_weight
    }
}

const too_small_or_too_tall__AND__too_light_or_too_heavy = {
  "@and": [
    too_small_or_too_tall,
    too_light_or_too_heavy
  ]
}

const too_small_or_too_tall__AND__too_light_or_too_heavy__short_expression = {
    "height@not between": [min_height, max_height],
    "mass@not between": [min_weight, max_weight],
}

/* 아래의 주석을 한 줄씩 번갈아 해제하면서 검색 결과의 차이를 비교해 보십시오. */
const hql_filter = too_small_or_too_tall;
// const hql_filter = too_light_or_too_heavy;
// const hql_filter = too_small_or_too_heavy;
// const hql_filter = too_small_or_too_tall__AND__too_light_or_too_heavy;
// const hql_filter = too_small_or_too_tall__AND__too_light_or_too_heavy__short_expression;

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
