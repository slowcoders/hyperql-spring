import { Layout } from "@/components/custom/layout";

export default function Home() {
  return (
    <Layout>
      <Layout.Body>
        <br/>
        <h3 className="scroll-m-20 text-4xl font-extrabold tracking-tight lg:text-5xl">Hyper Query Language(HyperQL)</h3>
        <br/>
          HyperQL 은 계층적 연결 구조를 가진 데이터의 검색을 위한 쿼리 언어이다.
        <p/>
        <br/>

        <h4>HyperQL vs GraphQL</h4>
          HyperQL 은 기능상으로 GraphQL 과 유사하나, RDB 환경에 보다 최적화된 문법을 제공한다.<br />
          HyperQL 은 <b>select</b> 문과 <b>filter</b> 문으로 구성된다. <br />
        <b>select</b> 는 GraphQL 의 query 문에 해당하며, 검색 결과를 계층적으로 구조화하기 위해 사용된다.<br />
        <b>filter</b> 는 SQL 의 where 조건절에 해당한다. Backend 코드 수정없이도 Frontend 에서 계층 구조를 가진 데이터를 선별하기 위한 Query 를 Json 기반의 단순한 문법으로 작성할 수 있다. <br />
        <p/>
        <br/>
        <h4>HyperJDBC </h4>
          Spring JDBC 를 기반으로 HyperQL을 구현한 라이브러리<br />
        <ul>
          <li>JDBC/JPA Repository 자동 생성</li>
          <li>SQL/JPQL 자동 생성 및 자동 Join 처리</li>
          <li>JSON 칼럼 검색 기능 지원</li>
          <li>Entity Manager 를 통한 JPA CacheManager 지원</li>
          <li>HyperQL Rest API Template 제공</li>
          <li>JDBC -&gt; JPA migration utility 제공</li>
        </ul>
        <br/>
        예제) bookstore.customer Table 에서 "Luke" 로 시작하는 Entity 검색하기.
        <div className="code">
          <code>
            curl -X 'POST' 'http://localhost:7007/api/hql/bookstore/customer/find' \<br />
            -H 'Content-Type: application/json' \<br />
            -d '&#123; "name@like": "Luke%" &#125;'
          </code>
        </div>
        <p/>
        <br/>
      </Layout.Body>
    </Layout>
  )
}
