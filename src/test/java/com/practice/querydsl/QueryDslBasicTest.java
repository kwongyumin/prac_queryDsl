package com.practice.querydsl;


import com.practice.querydsl.dto.MemberDto;
import com.practice.querydsl.dto.QMemberDto;
import com.practice.querydsl.dto.UserDto;
import com.practice.querydsl.model.Member;
import com.practice.querydsl.model.QMember;
import com.practice.querydsl.model.QTeam;
import com.practice.querydsl.model.Team;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.practice.querydsl.model.QMember.member;
import static com.practice.querydsl.model.QTeam.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.setMaxElementsForPrinting;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }
    @Test
    public void startJPQL() {
        //member1을 찾아라.
        String qlString =
                "select m from Member m " +
                        "where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    @Test
    public void startQuerydsl() {
        //컴파일 시점에서 오류를 잡아낼 수 있다.
        QMember m = member; // QMember member = QMember.member  //static 지정하여 member로 간단히 표현가능 (권장)
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) //파라미터 바인딩 처리
                .fetchOne();

        assert findMember != null;
        assertThat(findMember.getUsername()).isEqualTo("member1");

        /*
        동시성 문제  =  JPAQueryFactory 를 생성할 때 제공하는
        EntityManager(em) 에 달려있다.
        스프링은 여러 쓰레드에서 동시에 같은 EntityManager 에 접근해도 ,
        트랜젝션 마다 별도의 영속성 컨텍스트 를 제공하기 때문에 ,
        동시성 문제는 걱정하지 않아도 된다 .


         */

    }

    @Test // 검색
    public void search(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();


        assert findMember != null;
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test // 검색
    public void searchAndParam(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.between(10,30))  //여러개를 ', ' 나누어도 and와 같음 , 동적쿼리 생성 시 용이함.
                .fetchOne();

        assert findMember != null;
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void resultFetch(){
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch(); // 리스트반환
//
//        Member fetchOne = queryFactory
//                .selectFrom(QMember.member)
//                .fetchOne(); // 단건 조회 (2건 이상이면 notUniqueException 발생 )
//
//        Member fetchFirst = queryFactory
//                .selectFrom(QMember.member)
//                .fetchFirst(); // 처음 한 건 조회
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults(); // QueryResults 페이징에서 사용
        results.getTotal();
        List<Member> content = results.getResults();


        long total = queryFactory  //전체 갯수 만 조회
                .selectFrom(member)
                .fetchCount();
    }

    /*
        회원 정렬 순서
        1. 회원 나이 내림차순(desc)
        2. 회원 이름 올림차순(asc)
        2 에서 회원 이름 없을 시 , 마지막에 출력
     */

    @Test
    public void sort(){
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(
                        member.age.eq(100)
                )
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }

    @Test // 페이징 처리
    public void paging1(){
        List<Member> results = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 시작 인덱스
                .limit(2) // 호출할 갯수
                .fetch();

        assertThat(results.size()).isEqualTo(2);

    }

    @Test
    public void paging2(){
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4); // 전체 갯수
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);


    }

    @Test
    public void aggregation(){
        List<Tuple> result = queryFactory
                .select( // 결과값이 튜플로 나온다.
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);


    }
    /*
        팀의 이름과 각 팀의 평균연령을 구하자.

     */

    @Test
    public void group() throws Exception{
        List<Tuple> results = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team) // 멤버의 팀과 팀을 조인
                .groupBy(team.name) //팀의 이름으로 그룹핑
                .fetch();

        Tuple teamA = results.get(0);
        Tuple teamB = results.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /*
        팀 A에 소속된 모든 회원
     */

    @Test
    public void join(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team) //innerJoin ,leftJoin, rightJoin
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1","member2");

    }

    /*
        세타 조인 - 연관관계가 없는 테이블간 조인
        회원의 이름이 팀 이름과 같은 회원 조회
     */

    @Test
    public void theta_join(){

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory //모든 회원과 팀 의 테이블을 가져와 조인 시켜 값을 조회하는 방법 , DB가 성능 최적화를 한다.
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA","teamB");
    }


    /*
        회원과 팀을 조인하면서 , 팀 이름이 TeamA인 팀만 조인 , 회원은 모두 조회
        JPQL : select m , t from Member m left join m.team t on t.name = 'teamA'

     */

    @Test
    public void join_on_filtering(){
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA")) // innerJoin 일 경우 ,on절의 조건을 where 절에 사용할 수도 있다.
                .fetch();

        for(Tuple tuple : result){
            System.out.println("tuple ="+tuple);
        }

    }
    /*
        연관관계 없는 엔티티 외부 조인
        회원의 이름이 팀 이름과 같은 대상 외부 조인

     */

    @Test
    public void join_on_no_relation(){

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory //모든 회원과 팀 의 테이블을 가져와 조인 시켜 값을 조회하는 방법 , DB가 성능 최적화를 한다.
                .select(member,team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name)) // member.team 이 아닌 member를 바로 조인 시킨다.
                .fetch();

        for(Tuple tuple : result){
            System.out.println("tuple ="+tuple);
        }

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA","teamB");




    }
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo(){
        em.flush(); // 영속 컨텍스트에 남아있는 쿼리를 날린다.
        em.clear(); // 내부 저장소에 캐시를 지운다 .

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();


        assert findMember != null;
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); // 초기화가 된 엔티티인가를 구분 해주는 기능,

        assertThat(loaded).as("패치조인 미적용").isFalse(); // 패치조인이 적용이 안되었을경우 loaded 는 false 값이다.

    }

    @Test
    public void fetchJoinUse(){
        em.flush(); // 영속 컨텍스트에 남아있는 쿼리를 날린다.
        em.clear(); // 내부 저장소에 캐시를 지운다 .

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin() //멤버 조회시 연관된 팀 엔티티도 한번에 조회함. left , inner 상관 없이 fetchJoin 을 적용가능.
                .where(member.username.eq("member1"))
                .fetchOne();


        assert findMember != null;
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); // 초기화가 된 엔티티인가를 구분 해주는 기능,

        assertThat(loaded).as("패치조인 적용").isTrue(); // 패치조인이 적용이 안되었을경우 loaded 는 false 값이다.

    }
    /*
        나이가 가장 많은 회원 조회
     */

    @Test
    public void subQuery(){
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }
    /*
        나이가 평균 이상인 회원
     */

    @Test
    public void subQueryGoe(){
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30,40);
    }


    @Test
    public void subQueryIn(){
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20,30,40);
    }

    @Test
    public void selectSubQuery(){

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();


        for(Tuple tuple : result){
            System.out.println("tuple = "+ tuple);
        }


    }

    @Test
    public void basicCase(){
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for(String s : result){
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void constant(){
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }
    @Test
    public void concat(){

        //username_age
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }


    //프로젝션 (select 대상 지정 ) 결과 반환 - 기본
    @Test
    public void simpleProjection(){

        List<String> result = queryFactory // 프로젝션 대상이 하나 이기에 타입을 명확하게 지정
                .select(member.username)
                .from(member)
                .fetch();

        for(String s : result){
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection(){
        List<Tuple> result = queryFactory // 대상이 둘이기에 tuple 사용 or dto로 조회 가능
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username=" + username);
            System.out.println("age=" + age);
        }
    }

    @Test // 순수 JPA 에서는 NEW 명령어 와 패키지 경로를 다 적어줘야하기 때문에 지저분하다..
    public void findDtoByJPQL(){
        List<MemberDto> result = em.createQuery("select new com.practice.querydsl.dto.MemberDto(m.username,m.age) from Member m", MemberDto.class)
                .getResultList();

        for(MemberDto memberDto : result){
            System.out.println("memberDto =" + memberDto);
        }

    }

    @Test // 세터를 통한 방법
    public void findDtoBySetter(){
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for(MemberDto memberDto : result){
            System.out.println("memberDto =" + memberDto);
        }
    }


    @Test // 필드에 바로 값을 대입하는 방법
    public void findDtoByField(){
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for(MemberDto memberDto : result){
            System.out.println("memberDto =" + memberDto);
        }
    }

    @Test // 앨리어싱을 통해서 필드명이 다를 때 값을 받는 방법 .
    public void findUserDto(){
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),// 필드명이 다를 때에는 as 를 사용하여 맞춰준다.
                        //서브쿼리를 사용하여
                        //최대나이의 인원만 구한다.
                        //ExpressionUtils를 사용 ->  age 필드로 감싸서 매칭된다.
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub),"age")
                )) // .as 를 사용하여 바로 앨리어싱 하여주는 것이 더 깔끔 :)
                //서브쿼리 같은 경우는 age 와 같은 방향으로 해결
                .from(member)
                .fetch();

        for(UserDto userDto : result){
            System.out.println("memberDto =" + userDto);
        }
    }

    @Test //생성자로 값을 리턴
    public void findDtoByConstructor(){
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for(UserDto userDto : result){
            System.out.println("userDto = "+ userDto);
        }
    }

    @Test
    public void findDtoByQueryProjection(){    // 프로젝션 생성자와 비슷한 방식이지만 ,컴파일 시점에서 타입이 안맞다면 오류가 난다
        List<MemberDto> result = queryFactory  //생성자를 그대로 가져간다.
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for(MemberDto memberDto : result){
            System.out.println("memberDto =" + memberDto);
        }

        // QueryDsl에 대한 의존성에 대해서 생각해보며
        // 프로젝션 생성자 방식과 쿼리 프로젝션 방식을 선택해서 잘 사용하자.

    }


    @Test
    public void dynamicQuery_BooleanBuilder(){
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam,ageParam);

        assert result != null;
        assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder(); // 생성자에 초기값을 넣을 수 있다.
        if(usernameCond != null){
            builder.and(member.username.eq(usernameCond));
        }

        if(ageCond != null){
            builder.and(member.age.eq(ageCond));
        }


        return queryFactory
                .selectFrom(member)
                .where(builder) // 결과값이 나온 Builder를 넣기만하면된다.
                .fetch();

    }


    // booleanBuilder 에 비하여 코드의 가독성이 좋다.
    @Test
    public void dynamicQuery_WhereParam(){
      
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam,ageParam);

        assert result != null;
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
               // .where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allEq(usernameCond,ageCond))
                .fetch();
    }

    private BooleanExpression  usernameEq(String usernameCond) {
        //where 절에서 null 값은 무시한다.
        return usernameCond == null ? null : member.username.eq(usernameCond);
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond == null ? null : member.age.eq(ageCond);
    }
    // 자바 코드이기에 합성을 할 수 있다. == 재활용성이 높다.
    private BooleanExpression allEq(String usernameCond, Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    //Query
    @Test
    public void bulkUpdate(){
        //member1 = 10  -> 비회원
        //member2 = 20  -> 비회원

        // 벌크 연산은 영속 컨텍스트에 변경없이 바로 쿼리를 날려버린다.
        // 즉 , DB 와 애플리케이션 내의 영속컨텍스트의 값이 서로 달라진다.
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        //벌크 연산에는 영속 컨텍스트에 쿼리를 날려버리고,
        em.flush();
        // 초기화 시켜주면된다.
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        //값이 일치하는 것을 확인
        for(Member member : result){
            System.out.println("member = "+ member);
        }

    }

    @Test
    public void bulkAdd(){
        long count =queryFactory
                .update(member)
                .set(member.age , member.age.add(1)) // 곱셈 multifly
                .execute();
    }

    @Test
    public void bulkDelete(){
        long count =queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }


}

