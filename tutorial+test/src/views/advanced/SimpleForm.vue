<template>
  <form>
    <div>
      <div style="background-color: #F0F0F0">
        <table>
          <tr>
            <td>
              <label class="form-label">Table: </label>
            </td><td class="input-column">
              <b-form-select v-model="selectedTable"
                              :options="sampleTables"
                              @input="onTableChanged()">
              </b-form-select>
            </td>
          </tr>
        </table>
      </div>
      <br>
      <slot name="description" />

    </div>

    <!------------>
      <div id="formio" />
  </form>
</template>

<script>
// import CodeMirror from "codemirror-editor-vue3";
// import "codemirror/mode/javascript/javascript.js";
// import "codemirror/theme/dracula.css";
import "choices.js/public/assets/styles/choices.css";
import "@formio/js/dist/formio.full.css"

import { ref } from "vue";

import axios from "axios";
import { HqlApi, HqlForm } from "@/api/hqlApi";
import { Formio } from "@formio/js";

const dbSchema = 'bookstore';
const baseUrl = 'http://localhost:7007/api/hql'

const sampleStorages = [
  "bookstore",
  "bookstore_jpa",
]

const sampleTables = [
  "author",
  "customer",
  "book",
  "book_order",
]

export default {

  // components: { CodeMirror },
  data() {
    return {
      showSchemaInfo: false,
      storageNames: sampleStorages,
      sampleTables: sampleTables,
      selectedStorage: sampleStorages[0],
      selectedTable: sampleTables[0],
      schemaInfo: '',
      columns: [],
    }
  },

  mounted() {
    // this.codeView = this.$refs.codeView.cminstance;
    setTimeout(this.onTableChanged, 10);
  },

  computed: {
  },

  methods : {

    async onTableChanged() {
      const vm = this;
      const columns = formSchema[vm.selectedTable];
      await vm.setFormModel(columns);
      const model = {
        components: [ {
          type: "editgrid",
          label: vm.selectedTable,
          key: "children",
          input: false,
          components: columns, 
        }, {
            type: 'button',
            action: 'submit',
            label: 'Submit',
            theme: 'primary'
        }]
      }
      const select = columns.map(row => row.key)
      const api = new HqlApi(`${baseUrl}/bookstore/${vm.selectedTable}`);
      const res = await api.find(null, {select});
      console.log("editgrid", res)
      Formio.createForm(document.getElementById('formio'), model)
      .then(function(form) {
        form.submission = {
          data: {
            children: JSON.parse(JSON.stringify(res.content))
          }
        }
        form.on('submit', function(submission) {
          for (const row of submission.data.children) {
            const org = res.content.find((r) => r.id === row.id);
            if (!org) {
              api.insert(row);
            } else if (JSON.stringify(row) !== JSON.stringify(org)) {
              api.updateByIdList([row.id], row);
            }
          }
          // vm.$forceUpdate();
          return true;          
        });
      });
    },

    async setFormModel(columns) {
      this.columns = columns;
      for (const col of columns) {
        const ref = col.dataRef;
        if (ref) {
          const api = new HqlApi(`${baseUrl}/bookstore/${ref.table}`);
          const select = ref.value + ',' + ref.label
          const data = await api.find(ref.filter, {select});
          col.data = {
            values: data.content.map(item => ({
              value: item[ref.value],
              label: item[ref.label],
            }))}
          ;
          console.log(col.key, col.data)
        }
      }
    },

    show_http_error(err) {
      alert(err.message + "\n" + JSON.stringify(err.response, null, 4));
    },
  }
};

let book_columns = [
  HqlForm.hidden('id'),
  HqlForm.text('title', "제목"),
  HqlForm.select('author.id', "저자", {
      table: 'author',
      value: 'id',
      label: 'name'
  }),
  /*HqlForm.select('publisher.id', "출판사", {
      table: 'publisher',
      value: 'id',
      label: 'name'
  }),*/
  HqlForm.text('price', "가격"),
]

let author_columns = [
    HqlForm.number('id', "아이디"),
    HqlForm.text('name', "이름"),
    // HqlForm.text('profile.hometown', "출생지"),
    // HqlForm.text('profile.country', "국적"),
]

let customer_columns = [
    HqlForm.number('id', "아이디"),
    HqlForm.text('name', "이름"),
    HqlForm.tags('memo.favoriteGenre', '선호 장르', ['추리', '스릴러', 'SF', '로맨스', '무협', '공포', '판타지'])
]


let order_columns = [
  HqlForm.select('customer.id', "고객", {
      table: 'customer',
      value: 'id',
      label: 'name'
  }),
  HqlForm.select('book.id', "책", {
      table: 'book',
      value: 'id',
      label: 'title'
  }),
  // HqlForm.date('date', '주문일')
]

const formSchema = {
  book: book_columns,
  author: author_columns,
  book_order: order_columns,
  customer: customer_columns
};


</script>

<style>
  form {
    padding-top: 2em;
    padding-right: 2em;
    padding-bottom: 1em;
    display: grid;
    grid-template-rows: auto 1fr;
    height: 100vh;
    max-height: 100vh;
  }

  /*.test-result-view .CodeMirror {*/
  /*  overflow: auto;*/
  /*  height: 100%;*/
  /*}*/

  td > label {
    padding-top: 0.5em;
  }

  td.input-column {
    padding-right: 2em;
  }

  .details {
    padding-left: 1em;
    margin-bottom: 0.7em;
  }

  table td {
    padding: 5px
  }

</style>
