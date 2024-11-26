
import { Layout } from '@/components/custom/layout'
import { Search } from '@/components/search'
import ThemeSwitch from '@/components/theme-switch'
import { UserNav } from '@/components/user-nav'
import { DataTable } from './components/data-table'
import { columns } from './components/columns'
import { tasks } from './data/tasks'


import ReactCodeEditor from '@uiw/react-codemirror';
import "codemirror/mode/javascript/javascript.js";
import "codemirror/theme/dracula.css";

import axios from "axios";
import { Select, SelectContent, SelectItem, SelectTrigger } from '@/components/ui/select'
import { SelectValue } from '@radix-ui/react-select'
import { Button } from '@/components/ui/button'
import { Outlet } from 'react-router-dom'

const dbSchema = 'bookstore';
const baseUrl = 'http://localhost:7007/api/hql'

function count_lines(code: string) {
  const lines = code.split("\n");
  return lines.length;
}

const sampleStorages = [
  "bookstore",
  "bookstore_jpa",
]

const sampleTables = [
  "customer",
  "book",
  "author",
  "book_order",
  "customer_friend_link",
  "best_seller",
]

const editOptions = {
    mode: "text/javascript", // Language mode
    theme: "dark", // Theme
    lineNumbers: true, // Show line number
    smartIndent: true, // Smart indent
    indentUnit: 4, // The smart indent unit is 2 spaces in length
    foldGutter: true, // Code folding
    styleActiveLine: true, // Display the style of the selected row
}

const viewOptions = {
    mode: "text/javascript", // Language mode
    theme: "dark", // Theme
    lineNumbers: false, // Show line number
    smartIndent: true, // Smart indent
    indentUnit: 2, // The smart indent unit is 2 spaces in length
    foldGutter: true, // Code folding
    lineWrapping: true,
    styleActiveLine: true, // Display the style of the selected row
}

type LessonProps = {
    js_code : string,
    target_table: boolean,
};

export function LessonView(props: LessonProps) {

    const vm: any = {
        showSchemaInfo: false,
        storageNames: sampleStorages,
        selectedStorage: sampleStorages[0],
        tableNames: sampleTables,
        selectedTable: props?.target_table || sampleTables[0],
        disableTableSelector: !!props?.target_table,
        schemaInfo: '',
        selectableColumns: [],
        selectedColumns: [],
        allColumnSelected: true,
        sortOptions: [],
        first_sort: '',
        columns: '*',
        limit: 0,
        sampleCode: "--", //vm.make_sample_code(),
        test_result: '',
        sortBy: null,
        cntTest: 0,
        axios: axios,

    }

    // function mounted() {
    //     vm.codeView = vm.$refs.codeView.cminstance;
    //     vm.resultView = vm.$refs.resultView.cminstance;
    //     vm.selectedTable = vm.target_table || sampleTables[0];
    //     vm.disableTableSelector = !!vm.target_table
    //     setTimeout(vm.onTableChanged, 10);
    // }
    
    function execute() {
        try {
            eval(vm.sampleCode);
        } catch (e: any) {
           vm.show_error_in_result_view("Test source compile error.\n" + e.message);
        }
    }
    
    // function to_url_param(options: any) {
    //     if (!options) return "";

    //     let params = ""
    //     for (const k in options) {
    //         params += params.length > 1 ? '&' : '?';
    //         params += k + "=" + options[k];
    //     }
    //     return params;
    // }
    
    // function make_http_param() {
    //     let param = "select=";
    //     param += (vm.selectedColumns?.length > 0) ? vm.selectedColumns : '${hql_select}'
    //     if (vm.first_sort?.length > 0) {
    //         param += '&sort=' + vm.first_sort
    //     }
    //     if (vm.limit > 0) {
    //         param += '&limit=' + vm.limit
    //     }
    //     return param;
    // }
    
    // function  make_sample_code() {
    //     return ` // JQL Sample
    //         const dbSchema = '${vm.selectedStorage}'
    //         const dbTable = '${vm.selectedTable}'
    //         const AUTO = ""
    //         ${vm.js_code}
    //         const hql_param = \`${vm.make_http_param()}\`
    //         vm.http_post(\`\${baseUrl}/\${dbSchema}/\${dbTable}/find?\${hql_param}\`, hql_filter);
    //         ${vm.schemaInfo}`
    // }
    
    // function resetColumns() {    
    //     axios.get(`${baseUrl}/metadata/${dbSchema}/${vm.selectedTable}`).
    //     then((res) => {
    //         const sortOptions = [];
    //         const selectableColumns = [
    //             { value: "*", text: "* (All internal properties)" },
    //             { value: "0", text: "0 (Primary keys)" },
    //         ];
    //         for (const column of res.data.columns) {
    //             sortOptions.push(" " + column);
    //             sortOptions.push("-" + column);
    //             selectableColumns.push({value: column, text: column })
    //         }
    //         if (res.data.references?.length > 0) {
    //             for (const column of res.data.references) {
    //                 const skey = column + ".*"
    //                 selectableColumns.push({value: skey, text: skey, is_ref: true })
    //             }
    //         }
    //         vm.sortOptions = sortOptions;
    //         vm.selectableColumns = selectableColumns;
    //         console.log(res.data);
    //     })
    // }
    
    // function show_error_in_result_view(msg: string) {
    //     vm.resultView.setValue("!!!! " + msg);
    // }
    
    // function onSelectChanged() {
    //     if (vm.allColumnSelected) {
    //         if (vm.selectedColumns.filter((k: string) => k.indexOf('*') < 0).length > 0) {
    //             vm.selectedColumns = vm.selectedColumns.filter((k: string) => k != '*');
    //             vm.allColumnSelected = false;
    //         }
    //     }
    //     else {
    //         const wasAllColumnSelected = vm.allColumnSelected;
    //         vm.allColumnSelected = vm.selectedColumns.indexOf("*") >= 0;
    //         if (!wasAllColumnSelected && vm.allColumnSelected) {
    //             vm.selectedColumns = vm.selectedColumns.filter((k: string) => k.indexOf('*') >= 0);
    //         }
    //     }
    
    //     vm.sortColumn = null;
    //     vm.codeView.setValue(vm.make_sample_code());
    //     vm.resultView.setValue("");
    //     vm.resetColumns();
    // }
    
        
    // function onTableChanged() {
    //     if (vm.showSchemaInfo) {
    //         const url = `${baseUrl}/metadata/${dbSchema}/${vm.selectedTable}/Simple`
    //         axios.get(url).then(res => {
    //             vm.schemaInfo = `\n/*************** Schema<${vm.selectedTable}> ***********************\n${res.data}*/`;
    //             vm.onSelectChanged()
    //         }).catch(vm.show_http_error)
    //     } else {
    //         vm.onSelectChanged()
    //     }
    // }
    
    // function show_http_error(err: any) {
    //     let msg = err.message + "\n" + JSON.stringify(err.response.data, null, 4);
    //     vm.show_error_in_result_view(msg);
    // }
    
    // function http_post(url: string, hql: any) {
    //     const options = {
    //         headers: { "Content-Type": `application/json`}
    //     }
    //     axios.post(url, hql, options).then(res => {
    //         vm.cntTest ++;
    //         const header = "ex " + vm.cntTest + ") result: " + res.data.content.length + "\n\n";
    //         const results = JSON.stringify(res.data.content, null, 2);
    //         const sql = res.data.metadata?.lastExecutedSql ? "\n\n---------------\nexecuted sql:\n" + res.data.metadata.lastExecutedSql : "";
    //         vm.resultView.setValue(header + results + sql);
    //     }).catch(vm.show_http_error)
    // }
        
        
    return (
    <Layout>
      {/* ===== Top Heading ===== */}
      <Layout.Header sticky>
        <Search />
        <div className='ml-auto flex items-center space-x-4'>
          <ThemeSwitch />
          <UserNav />
        </div>
      </Layout.Header>

      <Layout.Body>
  <form>
    <div>
      <div style={{backgroundColor: '#F0F0F0'}}>
        <table><tr><td>
          <label className="form-label">Storage: </label>
        </td><td className="input-column">
        <Select>
      <SelectTrigger className="w-[180px]">
        <SelectValue placeholder="Select a fruit" />
      </SelectTrigger>
      <SelectContent>
            {vm.storageNames.map((name: string) => (
                <SelectItem value={name}>{name}</SelectItem>
            ))}            
        </SelectContent>
        </Select>
        </td><td>
         <label className="form-label">Table: </label>
       </td><td className="input-column">
         {/* <b-form-select v-model="selectedTable"
                        :options="tableNames"
                        :disabled="disableTableSelector"
                        @input="onTableChanged()">
         </b-form-select> */}
        </td><td>
          <label className="form-label">Select: </label>
        </td><td className="input-column">
          {/* <b-dropdown text="Columns" ref="dropDown">
            <b-dropdown-item @click.stop="">
              <b-form-checkbox-group v-model="selectedColumns"
                                       :options="selectableColumns"
                                       stacked
                                       @change="onSelectChanged"/>
            </b-dropdown-item>
          </b-dropdown> */}
        </td><td>
          <label className="form-label">Sort: </label>
        </td><td className="input-column">
          {/* <b-form-select
                         className="form-control"
                         v-model="first_sort"
                         @input="onTableChanged()">
            <b-form-select-option :value="''" key="-1">
               Sort Options
            </b-form-select-option>
            <b-form-select-option
                v-for="(value, i) in sortOptions"
                :key="i"
                :value="value.trim()">
              { value }
            </b-form-select-option>
          </b-form-select> */}
        </td><td>
          <label className="form-label">Limit: </label>
        </td><td className="input-column">
          {/* <b-form-input v-model="limit"
                        @input="onTableChanged()"/> */}
        </td>
        </tr>
        </table>
      </div>
      <br/>
      {/* <Outlet /> */}
    </div>

    <div id="code-area">
      <div className="code" style={{position: 'relative'}}>
        <ReactCodeEditor // ref="codeView"
                    value={vm.sampleCode}
                    {...editOptions}
                    // border
                    placeholder="test placeholder"
        />

        <Button style={{position: 'absolute', top:'5px', right: '10px'}} onClick={execute}>
          run
        </Button>
      </div>
      <div className="code">
        <ReactCodeEditor //ref="resultView"
            className="test-result-view col-sm-6"
            value={vm.test_result}
            {...viewOptions}
            // border
            placeholder="test placeholder"
            aria-readonly="true"
        />
      </div>
    </div>
  </form>
  </Layout.Body>
</Layout>
)
}


